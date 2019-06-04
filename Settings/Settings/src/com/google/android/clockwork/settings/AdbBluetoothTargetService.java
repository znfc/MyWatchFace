/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.google.android.clockwork.settings;

import com.google.android.apps.wearable.adboverbluetooth.AdbOverBluetooth;
import com.google.android.apps.wearable.adboverbluetooth.BytePiper;
import com.google.android.apps.wearable.adboverbluetooth.StreamCloser;
import com.google.android.apps.wearable.settings.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Foreground service that pipes ADB bytes between a Bluetooth-connected hub device and local adbd.
 * It can be turned on and off from the Developer Settings screen. It is off by default.
 */
public class AdbBluetoothTargetService extends Service {

    private static final String TAG = "AdbBluetoothTargetSvc";

    /**
     * This UUID is used to avoid getting assigned the same channel that Wear is using. It will
     * only be visible for about a millisecond; it is closed after a second listener is created.
     */
    private static final UUID DUMMY_UUID = UUID.fromString("99999999-9525-11e3-a5e2-0800200c9a66");
    private static final String DUMMY_SERVICE_NAME = "Adb Bluetooth Target Dummy Service";

    private static final int ONGOING_NOTIFICATION_ID = 11003;

    private static final long BACKOFF_INITAL_TIMEOUT = 1000;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final long BACKOFF_MAX_TIMEOUT = 60000;

    private final AtomicReference<Socket> mTcpSocketRef = new AtomicReference<Socket>();

    private final AtomicReference<BluetoothServerSocket> mBtServerSocketRef =
            new AtomicReference<BluetoothServerSocket>();

    private final AtomicReference<BluetoothSocket> mBtSocketRef =
            new AtomicReference<BluetoothSocket>();

    private PipeControlThread mPipeControlThread;
    private boolean mRunning = false;
    private final StreamCloser mCloser = new StreamCloser();
    private Object mPipeControlThreadLock = new Object();

    private final Object mRunningLock = new Object();

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                synchronized (mPipeControlThreadLock) {
                    if (mPipeControlThread != null) {
                        mPipeControlThread.wakeThreads();
                    }
                }
            }
        };

    /**
     * Main service thread. It creates a socket for each endpoint, and then pipes bytes both ways.
     * If any errors occur while piping, it closes both sockets and starts over.
     * This goes on until the service is stopped.
     */
    private class PipeControlThread extends Thread {
        private BackoffLoopingRunnable mAcceptBluetoothSocketLooper;
        private BackoffLoopingRunnable mConnectTcpSocketLooper;

        private Thread mTcpToBtThread;
        private Thread mBtToTcpThread;

        public PipeControlThread() {
            super("PipeControlThread");
        }

        @Override
        public void run() {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "PipeControlThread.run() BEGIN");
            }
            while (isRunning()) {
                createSockets();
                if (!isRunning()) {
                    break;
                }
                pipeBytes();
            }
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "PipeControlThread.run() END");
            }
        }

        public void wakeThreads() {
            if (mAcceptBluetoothSocketLooper != null) {
                mAcceptBluetoothSocketLooper.reset();
            }
            if (mConnectTcpSocketLooper != null) {
                mConnectTcpSocketLooper.reset();
            }
        }

        /**
         * Gets sockets, one to the hub, over Bluetooth, and one to adbd, over TCP.
         * Returns when connections have been made or when no longer running.
         */
        private void createSockets() {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "createSockets BEGIN");
            }

            mAcceptBluetoothSocketLooper =
                    new BackoffLoopingRunnable(TAG, "AcceptThread",
                                               BACKOFF_INITAL_TIMEOUT,
                                               BACKOFF_MULTIPLIER,
                                               BACKOFF_MAX_TIMEOUT) {
                        @Override
                        public boolean shouldRun() {
                            return isRunning();
                        }

                        @Override
                        public int loop() {
                            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                                Log.v(TAG, "BT accept socket thread, top of loop");
                            }
                            try {
                                BluetoothSocket socket = acceptBtConnection();
                                if (socket == null) {
                                    return BackoffLoopingRunnable.CONTINUE;
                                }
                                mBtSocketRef.set(socket);
                                mCloser.add(socket.getInputStream());
                                mCloser.add(socket.getOutputStream());
                                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                                    Log.v(TAG, "got a BT socket - returning");
                                }
                                return BackoffLoopingRunnable.FINISH;
                            } catch (IOException e) {
                                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                                    Log.v(TAG, "error accepting BT socket", e);
                                }
                            }
                            return BackoffLoopingRunnable.CONTINUE;
                        }
                    };
            mConnectTcpSocketLooper =
                    new BackoffLoopingRunnable(TAG, "ConnectThread",
                                               BACKOFF_INITAL_TIMEOUT,
                                               BACKOFF_MULTIPLIER,
                                               BACKOFF_MAX_TIMEOUT) {
                        @Override
                        public boolean shouldRun() {
                            return isRunning();
                        }

                        @Override
                        public int loop() {
                            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                                Log.v(TAG, "TCP create socket thread, top of loop");
                            }
                            try {
                                Socket socket = createTcpConnection();
                                if (socket == null) {
                                    return BackoffLoopingRunnable.CONTINUE;
                                }
                                mTcpSocketRef.set(socket);
                                mCloser.add(socket.getInputStream());
                                mCloser.add(socket.getOutputStream());
                                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                                    Log.v(TAG, "got a TCP socket - returning");
                                }
                                return BackoffLoopingRunnable.FINISH;
                            } catch (IOException e) {
                                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                                    Log.v(TAG, "error connecting TCP socket", e);
                                }
                            }
                            return BackoffLoopingRunnable.CONTINUE;
                        }
                    };


            Thread acceptBluetoothSocketThread = new Thread(mAcceptBluetoothSocketLooper,
                                                            mAcceptBluetoothSocketLooper.getName());
            Thread connectTcpSocketThread = new Thread(mConnectTcpSocketLooper,
                                                       mConnectTcpSocketLooper.getName());

            acceptBluetoothSocketThread.start();
            connectTcpSocketThread.start();

            try {
                acceptBluetoothSocketThread.join();
            } catch (InterruptedException e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Unexpected interruption", e);
                }
            }
            mConnectTcpSocketLooper.reset();
            try {
                connectTcpSocketThread.join();
            } catch (InterruptedException e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Unexpected interruption", e);
                }
            }
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "createSockets END");
            }
        }

        private Socket createTcpConnection() {
            Socket socket = null;
            try {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "before new TCP Socket");
                }
                socket = new Socket("localhost", AdbOverBluetooth.ADBD_TCP_PORT);
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "after new TCP Socket");
                }
            } catch (IOException e) {
                Log.w(TAG, "error during createTcpConnection", e);
            }
            broadcastChange();
            return socket;
        }

        private BluetoothSocket acceptBtConnection() throws IOException {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "acceptBtConnection()");
            }
            BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();

            // Bluedroid gives out the lowest channel number that has no listeners,
            // which is probably the one WearableBt is using. So grab that, but don't use it.
            BluetoothServerSocket dummyServerSocket = createServerSocket(
                    bt, DUMMY_SERVICE_NAME, DUMMY_UUID);
            if (dummyServerSocket == null) {
                return null;
            }
            BluetoothServerSocket serverSocket = createServerSocket(
                    bt, AdbOverBluetooth.BLUETOOTH_SERVICE_NAME, AdbOverBluetooth.BLUETOOTH_UUID);
            try {
                dummyServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "error closing dummy server socket", e);
            }
            if (serverSocket == null) {
                return null;
            }
            BluetoothSocket socket = null;
            try {
                mBtServerSocketRef.set(serverSocket);
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "wait for BT connection...");
                }
                socket = serverSocket.accept();
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "got BT connection");
                }
            } finally {
                serverSocket.close();
            }
            broadcastChange();
            return socket;
        }

        /**
         * @return a newly created socket server, or null if Bluedroid tries throwing a runtime
         * exception.
         */
        private BluetoothServerSocket createServerSocket(BluetoothAdapter bt, String serviceName,
                UUID uuid) throws IOException {
            try {
                return bt.listenUsingRfcommWithServiceRecord(serviceName, uuid);
            } catch (RuntimeException e) {
                Log.w(TAG, "Error creating BluetoothServerSocket", e);
                return null;
            }
        }

        /**
         * Copies bytes both ways between both sockets, on two threads, until there is an error,
         * or we stop running.
         */
        private void pipeBytes() {
            mTcpToBtThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    Socket tcpSocket = mTcpSocketRef.get();
                    BluetoothSocket btSocket = mBtSocketRef.get();
                    if (tcpSocket == null || btSocket == null) {
                        return;
                    }
                    try {
                        InputStream input = tcpSocket.getInputStream();
                        OutputStream output = btSocket.getOutputStream();
                        BytePiper piper = new BytePiper(TAG, new byte[1024], input, output);
                        while (isRunning()) {
                            piper.pipeBytes();
                        }
                    } catch (IOException e) {
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "error piping TCP to BT", e);
                        }
                    } finally {
                        closeEverything();
                    }
                }
            }, TAG + "-TcpToBtThread");
            mBtToTcpThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    Socket tcpSocket = mTcpSocketRef.get();
                    BluetoothSocket btSocket = mBtSocketRef.get();
                    if (tcpSocket == null || btSocket == null) {
                        return;
                    }
                    try {
                        InputStream input = btSocket.getInputStream();
                        OutputStream output = tcpSocket.getOutputStream();
                        BytePiper piper = new BytePiper(TAG, new byte[1024], input, output);
                        while (isRunning()) {
                            piper.pipeBytes();
                        }
                    } catch (IOException e) {
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "error piping BT to TCP", e);
                        }
                    } finally {
                        closeEverything();
                    }
                }
            }, TAG + "-BtToTcpThread");

            // start
            mTcpToBtThread.start();
            mBtToTcpThread.start();

            // join
            try {
                mTcpToBtThread.join();
            } catch (InterruptedException e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Unexpected interruption", e);
                }
            }
            try {
                mBtToTcpThread.join();
            } catch (InterruptedException e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Unexpected interruption", e);
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "onStartCommand");
        }
        setRunning(true);
        synchronized (mPipeControlThreadLock) {
            if (mPipeControlThread == null) {
                mPipeControlThread = new PipeControlThread();
                mPipeControlThread.start();
            }
        }
        Notification notification = createNotification();
        startForeground(ONGOING_NOTIFICATION_ID, notification);
        broadcastChange();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        registerReceiver(mBroadcastReceiver, filter);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "onDestroy BEGIN");
        }

        unregisterReceiver(mBroadcastReceiver);

        setRunning(false);
        closeEverything();

        broadcastChange();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "onDestroy END");
        }
        super.onDestroy();
    }

    private void closeEverything() {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "closeEverything BEGIN");
        }
        mCloser.closeAll();
        Socket tcpSocket = mTcpSocketRef.get();
        if (tcpSocket != null) {
            try {
                tcpSocket.close();
            } catch (IOException e) {
                Log.w(TAG, "error closing TCP socket", e);
            }
        }
        BluetoothServerSocket btServerSocket = mBtServerSocketRef.get();
        if (btServerSocket != null) {
            try {
                btServerSocket.close();
            } catch (IOException e) {
                Log.w(TAG, "error closing BT server socket", e);
            }
        }
        BluetoothSocket btSocket = mBtSocketRef.get();
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.w(TAG, "error closing BT socket", e);
            }
        }
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "closeEverything END");
        }
    }

    private void broadcastChange() {
        if (isRunning()) {
            updateNotification();
        } else {
            removeNotification();
        }
    }

    private void updateNotification() {
        NotificationManager notificationManager =
                (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(ONGOING_NOTIFICATION_ID, createNotification());

    }

    private void removeNotification() {
        NotificationManager notificationManager =
                (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(ONGOING_NOTIFICATION_ID);

    }

    private Notification createNotification() {
        PendingIntent i = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainSettingsActivity.class)
                        .setAction(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
                PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_sys_nt_bugs)
                .setContentTitle(getString(R.string.notification_adb_target_service_title))
                .setContentText(getString(R.string.notification_adb_target_service_text))
                .setContentIntent(i)
                .setPriority(Notification.PRIORITY_MIN)
                .build();
    }

    private void setRunning(boolean b) {
        synchronized (mRunningLock) {
            mRunning = b;
        }
    }

    private boolean isRunning() {
        synchronized (mRunningLock) {
            return mRunning;
        }
    }
}

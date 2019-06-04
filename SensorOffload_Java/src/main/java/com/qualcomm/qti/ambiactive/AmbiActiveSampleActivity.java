/**
 * Copyright (c) 2018 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 * Apache license notifications and license are retained
 * for attribution purposes only.
 */

/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License.
 */
package com.qualcomm.qti.ambiactive;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.view.Display;
import android.widget.Button;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.ProgressBar;
import com.android.server.LocalServices;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.Manifest;
import android.content.pm.PackageManager;
import com.google.android.clockwork.decomposablewatchface.ImageComponent;
import com.google.android.clockwork.decomposablewatchface.FontComponent;
import com.google.android.clockwork.decomposablewatchface.ProportionalFontComponent;
import com.google.android.clockwork.decomposablewatchface.NumberComponent;
import com.google.android.clockwork.decomposablewatchface.StringComponent;
import com.google.android.clockwork.decomposablewatchface.WatchFaceDecomposition;
import com.google.android.clockwork.decomposablewatchface.GlyphDescriptor;
import com.qualcomm.qti.ambiactive.offload.SidekickManager;
import com.qualcomm.qti.sidekickmetrics.SidekickMetricsManager;
import com.qualcomm.qti.sidekickmetrics.MetricSensorEventListener;
import com.qualcomm.qti.sidekickmetrics.MetricSensorStatusListener;
import com.qualcomm.qti.sidekickmetrics.MetricSensorInfo;
import com.qualcomm.qti.sidekickmetrics.MetricSensor;
import com.qualcomm.qti.sidekickmetrics.MetricSensorEvent;
import com.qualcomm.qti.sidekickmetrics.MetricSensorEventType;
import com.qualcomm.qti.sidekickmetrics.MetricBatchGroup;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Icon;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * This activity displays a stopwatch.
 */
public class AmbiActiveSampleActivity extends WearableActivity{

    private static final String TAG = "AmbiActiveSampleActivity";

    // Milliseconds between waking processor/screen for updates when active
    private static final long ACTIVE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1);
    // Milliseconds between waking processor/screen for updates when in ambient mode
    private static final long AMBIENT_INTERVAL_MS = TimeUnit.SECONDS.toMillis(10);
    // 60 seconds for updating the clock in active mode
    private static final long MINUTE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(60);
	// 1 Sec Batch time out in active mode
    private static final long ACTIVE_BATCH_PERIOD = 1000000000;
    // 5 Minute Batch time in ambient mode
    private static final long AMBIACTIVE_BATCH_PERIOD = 5*60*1000000000;

    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat mDebugTimeFormat = new SimpleDateFormat("HH:mm:ss");

    // Screen components
    private TextView mTimeViewHH;
    private TextView mTimeViewMM;
    private TextView mTimeViewSS;
    private TextView textviewBPM;
    private TextView textviewCAL;
    private TextView textviewMI;
    private TextView mStepView;
    private View mBackground;
    private TextClock mClockView;

    // The last time that the stop watch was updated or the start time.
    private long mLastTick = 0L;
    // Store time that was measured so far.
    private long mTimeSoFar = 0L;
    // Keep track to see if the stop watch is running.
    private boolean mRunning = false;
    // Handle
    private final Handler mActiveModeUpdateHandler = new UpdateStopwatchHandler(this);
    // Handler for updating the clock in active mode
    private final Handler mActiveClockUpdateHandler = new UpdateClockHandler(this);
    private static final String TEXT_NUM_STEPS = "Steps: ";
    private int numSteps;
    private int heartRate;
    private float calories;
    private float distance;
    private SidekickManager sidekickManager;
    private SidekickMetricsManager mSidekickMetricsManager;
    private long epochTime = 0;
    private int formatterSensorHandle, distanceSensorHandle, caloriesSensorHandle, heartRateHandle; //metricSensor handle
    //Metric Sensors HRM, Distance, Calorimeter
    private MetricSensor mMetricSensorDistance;
    private MetricSensor mMetricSensorCalories;
    private MetricSensor mMetricSensorHeartRate;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stopwatch);
        // Enable Ambient Mode
        setAmbientEnabled();
        // Enable offload feature to current activity
        setAmbientOffloadEnabled(true);
        // Get on screen items
        mTimeViewHH = (TextView) findViewById(R.id.timeviewHH);
        mTimeViewMM = (TextView) findViewById(R.id.timeviewMM);
        mTimeViewSS = (TextView) findViewById(R.id.timeviewSS);
        textviewBPM = (TextView) findViewById(R.id.textviewBPM);
        textviewCAL = (TextView) findViewById(R.id.textviewCAL);
        textviewMI = (TextView) findViewById(R.id.textviewMI);
        resetTimeView(); // initialise TimeView
        mStepView = (TextView) findViewById(R.id.stepview);
        resetStepView(); // initialise TimeView

        mBackground = findViewById(R.id.gridbackground);
        mClockView = (TextClock) findViewById(R.id.clock);
        sidekickManager = new SidekickManager();//creating new SidekickManager instance 
        mSidekickMetricsManager = SidekickMetricsManager.getInstance(this);//creating SidekickMetricsManager instance
        List<MetricSensorInfo> MetricSensors = mSidekickMetricsManager.getSupportedMetricSensors();//Gets all SSO HAL supported sensors
        for (MetricSensorInfo sensor : MetricSensors) {
            Log.d(TAG, "Sensor Name: " + sensor.name);
            Log.d(TAG, "Sensor sensorHandle: " + sensor.sensorHandle);
            Log.d(TAG, "Sensor type: " + sensor.type);
            Log.d(TAG, "Sensor vendor: " + sensor.vendor);
            Log.d(TAG, "Sensor version: " + sensor.version);
            switch (sensor.name) {
            case "formatter":
                formatterSensorHandle = sensor.sensorHandle;
                break;
            case "distance":
                distanceSensorHandle = sensor.sensorHandle;
                break;
            case "calories":
                caloriesSensorHandle = sensor.sensorHandle;
                break;
            case "sns_heart_rate":
                heartRateHandle = sensor.sensorHandle;
                break;
            }
        }
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS zzz");
        String currentTime = dateFormat.format(today);
        Log.d(TAG, "Current Time = " + currentTime);
        try {

            Date date = dateFormat.parse(currentTime);
            Log.d(TAG, "Epoch Time: " + date.getTime());
            epochTime = date.getTime() - (date.getTimezoneOffset() * 60 * 1000);
            Log.d(TAG, "Epoch Time: " + epochTime);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        /*Creating Metric Sensor objects
        * Each Sensor object has the following fields
        * int metric Id, int formatter handle, int sensor handle, string format, int refreshRate, boolean offloadable, boolean record data
		* format has to be supported by mdsp, refresh rate is the rate at which sensor data will be offloaded
        */
        if(formatterSensorHandle != 0 && heartRateHandle !=0)
            mMetricSensorHeartRate = new MetricSensor(1002, formatterSensorHandle,heartRateHandle,
                                            "{bpm:3}",1000, MetricBatchGroup.GROUP_0, true, true);
        if(formatterSensorHandle != 0 && caloriesSensorHandle !=0)
            mMetricSensorCalories = new MetricSensor(1003, formatterSensorHandle,caloriesSensorHandle,
                                            "{cal:3}",1000, MetricBatchGroup.GROUP_0, true, true);
        if(formatterSensorHandle != 0 && distanceSensorHandle !=0)
            mMetricSensorDistance = new MetricSensor(1004, formatterSensorHandle,distanceSensorHandle,
                                            "{steps:3}",1000, MetricBatchGroup.GROUP_0, true, true);
        if(mMetricSensorHeartRate != null)
            mSidekickMetricsManager.createMetricInstance(mMetricSensorHeartRate);
        if(mMetricSensorDistance != null)
            mSidekickMetricsManager.createMetricInstance(mMetricSensorDistance);
        if(mMetricSensorCalories != null)
            mSidekickMetricsManager.createMetricInstance(mMetricSensorCalories);
        mLastTick = 0L;
        mTimeSoFar = 0L;
        resetTimeView();
        resetStepView();
        toggleStartStop();


        mActiveClockUpdateHandler.sendEmptyMessage(R.id.msg_update);
    }
    //helper function to update display with BPM, Calories, Distance covered.
    private void updateDisplayAndSetRefresh() {
        if (!mRunning) {
            return;
        }
        incrementTimeSoFar();

        setTimeView(mTimeSoFar);

        setStepView(numSteps);
        textviewBPM.setText(String.format("%d", (int)heartRate));
        textviewCAL.setText(String.format("%3.1f", calories));
        textviewMI.setText(String.format("%3.1f", distance));
        //if (!isAmbient()) {
            // In Active mode update directly via handler.
            long timeMs = System.currentTimeMillis();
            long delayMs = ACTIVE_INTERVAL_MS - (timeMs % ACTIVE_INTERVAL_MS);
            Log.d(TAG, "NOT ambient - delaying by: " + delayMs);
            mActiveModeUpdateHandler
                    .sendEmptyMessageDelayed(R.id.msg_update, delayMs);
    }
    //updates the time on log
    private void incrementTimeSoFar() {
        // Update display time
        final long now = System.currentTimeMillis();
        Log.d(TAG, String.format("current time: %d. start: %d", now, mLastTick));
        mTimeSoFar = mTimeSoFar + now - mLastTick;
        mLastTick = now;
    }

    /**
     * Set the time view to its initial state.
     */
    private void resetTimeView() {
        setTimeView(mTimeSoFar);
    }

    /**
     * Set the time view to its initial state.
     */
    private void resetStepView() {
        setStepView(0);
    }
    /**
     * Set time view to a specified time.
     *
     * @param minutes The minutes to display.
     * @param seconds The seconds to display.
     */
    private void setStepView(int steps) {

        mStepView.setText(TEXT_NUM_STEPS+ steps);

    }
    /**
     * Set time view to a specified time.
     *
     * @param minutes The minutes to display.
     * @param seconds The seconds to display.
     */
    private void setTimeView(long diff) {
        long diffSeconds = diff / 1000 % 60;
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours = diff / (60 * 60 * 1000);
        mTimeViewSS.setText(String.format("%02d", (int)diffSeconds));
        mTimeViewMM.setText(String.format("%02d", (int)diffMinutes));
        mTimeViewHH.setText(String.format("%02d", (int)diffHours));
    }
    private void toggleStartStop() {
        Log.d(TAG, "mRunning: " + mRunning);
        if (mRunning) {
            // This can only happen in interactive mode - so we only need to stop the handler
            // AlarmManager should be clear
            mActiveModeUpdateHandler.removeMessages(R.id.msg_update);
            incrementTimeSoFar();
            // Currently running - turn it to stop
            mRunning = false;
        } else {
            mLastTick = System.currentTimeMillis();
            mRunning = true;
            updateDisplayAndSetRefresh();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //To Clear old offloadable asset which we send to BG
        sidekickManager.clearWatchFace();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //To send offloadable asset to BG
                sidekickManager.sendWatchFace(buildOffloadableDecomposition(AmbiActiveSampleActivity.this),false);
            }
        }, 1000);
        //To Start/registerListener the Sensor offload session
        if(mMetricSensorHeartRate != null) {
        mSidekickMetricsManager.registerListener(heartRateEventListner, mMetricSensorHeartRate); //registerListener takes sensor event listener and metric sensor
        mSidekickMetricsManager.registerMetricSensorStatusListener(heartRateSensorStatusListner, mMetricSensorHeartRate); //to get the status of listener whether paused or running
        mSidekickMetricsManager.start(mMetricSensorHeartRate, null); // starts the instance
        }
        if(mMetricSensorDistance != null) {
        mSidekickMetricsManager.registerListener(distanceEventListener, mMetricSensorDistance);
        mSidekickMetricsManager.registerMetricSensorStatusListener(distanceSensorStatusListener, mMetricSensorDistance);
        mSidekickMetricsManager.start(mMetricSensorDistance, null);
        }
        if(mMetricSensorCalories != null) {
        mSidekickMetricsManager.registerListener(caloriesEventListener, mMetricSensorCalories);
        mSidekickMetricsManager.registerMetricSensorStatusListener(caloriesSensorStatusListener, mMetricSensorCalories);
        mSidekickMetricsManager.start(mMetricSensorCalories, null);
        }

    }
        //for each sensor we have sensorEventListener and sensorStatusListener
        //HRM sensor event listener
        MetricSensorEventListener heartRateEventListner = new MetricSensorEventListener() {

        @Override
        public void onMetricSensorChanged(int  metricId, List<MetricSensorEvent> event) {
            Log.d(TAG, "onMetricSensorChanged " + metricId + " event " + event.size());
            if (mMetricSensorHeartRate.getMetricID() == metricId) {
                float value = convertByteArrayToFloat(event.get(event.size()-1).eventBuf, 4);
                Log.d(TAG, "onMetricSensorChanged " + metricId + " mMetricSensorHeartRate " + value);
                heartRate = (int)value;
            }
        }
        };
        //HRM sensor status listener
        MetricSensorStatusListener heartRateSensorStatusListner = new MetricSensorStatusListener() {

        @Override
        public void onMetricSensorStatusChanged(int  metricId, int status) {
            Log.d(TAG, "onMetricSensorStatusChanged " + metricId + " status " + status);

        }
        };
        //distance sensor event listener
        MetricSensorEventListener distanceEventListner = new MetricSensorEventListener() {

        @Override
        public void onMetricSensorChanged(int  metricId, List<MetricSensorEvent> event) {
            Log.d(TAG, "onMetricSensorChanged " + metricId + " event " + event.size());
            if (mMetricSensorDistance.getMetricID() == metricId) {
                float value = convertByteArrayToFloat(event.get(event.size()-1).eventBuf, 4);
                Log.d(TAG, "onMetricSensorChanged " + metricId + " mMetricSensorDistance " + value);
                distance = (float)value;
            }
        }
        };
        //distanc sensor status listener
        MetricSensorStatusListener distanceSensorStatusListner = new MetricSensorStatusListener() {

        @Override
        public void onMetricSensorStatusChanged(int  metricId, int status) {
            Log.d(TAG, "onMetricSensorStatusChanged " + metricId + " status " + status);

        }
        };
        //calorimeter sensor event listener
        MetricSensorEventListener caloriesEventListner = new MetricSensorEventListener() {

        @Override
        public void onMetricSensorChanged(int  metricId, List<MetricSensorEvent> event) {
            Log.d(TAG, "onMetricSensorChanged " + metricId + " event " + event.size());
            if (mMetricSensorDistance.getMetricID() == metricId) {
                float value = convertByteArrayToFloat(event.get(event.size()-1).eventBuf, 4);
                Log.d(TAG, "onMetricSensorChanged " + metricId + " mMetricSensorCalories " + value);
                calories = (float)value;
            }
        }
        };
        //calorimeter sensor listener status
        MetricSensorStatusListener caloriesSensorStatusListner = new MetricSensorStatusListener() {

        @Override
        public void onMetricSensorStatusChanged(int  metricId, int status) {
            Log.d(TAG, "onMetricSensorStatusChanged " + metricId + " status " + status);

        }
        };
    @Override
    protected void onPause() {
        super.onPause();
        //To Pause/unRegisterListener the sensor offload session
        if(mMetricSensorHeartRate != null) {
            mSidekickMetricsManager.unRegisterListener(mMetricSensorHeartRate);
            mSidekickMetricsManager.unRegisterMetricsStatusListener(mMetricSensorHeartRate);
            mSidekickMetricsManager.stop(mMetricSensorHeartRate);
        }
        if(mMetricSensorDistance != null) {
            mSidekickMetricsManager.unRegisterListener(mMetricSensorDistance);
            mSidekickMetricsManager.unRegisterMetricsStatusListener(mMetricSensorDistance);
            mSidekickMetricsManager.stop(mMetricSensorDistance);
        }
        if(mMetricSensorCalories != null) {
            mSidekickMetricsManager.unRegisterListener(mMetricSensorCalories);
            mSidekickMetricsManager.unRegisterMetricsStatusListener(mMetricSensorCalories);
            mSidekickMetricsManager.stop(mMetricSensorCalories);
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        Log.d(TAG, "ENTER Ambient");
        mSidekickMetricsManager.batch(MetricBatchGroup.GROUP_0, AMBIACTIVE_BATCH_PERIOD);
        super.onEnterAmbient(ambientDetails);

    }

    @Override
    public void onExitAmbient() {
        Log.d(TAG, "EXIT Ambient");
        super.onExitAmbient();
        mSidekickMetricsManager.batch(MetricBatchGroup.GROUP_0, ACTIVE_BATCH_PERIOD);
        mActiveClockUpdateHandler.sendEmptyMessage(R.id.msg_update);

        if (mRunning) {
            updateDisplayAndSetRefresh();
        }
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplayAndSetRefresh();
    }

    @Override
    protected void onStop() {
        //To Clear offloadable asset which we send to BG
        sidekickManager.clearWatchFace();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        mActiveModeUpdateHandler.removeMessages(R.id.msg_update);
        mActiveClockUpdateHandler.removeMessages(R.id.msg_update);
        //To Remove the sensor offload session
        if(mMetricSensorHeartRate != null)
            mSidekickMetricsManager.destroyMetricInstance(mMetricSensorHeartRate);
        if(mMetricSensorHeartRate != null)
            mSidekickMetricsManager.destroyMetricInstance(mMetricSensorDistance);
        if(mMetricSensorHeartRate != null)
            mSidekickMetricsManager.destroyMetricInstance(mMetricSensorCalories);
        super.onDestroy();
    }

    public float convertByteArrayToFloat(ArrayList<Byte> values, int length) {
        byte[] result = new byte[length];
        for(int i = 0; i < length; i++) {
            result[i] = values.get(i).byteValue();
            Log.d(TAG, "onMetricSensorChanged byteValue " + result[i]);
        }
        return ByteBuffer.wrap(result, 0, length).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    /**
     * Simplify update handling for different types of updates.
     */
    private static abstract class UpdateHandler extends Handler {

        private final WeakReference<AmbiActiveSampleActivity> mAmbiActiveSampleActivityWeakReference;

        public UpdateHandler(AmbiActiveSampleActivity reference) {
            mAmbiActiveSampleActivityWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message message) {
            AmbiActiveSampleActivity ambiActiveSampleActivity = mAmbiActiveSampleActivityWeakReference.get();

            if (ambiActiveSampleActivity == null) {
                return;
            }
            switch (message.what) {
                case R.id.msg_update:
                    handleUpdate(ambiActiveSampleActivity);
                    break;
            }
        }

        /**
         * Handle the update within this method.
         *
         * @param AmbiActiveSampleActivity The activity that handles the update.
         */
        public abstract void handleUpdate(AmbiActiveSampleActivity ambiActiveSampleActivity);
    }

    /**
     * Handle clock updates every minute.
     */
    private static class UpdateClockHandler extends UpdateHandler {

        public UpdateClockHandler(AmbiActiveSampleActivity reference) {
            super(reference);
        }

        @Override
        public void handleUpdate(AmbiActiveSampleActivity ambiActiveSampleActivity) {
            long timeMs = System.currentTimeMillis();
            long delayMs = MINUTE_INTERVAL_MS - (timeMs % MINUTE_INTERVAL_MS);
            Log.d(TAG, "NOT ambient - delaying by: " + delayMs);
            ambiActiveSampleActivity.mActiveClockUpdateHandler
                    .sendEmptyMessageDelayed(R.id.msg_update, delayMs);
        }
    }
    /**
     * Handle stopwatch changes in active mode.
     */
    private static class UpdateStopwatchHandler extends UpdateHandler {

        public UpdateStopwatchHandler(AmbiActiveSampleActivity reference) {
            super(reference);
        }

        @Override
        public void handleUpdate(AmbiActiveSampleActivity ambiActiveSampleActivity) {
            ambiActiveSampleActivity.updateDisplayAndSetRefresh();
        }
    }
    public WatchFaceDecomposition buildOffloadableDecomposition(Context mContext) {
        final float W = 390.0f;
        final float H = 390.0f;
        //Image component to display backgrond
        ImageComponent backgroundImageComponent = new ImageComponent.Builder()
                    .setComponentId(1)
                    .setZOrder(1)
                    .setImage(Icon.createWithResource(mContext, R.drawable.bg))
                    .setBounds(new RectF(0F, 0F, 1F, 1F))
                    .build();
        //Font component for Small Number Component
        FontComponent smallNumberFontComponent = new FontComponent.Builder()
                    .setComponentId(11111)
                    .setImage(Icon.createWithResource(mContext, R.drawable.q_s))
                    .setDigitCount(10)
                    .build();
        //Glyph Descriptor for to display 0 to 9 with .
        ArrayList<GlyphDescriptor> glyphDescriptorList = new ArrayList<GlyphDescriptor>();
        glyphDescriptorList.add(new GlyphDescriptor((short)27, (byte)48));
        glyphDescriptorList.add(new GlyphDescriptor((short)27, (byte)49));
        glyphDescriptorList.add(new GlyphDescriptor((short)27, (byte)50));
        glyphDescriptorList.add(new GlyphDescriptor((short)27, (byte)51));
        glyphDescriptorList.add(new GlyphDescriptor((short)27, (byte)52));
        glyphDescriptorList.add(new GlyphDescriptor((short)27, (byte)53));
        glyphDescriptorList.add(new GlyphDescriptor((short)27, (byte)54));
        glyphDescriptorList.add(new GlyphDescriptor((short)27, (byte)55));
        glyphDescriptorList.add(new GlyphDescriptor((short)27, (byte)56));
        glyphDescriptorList.add(new GlyphDescriptor((short)27, (byte)57));
        glyphDescriptorList.add(new GlyphDescriptor((short)27, (byte)46));
        //Propotional Font component for String Component
        ProportionalFontComponent mediumNumberFontComponent = new ProportionalFontComponent.Builder()
                    .setComponentId(11112)
                    .setImage(Icon.createWithResource(mContext, R.drawable.q_m))
                    .setGlyphDescriptors(glyphDescriptorList)
                    .build();
        //Font component for Large Number Component
        FontComponent largeNumberFontComponent = new FontComponent.Builder()
                    .setComponentId(11113)
                    .setImage(Icon.createWithResource(mContext, R.drawable.q_l))
                    .setDigitCount(10)
                    .build();
        //Font component for AM PM Number Component
        FontComponent amPmFontComponent = new FontComponent.Builder()
                    .setComponentId(11114)
                    .setImage(Icon.createWithResource(mContext, R.drawable.q_am_pm))
                    .setDigitCount(2)
                    .build();
        //Number Component to display HH
        NumberComponent hourNumberComponent = new NumberComponent.Builder()
                    .setComponentId(1120)
                    .setMsPerIncrement(TimeUnit.HOURS.toMillis(1L))
                    .setLowestValue(0L)
                    .setHighestValue(11L)
                    .setTimeOffsetMs(0)
                    .setMinDigitsShown(2)
                    .setFontComponentId(smallNumberFontComponent.getComponentId())
                    .setZOrder(2)
                    .setPosition(new PointF(133 / W, 40 / H))
                    .build();
        //Number Component to display MM
        NumberComponent minNumberComponent = new NumberComponent.Builder()
                    .setComponentId(1121)
                    .setMsPerIncrement(TimeUnit.MINUTES.toMillis(1L))
                    .setLowestValue(0L)
                    .setHighestValue(59L)
                    .setTimeOffsetMs(0)
                    .setMinDigitsShown(2)
                    .setFontComponentId(smallNumberFontComponent.getComponentId())
                    .setZOrder(2)
                    .setPosition(new PointF(177 / W, 40 / H))
                    .build();
        //Number Component to display AM PM
        NumberComponent amPmNumberComponent = new NumberComponent.Builder()
                    .setComponentId(1123)
                    .setMsPerIncrement(TimeUnit.HOURS.toMillis(12L))
                    .setLowestValue(0L)
                    .setHighestValue(1L)
                    .setTimeOffsetMs(0)
                    .setMinDigitsShown(1)
                    .setFontComponentId(amPmFontComponent.getComponentId())
                    .setZOrder(2)
                    .setPosition(new PointF(221 / W, 40 / H))
                    .build();
        //Number Component to display Stop watch HH
        NumberComponent hourTimerComponent = new NumberComponent.Builder()
                    .setComponentId(2011)
                    .setMsPerIncrement(TimeUnit.HOURS.toMillis(1L))
                    .setLowestValue(0L)
                    .setHighestValue(11L)
                    .setTimeOffsetMs(-epochTime)
                    .setMinDigitsShown(2)
                    .setFontComponentId(largeNumberFontComponent.getComponentId())
                    .setZOrder(2)
                    .setPosition(new PointF(75 / W, 83 / H))
                    .build();
        //Number Component to display Stop watch MM
        NumberComponent minTimerComponent = new NumberComponent.Builder()
                    .setComponentId(2112)
                    .setMsPerIncrement(TimeUnit.MINUTES.toMillis(1L))
                    .setLowestValue(0L)
                    .setHighestValue(59L)
                    .setTimeOffsetMs(-epochTime)
                    .setMinDigitsShown(2)
                    .setFontComponentId(largeNumberFontComponent.getComponentId())
                    .setZOrder(2)
                    .setPosition(new PointF(160 / W, 83 / H))
                    .build();
        //Number Component to display Stop watch SS
        NumberComponent secTimerComponent = new NumberComponent.Builder()
                    .setComponentId(2213)
                    .setMsPerIncrement(TimeUnit.SECONDS.toMillis(1L))
                    .setLowestValue(0L)
                    .setHighestValue(59L)
                    .setTimeOffsetMs(-epochTime)
                    .setMinDigitsShown(2)
                    .setFontComponentId(largeNumberFontComponent.getComponentId())
                    .setZOrder(2)
                    .setPosition(new PointF(248 / W, 83 / H))
                    .build();
        //String Component to Sensor Data BPM
        StringComponent bpmComponent = new StringComponent.Builder()
                    .setComponentId(2014)
                    .setAlignment(StringComponent.Alignment.RIGHT)
                    .setStringSourceId(1002) // MetricID of BPM
                    .setFontComponentId(mediumNumberFontComponent.getComponentId())
                    .setZOrder(2)
                    .setPosition(new PointF(22 / W, 206 / H))
                    .build();
        //String Component to Sensor Data CAL
        StringComponent calTimerComponent = new StringComponent.Builder()
                    .setComponentId(2115)
                    .setAlignment(StringComponent.Alignment.RIGHT)
                    .setStringSourceId(1003) // MetricID of CAL
                    .setFontComponentId(mediumNumberFontComponent.getComponentId())
                    .setZOrder(2)
                    .setPosition(new PointF(138 / W, 206 / H))
                    .build();
        //String Component to Sensor Data MILE
        StringComponent miTimerComponent = new StringComponent.Builder()
                    .setComponentId(2216)
                    .setAlignment(StringComponent.Alignment.RIGHT)
                    .setStringSourceId(1004) // MetricID of MILE
                    .setFontComponentId(mediumNumberFontComponent.getComponentId())
                    .setZOrder(2)
                    .setPosition(new PointF(266 / W, 206 / H))
                    .build();
        return new WatchFaceDecomposition.Builder()
                    .addImageComponents(new ImageComponent[] { backgroundImageComponent})
                    .addNumberComponents(new NumberComponent[] {hourNumberComponent, minNumberComponent,amPmNumberComponent,hourTimerComponent,minTimerComponent,secTimerComponent})
                    .addFontComponents(new FontComponent[] { smallNumberFontComponent,amPmFontComponent,largeNumberFontComponent})
                    .addStringComponents(new StringComponent[] { bpmComponent,calTimerComponent,miTimerComponent})
                    .addProportionalFontComponents(new ProportionalFontComponent[] { mediumNumberFontComponent})
                    .build();
    }

}

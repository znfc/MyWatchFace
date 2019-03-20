/**
 * Copyright (c) 2017 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 * Apache license notifications and license are retained
 * for attribution purposes only.
 */

/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.qualcomm.qti.sidekickwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.widget.Toast;
import android.os.AsyncTask;
import java.nio.ByteBuffer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.io.FileOutputStream;
import java.io.InputStream;


import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.os.Handler;
import java.lang.Runnable;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 */
public class AnalogWatchFace extends CanvasWatchFaceService {

    private static final String TAG = "AnalogWatchFaceBg";
    /*
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1)/10;

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<AnalogWatchFace.Engine> mWeakReference;

        public EngineHandler(AnalogWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            AnalogWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private static final float HOUR_STROKE_WIDTH = 5f;
        private static final float MINUTE_STROKE_WIDTH = 3f;
        private static final float SECOND_TICK_STROKE_WIDTH = 2f;

        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;

        private static final int SHADOW_RADIUS = 6;
        private final Rect mPeekCardBounds = new Rect();
        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode;
        private float mCenterX;
        private float mCenterY;
        private float mSecondHandLength;
        private float sMinuteHandLength;
        private float sHourHandLength;
        private Bitmap mHourHandBitmap;
        private Bitmap mHourHandAmbBitmap;
        private float mHourHandHeight;
        private float mHourHandWidth;
        private Bitmap mMinHandBitmap;
        private Bitmap mMinHandAmbBitmap;
        private float mMinHandHeight;
        private float mMinHandWidth;
        private Bitmap mSecHandBitmap;
        private Bitmap mSecHandAmbBitmap;
        private float mSecHandHeight;
        private float mSecHandWidth;
        private Paint mTickAndCirclePaint;
        private Paint mBackgroundPaint;
        private Bitmap mBackgroundBitmap;
        private Bitmap mGrayBackgroundBitmap;
        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(AnalogWatchFace.this)
                        .setAcceptsTapEvents(true)
                        .build());

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);
            mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg_interactive);
            mGrayBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg_interactive);
            mHourHandBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hour_interactive);
            mHourHandAmbBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hour_interactive);
            mHourHandHeight = mHourHandBitmap.getHeight();
            mHourHandWidth = mHourHandBitmap.getWidth();

            mMinHandBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.min_interactive);
            mMinHandAmbBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.min_interactive);
            mMinHandHeight = mMinHandBitmap.getHeight();
            mMinHandWidth  = (mMinHandBitmap.getWidth());

            mSecHandBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sec_interactive);
            mSecHandAmbBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sec_interactive);

            mSecHandHeight = mSecHandBitmap.getHeight();
            mSecHandWidth = mSecHandBitmap.getWidth();

            mCalendar = Calendar.getInstance();

            final OffloadController mOffloadController = new OffloadController();
            final AnalogDecomposition mDecomposition = new AnalogDecomposition();
            mOffloadController.clearDecomposition();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
//                    mOffloadController.sendDecomposition(mDecomposition.buildWatchFaceDecomposition(AnalogWatchFace.this),false);

                }
            }, 1000);
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            mAmbient = inAmbientMode;

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                /*mHourPaint.setAlpha(inMuteMode ? 100 : 255);
                mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                mSecondPaint.setAlpha(inMuteMode ? 80 : 255);*/
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            Log.e("MAD", "onSurfaceChanged called width:"+width+" height:"+height);
            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;


            /*
             * Calculate lengths of different hands based on watch screen size.
             */
            mSecondHandLength = (float) (mCenterX * 0.875);
            sMinuteHandLength = (float) (mCenterX * 0.75);
            sHourHandLength = (float) (mCenterX * 0.5);

            float scale = ((float)height) / (float) mHourHandBitmap.getHeight();
            mHourHandWidth = (float) (mHourHandWidth * scale );
            mHourHandHeight = (float) (mHourHandHeight * scale);

            mHourHandBitmap = Bitmap.createScaledBitmap(mHourHandBitmap,
                    (int) (mHourHandWidth),
                    (int) (mHourHandHeight), true);

            mHourHandAmbBitmap = Bitmap.createScaledBitmap(mHourHandAmbBitmap,
                    (int) (mHourHandWidth),
                    (int) (mHourHandHeight), true);

            scale = ((float)height) / (float) mMinHandBitmap.getHeight();
            mMinHandWidth = (float) (mMinHandWidth * scale );
            mMinHandHeight = (float) (mMinHandHeight * scale);
            mMinHandBitmap = Bitmap.createScaledBitmap(mMinHandBitmap,
                    (int) (mMinHandWidth),
                    (int) (mMinHandHeight), true);

            mMinHandAmbBitmap = Bitmap.createScaledBitmap(mMinHandAmbBitmap,
                    (int) (mMinHandWidth),
                    (int) (mMinHandHeight), true);

            scale = ((float)height) / (float) mSecHandBitmap.getHeight();
            mSecHandWidth = (float) (mSecHandWidth * scale);
            mSecHandHeight = (float) (mSecHandHeight * scale);
             mSecHandBitmap = Bitmap.createScaledBitmap(mSecHandBitmap,
                    (int) (mSecHandWidth),
                    (int) (mSecHandHeight), true);
            mSecHandAmbBitmap = mSecHandBitmap;

            /* Scale loaded background image (more efficient) if surface dimensions change. */
            scale = ((float) width) / (float) mBackgroundBitmap.getWidth();

            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                    (int) (mBackgroundBitmap.getWidth() * scale),
                    (int) (mBackgroundBitmap.getHeight() * scale), true);

        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    Toast.makeText(getApplicationContext(), R.string.message, Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, null);
            } else if (mAmbient) {
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, null);
            } else {
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, null);//mBackgroundPaint
            }

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float seconds =
                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutes =
                    mCalendar.get(Calendar.MINUTE) + (mCalendar.get(Calendar.SECOND) / 60f);
            final float minutesRotation = minutes * 6f;
            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save();

            Paint p = new Paint();
            p.setAntiAlias(true);
            p.setFilterBitmap(true);
            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            if (mAmbient) {
                canvas.drawBitmap(
                        mHourHandAmbBitmap,
                        mCenterX - (mHourHandWidth / 2),
                        mCenterY - (mHourHandHeight / 2),
                        p);

                canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
                canvas.drawBitmap(
                        mMinHandAmbBitmap,
                        mCenterX - (mMinHandWidth / 2),
                        mCenterY - (mMinHandHeight / 2),
                        p);
            } else {
                canvas.drawBitmap(
                        mHourHandBitmap,
                        mCenterX - (mHourHandWidth / 2),
                        mCenterY - (mHourHandHeight / 2),
                        p);

                canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
                canvas.drawBitmap(
                        mMinHandBitmap,
                        mCenterX - (mMinHandWidth / 2),
                        mCenterY - (mMinHandHeight / 2),
                        p);
            }
            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);

                canvas.drawBitmap(
                        mSecHandBitmap,
                        mCenterX - (mSecHandWidth/2),
                        mCenterY - (mSecHandHeight/2),
                        p);
            } else {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);

                canvas.drawBitmap(
                        mSecHandAmbBitmap,
                        mCenterX - (mSecHandWidth/2),
                        mCenterY - (mSecHandHeight/2),
                        p);
            }

            /* Restore the canvas' original orientation. */
            canvas.restore();

            /* Draw rectangle behind peek card in ambient mode to improve readability. */
            if (mAmbient) {
                canvas.drawRect(mPeekCardBounds, mBackgroundPaint);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            AnalogWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            AnalogWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}

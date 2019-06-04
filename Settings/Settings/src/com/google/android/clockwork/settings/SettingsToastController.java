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
 * limitations under the License
 */

package com.google.android.clockwork.settings;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.support.annotation.VisibleForTesting;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.os.MinimalHandler;
import com.google.android.clockwork.common.os.DefaultMinimalHandler;
import com.google.android.wearable.display.WearableDisplayHelper;

/**
 * Controls displaying a toast with a black background.
 */
public class SettingsToastController {

    private static final int DISPLAY_DURATION_MS = 2500;

    public interface Callback {
        void onShowComplete();
    }

    private final View mRootView;
    private final WindowManager mWindowManager;
    private final WindowManager.LayoutParams mLayoutParams;
    private final MinimalHandler mHandler;
    private final Toast mToast;

    public SettingsToastController(Context context, String message) {
        this(context, message, new DefaultMinimalHandler(new Handler()), 
            Toast.makeText(context, message, DISPLAY_DURATION_MS));
    }

    @VisibleForTesting
    SettingsToastController(Context c, String message, MinimalHandler handler, Toast toast) {
        final Context context = c.getApplicationContext();
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        mHandler = handler;
        mToast = toast;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mRootView = inflater.inflate(R.layout.toast_activity, null);

        mLayoutParams = getWindowParams(context, mWindowManager.getDefaultDisplay());

        new OrientationEventListener(context) {
            @Override
            public void onOrientationChanged(int orientation) {
                rotate(orientation);
            }
        };
    }

    private static WindowManager.LayoutParams getWindowParams(Context context, Display display) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        int addY = context.getResources().getInteger(
                com.android.internal.R.integer.config_windowOutsetBottom);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels + addY,
                WindowManager.LayoutParams.TYPE_TOAST,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN,
                PixelFormat.TRANSLUCENT);
        params.windowAnimations = android.R.style.Animation_Toast;
        params.gravity = Gravity.LEFT | Gravity.TOP;
        return params;
    }

    private void rotate(int orientation) {
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return;
        }
        if (orientation >= 225 && orientation <= 315) {
            mRootView.setRotation(90);
        } else if (orientation > 135 && orientation < 225) {
            mRootView.setRotation(180);
        } else if (orientation >= 45 && orientation <= 135) {
            mRootView.setRotation(270);
        } else {
            mRootView.setRotation(0);
        }
    }

    public void remove() {
        if (mRootView.isAttachedToWindow()) {
            mWindowManager.removeView(mRootView);
        }
    }

    public void show(Callback listener) {
        mWindowManager.addView(mRootView, mLayoutParams);
        mToast.show();
        mHandler.postDelayed(() -> listener.onShowComplete(), DISPLAY_DURATION_MS);
    }
}

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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.clockwork.common.os.MinimalHandler;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@RunWith(ClockworkRobolectricTestRunner.class)
public class SettingsToastControllerTest {

    private static final String TEST_MESSAGE = "test_message";
    @Mock private Context mContext;
    @Mock private Resources mResources;
    @Mock private LayoutInflater mLayoutInflater;
    @Mock private WindowManager mWindowManager;
    @Mock private SensorManager mSensorManager;
    @Mock private Display mDisplay;
    @Mock private View mRootView;
    @Mock private MinimalHandler mHandler;
    @Mock private Toast mToast;
    private SettingsToastController mSettingsToastController;

    @Before
    public void setup() {
        initMocks(this);
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getInteger(anyInt())).thenReturn(0);
        when(mContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(mSensorManager);
        when(mContext.getSystemService(Context.WINDOW_SERVICE)).thenReturn(mWindowManager);
        when(mWindowManager.getDefaultDisplay()).thenReturn(mDisplay);
        when(mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).thenReturn(
                mLayoutInflater);
        when(mLayoutInflater.inflate(anyInt(), eq(null))).thenReturn(mRootView);
        when(mRootView.isAttachedToWindow()).thenReturn(true);

        mSettingsToastController = 
            new SettingsToastController(mContext, TEST_MESSAGE, mHandler, mToast);
    }

    @Test
    public void testShow() {
        SettingsToastController.Callback myCallback = mock(SettingsToastController.Callback.class);
        mSettingsToastController.show(myCallback);
        verify(mWindowManager).addView(eq(mRootView), any());
        verify(mToast).show();
        verify(mHandler).postDelayed(any(Runnable.class), anyLong());
        verify(myCallback, never()).onShowComplete();
    }

    @Test
    public void testRemove() {
        mSettingsToastController.remove();
        verify(mWindowManager).removeView(mRootView);
    }

    @Test
    public void testOnToastComplete() {
        SettingsToastController.Callback myCallback = mock(SettingsToastController.Callback.class);
        mSettingsToastController.show(myCallback);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mHandler).postDelayed(runnableCaptor.capture(), anyLong());
        runnableCaptor.getValue().run();
        verify(myCallback).onShowComplete();
    }
}

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

package com.google.android.clockwork.settings.display;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.clockwork.settings.personal.buttons.StemPressedActivity;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;

/** Test wet mode service */
@RunWith(ClockworkRobolectricTestRunner.class)
public class WetModeServiceTest {
    @Mock
    private PowerManager mPm;
    @Mock
    private WindowManager mWm;
    @Mock
    private ImageView mView;
    @Mock
    private KeyEvent mKeyEvent;
    @Mock
    private MotionEvent mMotionEvent;
    @Mock
    private Drawable mDrawable;
    @Mock
    private Resources mResources;
    @Mock
    private WetModeService mMockService;
    @Mock
    private SharedPreferences mSharedPreferences;
    @Mock
    private SharedPreferences.Editor mEditor;

    private TestWetModeService mService;

    @Before
    public void setUp() {
        initMocks(this);

        ServiceController<TestWetModeService> serviceController
                = Robolectric.buildService(TestWetModeService.class);
        mService = serviceController.get();
        mService.init(mPm, mWm, mView, mSharedPreferences);

        when(mView.isAttachedToWindow()).thenReturn(true);
        when(mMockService.getWindowManager()).thenReturn(mWm);
        when(mMockService.getDrawable(anyInt())).thenReturn(mDrawable);
        when(mMockService.getResources()).thenReturn(mResources);
        when(mResources.getDimension(anyInt())).thenReturn(0f);
        when(mSharedPreferences.getBoolean(eq("cw.wetmode.show_dialog"), anyBoolean()))
                .thenReturn(false);
        when(mSharedPreferences.edit())
                .thenReturn(mEditor);
        when(mEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mEditor);
    }

    @Test
    public void testEnableWetMode() throws Exception {
        mService.setupWetMode();
        verify(mWm).addView(eq(mView), any());
        verify(mPm).goToSleep(anyLong());
    }

    @Test
    public void testDisableWetModeExplicit() throws Exception {
        mService.setupWetMode();
        reset(mWm);
        mService.sendBroadcast(new Intent("com.google.android.clockwork.actions.END_WET_MODE"),
                "com.google.android.clockwork.settings.END_WET_MODE");
        verify(mWm).removeView(mView);
    }

    @Test
    public void testDisableWetModeScreen() throws Exception {
        mService.setupWetMode();
        reset(mWm);
        mService.sendBroadcast(new Intent(Intent.ACTION_SCREEN_ON));
        verify(mWm).removeView(mView);
    }

    @Test
    public void testWetModeBinders() throws Exception {
        mService.setupWetMode();
        IBinder binder = mService.onBind(new Intent());
        Assert.assertEquals(WetModeService.TiltCallback.class, binder.getClass());
    }

    @Test
    public void testWetModeIgnoreScreenOn() throws Exception {
        mService.setupWetMode();
        reset(mWm);
        IBinder binder = mService.onBind(new Intent());
        ((WetModeService.TiltCallback) binder).incomingTiltToWake();
        mService.sendBroadcast(new Intent(Intent.ACTION_SCREEN_ON));
        verify(mWm, never()).removeView(mView);
    }

    @Test
    public void testWetModeIgnoreScreenOnOutOfOrder() throws Exception {
        mService.setupWetMode();
        reset(mWm);
        IBinder binder = mService.onBind(new Intent());
        ((WetModeService.TiltCallback) binder).incomingTiltToWake(); // TTW occurs
        mService.sendBroadcast(new Intent(Intent.ACTION_SCREEN_ON)); // Screen on is ignored
        ((WetModeService.TiltCallback) binder).incomingTiltToWake(); // Next TTW before screen off
        mService.sendBroadcast(new Intent(Intent.ACTION_SCREEN_OFF)); // Screen off
        mService.sendBroadcast(new Intent(Intent.ACTION_SCREEN_ON)); // This should be ignored too
        verify(mWm, never()).removeView(mView);
    }

    @Test
    public void testDontShowDialog() throws Exception {
        mService.setupWetMode();
        Assert.assertNull(mService.mDialog);
    }

    @Test
    public void testShowDialog() throws Exception {
        when(mSharedPreferences.getBoolean(eq("cw.wetmode.show_dialog"), anyBoolean()))
                .thenReturn(true);
        mService.setupWetMode();
        Assert.assertTrue(pressDialogButton(AlertDialog.BUTTON_POSITIVE));
        verify(mWm).addView(eq(mView), any());
        verify(mPm).goToSleep(anyLong());
    }

    @Test
    public void testDisableDialog() throws Exception {
        when(mSharedPreferences.getBoolean(eq("cw.wetmode.show_dialog"), anyBoolean()))
                .thenReturn(true);
        mService.setupWetMode();
        Assert.assertTrue(pressDialogButton(AlertDialog.BUTTON_NEGATIVE));
        verify(mSharedPreferences).edit();
        verify(mEditor).putBoolean("cw.wetmode.show_dialog", false);
        verify(mEditor).apply();
    }

    private boolean pressDialogButton(int which) {
        AlertDialog dialog = mService.mDialog;
        if (dialog != null) {
            Button b = dialog.getButton(which);
            if (b != null) {
                return b.performClick();
            }
        }
        return false;
    }

    private static class TestWetModeService extends WetModeService {
        void init(PowerManager pm, WindowManager wm, ImageView view,
                SharedPreferences prefs) {
            mPowerManager = pm;
            mWindowManager = wm;
            mOverlayView = view;
            mPrefs = prefs;
        }
    }
}

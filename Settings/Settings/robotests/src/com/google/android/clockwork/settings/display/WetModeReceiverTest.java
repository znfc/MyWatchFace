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

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;

import com.google.android.clockwork.robolectric.shadows.ShadowGservices;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowPackageManager;

/** Test touch controller reciever */
@RunWith(ClockworkRobolectricTestRunner.class)
@Config(sdk = 28)
public class WetModeReceiverTest {

    private ShadowApplication application;

    @Mock
    private ShadowPackageManager mPackageManager;

    WetModeReceiver mWetModeReceiver;

    @Before
    public void setUp() {
        initMocks(this);
        application = ShadowApplication.getInstance();
        mPackageManager = shadowOf(RuntimeEnvironment.application.getPackageManager());

        mWetModeReceiver = new WetModeReceiver();
    }

    @Test
    public void testEnableWetMode() throws Exception {
        ShadowGservices.override("cw:wet_mode_home_version", 1);
        PackageInfo homeInfo = new PackageInfo();
        homeInfo.packageName = "com.google.android.wearable.app";
        homeInfo.versionCode = 2;
        mPackageManager.addPackage(homeInfo);
        mWetModeReceiver.onReceive(application.getApplicationContext(),
                new Intent(WetModeReceiver.ACTION_ENABLE_WET_MODE));
        assertEquals(WetModeService.class.getCanonicalName(),
                application.getNextStartedService().getComponent().getClassName());
    }

    @Test
    public void testEnableWetModeWrongHomeVersion() throws Exception {
        ShadowGservices.override("cw:wet_mode_home_version", 2);
        PackageInfo homeInfo = new PackageInfo();
        homeInfo.packageName = "com.google.android.wearable.app";
        homeInfo.versionCode = 1;
        mPackageManager.addPackage(homeInfo);
        mWetModeReceiver.onReceive(application.getApplicationContext(),
                new Intent(WetModeReceiver.ACTION_ENABLE_WET_MODE));
        assertEquals(null, application.getNextStartedService());
    }
}

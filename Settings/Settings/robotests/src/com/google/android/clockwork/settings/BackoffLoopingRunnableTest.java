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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;

@RunWith(ClockworkRobolectricTestRunner.class)
public class BackoffLoopingRunnableTest {
    static final String TAG = BackoffLoopingRunnableTest.class.getSimpleName();
    static final String NAME = "LooperUnderTest";
    static final long INITIAL_TIMEOUT = 1000;
    static final double MULTIPLIER = 2.0;
    static final long MAX_TIMEOUT = 3000;

    int mLoopStatus = BackoffLoopingRunnable.CONTINUE;
    @Spy BackoffLoopingRunnable mTestRunnable =
            new BackoffLoopingRunnable(TAG, NAME, INITIAL_TIMEOUT, MULTIPLIER,
                                       MAX_TIMEOUT) {
                public int loop() {
                    return mLoopStatus;
                }
            };

    long mTime;
    @Spy MockTimer mMockTimer = new MockTimer();

    @Before
    public void setUp() {
        initMocks(this);
        mTime = 0L;
        mTestRunnable.setTimer(mMockTimer);
    }

    @Test
    public void testNoLoop() {
        when(mTestRunnable.loop())
                .thenReturn(BackoffLoopingRunnable.FINISH);

        mTestRunnable.run();

        verify(mMockTimer, times(0)).waitFor(anyLong());
        verify(mTestRunnable, times(1)).loop();
    }

    @Test
    public void testBackoff() {
        when(mTestRunnable.loop())
                .thenReturn(BackoffLoopingRunnable.CONTINUE)
                .thenReturn(BackoffLoopingRunnable.CONTINUE)
                .thenReturn(BackoffLoopingRunnable.FINISH);

        mTestRunnable.run();

        InOrder inOrder = inOrder(mMockTimer);
        inOrder.verify(mMockTimer, times(1)).waitFor(1000);
        inOrder.verify(mMockTimer, times(1)).waitFor(2000);
        inOrder.verify(mMockTimer, times(0)).waitFor(anyLong());
        verify(mTestRunnable, times(3)).loop();
    }

    @Test
    public void testMax() {
        when(mTestRunnable.loop())
                .thenReturn(BackoffLoopingRunnable.CONTINUE)
                .thenReturn(BackoffLoopingRunnable.CONTINUE)
                .thenReturn(BackoffLoopingRunnable.CONTINUE)
                .thenReturn(BackoffLoopingRunnable.CONTINUE)
                .thenReturn(BackoffLoopingRunnable.FINISH);

        mTestRunnable.run();

        InOrder inOrder = inOrder(mMockTimer);
        inOrder.verify(mMockTimer, times(1)).waitFor(1000);
        inOrder.verify(mMockTimer, times(1)).waitFor(2000);
        inOrder.verify(mMockTimer, times(2)).waitFor(3000);
        inOrder.verify(mMockTimer, times(0)).waitFor(anyLong());
    }

    class MockTimer extends BackoffLoopingRunnable.Timer {
        @Override
        public long getTime() {
            return mTime;
        }

        @Override
        public void waitFor(long timeout) {
            mTime += timeout;
        }
    }
}

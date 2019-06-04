package com.google.android.clockwork.settings;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.shadows.ShadowLooper.runUiThreadTasks;

import android.content.Context;
import android.media.AudioManager;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(ClockworkRobolectricTestRunner.class)
public class VolumeUiShowerTest {
    private VolumeUI mVolumeUI;
    @Mock private VolumeUI.Ui mMockUi;

    @Before
    public void setUp() {
        initMocks(this);
        mVolumeUI = new VolumeUI(mMockUi);
    }

    @Test
    public void absoluteVolumeChangesShouldBeIgnored() {
        // WHEN a volume change occurs with FLAG_BLUETOOTH_ABS_VOLUME set
        mVolumeUI.postVolumeChanged(
                AudioManager.STREAM_MUSIC,
                AudioManager.FLAG_BLUETOOTH_ABS_VOLUME | AudioManager.FLAG_SHOW_UI);
        // THEN it's ignored
        verify(mMockUi, never()).showVolumeActivityForStream(anyInt());
    }

    @Test
    public void volumeChangesWithoutShowUiShouldBeIgnored() {
        // WHEN a volume change occurs without FLAG_SHOW_UI set
        mVolumeUI.postVolumeChanged(
                AudioManager.STREAM_MUSIC,
                0 /* no flags */);
        // THEN it's ignored
        verify(mMockUi, never()).showVolumeActivityForStream(anyInt());
    }

    @Test
    public void volumeChangesShowUiShouldTriggerActivtiyShow() {
        // WHEN a volume change occurs with FLAG_SHOW_UI set
        mVolumeUI.postVolumeChanged(
                AudioManager.STREAM_MUSIC,
                AudioManager.FLAG_SHOW_UI);
        // THEN the activity when the handler is run
        verify(mMockUi).showVolumeActivityForStream(AudioManager.STREAM_MUSIC);
    }
}

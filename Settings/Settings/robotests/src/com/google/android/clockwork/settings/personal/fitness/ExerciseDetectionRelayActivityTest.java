package com.google.android.clockwork.settings.personal.fitness;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.BIKING_KEY;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.IMPLICIT_INTENT_EXTRA;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Intent;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;

/** Tests for {@link ExerciseDetectionRelayActivity}. */
@RunWith(ClockworkRobolectricTestRunner.class)
public class ExerciseDetectionRelayActivityTest {

    private Intent mBikingIntent = ExerciseConstants.INTENTS.get(BIKING_KEY);
    private Intent mFullIntent = new Intent().putExtra(IMPLICIT_INTENT_EXTRA, mBikingIntent);

    @Mock ExerciseDetectionRelayPresenter mPresenter;
    @Captor ArgumentCaptor<Intent> mIntentCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void handleIntentStartsRelayIntentService() {
        ExerciseDetectionRelayActivity activity = Robolectric
                .buildActivity(
                    ExerciseDetectionRelayActivity.class,
                    new Intent().putExtra(IMPLICIT_INTENT_EXTRA, mBikingIntent))
                .get();
        ExerciseDetectionRelayActivity spy = spy(activity);

        spy.handleIntent(mFullIntent);

        verify(spy).startService(mIntentCaptor.capture());
        Intent intent = mIntentCaptor.getValue();
        assertEquals(RelayIntentService.class.getName(), intent.getComponent().getClassName());
        assertEquals(mBikingIntent, intent.getParcelableExtra(IMPLICIT_INTENT_EXTRA));
    }

    @Test
    public void testFullLifeCycle() throws Exception {
        Robolectric.buildActivity(ExerciseDetectionRelayActivity.class, mFullIntent)
                .create().start().resume().visible()
                .get();
    }
}

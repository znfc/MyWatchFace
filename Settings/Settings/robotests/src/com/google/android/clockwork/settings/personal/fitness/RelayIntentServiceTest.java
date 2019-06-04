package com.google.android.clockwork.settings.personal.fitness;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.BIKING_KEY;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.IMPLICIT_INTENT_EXTRA;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.annotation.Nullable;
import android.content.Intent;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.IntentServiceController;

/**
 * Tests for {@link RelayIntentService}.
 */
@RunWith(ClockworkRobolectricTestRunner.class)
public class RelayIntentServiceTest extends TestCase {

    private final Intent mImplicitBikingIntent = ExerciseConstants.INTENTS.get(BIKING_KEY);
    @Mock ExerciseDetectionRelayPresenter mPresenter;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void testWithExpectedInput() throws Exception {
        Intent intent = new Intent().putExtra(IMPLICIT_INTENT_EXTRA, mImplicitBikingIntent);
        withIntent(intent);

        verify(mPresenter).onRelayRequest(BIKING_KEY, mImplicitBikingIntent);
    }

    @Test
    public void testWithNoType() throws Exception {
        Intent intent = new Intent();
        withIntent(intent);

        verify(mPresenter, never()).onRelayRequest(anyString(), any(Intent.class));
    }

    @Test
    public void testWithUnsupportedType() throws Exception {
        Intent intent = new Intent().putExtra(IMPLICIT_INTENT_EXTRA, new
                                              Intent().setType("invalid_type"));
        withIntent(intent);

        //noinspection WrongConstant - anyString not an @ExerciseKey.
        verify(mPresenter, never()).onRelayRequest(anyString(), any(Intent.class));
    }

    @Test
    public void testWithNullBaseIntent() throws Exception {
        withIntent(null);

        //noinspection WrongConstant - anyString not an @ExerciseKey.
        verify(mPresenter, never()).onRelayRequest(anyString(), any(Intent.class));
    }

    @Test
    public void testWithNoImplicitIntent() throws Exception {
        Intent intent = new Intent();
        withIntent(intent);

        //noinspection WrongConstant - anyString not an @ExerciseKey.
        verify(mPresenter, never()).onRelayRequest(anyString(), any(Intent.class));
    }

    private void withIntent(@Nullable Intent intent) {
        IntentServiceController<RelayIntentService> mController
            = Robolectric.buildIntentService(RelayIntentService.class,
                    intent);
        mController.create();
        mController.get().mPresenter = mPresenter;
        mController.handleIntent().unbind().destroy();
    }
}

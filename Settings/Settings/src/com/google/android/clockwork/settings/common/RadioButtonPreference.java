package com.google.android.clockwork.settings.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.util.Log;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.logging.CwEventLogger;
import com.google.protos.wireless.android.clockwork.apps.logs.CwEnums.CwSettingsUiEvent;

/**
 * Radio button preference that enables logging for this app.
 */
public class RadioButtonPreference extends CheckBoxPreference {
    private static final String TAG = "RadioButtonPref";

    /**
     * Clients must implement this listener callback to receive events.
     */
    public interface OnClickListener {
        public void onRadioButtonClicked(RadioButtonPreference emiter);
    }

    private OnClickListener mListener;
    private String mEntryValue;

    public RadioButtonPreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
        setPersistent(false);

        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.RadioButtonPreference, defStyleAttr, defStyleRes);
        mEntryValue = a.getString(R.styleable.RadioButtonPreference_entryValue);
        a.recycle();
    }

    public RadioButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RadioButtonPreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.checkBoxPreferenceStyle);
    }

    public RadioButtonPreference(Context context) {
        this(context, null);
    }

    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    @Override
    public void onClick() {
        if (mListener != null) {
            final String prefLoggingKey = this.getExtras().getString(
                    SettingsPreferenceFragment.EXTRA_LOGGING_KEY, this.getKey());
            final CwSettingsUiEvent event
                = SettingsPreferenceLogConstants.getLoggingId(prefLoggingKey);

            // This may be run in a non-system server process (e.g. phone process) which
            // does not have a logger setup.  So we catch those cases under the assumption
            // that we are in the phone process, not that there is a problem with the
            // clockwork logger setting.
            // TODO(cmanton) Remove once all preferences running in phone process are eliminated.
            // b/62660987
            try {
                LogUtils.logPreferenceSelection(CwEventLogger.getInstance(getContext()), event);
            } catch (java.lang.NullPointerException e) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Logger is unavailable, must be phone process");
                }
            }
            mListener.onRadioButtonClicked(this);
        }
    }

    public int getEntryValueInt() {
        return Integer.parseInt(mEntryValue);
    }

    public String getEntryValue() {
        return mEntryValue;
    }

    public void setEntryValue(String entryValue) {
        mEntryValue = entryValue;
    }

    public void setEntryValue(int entryValue) {
        mEntryValue = Integer.toString(entryValue);
    }
}

package com.google.android.clockwork.settings.system;

import android.app.Dialog;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import com.google.android.apps.wearable.settings.R;
import com.google.android.wearable.libs.datetimepicker.TimePicker;

public class TimePickerPreference extends Preference {
    private TimePicker.TimeSelectedListener mListener;

    public TimePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimePickerPreference(Context context) {
        super(context);
    }

    public void setTimeSelectedListener(TimePicker.TimeSelectedListener listener) {
        mListener = listener;
    }

    @Override
    protected void onClick() {
        super.onClick();
        View v = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.time_picker_dialog, null);
        final Dialog dialog = new Dialog(getContext(), android.R.style.Theme_DeviceDefault_Dialog);
        ((TimePicker) v.findViewById(R.id.container)).setCallback((hour, min) -> {
            if (mListener != null) {
                mListener.onTimeSelected(hour, min);
            }
            if (dialog != null) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(v);
        dialog.show();

        AccessibilityManager manager =
                (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain();
            event.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            event.getText().add(getContext().getString(R.string.set_time_instruction));
            manager.sendAccessibilityEvent(event);
        }
    }
}

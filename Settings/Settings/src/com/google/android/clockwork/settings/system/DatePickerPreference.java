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
import com.google.android.wearable.libs.datetimepicker.DatePicker;

public class DatePickerPreference extends Preference {
    private DatePicker.DateSelectedListener mListener;

    public DatePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DatePickerPreference(Context context) {
        super(context);
    }

    public void setDateSelectedListener(DatePicker.DateSelectedListener listener) {
        mListener = listener;
    }

    @Override
    protected void onClick() {
        super.onClick();
        View v = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
            .inflate(R.layout.date_picker_dialog, null);
        final Dialog dialog = new Dialog(getContext(), android.R.style.Theme_DeviceDefault_Dialog);
        ((DatePicker) v.findViewById(R.id.container)).setCallback((year, month, day) -> {
            if (mListener != null) {
                mListener.onDateSelected(year, month, day);
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
            event.getText().add(getContext().getString(R.string.set_date_instruction));
            manager.sendAccessibilityEvent(event);
        }
    }
}

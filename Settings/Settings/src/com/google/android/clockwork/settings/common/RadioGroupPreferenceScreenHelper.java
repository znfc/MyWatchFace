package com.google.android.clockwork.settings.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.BaseAdapter;

import com.google.android.apps.wearable.settings.R;

public class RadioGroupPreferenceScreenHelper implements RadioButtonPreference.OnClickListener {
    public static interface OnCheckedChangeListener {
        public void onCheckedChanged(PreferenceGroup group, RadioButtonPreference preference);
    }

    private PreferenceGroup mGroup;
    private OnCheckedChangeListener mListener;
    private RadioButtonPreference mSelected;
    private PreferenceScreen mBaseScreen;
    private CharSequence mDefaultSummary;

    public RadioGroupPreferenceScreenHelper(PreferenceGroup group) {
        mGroup = group;
        mSelected = null;

        initScreen();
    }

    public void check(RadioButtonPreference preference) {
        if (mSelected != preference) {
            for (int i = 0; i < mGroup.getPreferenceCount(); ++i) {
                Preference p = mGroup.getPreference(i);
                if (p instanceof RadioButtonPreference) {
                    ((RadioButtonPreference) p).setChecked(preference == p);
                }
            }
            if (mBaseScreen != null) {
                mGroup.setSummary(preference == null ? mDefaultSummary : preference.getTitle());
                ((BaseAdapter) mBaseScreen.getRootAdapter()).notifyDataSetChanged();
            }
            if (mListener != null) {
                mListener.onCheckedChanged(mGroup, preference);
            }
            mSelected = preference;
        }
    }

    public void checkByEntryValue(String entryValue) {
        check(findPreferenceByEntryValue(entryValue));
    }

    public void checkByEntryValue(int entryValue) {
        check(findPreferenceByEntryValue(entryValue));
    }

    public void enableAutoSummary(PreferenceScreen baseScreen, CharSequence defaultSummary) {
        mBaseScreen = baseScreen;
        mDefaultSummary = defaultSummary;
    }

    public RadioButtonPreference getSelected() {
        return mSelected;
    }

    public void setOnCheckedChangedListener(OnCheckedChangeListener listener) {
        mListener = listener;
    }

    public RadioButtonPreference findPreferenceByEntryValue(String entryValue) {
        for (int i = 0; i < mGroup.getPreferenceCount(); ++i) {
            Preference p = mGroup.getPreference(i);
            if (p instanceof RadioButtonPreference) {
                RadioButtonPreference radioButtonPreference = (RadioButtonPreference) p;
                if (TextUtils.equals(radioButtonPreference.getEntryValue(), entryValue)) {
                    return radioButtonPreference;
                }
            }
        }
        return null;
    }

    public RadioButtonPreference findPreferenceByEntryValue(int entryValue) {
        for (int i = 0; i < mGroup.getPreferenceCount(); ++i) {
            Preference p = mGroup.getPreference(i);
            if (p instanceof RadioButtonPreference) {
                RadioButtonPreference radioButtonPreference = (RadioButtonPreference) p;
                try {
                    if (radioButtonPreference.getEntryValueInt() == entryValue) {
                        return radioButtonPreference;
                    }
                } catch (NumberFormatException e) {
                    // do nothing...
                }
            }
        }
        return null;
    }

    private void initScreen() {
        for (int i = 0; i < mGroup.getPreferenceCount(); ++i) {
            Preference p = mGroup.getPreference(i);
            if (p instanceof RadioButtonPreference) {
                ((RadioButtonPreference) p).setOnClickListener(this);
            }
        }
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference emiter) {
        check(emiter);
    }
}

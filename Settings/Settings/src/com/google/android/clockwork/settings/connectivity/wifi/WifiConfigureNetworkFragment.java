package com.google.android.clockwork.settings.connectivity.wifi;

import android.annotation.StringRes;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import java.util.Optional;

/** Fragment containing actions for Wifi network configuration */
public class WifiConfigureNetworkFragment extends SettingsPreferenceFragment {
    private static final String KEY_WIFI_NETWORK_CONFIGURATION = "pref_wifiNetworkConfiguration";
    private static final String KEY_WIFI_NETWORK_MESSAGE = "pref_wifiNetworkMessage";
    private static final String KEY_WIFI_RETRY = "pref_wifiRetry";
    private static final String KEY_WIFI_OPEN_ON_PHONE = "pref_wifiOpenOnPhone";
    private static final String KEY_WIFI_OPEN_ON_WATCH = "pref_wifiOpenOnWatch";
    private static final String KEY_WIFI_FORGET_NETWORK = "pref_wifiForgetNetwork";

    static final String EXTRA_SSID_KEY = "ssid";
    static final String EXTRA_DISABLED_KEY = "disabled";
    static final String EXTRA_AUTH_FAILURE_KEY = "auth_failure";

    static final String EXTRA_RESULT_ACTION = "result_action";
    static final String EXTRA_RESULT_PASSWORD = "result_password";

    static final int RETRY_RESULT = 0;
    static final int ENTER_ON_WATCH_RESULT = 1;
    static final int ENTER_ON_PHONE_RESULT = 2;
    static final int FORGET_NETWORK_RESULT = 3;

    InputMethodManager inputMethodManager;
    EditText passwordInput;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_wifi_configure);

        initPasswordInput();

        PreferenceScreen configureNetworkPref =
                (PreferenceScreen) findPreference(KEY_WIFI_NETWORK_CONFIGURATION);
        Preference retryPref = findPreference(KEY_WIFI_RETRY);
        Preference openOnWatchPref = findPreference(KEY_WIFI_OPEN_ON_WATCH);
        Preference openOnPhonePref = findPreference(KEY_WIFI_OPEN_ON_PHONE);
        Preference forgetNetworkPref = findPreference(KEY_WIFI_FORGET_NETWORK);

        openOnWatchPref.setOnPreferenceClickListener(
                (p) -> {
                    showPasswordEntry();
                    return true;
                });
        setClickListener(openOnPhonePref, ENTER_ON_PHONE_RESULT);

        Bundle arguments = getArguments();
        String ssid = arguments.getString(EXTRA_SSID_KEY);
        boolean isDisabled = arguments.getBoolean(EXTRA_DISABLED_KEY);
        boolean authFailure = arguments.getBoolean(EXTRA_AUTH_FAILURE_KEY);

        if (isDisabled) {
            configureNetworkPref.removePreference(findPreference(KEY_WIFI_NETWORK_MESSAGE));
            setClickListener(retryPref, RETRY_RESULT);
            openOnWatchPref.setTitle(R.string.action_open_on_watch);
            openOnPhonePref.setTitle(R.string.action_open_on_phone);
            if (authFailure) {
                configureNetworkPref.removePreference(forgetNetworkPref);
                configureNetworkPref.setTitle(R.string.wifi_disabled_password_failure);
            } else {
                setClickListener(forgetNetworkPref, FORGET_NETWORK_RESULT, ssid);
                forgetNetworkPref.setTitle(
                        getContext().getString(R.string.wifi_forget_network_title, ssid));
                configureNetworkPref.setTitle(R.string.wifi_disabled_wifi_failure);
            }
        } else {
            configureNetworkPref.removePreference(retryPref);
            configureNetworkPref.removePreference(forgetNetworkPref);
            configureNetworkPref.setTitle(ssid);
        }
    }

    private void initPasswordInput() {
        inputMethodManager =
                (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        passwordInput = new EditText(getContext());
        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        setPasswordHint(passwordInput, R.string.keyboard_password_hint);
        passwordInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        passwordInput.setOnEditorActionListener(passwordInputListener);
        getActivity().addContentView(passwordInput, new LayoutParams(1, 0));
    }

    private void setPasswordHint(EditText passwordInput, @StringRes int res) {
        SpannableString hintString = new SpannableString(getContext().getString(res));
        TextAppearanceSpan span =
                new TextAppearanceSpan(
                        getContext(), android.R.style.TextAppearance_Material_Subhead);
        hintString.setSpan(span, 0, hintString.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        passwordInput.setHint(hintString);
    }

    private OnEditorActionListener passwordInputListener =
            (TextView textView, int actionId, KeyEvent event) -> {
                boolean isKeyboardEnterKey =
                        event != null
                                && KeyEvent.isConfirmKey(event.getKeyCode())
                                && event.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId != EditorInfo.IME_ACTION_DONE && !isKeyboardEnterKey) {
                    return false;
                }
                inputMethodManager.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                finishActivity(ENTER_ON_WATCH_RESULT, Optional.of(textView.getText().toString()));
                return true;
            };

    private void setClickListener(Preference pref, int resultCode) {
        setClickListener(pref, resultCode, null);
    }

    private void setClickListener(Preference pref, int resultCode, final String ssid) {
        pref.setOnPreferenceClickListener(
                (p) -> {
                    finishActivity(resultCode, Optional.<String>empty(), ssid);
                    return true;
                });
    }

    private void finishActivity(int resultCode, Optional<String> password) {
        finishActivity(resultCode, password, null);
    }

    private void finishActivity(int resultCode, Optional<String> password, String ssid) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT_ACTION, resultCode);
        if (password.isPresent()) {
            intent.putExtra(EXTRA_RESULT_PASSWORD, password.get());
        }
        if (ssid != null) {
            intent.putExtra(WifiConfigureNetworkFragment.EXTRA_SSID_KEY, ssid);
        }
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }

    private void showPasswordEntry() {
        passwordInput.requestFocus();
        inputMethodManager.showSoftInput(passwordInput, InputMethod.SHOW_FORCED);
    }
}

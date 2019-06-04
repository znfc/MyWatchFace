package com.google.android.clockwork.settings.keyguard;

import android.annotation.StringRes;
import android.content.Context;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

class PasswordEntryHelper {
    private static final String FONT_FAMILY = "sans-serif-condensed";
    private final Context mContext;
    private final EditText mPasswordEntry;
    private final InputMethodManager mInputManager;

    PasswordEntryHelper(Context context, boolean isAlphaMode) {
        mContext = context;
        mInputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        mPasswordEntry = createPasswordEntry(isAlphaMode);
    }

    public EditText getPasswordEntryView() {
        return mPasswordEntry;
    }

    public void setOnEditorActionListener(TextView.OnEditorActionListener listener) {
        mPasswordEntry.setOnEditorActionListener(listener);
    }

    public void clearText() {
        mPasswordEntry.setText("");
    }

    public void setHint(@StringRes int hintRes) {
        SpannableString hintString = new SpannableString(mContext.getString(hintRes));
        TypefaceSpan span = new TypefaceSpan(FONT_FAMILY);
        hintString.setSpan(span, 0, hintString.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        mPasswordEntry.setHint(hintString);
    }

    public void showKeyboard() {
        mPasswordEntry.requestFocus();
        mInputManager.showSoftInput(mPasswordEntry, InputMethod.SHOW_FORCED);
    }

    public void hideKeyboard() {
        mInputManager.hideSoftInputFromWindow(mPasswordEntry.getWindowToken(), 0);
    }

    private EditText createPasswordEntry(boolean isAlphaMode) {
        final EditText passwordEditText = new EditText(mContext);
        passwordEditText.setInputType(isAlphaMode ?
                (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                : (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD));
        passwordEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);

        return passwordEditText;
    }
}

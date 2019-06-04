package com.google.android.clockwork.settings.cellular.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.google.android.apps.wearable.settings.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * This view displays the pin pad for SIM unlocking.
 * Adapted from NumPadView.
 */
public class PinPadView extends ConstraintLayout implements View.OnClickListener,
        View.OnTouchListener {

    public interface PinPadListener {
        void onKeyHover(int keyCode);

        void onKeyPressed(int keyCode);
    }

    private ColorStateList mTouchColor;
    private ArrayList<PinPadListener> mListeners;

    private final int[] mButtonIds = new int[]{R.id.button_0, R.id.button_1, R.id.button_2,
            R.id.button_3, R.id.button_4, R.id.button_5, R.id.button_6, R.id.button_7,
            R.id.button_8, R.id.button_9, R.id.button_enter, R.id.button_del};
    private final int[] mKeyCodes = new int[]{KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1,
            KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5,
            KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9,
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DEL};

    public PinPadView(Context context) {
        this(context, null);
    }

    public PinPadView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PinPadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.pin_pad, this);
        mListeners = new ArrayList<>();
        mTouchColor = getResources().getColorStateList(R.color.pin_pad_touch_tint);
        setupNumbers();
    }

    // Adapted from {@link DialpadView.setupKeypad}
    private void setupNumbers() {
        final Resources resources = getResources();
        final Locale currentLocale = resources.getConfiguration().locale;
        final NumberFormat nf;
        // We translate dialpad numbers only for "fa" and not any other locale
        // ("ar" anybody ?).
        if ("fa".equals(currentLocale.getLanguage())) {
            nf = DecimalFormat.getInstance(resources.getConfiguration().locale);
        } else {
            nf = DecimalFormat.getInstance(Locale.ENGLISH);
        }

        for (int i = 0; i < mButtonIds.length; i++) {
            final View buttonView = findViewById(mButtonIds[i]);

            if (mButtonIds[i] != R.id.button_enter && mButtonIds[i] != R.id.button_del) {
                final TextView numView = (TextView) buttonView;

                final String numberString;
                numberString = nf.format(i);

                numView.setText(numberString);
            }

            buttonView.setOnClickListener(this);
            buttonView.setOnTouchListener(this);
            Drawable background = buttonView.getBackground();

            if (background instanceof RippleDrawable) {
                ((RippleDrawable) background).setColor(mTouchColor);
            }
        }
    }

    @Override
    public void onClick(View view) {

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        final int keyIndex = getKeyIndex(view.getId());

        if (keyIndex < 0) {
            return false;
        }

        final int keyCode = mKeyCodes[keyIndex];

        for (PinPadListener listener : mListeners) {
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    listener.onKeyHover(keyCode);
                    break;
                case MotionEvent.ACTION_UP:
                    listener.onKeyPressed(keyCode);
                    break;
            }
        }

        return false;
    }

    public void registerListener(PinPadListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(PinPadListener listener) {
        mListeners.remove(listener);
    }

    private int getKeyIndex(int id) {
        for (int i = 0; i < mButtonIds.length; ++i) {
            if (mButtonIds[i] == id) {
                return i;
            }
        }

        return -1;
    }
}

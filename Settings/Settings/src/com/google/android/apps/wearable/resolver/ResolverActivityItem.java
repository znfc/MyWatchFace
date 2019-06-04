package com.google.android.apps.wearable.resolver;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.views.ExtendedOnCenterProximityListener;

public class ResolverActivityItem extends RelativeLayout
        implements ExtendedOnCenterProximityListener {

    private static final float FULL_ALPHA = 1f;
    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 1.2f;

    private TextView mTextView;
    private ImageView mIconView;
    private final float mFadedTextAlpha;

    public ResolverActivityItem(Context context) {
        this(context, null);
    }

    public ResolverActivityItem(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ResolverActivityItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mFadedTextAlpha = getResources().getInteger(R.integer.action_text_faded_alpha) / 100f;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIconView = (ImageView) findViewById(R.id.resolver_item_application_image);
        mTextView = (TextView) findViewById(R.id.resolver_item_application_name);
        mTextView.setAlpha(mFadedTextAlpha);
        setClipToPadding(false);
    }

    @Override
    public float getProximityMinValue() {
        return MIN_SCALE;
    }

    @Override
    public float getProximityMaxValue() {
        return MAX_SCALE;
    }

    @Override
    public float getCurrentProximityValue() {
        return mIconView.getScaleX();
    }

    @Override
    public void setScalingAnimatorValue(float scale) {
        setClipChildren(scale == MIN_SCALE);
        mIconView.setScaleX(scale);
        mIconView.setScaleY(scale);
    }

    @Override
    public void onCenterPosition(boolean animate) {
        mTextView.setAlpha(FULL_ALPHA);
    }

    @Override
    public void onNonCenterPosition(boolean animate) {
        mTextView.setAlpha(mFadedTextAlpha);
    }
}

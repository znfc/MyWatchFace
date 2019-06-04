package com.google.android.clockwork.settings;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.views.ExtendedOnCenterProximityListener;

public class DialogItemLayout extends RelativeLayout implements ExtendedOnCenterProximityListener {

    private int mProximityGrowth;

    private int mInitialIconSize;

    private ImageView mIcon;

    private float mProximity;

    public DialogItemLayout(Context context) {
        this(context, null);
    }

    public DialogItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DialogItemLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mInitialIconSize =
                (int) getResources().getDimension(R.dimen.full_screen_dialog_item_icon_size);
        mProximityGrowth =
                (int) getResources().getDimension(R.dimen.full_screen_dialog_item_icon_growth);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIcon = (ImageView) findViewById(R.id.full_screen_dialog_item_icon);
    }

    @Override
    public float getProximityMinValue() {
        return mInitialIconSize;
    }

    @Override
    public float getProximityMaxValue() {
        return mInitialIconSize + mProximityGrowth;
    }

    @Override
    public float getCurrentProximityValue() {
        return mProximity;
    }

    @Override
    public void setScalingAnimatorValue(float proximity) {
        mProximity = proximity;
        final ShapeDrawable drawable = ((ShapeDrawable) mIcon.getDrawable());
        final int halfSize = (int) proximity / 2;
        Rect old = drawable.getBounds();
        drawable.setBounds(old.centerX() - halfSize, old.centerY() - halfSize,
                old.centerX() + halfSize, old.centerY() + halfSize);
        setClipChildren(proximity == mInitialIconSize);
        invalidate();
    }

    @Override
    public void onCenterPosition(boolean animate) {

    }

    @Override
    public void onNonCenterPosition(boolean animate) {

    }
}

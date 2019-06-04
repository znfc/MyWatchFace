package com.google.android.clockwork.settings;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.views.ViewUtils;

/**
 * A helper class to setup list info items' content and formatting.
 */
public class InfoListItemInitializer {

    private final Context mContext;

    // List items' padding and dimensions
    private int mListItemPaddingLeft;
    private int mListItemPaddingRight;
    private int mListItemPaddingTop;
    private int mFirstListItemPaddingTop;
    private int mListItemPaddingBottom;
    private int mLastListItemPaddingBottom;
    private int mListItemContentMinHeight;

    public InfoListItemInitializer(Context context, boolean extraPaddingAtTheEnd) {
        mContext = context;
        Resources resources = mContext.getResources();
        mListItemPaddingLeft = resources.getDimensionPixelSize(R.dimen.info_item_padding_left);
        mListItemPaddingRight = resources.getDimensionPixelSize(R.dimen.info_item_padding_right);
        mListItemPaddingTop = resources.getDimensionPixelSize(R.dimen.info_item_padding_top);
        mFirstListItemPaddingTop = mListItemPaddingTop;
        mListItemPaddingBottom = resources.getDimensionPixelSize(R.dimen.info_item_padding_bottom);
        mLastListItemPaddingBottom =
                resources.getDimensionPixelSize(R.dimen.info_item_padding_last_bottom);
        mListItemContentMinHeight = resources.getDimensionPixelSize(R.dimen.info_item_min_height)
                - mListItemPaddingTop - mListItemPaddingBottom;
        if (ViewUtils.isCircular(mContext)) {
            mListItemPaddingLeft +=
                    resources.getDimensionPixelOffset(R.dimen.round_content_padding_left);
            mListItemPaddingRight +=
                    resources.getDimensionPixelOffset(R.dimen.round_content_padding_right);
            int verticalPadding =
                    resources.getDimensionPixelOffset(R.dimen.cw_circular_margins_vertical);
            mFirstListItemPaddingTop += verticalPadding;
            if (extraPaddingAtTheEnd) {
                mLastListItemPaddingBottom += verticalPadding;
            }
        }
    }

    /**
     * Initialize an info list item view with the given strings and set an appropriate padding
     * based on the position of the item within the info list.
     *
     * @param view the root view of the list's item. It was created by inflating info_list_item.xml
     *         layout.
     * @param position the position of the item within the list.
     * @param count the total number of items in the list.
     * @param title the title of the list's item
     * @param value the value of the list's item. If the title is empty, the value will be centered
     *         inside the view (excluding the view's padding); otherwise it will be positioned
     *         below the title.
     */
    public void initListItemView(View view, int position, int count, CharSequence title,
            CharSequence value) {
        view.setPadding(
                mListItemPaddingLeft,
                position == 0 ? mFirstListItemPaddingTop : mListItemPaddingTop,
                mListItemPaddingRight,
                position == count - 1 ? mLastListItemPaddingBottom : mListItemPaddingBottom);

        TextView titleView = (TextView) view.findViewById(R.id.title);
        TextView valueView = (TextView) view.findViewById(R.id.value);

        titleView.setText(title);
        if (TextUtils.isEmpty(title)) {
            // Center the value's text vertically in the list item (excluding its padding).
            valueView.setMinHeight(mListItemContentMinHeight);
            valueView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            titleView.setVisibility(View.GONE);
        } else {
            // Align the value's text below the title.
            valueView.setMinHeight(0);
            valueView.setGravity(Gravity.TOP | Gravity.LEFT);
            titleView.setVisibility(View.VISIBLE);
        }
        valueView.setText(value);
    }

}

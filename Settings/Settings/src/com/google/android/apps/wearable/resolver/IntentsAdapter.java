package com.google.android.apps.wearable.resolver;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.views.ExtendedViewHolder;

public class IntentsAdapter extends WearableListView.Adapter {

    private final Context mContext;

    private ResolverActivity.ResolveListHelper mListHelper;

    public IntentsAdapter(Context context, ResolverActivity.ResolveListHelper listHelper) {
        mContext = context;
        mListHelper = listHelper;
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(
                R.layout.resolver_intent_item, null);
        return new ExtendedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
        View view = holder.itemView;
        Intent intent = mListHelper.intentForPosition(position);
        ActivityInfo info = intent.resolveActivityInfo(mContext.getPackageManager(), 0);
        TextView textView = (TextView) view.findViewById(R.id.resolver_item_application_name);
        ImageView imageView = (ImageView) view.findViewById(
                R.id.resolver_item_application_image);
        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = info;

        textView.setText(resolveInfo.loadLabel(mContext.getPackageManager()));
        imageView.setImageDrawable(resolveInfo.loadIcon(mContext.getPackageManager()));

    }

    @Override
    public int getItemCount() {
        return mListHelper.getCount();
    }
}

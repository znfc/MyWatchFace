/**
 * Copyright (c) 2017 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sidekickwatchface;

import android.content.Context;
import com.qualcomm.qti.sidekickwatchface.R;
import android.support.wearable.watchface.decomposition.ImageComponent;
import android.support.wearable.watchface.decomposition.WatchFaceDecomposition;
import android.support.wearable.watchface.decompositionface.DecompositionWatchFaceService;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Icon;

public class AnalogDecomposition extends DecompositionWatchFaceService
{

    public static WatchFaceDecomposition buildWatchFaceDecomposition(Context mContext) {

        ImageComponent backgroundImageComponent = new ImageComponent.Builder()
                    .setComponentId(1)
                    .setZOrder(1)
                    .setImage(Icon.createWithResource(mContext, R.drawable.bg))
                    .setBounds(new RectF(0F, 0F, 1F, 1F))
                    .build();
        ImageComponent hourImageComponent = new ImageComponent.Builder()
                    .setComponentId(10)
                    .setZOrder(2)
                    .setImage(Icon.createWithResource(mContext, R.drawable.hour))
                    .setBounds(new RectF(0.471794871F, 0.2641026F, 0.530769F, 0.53333333F))
                    .setDegreesPerDay(720.0F)
                    .setPivot(new PointF(0.5F, 0.5F))
                    .build();
        ImageComponent minImageComponent = new ImageComponent.Builder()
                    .setComponentId(12)
                    .setZOrder(3)
                    .setImage(Icon.createWithResource(mContext, R.drawable.min))
                    .setBounds(new RectF(0.46923076F, 0.06923076F, 0.530769F, 0.53333333F))
                    .setDegreesPerDay(8640.0F)
                    .setPivot(new PointF(0.5F, 0.5F))
                    .build();
        ImageComponent secImageComponent = new ImageComponent.Builder()
                    .setComponentId(15)
                    .setZOrder(4)
                    .setImage(Icon.createWithResource(mContext, R.drawable.sec))
                    .setBounds(new RectF(0.47948717F, 0.01794871F, 0.52051282F, 0.6179487179F))
                    .setDegreesPerDay(518400.0F)
                    .setPivot(new PointF(0.5F, 0.5F))
                    .build();
        return new WatchFaceDecomposition.Builder()
                    .addImageComponents(new ImageComponent[] { backgroundImageComponent, hourImageComponent, minImageComponent, secImageComponent })
                    .build();
        }

    @Override
    protected WatchFaceDecomposition buildDecomposition() {
        return buildWatchFaceDecomposition(this);
    }
}
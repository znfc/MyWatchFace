/**
 * Copyright (c) 2017 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sidekickwatchface;

import android.content.Context;
import com.qualcomm.qti.sidekickwatchface.R;
import com.google.android.clockwork.decomposablewatchface.FontComponent;
import com.google.android.clockwork.decomposablewatchface.ImageComponent;
import com.google.android.clockwork.decomposablewatchface.NumberComponent;
import com.google.android.clockwork.decomposablewatchface.WatchFaceDecomposition;

import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Icon;
import java.util.concurrent.TimeUnit;

public class DigitalDecomposition
{

    public WatchFaceDecomposition buildWatchFaceDecomposition(Context mContext) {

        ImageComponent backgroundImageComponent = new ImageComponent.Builder()
                    .setComponentId(1)
                    .setZOrder(1)
                    .setImage(Icon.createWithResource(mContext, R.drawable.bg_digital))
                    .setBounds(new RectF(0F, 0F, 1F, 1F))
                    .build();
        ImageComponent ringImageComponent = new ImageComponent.Builder()
                    .setComponentId(10)
                    .setZOrder(2)
                    .setImage(Icon.createWithResource(mContext, R.drawable.ring))
                    .setBounds(new RectF(0F, 0F, 1F, 1F))
                    .build();
        ImageComponent logoImageComponent = new ImageComponent.Builder()
                    .setComponentId(11)
                    .setZOrder(3)
                    .setImage(Icon.createWithResource(mContext, R.drawable.logo))
                    .setBounds(new RectF(0.45384615F, 0.11025641F, 0.54615384F, 0.22051282F))
                    .build();
        ImageComponent hourColonImageComponent = new ImageComponent.Builder()
                    .setComponentId(12)
                    .setZOrder(4)
                    .setImage(Icon.createWithResource(mContext, R.drawable.colon))
                    .setBounds(new RectF(0.31538461F, 0.46923076F, 0.38205128F, 0.55641025F))
                    .build();
        ImageComponent commaComponent = new ImageComponent.Builder()
                    .setComponentId(13)
                    .setZOrder(4)
                    .setImage(Icon.createWithResource(mContext, R.drawable.comma))
                    .setBounds(new RectF(0.44615384F, 0.78717948F, 0.47692307F, 0.80256410F))
                    .build();
        ImageComponent minColonImageComponent = new ImageComponent.Builder()
                    .setComponentId(14)
                    .setZOrder(4)
                    .setImage(Icon.createWithResource(mContext, R.drawable.colon))
                    .setBounds(new RectF(0.65897435F, 0.46923076F, 0.68974358F, 0.55641025F))
                    .build();
        FontComponent bigNumberFontComponent = new FontComponent.Builder()
                    .setComponentId(15)
                    .setImage(Icon.createWithResource(mContext, R.drawable.number_big))
                    .setDigitCount(10)
                    .setDigitDimensions(new PointF(52, 79))
                    .build();
        FontComponent smallNumberFontComponent = new FontComponent.Builder()
                    .setComponentId(16)
                    .setImage(Icon.createWithResource(mContext, R.drawable.number_small))
                    .setDigitCount(10)
                    .setDigitDimensions(new PointF(38, 61))
                    .build();
        FontComponent dateFontComponent = new FontComponent.Builder()
                    .setComponentId(17)
                    .setImage(Icon.createWithResource(mContext, R.drawable.date))
                    .setDigitCount(31)
                    .setDigitDimensions(new PointF(26, 29))
                    .build();
        FontComponent dayFontComponent = new FontComponent.Builder()
                    .setComponentId(18)
                    .setImage(Icon.createWithResource(mContext, R.drawable.day))
                    .setDigitCount(7)
                    .setDigitDimensions(new PointF(47, 27))
                    .build();
        FontComponent monthFontComponent = new FontComponent.Builder()
                    .setComponentId(19)
                    .setImage(Icon.createWithResource(mContext, R.drawable.month))
                    .setDigitCount(12)
                    .setDigitDimensions(new PointF(46, 28))
                    .build();
        NumberComponent hourNumberComponent = new NumberComponent.Builder()
                    .setComponentId(20)
                    .setMsPerIncrement(TimeUnit.HOURS.toMillis(1L))
                    .setLowestValue(0L)
                    .setHighestValue(11L)
                    .setTimeOffsetMs(0)
                    .setMinDigitsShown(1)
                    .setFontComponentId(bigNumberFontComponent.getComponentId())
                    .setZOrder(2)
                    .setPosition(new PointF(0.03846153f, 0.42307692f))
                    .build();
        NumberComponent minNumberComponent = new NumberComponent.Builder()
                    .setComponentId(21)
                    .setMsPerIncrement(TimeUnit.MINUTES.toMillis(1L))
                    .setLowestValue(0L)
                    .setHighestValue(59L)
                    .setTimeOffsetMs(0)
                    .setMinDigitsShown(1)
                    .setFontComponentId(bigNumberFontComponent.getComponentId())
                    .setZOrder(3)
                    .setPosition(new PointF(0.38205128f, 0.42307692f))
                    .build();
        NumberComponent secNumberComponent = new NumberComponent.Builder()
                    .setComponentId(22)
                    .setMsPerIncrement(TimeUnit.SECONDS.toMillis(1L))
                    .setLowestValue(0L)
                    .setHighestValue(59L)
                    .setTimeOffsetMs(0)
                    .setMinDigitsShown(1)
                    //.setFontComponentId(smallNumberFontComponent.getComponentId())
                    .setFontComponentId(bigNumberFontComponent.getComponentId())
                    .setZOrder(4)
                    .setPosition(new PointF(0.72564102f, 0.44615384f))
                    .build();
        NumberComponent dayNumberComponent = new NumberComponent.Builder()
                    .setComponentId(23)
                    .setMsPerIncrement(TimeUnit.DAYS.toMillis(1L))
                    .setLowestValue(0)
                    .setHighestValue(7)
                    .setTimeOffsetMs(0)
                    .setMinDigitsShown(1)
                    .setFontComponentId(dayFontComponent.getComponentId())
                    .setZOrder(4)
                    .setPosition(new PointF(0.12307692f, 0.75384615f))
                    .build();
        NumberComponent monthNumberComponent = new NumberComponent.Builder()
                    .setComponentId(24)
                    .setMsPerIncrement(TimeUnit.DAYS.toMillis(30L))
                    .setLowestValue(1)
                    .setHighestValue(12)
                    .setTimeOffsetMs(0)
                    .setMinDigitsShown(1)
                    .setFontComponentId(monthFontComponent.getComponentId())
                    .setZOrder(4)
                    .setPosition(new PointF(0.37435897f, 0.75384615f))
                    .build();
        NumberComponent dateNumberComponent = new NumberComponent.Builder()
                    .setComponentId(25)
                    .setMsPerIncrement(TimeUnit.DAYS.toMillis(1L))
                    .setLowestValue(0)
                    .setHighestValue(30)
                    .setTimeOffsetMs(0)
                    .setMinDigitsShown(1)
                    .setFontComponentId(dateFontComponent.getComponentId())
                    .setZOrder(4)
                    .setPosition(new PointF(0.61282051f, 0.75384615f))
                    .build();

        return new WatchFaceDecomposition.Builder()
                    .addImageComponents(new ImageComponent[] { backgroundImageComponent, ringImageComponent, logoImageComponent, hourColonImageComponent,  commaComponent, minColonImageComponent})
                    .addNumberComponents(new NumberComponent[] {hourNumberComponent, minNumberComponent, secNumberComponent, dayNumberComponent, monthNumberComponent, dateNumberComponent})
                    .addFontComponents(new FontComponent[] { bigNumberFontComponent, /*smallNumberFontComponent,*/ dateFontComponent, dayFontComponent,  monthFontComponent})
                    .build();
        }
}
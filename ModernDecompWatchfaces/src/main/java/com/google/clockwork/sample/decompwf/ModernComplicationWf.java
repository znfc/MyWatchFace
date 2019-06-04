package com.google.clockwork.sample.decompwf;

import static android.support.wearable.complications.ComplicationData.TYPE_ICON;
import static android.support.wearable.complications.ComplicationData.TYPE_RANGED_VALUE;
import static android.support.wearable.complications.ComplicationData.TYPE_SHORT_TEXT;
import static android.support.wearable.complications.ComplicationData.TYPE_SMALL_IMAGE;
import static android.support.wearable.watchface.decomposition.ImageComponent.Builder.HOUR_HAND;
import static android.support.wearable.watchface.decomposition.ImageComponent.Builder.MINUTE_HAND;
import static android.support.wearable.watchface.decomposition.ImageComponent.Builder.TICKING_SECOND_HAND;

import android.content.Context;
import android.graphics.RectF;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.SystemProviders;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.decomposition.ComplicationComponent;
import android.support.wearable.watchface.decomposition.ImageComponent;
import android.support.wearable.watchface.decomposition.WatchFaceDecomposition;
import android.support.wearable.watchface.decompositionface.DecompositionWatchFaceService;

public class ModernComplicationWf extends DecompositionWatchFaceService {
  static WatchFaceDecomposition createDecomposition(Context context) {
    final float W = 454.0f;
    final float H = 454.0f;

    ImageComponent background =
        new ImageComponent.Builder()
            .setComponentId(1)
            .setZOrder(1)
            .setImage(Icon.createWithResource(context, R.drawable.bg3))
            .build();

    ImageComponent hourHand =
        new ImageComponent.Builder(HOUR_HAND)
            .setComponentId(10)
            .setZOrder(3)
            .setImage(Icon.createWithResource(context, R.drawable.hourhand_modern21))
            .setBounds(new RectF(179 / W, 81 / H, 211 / W, 229 / H))
            .build();

    ImageComponent minuteHand =
        new ImageComponent.Builder(MINUTE_HAND)
            .setComponentId(12)
            .setZOrder(4)
            .setImage(Icon.createWithResource(context, R.drawable.minutehand_modern32))
            .setBounds(new RectF(181 / W, 35 / H, 209 / W, 227 / H))
            .build();

    ImageComponent secondHand =
        new ImageComponent.Builder(TICKING_SECOND_HAND)
            .setComponentId(15)
            .setZOrder(5)
            .setImage(Icon.createWithResource(context, R.drawable.secondhand_modern32))
            .setBounds(new RectF(181 / W, 19 / H, 209 / W, 209 / H))
            .build();

    ComplicationDrawable compDrawable = new ComplicationDrawable(context);
    compDrawable.setBorderStyleActive(ComplicationDrawable.BORDER_STYLE_NONE);
    compDrawable.setBorderStyleAmbient(ComplicationDrawable.BORDER_STYLE_NONE);

    int color = 0xFF00B6FF;
    compDrawable.setTextColorActive(color);
    compDrawable.setTitleColorActive(color);
    compDrawable.setIconColorActive(color);
    compDrawable.setRangedValuePrimaryColorActive(color);
    compDrawable.setHighlightColorActive(color);

    ComplicationComponent complicationLeft =
        new ComplicationComponent.Builder()
            .setWatchFaceComplicationId(1)
            .setComponentId(101)
            .setZOrder(2)
            .setBounds(new RectF(0.16f, 0.375f, 0.41f, 0.625f))
            .setComplicationTypes(TYPE_RANGED_VALUE, TYPE_SHORT_TEXT, TYPE_SMALL_IMAGE, TYPE_ICON)
            .setDefaultSystemProvider(SystemProviders.DATE, TYPE_SHORT_TEXT)
            .setComplicationDrawable(compDrawable)
            .build();

    ComplicationComponent complicationTop =
        new ComplicationComponent.Builder()
            .setWatchFaceComplicationId(2)
            .setComponentId(102)
            .setZOrder(2)
            .setBounds(new RectF(0.375f, 0.16f, 0.625f, 0.41f))
            .setComplicationTypes(TYPE_RANGED_VALUE, TYPE_SHORT_TEXT, TYPE_SMALL_IMAGE, TYPE_ICON)
            .setDefaultSystemProvider(SystemProviders.WORLD_CLOCK, TYPE_SHORT_TEXT)
            .setComplicationDrawable(compDrawable)
            .build();

    ComplicationComponent complicationRight =
        new ComplicationComponent.Builder()
            .setWatchFaceComplicationId(3)
            .setComponentId(103)
            .setZOrder(2)
            .setBounds(new RectF(0.59f, 0.375f, 0.84f, 0.625f))
            .setComplicationTypes(TYPE_RANGED_VALUE, TYPE_SHORT_TEXT, TYPE_SMALL_IMAGE, TYPE_ICON)
            .setDefaultSystemProvider(SystemProviders.WATCH_BATTERY, TYPE_RANGED_VALUE)
            .setComplicationDrawable(compDrawable)
            .build();

    ComplicationComponent complicationBottom =
        new ComplicationComponent.Builder()
            .setWatchFaceComplicationId(4)
            .setComponentId(104)
            .setZOrder(2)
            .setBounds(new RectF(0.375f, 0.59f, 0.625f, 0.84f))
            .setComplicationTypes(TYPE_RANGED_VALUE, TYPE_SHORT_TEXT, TYPE_SMALL_IMAGE, TYPE_ICON)
            .setDefaultSystemProvider(SystemProviders.NEXT_EVENT, TYPE_SHORT_TEXT)
            .setComplicationDrawable(compDrawable)
            .build();

    return new WatchFaceDecomposition.Builder()
        .addImageComponents(background, hourHand, minuteHand, secondHand)
        .addComplicationComponents(
            complicationLeft, complicationTop, complicationRight, complicationBottom)
        .build();
  }

  @Override
  public WatchFaceDecomposition buildDecomposition() {
    return createDecomposition(this);
  }
}

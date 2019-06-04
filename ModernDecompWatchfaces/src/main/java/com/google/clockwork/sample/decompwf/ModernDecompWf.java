package com.google.clockwork.sample.decompwf;

import static android.support.wearable.watchface.decomposition.ImageComponent.Builder.HOUR_HAND;
import static android.support.wearable.watchface.decomposition.ImageComponent.Builder.MINUTE_HAND;
import static android.support.wearable.watchface.decomposition.ImageComponent.Builder.TICKING_SECOND_HAND;

import android.content.Context;
import android.graphics.RectF;
import android.graphics.drawable.Icon;
import android.support.wearable.watchface.decomposition.ImageComponent;
import android.support.wearable.watchface.decomposition.WatchFaceDecomposition;
import android.support.wearable.watchface.decompositionface.DecompositionWatchFaceService;

public class ModernDecompWf extends DecompositionWatchFaceService {
  static WatchFaceDecomposition createDecomposition(Context context) {
    final float W = 390.0f;
    final float H = 390.0f;

    ImageComponent background =
        new ImageComponent.Builder()
            .setComponentId(1)
            .setZOrder(1)
            .setImage(Icon.createWithResource(context, R.drawable.bg3))
            .build();

    ImageComponent hourHand =
        new ImageComponent.Builder(HOUR_HAND)
            .setComponentId(10)
            .setZOrder(2)
            .setImage(Icon.createWithResource(context, R.drawable.hourhand_modern))
            .setBounds(new RectF(179 / W, 81 / H, 211 / W, 229 / H))
            .build();

    ImageComponent minuteHand =
        new ImageComponent.Builder(MINUTE_HAND)
            .setComponentId(12)
            .setZOrder(3)
            .setImage(Icon.createWithResource(context, R.drawable.minutehand_modern))
            .setBounds(new RectF(181 / W, 35 / H, 209 / W, 227 / H))
            .build();

    ImageComponent secondHand =
        new ImageComponent.Builder(TICKING_SECOND_HAND)
            .setComponentId(15)
            .setZOrder(4)
            .setImage(Icon.createWithResource(context, R.drawable.secondhand_modern))
            .setBounds(new RectF(181 / W, 19 / H, 209 / W, 209 / H))
            .build();

    return new WatchFaceDecomposition.Builder()
        .addImageComponents(background, hourHand, minuteHand, secondHand)
        .build();
  }

  @Override
  public WatchFaceDecomposition buildDecomposition() {
    return createDecomposition(this);
  }
}

/**
 * Copyright (c) 2017 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.sidekickwatchface;

import android.graphics.PointF;
import com.google.android.clockwork.decomposablewatchface.ImageComponent;
import com.google.android.clockwork.decomposablewatchface.FontComponent;
import com.google.android.clockwork.decomposablewatchface.NumberComponent;
import com.google.android.clockwork.decomposablewatchface.WatchFaceDecomposition;
import android.util.Log;
//import com.google.android.clockwork.sidekick.SidekickManager;
//import com.google.android.clockwork.watchface.decomposed.ImageComponentBridge;
//import com.google.android.clockwork.watchface.decomposed.FontComponentBridge;
//import com.google.android.clockwork.watchface.decomposed.NumberComponentBridge;
//import com.google.android.clockwork.watchface.decomposed.WatchFaceBridge;

public class OffloadController {

//  private final SidekickManager sidekickManager;

  public OffloadController() {
//    sidekickManager = new SidekickManager();
  }
//
  public void sendDecomposition(WatchFaceDecomposition decomposition, boolean forTWM) {
//    sidekickManager.sendWatchFace(convertDecomposition(decomposition), forTWM);
    Log.i("zhao11","forTWM:"+forTWM);
  }
//
  public void clearDecomposition() {
//    sidekickManager.clearWatchFace();
  }
//
//  private ImageComponentBridge convertImageComponent(ImageComponent component) {
//    PointF pivot = component.getPivot();
//    if (pivot == null) {
//      pivot = new PointF(0.0f, 0.0f);
//    }
//    return new ImageComponentBridge.Builder()
//        .setComponentId(component.getComponentId())
//        .setImage(component.getImage())
//        .setBounds(component.getBounds())
//        .setZOrder(component.getZOrder())
//        .setDegreesPerDay(component.getDegreesPerDay())
//        .setPivot(pivot)
//        .setOffsetDegrees(component.getOffsetDegrees())
//        .build();
//  }
//
//  private FontComponentBridge convertFontComponent(FontComponent component) {
//
//    return new FontComponentBridge.Builder()
//        .setComponentId(component.getComponentId())
//        .setImage(component.getImage())
//        .setDigitCount(component.getDigitCount())
//        .setDigitDimensions(component.getDigitDimensions())
//        .build();
//  }
//
//  private NumberComponentBridge convertNumberComponent(NumberComponent component) {
//
//    return new NumberComponentBridge.Builder()
//        .setComponentId(component.getComponentId())
//        .setMsPerIncrement(component.getMsPerIncrement())
//        .setLowestValue(component.getLowestValue())
//        .setHighestValue(component.getHighestValue())
//        .setTimeOffsetMs(component.getTimeOffsetMs())
//        .setFontComponentId(component.getFontComponentId())
//        .setZOrder(component.getZOrder())
//        .setPosition(component.getPosition())
//        .build();
//  }
//
//  private WatchFaceBridge convertDecomposition(WatchFaceDecomposition decomposition) {
//    WatchFaceBridge.Builder builder = new WatchFaceBridge.Builder();
//
//    for (ImageComponent imageComponent : decomposition.getImageComponents()) {
//      builder.addImageComponents(convertImageComponent(imageComponent));
//    }
//    for (NumberComponent numberComponent : decomposition.getNumberComponents()) {
//      builder.addNumberComponents(convertNumberComponent(numberComponent));
//    }
//    for (FontComponent fontComponent : decomposition.getFontComponents()) {
//      builder.addFontComponents(convertFontComponent(fontComponent));
//    }
//
//    return builder.build();
//  }

}

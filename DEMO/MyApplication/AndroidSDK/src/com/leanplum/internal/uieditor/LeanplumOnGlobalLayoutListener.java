// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.app.Activity;
import android.view.ViewTreeObserver;

import com.leanplum.internal.Util;

/**
 * Listens for global layout changes and triggers sending update and applying ui updates.
 *
 * @author Ben Marten
 */
public class LeanplumOnGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
  private Activity activity;

  /**
   * Constructor for the global layout listener that saves a reference to the activity.
   *
   * @param activity the activity to observe ui changes.
   */
  public LeanplumOnGlobalLayoutListener(Activity activity) {
    this.activity = activity;
  }

  @Override
  public void onGlobalLayout() {
    try {
      if (!LeanplumViewManager.getIsUpdateInProgress()) {
        LeanplumListenerSwizzler.swizzleScrollViewListenersDelayed(activity);
        LeanplumInterfaceEditor.sendUpdateDelayed(
            LeanplumInterfaceEditor.UPDATE_DELAY_LAYOUT_CHANGE);
        LeanplumViewManager.applyInterfaceAndEventUpdateRulesDelayed(activity);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }
}

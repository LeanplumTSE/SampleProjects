// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import com.leanplum.internal.CollectionUtil;
import com.leanplum.internal.Log;

import java.util.Map;

/**
 * Custom Property class containing all custom properties.
 *
 * @author Ben Marten
 */
public class LeanplumCustomProperties {
  /**
   * Returns the absolute frame of a view as a map.
   *
   * @param view The view to handle.
   * @return A map containing, "x", "y", "width" and "height" values.
   */
  public static Map<String, Number> getAbsoluteFrame(final View view) {
    final int[] location = new int[] {0, 0};
    view.getLocationOnScreen(location);
    return CollectionUtil.newHashMap(
        "x", location[0],
        "y", location[1],
        "width", view.getWidth(),
        "height", view.getHeight()
    );
  }

  /**
   * Returns the relative frame of a view as a map.
   *
   * @param view The view to handle.
   * @return A map containing, "x", "y", "width" and "height" values.
   */
  public static Map<String, Number> getFrame(final View view) {
    return CollectionUtil.newHashMap(
        "x", view.getLeft(),
        "y", view.getTop(),
        "width", view.getWidth(),
        "height", view.getHeight()
    );
  }

  /**
   * Sets the frame of a view based on a map.
   *
   * @param view The view to manipulate.
   * @param frame A map containing, "x", "y", "width" and "height" values.
   */
  @TargetApi(11)
  public static void setFrame(final View view, Map<String, Number> frame) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      view.setX(frame.get("x").intValue());
      view.setY(frame.get("y").intValue());
      ViewGroup.LayoutParams params = view.getLayoutParams();
      params.width = frame.get("width").intValue();
      params.height = frame.get("height").intValue();
      view.requestLayout();
    } else {
      Log.w("Frame not set, API level is lower then 11.");
    }
  }
}

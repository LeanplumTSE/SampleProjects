// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * Helper Class for the ViewPager class.
 *
 * @author Ben Marten
 */
public class ViewPagerUtil {
  /**
   * Get the current view at the given position.
   *
   * @param viewPager The view pager to receive the view from.
   * @param currentPosition The position to retrieve the view for.
   * @return The view for the given position.
   */
  public static View getViewAtPosition(final ViewPager viewPager, int currentPosition) {
    final PagerAdapter adapter = viewPager.getAdapter();
    if (null == adapter || adapter.getCount() == 0 || viewPager.getChildCount() == 0) {
      return null;
    }

    Integer position;

    for (int i = 0; i < viewPager.getChildCount(); i++) {
      final View child = viewPager.getChildAt(i);
      final ViewPager.LayoutParams layoutParams = (ViewPager.LayoutParams) child.getLayoutParams();
      if (layoutParams.isDecor) {
        continue;
      }
      position = ClassUtil.getIntegerField(layoutParams, "position");
      if (position == null) {
        break;
      } else if (position == currentPosition) {
        return child;
      }
    }
    return null;
  }
}

// Copyright 2014, Leanplum, Inc.

package com.leanplum.utils;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * Utilities for converting between different size units.
 * @author Martin Yanakiev
 */
public class SizeUtil {
  public static int dp2;
  public static int dp30;
  public static int dp5;
  public static int dp20;
  public static int dp18;
  public static int dp16;
  public static int dp14;
  public static int dp10;
  public static int dp7;
  public static int dp100;
  public static int dp200;
  public static int dp250;
  public static int dp50;

  public static final int textSize0_2 = 16;
  public static final int textSize0_1 = 18;
  public static final int textSize0 = 20;
  public static final int textSize1 = 22;
  public static final int textSize2 = 24;

  private static boolean hasInited = false;

  public static void init(Context context) {
    if (hasInited) {
      return;
    }
    hasInited = true;
    dp30 = dpToPx(context, 30);
    dp5 = dpToPx(context, 5);
    dp20 = dpToPx(context, 20);
    dp10 = dpToPx(context, 10);
    dp7 = dpToPx(context, 7);
    dp18 = dpToPx(context, 18);
    dp16 = dpToPx(context, 16);
    dp14 = dpToPx(context, 14);
    dp100 = dpToPx(context, 100);
    dp200 = dpToPx(context, 200);
    dp250 = dpToPx(context, 250);
    dp2 = dpToPx(context, 2);
    dp50 = dpToPx(context, 50);
  }

  public static int dpToPx(Context context, int dp) {
    init(context);
    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    int px = Math.round(dp
        * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    return px;
  }

  public static int pxToDp(Context context, int px) {
    init(context);
    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    int dp = Math.round(px
        / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    return dp;
  }

  public static int spToPx(Context context, int sp) {
    init(context);
    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    return (int) (sp * displayMetrics.scaledDensity);
  }

  public static int pxToSp(Context context, int px) {
    init(context);
    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    return (int) (px / displayMetrics.scaledDensity);
  }

  public static int getStatusBarHeight(Activity activity) {
    init(activity);
    boolean full = (activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == WindowManager.LayoutParams.FLAG_FULLSCREEN;
    if (full) {
      return 0;
    }
    int result = 0;
    int resourceId = activity.getResources().getIdentifier("status_bar_height",
        "dimen", "android");
    if (resourceId > 0) {
      result = activity.getResources().getDimensionPixelSize(resourceId);
    }
    return result;
  }
}

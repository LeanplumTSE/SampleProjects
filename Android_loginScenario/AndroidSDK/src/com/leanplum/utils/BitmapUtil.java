// Copyright 2014, Leanplum, Inc.

package com.leanplum.utils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.view.View;

/**
 * Bitmap manipulation utilities.
 * @author Martin Yanakiev
 */
public class BitmapUtil {
  public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
    if (bitmap == null) {
      return null;
    }

    Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
        Config.ARGB_8888);
    android.graphics.Canvas canvas = new android.graphics.Canvas(output);

    final int color = 0xff000000;
    final android.graphics.Paint paint = new android.graphics.Paint();
    final android.graphics.Rect rect = new android.graphics.Rect(0, 0,
        bitmap.getWidth(), bitmap.getHeight());
    final android.graphics.RectF rectF = new android.graphics.RectF(rect);
    final float roundPx = pixels;

    paint.setAntiAlias(true);
    canvas.drawARGB(0, 0, 0, 0);
    paint.setColor(color);
    canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

    paint.setXfermode(new android.graphics.PorterDuffXfermode(Mode.SRC_IN));
    canvas.drawBitmap(bitmap, rect, rect, paint);

    return output;
  }

  public static void stateBackgroundDarkerByPercentage(View v,
      int normalStateColor, int percentage) {
    int darker = getDarker(normalStateColor, percentage);
    stateBackground(v, normalStateColor, darker);
  }

  public static int getDarker(int color, int percentage) {
    if (percentage < 0 || percentage > 100)
      percentage = 0;
    double d = ((100 - percentage) / (double) 100);
    int a = (color >> 24) & 0xFF;
    int r = (int) (((color >> 16) & 0xFF) * d) & 0xFF;
    int g = (int) (((color >> 8) & 0xFF) * d) & 0xFF;
    int b = (int) ((color & 0xFF) * d) & 0xFF;
    a = a << 24;
    r = r << 16;
    g = g << 8;
    return a | r | g | b;
  }

  public static void stateBackground(View v, int normalStateColor,
      int pressedStateColor) {
    if (Build.VERSION.SDK_INT >= 16) {
      v.setBackground(getBackground(normalStateColor, pressedStateColor));
    } else {
      v.setBackgroundColor(normalStateColor);
    }
  }

  private static Drawable getBackground(int normalStateColor,
      int pressedStateColor) {
    StateListDrawable background = new StateListDrawable();
    int c = SizeUtil.dp10;
    float[] r = new float[] { c, c, c, c, c, c, c, c };
    RoundRectShape rr = new RoundRectShape(r, null, null);
    ShapeDrawable cd = new ShapeDrawable();
    cd.setShape(rr);
    cd.getPaint().setColor(pressedStateColor);
    background.addState(new int[] { android.R.attr.state_pressed,
        android.R.attr.state_focused }, cd);
    background.addState(new int[] { -android.R.attr.state_pressed,
        android.R.attr.state_focused }, cd);
    background.addState(new int[] { android.R.attr.state_pressed,
        -android.R.attr.state_focused }, cd);
    ShapeDrawable cd1 = new ShapeDrawable();
    cd1.setShape(rr);
    cd1.getPaint().setColor(normalStateColor);
    background.addState(new int[] { -android.R.attr.state_pressed,
        -android.R.attr.state_focused }, cd1);
    return background;
  }
}

// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.util.Base64;
import android.view.View;

import com.leanplum.internal.ClassUtil;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;
import com.leanplum.internal.uieditor.model.ViewRootData;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static android.graphics.Bitmap.Config.ARGB_8888;

/**
 * Helper Class to get screenshots of views.
 *
 * @author Ben Marten
 */
public class ScreenshotUtil {
  // From WindowManager.java
  private static final int FLAG_DIM_BEHIND = 0x00000002;
  private static final int PNG_IMAGE_SIZE_LIMIT = 250000;

  /**
   * Creates a base64 string screenshot of a view.
   *
   * @param activity - The activity to take a screenshot of
   * @return String - base64 encoded image
   */
  public static String getScreenshotBase64(Activity activity) {
    Bitmap bitmap = ScreenshotUtil.captureScreenShot(activity);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    if (bitmap == null) {
      return null;
    }
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
    byte[] byteArray = byteArrayOutputStream.toByteArray();

    if (byteArray.length > PNG_IMAGE_SIZE_LIMIT) {
      byteArrayOutputStream = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);
      byteArray = byteArrayOutputStream.toByteArray();
    }

    return Base64.encodeToString(byteArray, Base64.NO_WRAP);
  }

  /**
   * Creates a bitmap screenshot of the activity.
   *
   * @param activity the activity
   * @return Bitmap - the screenshot
   */
  public static Bitmap captureScreenShot(Activity activity) {
    if (activity == null) {
      throw new IllegalArgumentException("Parameter activity cannot be null.");
    }

    try {
      return takeBitmapUnchecked(activity);
    } catch (InterruptedException e) {
      Log.p("Unable to take screenshot of activity " + activity.getClass().getName());
    }

    return null;
  }

  private static Bitmap takeBitmapUnchecked(Activity activity) throws InterruptedException {
    final List<ViewRootData> viewRoots = ClassUtil.getRootViews(activity);
    View main = activity.getWindow().getDecorView();
    final Bitmap bitmap = Bitmap.createBitmap(main.getWidth(), main.getHeight(), ARGB_8888);
    if (viewRoots == null) {
      return bitmap;
    }
    try {
      // Need to run on main thread
      if (Looper.myLooper() == Looper.getMainLooper()) {
        drawRootsToBitmap(viewRoots, bitmap);
      } else {
        final CountDownLatch latch = new CountDownLatch(1);
        activity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            try {
              drawRootsToBitmap(viewRoots, bitmap);
            } catch (Throwable t) {
              Util.handleException(t);
            } finally {
              latch.countDown();
            }
          }
        });

        latch.await();
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }

    return bitmap;
  }

  private static void drawRootsToBitmap(List<ViewRootData> viewRoots, Bitmap bitmap) {
    for (ViewRootData rootData : viewRoots) {
      drawRootToBitmap(rootData, bitmap);
    }
  }

  private static void drawRootToBitmap(ViewRootData config, Bitmap bitmap) {
    try {
      // Manually dim the layer if its not the first layer (e.g. background to dialog).
      if ((config.getLayoutParams().flags & FLAG_DIM_BEHIND) == FLAG_DIM_BEHIND) {
        Canvas dimCanvas = new Canvas(bitmap);

        int alpha = (int) (255 * config.getLayoutParams().dimAmount);
        dimCanvas.drawARGB(alpha, 0, 0, 0);
      }

      Canvas canvas = new Canvas(bitmap);
      canvas.translate(config.getWinFrame().left, config.getWinFrame().top);
      config.getView().draw(canvas);
    } catch (NullPointerException e) {
      // TODO(Ben): find & fix root cause
      Log.p("Error taking screenshot: " + e.getMessage());
    }
  }

}

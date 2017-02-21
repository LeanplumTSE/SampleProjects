// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.app.Activity;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.view.OrientationEventListener;
import android.view.View;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.internal.CollectionUtil;
import com.leanplum.internal.Log;
import com.leanplum.internal.Socket;
import com.leanplum.internal.Util;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Interface Editor Class for handling UI updates.
 *
 * @author Ben Marten
 */
public class LeanplumInterfaceEditor {
  public static final int UPDATE_DELAY_SCROLL = 1000;
  public static final int UPDATE_DELAY_LAYOUT_CHANGE = 500;
  public static final int UPDATE_DELAY_ACTIVITY_CHANGE = 200;
  public static final int UPDATE_DELAY_ORIENTATION_CHANGE = 1000;
  private static final int UPDATE_DELAY_DEFAULT = 100;
  private static LeanplumEditorMode mode;
  private static boolean isUpdating = false;
  private static Handler delayedUpdateHandler;
  private static Runnable delayedUpdateRunnable;
  private static OrientationEventListener orientationListener;

  /**
   * Sets the update flag to true.
   */
  public static void startUpdating() {
    isUpdating = true;
    enableOrientationChangeListener();
  }

  /**
   * Sets the update flag to false.
   */
  public static void stopUpdating() {
    isUpdating = false;
    disableOrientationChangeListener();
  }

  /**
   * Send an immediate update of the UI to the LP server.
   */
  public static void sendUpdate() {
    sendUpdateDelayed(0);
  }

  /**
   * Send an update with given delay of the UI to the LP server.
   */
  public static void sendUpdateDelayed(int delay) {
    if (!isUpdating) {
      Log.p("Updating disabled or currently sending update already.");
      return;
    }

    if (delayedUpdateHandler == null) {
      Log.p("Canceled existing scheduled update thread.");
      delayedUpdateHandler = new Handler(Looper.getMainLooper());
    }

    if (delayedUpdateRunnable != null) {
      Log.p("Canceled existing scheduled update thread.");
      delayedUpdateHandler.removeCallbacks(delayedUpdateRunnable);
    }

    try {
      // Create a Runnable object that executes the SendUpdateTask, after a certain delay.
      delayedUpdateRunnable = new Runnable() {
        @Override
        public void run() {
          try {
            new SendUpdateTask().execute();
          } catch (Throwable t) {
            Util.handleException(t);
          }
        }
      };
      boolean success = delayedUpdateHandler.postDelayed(delayedUpdateRunnable, delay);
      if (!success) {
        Log.p("Could not post update delayed runnable.");
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Send an update of the UI to the LP server, delayed by the default time.
   */
  public static void sendUpdateDelayedDefault() {
    sendUpdateDelayed(UPDATE_DELAY_DEFAULT);
  }

  /**
   * Returns the current editor mode.
   *
   * @return The current editor mode.
   */
  public static LeanplumEditorMode getMode() {
    return mode;
  }

  /**
   * Sets the current editor mode.
   *
   * @param mode The editor mode to set.
   */
  public static void setMode(LeanplumEditorMode mode) {
    LeanplumInterfaceEditor.mode = mode;
  }

  /**
   * Enables listening for orientation change.
   */
  public static void enableOrientationChangeListener() {
    if (orientationListener == null) {
      Activity currentActivity = LeanplumActivityHelper.getCurrentActivity();
      if (currentActivity == null) {
        return;
      }
      orientationListener = new OrientationEventListener(
          currentActivity.getApplication().getApplicationContext(),
          SensorManager.SENSOR_DELAY_UI
      ) {
        public void onOrientationChanged(int orientation) {
          LeanplumInterfaceEditor.sendUpdateDelayed(
              LeanplumInterfaceEditor.UPDATE_DELAY_ORIENTATION_CHANGE
          );
        }
      };
    }
    orientationListener.enable();
  }

  /**
   * Disables listening for orientation changes.
   */
  public static void disableOrientationChangeListener() {
    if (orientationListener != null) {
      orientationListener.disable();
      orientationListener = null;
    }
  }

  /**
   * Async Task to collect view info on UI thread and send to server in background.
   */
  static class SendUpdateTask extends AsyncTask<Void, Void, Void> {
    private Activity activity;
    private String screenshotBase64;
    private Integer rootViewWidth;
    private Integer rootViewHeight;
    private Map<String, List> viewHierarchy;

    /**
     * Get all the view information by running scanning on the UI thread.
     */
    @Override
    protected void onPreExecute() {
      try {
        activity = LeanplumActivityHelper.getCurrentActivity();
        View rootView = activity.getWindow().getDecorView().getRootView();
        rootViewWidth = rootView.getWidth();
        rootViewHeight = rootView.getHeight();
        viewHierarchy = LeanplumHierarchyScanner.scanViewTree(activity);
      } catch (Throwable t) {
        Util.handleException(t);
      }
    }

    protected Void doInBackground(Void... params) {
      if (activity == null) {
        Log.p("Can't send update, current activity can't be resolved.");
      } else if (!Socket.getInstance().isConnected()) {
        Log.p("Can't send update, socket not connected.");
      } else if (!Leanplum.isInterfaceEditingEnabled()) {
        Socket.getInstance().sendEvent("viewHierarchy",
            CollectionUtil.<String, Object>newHashMap("error", "editingDisabled"));
      } else {
        screenshotBase64 = ScreenshotUtil.getScreenshotBase64(activity);
        if (screenshotBase64 == null) {
          Log.p("Error capturing screenshot!");
        } else {
          Log.p("Sending view hierarchy.");
          Socket.getInstance().sendEvent("viewChanged",
              Collections.<String, String>emptyMap());
          Map<String, Object> data = CollectionUtil.newHashMap(
              "imageData", screenshotBase64,
              "viewHierarchy", viewHierarchy,
              "width", rootViewWidth,
              "height", rootViewHeight);
          Socket.getInstance().sendEvent("viewHierarchy", data);
        }
      }
      return null;
    }
  }
}

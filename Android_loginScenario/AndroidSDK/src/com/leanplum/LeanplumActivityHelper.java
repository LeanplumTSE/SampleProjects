// Copyright 2013, Leanplum, Inc.

package com.leanplum;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;

import com.leanplum.annotations.Parser;
import com.leanplum.callbacks.VariablesChangedCallback;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Utility class for handling activity lifecycle events.
 * Call these methods from your activity if you don't extend
 * one of the Leanplum*Activity classes.
 * 
 * @author Andrew First
 */
public class LeanplumActivityHelper {
  /**
   * Whether any of the activities are paused.
   */
  static boolean isActivityPaused = false;

  /**
   * Whether lifecycle callbacks were registered.
   * This is only supported on Android OS >= 4.0.
   */
  private static boolean registeredCallbacks;

  static Activity currentActivity;

  private Activity activity;
  private LeanplumResources res;
  private LeanplumInflater inflater;

  private static final Queue<VariablesChangedCallback> pendingActions =
      new LinkedList<VariablesChangedCallback>();

  public LeanplumActivityHelper(Activity activity) {
    this.activity = activity;
    Leanplum.setApplicationContext(activity.getApplicationContext());
    Parser.parseVariables(activity);
  }

  /**
   * Retrieves the currently active activity.
   */
  public static Activity getCurrentActivity() {
    return currentActivity;
  }
  
  /**
   * Enables lifecycle callbacks for Android devices with Android OS >= 4.0
   */
  public static void enableLifecycleCallbacks(Application app) {
    Leanplum.setApplicationContext(app.getApplicationContext());
    if (Build.VERSION.SDK_INT < 14) {
      return;
    }
    app.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
      @Override
      public void onActivityStopped(Activity activity) {
        try {
          onStop(activity);
        } catch (Throwable t) {
          Util.handleException(t);
        }
      }

      @Override
      public void onActivityResumed(Activity activity) {
        try {
          onResume(activity);
        } catch (Throwable t) {
          Util.handleException(t);
        }
      }

      @Override
      public void onActivityPaused(Activity activity) {
        try {
          onPause(activity);
        } catch (Throwable t) {
          Util.handleException(t);
        }
      }

      @Override public void onActivityStarted(Activity activity) {}
      @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
      @Override public void onActivityDestroyed(Activity activity) {}
      @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
    });
    registeredCallbacks = true;
  }

  public LeanplumResources getLeanplumResources() {
    return getLeanplumResources(null);
  }

  public LeanplumResources getLeanplumResources(Resources baseResources) {
    if (res != null) {
      return res;
    }
    if (baseResources == null) {
      baseResources = activity.getResources();
    }
    if (baseResources instanceof LeanplumResources) {
      return (LeanplumResources) baseResources;
    }
    res = new LeanplumResources(baseResources);
    return res;
  }

  /**
   * Sets the view from a layout file.
   */
  public void setContentView(final int layoutResID) {
    if (inflater == null) {
      inflater = LeanplumInflater.from(activity);
    }
    activity.setContentView(inflater.inflate(layoutResID));
  }
  
  private static void onPause(Activity activity) {
    isActivityPaused = true;
  }

  /**
   * Call this when your activity gets paused.
   */
  public void onPause() {
    try {
      if (!registeredCallbacks) {
        onPause(activity);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  private static void onResume(Activity activity) {
    isActivityPaused = false;
    currentActivity = activity;
    if (Leanplum.isPaused || Leanplum.startedInBackground) {
      Leanplum.resume();
      LocationManager locationManager = ActionManager.getLocationManager();
      if (locationManager != null) {
        locationManager.updateGeofencing();
      }
    }
    runPendingActions();
  }

  /**
   * Call this when your activity gets resumed.
   */
  public void onResume() {
    try {
      if (!registeredCallbacks) {
        onResume(activity);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }
  
  private static void onStop(Activity activity) {
    // onStop is called when the activity gets hidden, and is
    // called after onPause.
    //
    // However, if we're switching to another activity, that activity
    // will call onResume, so we shouldn't pause if that's the case.
    //
    // Thus, we can call pause from here, only if all activities are paused.
    if (isActivityPaused) {
      Leanplum.pause();
      LocationManager locationManager = ActionManager.getLocationManager();
      if (locationManager != null) {
        locationManager.updateGeofencing();
      }
    }
    if (currentActivity == activity) {
      // Don't leak activities.
      currentActivity = null;
    }
  }

  /**
   * Call this when your activity gets stopped.
   */
  public void onStop() {
    try {
      if (!registeredCallbacks) {
        onStop(activity);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Enqueues a callback to invoke when an activity reaches in the foreground.
   */
  public static void queueActionUponActive(
      VariablesChangedCallback variablesChangedCallback) {
    try {
      if (currentActivity != null && !currentActivity.isFinishing() && !isActivityPaused) {
        variablesChangedCallback.variablesChanged();
      } else {
        synchronized (pendingActions) {
          pendingActions.add(variablesChangedCallback);
        }
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Runs any pending actions that have been queued.
   */
  private static void runPendingActions() {
    Queue<VariablesChangedCallback> runningActions;
    synchronized (pendingActions) {
      runningActions = new LinkedList<VariablesChangedCallback>(pendingActions);
      pendingActions.clear();
    }
    for (VariablesChangedCallback action : runningActions) {
      action.variablesChanged();
    }
  }
}

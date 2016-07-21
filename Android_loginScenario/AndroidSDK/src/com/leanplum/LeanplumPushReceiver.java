// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.leanplum.Constants.Keys;
import com.leanplum.Constants.Values;
import com.leanplum.callbacks.VariablesChangedCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles push notification intents, for example, by tracking opens and performing the open action.
 *
 * @author Aleksandar Gyorev
 */
public class LeanplumPushReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    try {
      if (intent == null) {
        Log.e("Leanplum", "Received a null intent");
        return;
      }
      LeanplumPushService.openNotification(context, intent.getExtras());
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }
}

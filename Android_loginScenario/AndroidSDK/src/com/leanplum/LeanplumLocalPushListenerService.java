// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Listener Service for local push notifications.
 *
 * @author Aleksandar Gyorev
 */
public class LeanplumLocalPushListenerService extends IntentService {
  public LeanplumLocalPushListenerService() {
    super("LeanplumLocalPushListenerService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    try {
      if (intent == null) {
        Log.e("Leanplum", "The intent cannot be null");
        return;
      }
      Bundle extras = intent.getExtras();
      if (!extras.isEmpty() && extras.containsKey(Constants.Keys.PUSH_MESSAGE_TEXT)) {
        LeanplumPushService.handleNotification(this, extras);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }
}

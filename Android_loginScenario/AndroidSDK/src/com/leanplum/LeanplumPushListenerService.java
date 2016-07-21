// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.leanplum.callbacks.VariablesChangedCallback;
import com.leanplum.Constants.Keys;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * GCM listener service, which enables handling messages on the app's behalf.
 *
 * @author Aleksandar Gyorev
 */
public class LeanplumPushListenerService extends GcmListenerService {
  /**
   * Called when a message is received.
   *
   * @param senderId Sender ID of the sender.
   * @param data Data bundle containing the message data as key-value pairs.
   */
  @Override
  public void onMessageReceived(String senderId, Bundle data) {
    try {
      if (data.containsKey(Keys.PUSH_MESSAGE_TEXT)) {
        LeanplumPushService.handleNotification(this, data);
      }
      Log.i("Leanplum", "Received: " + data.toString());
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }
}

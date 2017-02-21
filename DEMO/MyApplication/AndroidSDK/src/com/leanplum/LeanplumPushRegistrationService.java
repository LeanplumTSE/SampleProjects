// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.firebase.iid.FirebaseInstanceId;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

import java.io.IOException;

/**
 * Registration service that handles registration with the Google Cloud Messaging services, using
 * the GCM sender ID.
 *
 * @author Aleksandar Gyorev
 */
public class LeanplumPushRegistrationService extends IntentService {
  private static String existingRegistrationId;

  public LeanplumPushRegistrationService() {
    super("LeanplumPushRegistrationService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    LeanplumCloudMessagingProvider provider = LeanplumPushService.getCloudMessagingProvider();
    if (provider == null) {
      Log.e("Failed to complete registration token refresh.");
      return;
    }
    String registrationId = provider.getRegistrationId();
    if (registrationId != null) {
      if (existingRegistrationId != null && !registrationId.equals(existingRegistrationId)) {
        Log.e("WARNING: It appears your app is registering " +
            "with GCM/FCM using multiple GCM/FCM sender ids. Please be sure to call " +
            "LeanplumPushService.setGcmSenderIds() with " +
            "all of the GCM sender ids that you use, not just the one that you use with " +
            "Leanplum. Otherwise, GCM/FCM push notifications may not work consistently.");
      }
      existingRegistrationId = registrationId;
      provider.onRegistrationIdReceived(getApplicationContext(), registrationId);
    }
  }
}

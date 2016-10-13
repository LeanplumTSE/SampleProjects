// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

/**
 * Registration service that handles registration with the Google Cloud Messaging services, using
 * the GCM sender ID.
 *
 * @author Aleksandar Gyorev
 */
public class LeanplumPushRegistrationService extends IntentService {
  private static final String ERROR_TIMEOUT = "TIMEOUT";
  private static final String ERROR_INVALID_SENDER= "INVALID_SENDER";
  private static final String ERROR_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
  private static final String ERROR_PHONE_REGISTRATION_ERROR = "PHONE_REGISTRATION_ERROR";
  private static final String ERROR_TOO_MANY_REGISTRATIONS = "TOO_MANY_REGISTRATIONS";

  private static String existingRegistrationId;

  public LeanplumPushRegistrationService() {
    super("LeanplumPushRegistrationService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    // If getToken gets called in the main thread it may cause a TIMEOUT exception, which is why we
    // run it in a separate one.
    Runnable gcmRegistration = new Runnable() {
      @Override
      public void run() {
        try {
          InstanceID instanceID = InstanceID.getInstance(LeanplumPushRegistrationService.this);
          String registrationId = instanceID.getToken(LeanplumPushService.getGcmSenderIds(),
              GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
          if (registrationId != null) {
            if (existingRegistrationId != null && !registrationId.equals(existingRegistrationId)) {
              Log.e("Leanplum", "WARNING: It appears your app is registering with GCM using " +
                  "multiple GCM sender ids. Please be sure to call " +
                  "LeanplumPushService.setGcmSenderIds() with " +
                  "all of the GCM sender ids that you use, not just the one that you use with " +
                  "Leanplum. Otherwise, GCM push notifications may not work consistently.");
            }
            existingRegistrationId = registrationId;
            LeanplumPushService.onRegistrationIdReceived(getApplicationContext(), registrationId);
          }
        } catch (IOException e) {
          if (GoogleCloudMessaging.ERROR_SERVICE_NOT_AVAILABLE.equals(e.getMessage())) {
            Log.w("Leanplum", "GCM service is not available. Will try to register again next " +
                "time the app starts.");
          } else if (ERROR_TIMEOUT.equals(e.getMessage())) {
            Log.w("Leanplum", "Retrieval of GCM registration token timed out. Will try to " +
                "register again next time the app starts.");
          } else if (ERROR_INVALID_SENDER.equals(e.getMessage())) {
            Log.e("Leanplum", "The GCM sender account is not recognized. Please be sure to call " +
                "LeanplumPushService.setGcmSenderId() with a valid GCM sender id.");
          } else if (ERROR_AUTHENTICATION_FAILED.equals(e.getMessage())) {
            Log.w("Leanplum", "Bad Google Account password.");
          } else if (ERROR_PHONE_REGISTRATION_ERROR.equals(e.getMessage())) {
            Log.w("Leanplum", "This phone doesn't currently support GCM.");
          } else if (ERROR_TOO_MANY_REGISTRATIONS.equals(e.getMessage())) {
            Log.w("Leanplum", "This phone has more than the allowed number of apps that are " +
                "registered with GCM.");
          } else {
            Log.e("Leanplum", "Failed to complete registration token refresh.");
            Util.handleException(e);
          }
        } catch (Throwable t) {
          Log.e("Leanplum", "Failed to complete registration token refresh.");
          Util.handleException(t);
        }
      }
    };

    try {
      new Thread(gcmRegistration).start();
    } catch (Throwable t) {
      Log.e("Leanplum", "Failed to complete registration token refresh.");
      Util.handleException(t);
    }
  }
}

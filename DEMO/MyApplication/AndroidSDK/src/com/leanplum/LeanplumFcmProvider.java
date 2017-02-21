// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import com.google.firebase.iid.FirebaseInstanceId;
import com.leanplum.internal.Log;

import java.util.Collections;

/**
 * Leanplum provider for work with Firebase.
 *
 * @author Anna Orlova
 */
class LeanplumFcmProvider extends LeanplumCloudMessagingProvider {
  private static final String INSTANCE_ID_EVENT = "com.google.firebase.INSTANCE_ID_EVENT";
  private static final String MESSAGING_EVENT = "com.google.firebase.MESSAGING_EVENT";
  private static final String PUSH_FCM_LISTENER_SERVICE =
      "com.leanplum.LeanplumPushFirebaseMessagingService";
  private static final String INSTANCE_ID_FCM_SERVICE =
      "com.leanplum.LeanplumPushFcmListenerService";

  public String getRegistrationId() {
    return FirebaseInstanceId.getInstance().getToken();
  }

  public boolean isInitialized() {
    return true;
  }

  public boolean isManifestSetUp() {
    boolean hasReceivers =
        checkComponent(ApplicationComponent.RECEIVER,
            LeanplumPushReceiver.class.getName(), false, null,
            Collections.singletonList(PUSH_FCM_LISTENER_SERVICE), null);

    boolean hasServices = checkComponent(ApplicationComponent.SERVICE,
        PUSH_FCM_LISTENER_SERVICE, false, null,
        Collections.singletonList(MESSAGING_EVENT), null)
        && checkComponent(ApplicationComponent.SERVICE,
        INSTANCE_ID_FCM_SERVICE, false, null,
        Collections.singletonList(INSTANCE_ID_EVENT), null)
        && checkComponent(ApplicationComponent.SERVICE,
        LeanplumPushRegistrationService.class.getName(), false, null, null, null);

    return hasReceivers && hasServices;
  }

  /**
   * Unregister from FCM.
   */
  public void unregister() {
    try {
      FirebaseInstanceId.getInstance().deleteInstanceId();
      Log.i("Application was unregistred from FCM.");
    } catch (Exception e) {
      Log.e("Failed to unregister from FCM.");
    }
  }
}

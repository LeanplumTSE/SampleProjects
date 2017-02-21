// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.leanplum.internal.Constants;
import com.leanplum.internal.Log;
import com.leanplum.utils.SharedPreferencesUtil;

import java.util.List;

/**
 * Leanplum Cloud Messaging provider.
 *
 * @author Anna Orlova
 */
abstract class LeanplumCloudMessagingProvider {
  private static String registrationId;

  enum ApplicationComponent {SERVICE, RECEIVER}

  /**
   * Registration app for Cloud Messaging.
   *
   * @return String - registration id for app.
   */
  public abstract String getRegistrationId();

  /**
   * Verifies that Android Manifest is set up correctly.
   *
   * @return true If Android Manifest is set up correctly.
   */
  public abstract boolean isManifestSetUp();

  public abstract boolean isInitialized();

  /**
   * Unregister from cloud messaging.
   */
  public abstract void unregister();

  static String getCurrentRegistrationId() {
    return registrationId;
  }

  void onRegistrationIdReceived(Context context, String registrationId) {
    if (registrationId == null) {
      Log.w("Registration ID is undefined.");
      return;
    }
    LeanplumCloudMessagingProvider.registrationId = registrationId;
    // Check if received push notification token is different from stored one and send new one to
    // server.
    if (!LeanplumCloudMessagingProvider.registrationId.equals(SharedPreferencesUtil.getString(
        context, Constants.Defaults.LEANPLUM_PUSH, Constants.Defaults.PROPERTY_REGISTRATION_ID))) {
      Log.i("Device registered for push notifications with registration token", registrationId);
      sendRegistrationIdToBackend(LeanplumCloudMessagingProvider.registrationId);
      storePreferences(context.getApplicationContext());
    }
  }

  /**
   * Sends the registration ID to the server over HTTP.
   */
  private static void sendRegistrationIdToBackend(String registrationId) {
    Leanplum.setRegistrationId(registrationId);
  }

  /**
   * Stores the registration ID in the application's {@code SharedPreferences}.
   *
   * @param context application's context.
   */
  public void storePreferences(Context context) {
    Log.v("Saving the registration ID in the shared preferences.");
    SharedPreferencesUtil.setString(context, Constants.Defaults.LEANPLUM_PUSH,
        Constants.Defaults.PROPERTY_REGISTRATION_ID, registrationId);
  }

  static boolean checkPermission(String permission, boolean definesPermission,
      boolean logError) {
    Context context = Leanplum.getContext();
    if (context.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
      String definition;
      if (definesPermission) {
        definition = "<permission android:name=\"" + permission +
            "\" android:protectionLevel=\"signature\" />\n";
      } else {
        definition = "";
      }
      if (logError) {
        Log.e("In order to use push notifications, you need to enable " +
            "the " + permission + " permission in your AndroidManifest.xml file. " +
            "Add this within the <manifest> section:\n" +
            definition + "<uses-permission android:name=\"" + permission + "\" />");
      }
      return false;
    }
    return true;
  }

  /**
   * Verifies that a certain component (receiver or sender) is implemented in the
   * AndroidManifest.xml file or the application, in order to make sure that push notifications
   * work.
   *
   * @param componentType A receiver or a service.
   * @param name The name of the class.
   * @param exported What the exported option should be.
   * @param permission Whether we need any permission.
   * @param actions What actions we need to check for in the intent-filter.
   * @param packageName The package name for the category tag, if we require one.
   * @return true if the respective component is in the manifest file, and false otherwise.
   */
  static boolean checkComponent(ApplicationComponent componentType, String name,
      boolean exported, String permission, List<String> actions, String packageName) {
    Context context = Leanplum.getContext();
    if (actions != null) {
      for (String action : actions) {
        List<ResolveInfo> components = (componentType == ApplicationComponent.RECEIVER)
            ? context.getPackageManager().queryBroadcastReceivers(new Intent(action), 0)
            : context.getPackageManager().queryIntentServices(new Intent(action), 0);
        boolean foundComponent = false;
        for (ResolveInfo component : components) {
          ComponentInfo componentInfo = (componentType == ApplicationComponent.RECEIVER)
              ? component.activityInfo : component.serviceInfo;
          if (componentInfo != null && componentInfo.name.equals(name)) {
            if (packageName == null || componentInfo.packageName.equals(packageName)) {
              foundComponent = true;
            }
          }
        }
        if (!foundComponent) {
          Log.e(getComponentError(componentType, name, exported,
              permission, actions, packageName));
          return false;
        }
      }
    } else {
      try {
        if (componentType == ApplicationComponent.RECEIVER) {
          context.getPackageManager().getReceiverInfo(
              new ComponentName(context.getPackageName(), name), 0);
        } else {
          context.getPackageManager().getServiceInfo(
              new ComponentName(context.getPackageName(), name), 0);
        }
      } catch (PackageManager.NameNotFoundException e) {
        Log.e(getComponentError(componentType, name, exported,
            permission, actions, packageName));
        return false;
      }
    }
    return true;
  }

  /**
   * @return String of error message with instruction how to set up AndroidManifest.xml for push
   * notifications.
   */
  static String getComponentError(ApplicationComponent componentType, String name,
      boolean exported, String permission, List<String> actions, String packageName) {
    StringBuilder errorMessage = new StringBuilder("Push notifications requires you to add the " +
        componentType.name().toLowerCase() + " " + name + " to your AndroidManifest.xml file." +
        "Add this code within the <application> section:\n");
    errorMessage.append("<").append(componentType.name().toLowerCase()).append("\n");
    errorMessage.append("    android:name=\"").append(name).append("\"\n");
    errorMessage.append("    android:exported=\"").append(Boolean.toString(exported)).append("\"");
    if (permission != null) {
      errorMessage.append("\n    android:permission=\"").append(permission).append("\"");
    }
    errorMessage.append(">\n");
    if (actions != null) {
      errorMessage.append("    <intent-filter>\n");
      for (String action : actions) {
        errorMessage.append("        <action android:name=\"").append(action).append("\" />\n");
      }
      if (packageName != null) {
        errorMessage.append("        <category android:name=\"").append(packageName)
            .append("\" />\n");
      }
      errorMessage.append("    </intent-filter>\n");
    }
    errorMessage.append("</").append(componentType.name().toLowerCase()).append(">");
    return errorMessage.toString();
  }
}

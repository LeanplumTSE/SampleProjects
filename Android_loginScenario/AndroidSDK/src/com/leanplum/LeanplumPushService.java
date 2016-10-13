// Copyright 2014, Leanplum, Inc.

package com.leanplum;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.leanplum.Constants.Keys;
import com.leanplum.Constants.Methods;
import com.leanplum.Constants.Params;
import com.leanplum.callbacks.VariablesChangedCallback;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Leanplum push notification service class, handling initialization, opening, showing, integration
 * verification and registration for push notifications.
 *
 * @author Andrew First
 */
public class LeanplumPushService {
  /**
   * Leanplum's built-in Google Cloud Messaging sender ID.
   */
  public static final String LEANPLUM_SENDER_ID = "44059457771";

  private static final String MESSAGE_TYPE_MESSAGE = "gcm";

  private static Class<? extends Activity> callbackClass;

  private static final String PROPERTY_REG_ID = "registration_id";
  private static final String PROPERTY_SENDER_IDS = "sender_ids";
  private static final String PROPERTY_APP_VERSION = "appVersion";

  private static final String SEND_PERMISSION = "com.google.android.c2dm.permission.SEND";
  private static final String RECEIVE_PERMISSION = "com.google.android.c2dm.permission.RECEIVE";
  private static final String RECEIVE_ACTION = "com.google.android.c2dm.intent.RECEIVE";
  private static final String REGISTRATION_ACTION = "com.google.android.c2dm.intent.REGISTRATION";
  private static final String INSTANCE_ID_ACTION = "com.google.android.gms.iid.InstanceID";
  private static final String GCM_RECEIVER = "com.google.android.gms.gcm.GcmReceiver";
  private static final String PUSH_LISTENER_SERVICE = "com.leanplum.LeanplumPushListenerService";
  private static final String INSTANCE_ID_SERVICE = "com.leanplum.LeanplumPushInstanceIDService";

  private static String gcmSenderIds;
  private static String gcmRegistrationId;

  private enum ApplicationComponent {SERVICE, RECEIVER};

  static final int NOTIFICATION_ID = 1;
  static LeanplumPushNotificationCustomizer customizer;

  /**
   * Changes the default activity to launch if the user opens a push notification.
   * @param callbackClass The activity class.
   */
  public static void setDefaultCallbackClass(Class<? extends Activity> callbackClass) {
    LeanplumPushService.callbackClass = callbackClass;
  }

  /**
   * Sets an object used to customize the appearance of notifications.
   * <p>Call this from your Application class's onCreate method so that the customizer is set
   * when your application starts in the background.
   */
  public static void setCustomizer(LeanplumPushNotificationCustomizer customizer) {
    LeanplumPushService.customizer = customizer;
  }

  /**
   * Sets the Google Cloud Messaging sender ID. Required for push notifications to work.
   * @param senderId The GCM sender ID to permit notifications from.
   *     Use {@link LeanplumPushService#LEANPLUM_SENDER_ID} to use the built-in sender ID.
   *     If you have multiple sender IDs, use {@link LeanplumPushService#setGcmSenderIds}.
   */
  public static void setGcmSenderId(String senderId) {
    LeanplumPushService.gcmSenderIds = senderId;
  }

  /**
   * Sets the Google Cloud Messaging sender ID. Required for push notifications to work.
   * @param senderIds The GCM sender IDs to permit notifications from.
   *     Use {@link LeanplumPushService#LEANPLUM_SENDER_ID} to use the built-in sender ID.
   */
  public static void setGcmSenderIds(String... senderIds) {
    StringBuffer joinedSenderIds = new StringBuffer();
    for (String senderId : senderIds) {
      if (joinedSenderIds.length() > 0) {
        joinedSenderIds.append(',');
      }
      joinedSenderIds.append(senderId);
    }
    LeanplumPushService.gcmSenderIds = joinedSenderIds.toString();
  }

  static Class<? extends Activity> getCallbackClass() {
    return callbackClass;
  }

  static boolean areActionsEmbedded(final Bundle message) {
    return message.containsKey(Keys.PUSH_MESSAGE_ACTION);
  }

  static void requireMessageContent(
      final String messageId, final VariablesChangedCallback onComplete) {
    Leanplum.addOnceVariablesChangedAndNoDownloadsPendingHandler(new VariablesChangedCallback() {
      @Override
      public void variablesChanged() {
        try {
          Map<String, Object> messages = VarCache.messages();
          if (messageId == null || (messages != null && messages.containsKey(messageId))) {
            onComplete.variablesChanged();
          } else {
            // Try downloading the messages again if it doesn't exist.
            // Maybe the message was created while the app was running.
            Map<String, Object> params = new HashMap<String, Object>();
            params.put(Params.INCLUDE_DEFAULTS, "" + false);
            params.put(Params.INCLUDE_MESSAGE_ID, messageId);
            LeanplumRequest req = LeanplumRequest.post(Methods.GET_VARS, params);
            req.onResponse(new LeanplumRequest.ResponseCallback() {
              @Override
              public void response(JSONObject response) {
                try {
                  JSONObject getVariablesResponse = LeanplumRequest.getLastResponse(response);
                  if (getVariablesResponse == null) {
                    Log.e("Leanplum",
                        "No response received from the server. Please contact us to investigate.");
                  } else {
                    Map<String, Object> values = JsonConverter.mapFromJson(
                        getVariablesResponse.optJSONObject(Constants.Keys.VARS));
                    Map<String, Object> messages = JsonConverter.mapFromJson(
                        getVariablesResponse.optJSONObject(Constants.Keys.MESSAGES));
                    Map<String, Object> regions = JsonConverter.mapFromJson(
                        getVariablesResponse.optJSONObject(Constants.Keys.REGIONS));
                    List<Map<String, Object>> variants = JsonConverter.listFromJson(
                        getVariablesResponse.optJSONArray(Constants.Keys.VARIANTS));
                    if (!Constants.canDownloadContentMidSessionInProduction ||
                        VarCache.diffs().equals(values)) {
                      values = null;
                    }
                    if (VarCache.messageDiffs().equals(messages)) {
                      messages = null;
                    }
                    if (values != null || messages != null) {
                      VarCache.applyVariableDiffs(values, messages, regions, variants);
                    }
                  }
                  onComplete.variablesChanged();
                } catch (Throwable t) {
                  Util.handleException(t);
                }
              }
            });
            req.onError(new LeanplumRequest.ErrorCallback() {
              @Override
              public void error(Exception e) {
                onComplete.variablesChanged();
              }
            });
            req.sendIfConnected();
          }
        } catch (Throwable t) {
          Util.handleException(t);
        }
      }
    });
  }

  static String getMessageId(Bundle message) {
    String messageId = message.getString(Keys.PUSH_MESSAGE_ID_NO_MUTE_WITH_ACTION);
    if (messageId == null) {
      messageId = message.getString(Keys.PUSH_MESSAGE_ID_MUTE_WITH_ACTION);
      if (messageId == null) {
        messageId = message.getString(Keys.PUSH_MESSAGE_ID_NO_MUTE);
        if (messageId == null) {
          messageId = message.getString(Keys.PUSH_MESSAGE_ID_MUTE);
        }
      }
    }
    if (messageId != null) {
      message.putString(Keys.PUSH_MESSAGE_ID, messageId);
    }
    return messageId;
  }

  static void handleNotification(final Context context, final Bundle message) {
    if (LeanplumActivityHelper.currentActivity != null
        && !LeanplumActivityHelper.isActivityPaused
        && (message.containsKey(Keys.PUSH_MESSAGE_ID_MUTE_WITH_ACTION)
        || message.containsKey(Keys.PUSH_MESSAGE_ID_MUTE))) {
      // Mute notifications that have "Mute inside app" set if the app is open.
      return;
    }

    final String messageId = LeanplumPushService.getMessageId(message);
    if (messageId == null || !Leanplum.calledStart) {
      showNotification(context, message);
      return;
    }

    // Can only track displays if we call Leanplum.start explicitly above where it says
    // if (!Leanplum.calledStart). However, this is probably not worth it.
    //
    // Map<String, String> requestArgs = new HashMap<String, String>();
    // requestArgs.put(Constants.Params.MESSAGE_ID, messageId);
    // Leanplum.track("Displayed", 0.0, null, null, requestArgs);

    showNotification(context, message);
  }

  /**
   * Put the message into a notification and post it.
   */
  static void showNotification(Context context, Bundle message) {
    NotificationManager notificationManager = (NotificationManager)
        context.getSystemService(Context.NOTIFICATION_SERVICE);

    Intent intent = new Intent(context, LeanplumPushReceiver.class);
    intent.addCategory("lpAction");
    intent.putExtras(message);
    PendingIntent contentIntent = PendingIntent.getBroadcast(
        context.getApplicationContext(), new Random().nextInt(),
        intent, 0);

    String title = Util.getApplicationName(context.getApplicationContext());
    if (message.getString("title") != null) {
      title = message.getString("title");
    }
    NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
        .setSmallIcon(context.getApplicationInfo().icon)
        .setContentTitle(title)
        .setStyle(new NotificationCompat.BigTextStyle()
            .bigText(message.getString(Keys.PUSH_MESSAGE_TEXT)))
        .setContentText(message.getString(Keys.PUSH_MESSAGE_TEXT));
    builder.setAutoCancel(true);
    builder.setContentIntent(contentIntent);

    if (LeanplumPushService.customizer != null) {
      LeanplumPushService.customizer.customize(builder, message);
    }

    int notificationId;
    Object notificationIdObject = message.get("lp_notificationId");
    if (notificationIdObject instanceof Number) {
      notificationId = ((Number) notificationIdObject).intValue();
    } else if (notificationIdObject instanceof String) {
      try {
        notificationId = Integer.parseInt((String) notificationIdObject);
      } catch (NumberFormatException e) {
        notificationId = LeanplumPushService.NOTIFICATION_ID;
      }
    } else if (message.containsKey(Keys.PUSH_MESSAGE_ID)) {
      notificationId = message.getString(Keys.PUSH_MESSAGE_ID).hashCode();
    } else {
      notificationId = LeanplumPushService.NOTIFICATION_ID;
    }

    notificationManager.notify(notificationId, builder.build());
  }

  static void openNotification(Context context, final Bundle notification) {
    Log.i("Leanplum", "Opening notification");

    // Start activity.
    Class<? extends Activity> callbackClass = LeanplumPushService.getCallbackClass();
    boolean shouldStartActivity = true;
    if (LeanplumActivityHelper.currentActivity != null &&
        !LeanplumActivityHelper.isActivityPaused) {
      if (callbackClass == null) {
        shouldStartActivity = false;
      } else if (callbackClass.isInstance(LeanplumActivityHelper.currentActivity)) {
        shouldStartActivity = false;
      }
    }
    if (shouldStartActivity) {
      Intent actionIntent = getActionIntent(context);
      actionIntent.putExtras(notification);
      actionIntent.addFlags(
          Intent.FLAG_ACTIVITY_CLEAR_TOP |
              Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(actionIntent);
    }

    // Perform action.
    LeanplumActivityHelper.queueActionUponActive(new VariablesChangedCallback() {
      @Override
      public void variablesChanged() {
        try {
          final String messageId = LeanplumPushService.getMessageId(notification);
          final String actionName = Constants.Values.DEFAULT_PUSH_ACTION;

          // Make sure content is available.
          if (messageId != null) {
            if (LeanplumPushService.areActionsEmbedded(notification)) {
              Map<String, Object> args = new HashMap<String, Object>();
              args.put(actionName, JsonConverter.fromJson(
                  notification.getString(Keys.PUSH_MESSAGE_ACTION)));
              ActionContext context = new ActionContext(
                  ActionManager.PUSH_NOTIFICATION_ACTION_NAME, args, messageId);
              context.preventRealtimeUpdating();
              context.update();
              context.runTrackedActionNamed(actionName);
            } else {
              Leanplum.addOnceVariablesChangedAndNoDownloadsPendingHandler(new VariablesChangedCallback() {
                @Override
                public void variablesChanged() {
                  try {
                    LeanplumPushService.requireMessageContent(messageId, new VariablesChangedCallback() {
                      @Override
                      public void variablesChanged() {
                        try {
                          Leanplum.performTrackedAction(actionName, messageId);
                        } catch (Throwable t) {
                          Util.handleException(t);
                        }
                      }
                    });
                  } catch (Throwable t) {
                    Util.handleException(t);
                  }
                }
              });
            }
          }
        } catch (Throwable t) {
          Util.handleException(t);
        }
      }
    });
  }

  static Intent getActionIntent(Context context) {
    Class<? extends Activity> callbackClass = LeanplumPushService.getCallbackClass();
    if (callbackClass != null) {
      return new Intent(context, callbackClass);
    } else {
      PackageManager pm = context.getPackageManager();
      return pm.getLaunchIntentForPackage(context.getPackageName());
    }
  }

  static String getGcmSenderIds() {
    return gcmSenderIds;
  }

  private static String getStoredRegistrationId(Context context) {
    return getGcmPreferences(context).getString(PROPERTY_REG_ID, "");
  }

  /**
   * Checks if we need to register with Google Cloud Messaging services (i.e. when the developer
   * uses {@link LeanplumPushService#setGcmSenderIds}) for a new registration token, or we have
   * already received one manually, using {@link LeanplumPushService#setGcmRegistrationId}.
   *
   * @return true if we need to register with GCM, false otherwise.
   */
  private static boolean needsGcmRegistration(Context context) {
    if (gcmRegistrationId != null) {
      return false;
    }
    final SharedPreferences prefs = getGcmPreferences(context);
    String registrationId = prefs.getString(PROPERTY_REG_ID, "");
    String storedSenderIds = prefs.getString(PROPERTY_SENDER_IDS, "");
    if (registrationId.length() == 0) {
      Log.i("Leanplum", "GCM registration not found.");
      return true;
    }
    if (gcmSenderIds != null && !gcmSenderIds.equals(storedSenderIds)) {
      Log.i("Leanplum", "GCM sender IDs have changed.");
      return true;
    }
    // Check if app was updated; if so, it must clear the registration ID
    // since the existing regID is not guaranteed to work with the new
    // app version.
    int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
    int currentVersion = getAppVersion(context);
    return registeredVersion != currentVersion;
  }

  /**
   * @return Application's version code from the {@code PackageManager}.
   */
  private static int getAppVersion(Context context) {
    try {
      PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
          context.getPackageName(), 0);
      return packageInfo.versionCode;
    } catch (NameNotFoundException e) {
      // should never happen
      throw new RuntimeException("Could not get package name: " + e);
    }
  }

  /**
   * @return Application's {@code SharedPreferences}.
   */
  private static SharedPreferences getGcmPreferences(Context context) {
    // This sample app persists the registration ID in shared preferences, but
    // how you store the regID in your app is up to you.
    return context.getSharedPreferences("__leanplum_push__", Context.MODE_PRIVATE);
  }

  /**
   * Unregisters the device from all GCM push notifications. You shouldn't need to call this
   * method in production.
   */
  public static void unregister() {
    try {
      Intent unregisterIntent = new Intent("com.google.android.c2dm.intent.UNREGISTER");
      Context context = Leanplum.getContext();
      unregisterIntent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
      unregisterIntent.setPackage("com.google.android.gms");
      context.startService(unregisterIntent);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Registers the application with GCM servers asynchronously.
   * <p>
   * Stores the registration ID and app versionCode in the application's
   * shared preferences.
   */
  private static void registerInBackground() {
    Context context = Leanplum.getContext();
    Intent registerIntent = new Intent(context, LeanplumPushRegistrationService.class);
    context.startService(registerIntent);
  }

  /**
   * Register manually for Google Cloud Messaging services.
   * @param token The registration ID token or the instance ID security token.
   */
  public static void setGcmRegistrationId(String token) {
    onRegistrationIdReceived(Leanplum.getContext().getApplicationContext(), token);
  }

  static void onRegistrationIdReceived(Context context, String registrationId) {
    gcmRegistrationId = registrationId;
    if (!gcmRegistrationId.equals(getStoredRegistrationId(context))) {
      Log.i("Leanplum", "Device registered for push notifications with registration token " +
          registrationId);
      sendRegistrationIdToBackend(gcmRegistrationId);
    }
    storeGcmPreferences(context.getApplicationContext());
  }

  /**
   * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
   * or CCS to send messages to your app. Not needed for this demo since the
   * device sends upstream messages to a server that echoes back the message
   * using the 'from' address in the message.
   */
  private static void sendRegistrationIdToBackend(String regid) {
    Leanplum.setGcmRegistrationId(regid);
  }

  /**
   * Stores the registration ID, sender ID and app versionCode in the application's
   * {@code SharedPreferences}.
   *
   * @param context application's context.
   */
  private static void storeGcmPreferences(Context context) {
    final SharedPreferences prefs = getGcmPreferences(context);
    int appVersion = getAppVersion(context);
    if (Constants.enableVerboseLoggingInDevelopmentMode) {
      Log.i("Leanplum", "Saving GCM preferences on app version " + appVersion);
    }
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(PROPERTY_REG_ID, gcmRegistrationId);
    editor.putString(PROPERTY_SENDER_IDS, gcmSenderIds);
    editor.putInt(PROPERTY_APP_VERSION, appVersion);
    try {
      editor.apply();
    } catch (NoSuchMethodError e) {
      editor.commit();
    }
  }

  /**
   * Call this when Leanplum starts.
   */
  static void onStart() {
    try {
      if (Util.hasPlayServices()) {
        initPushService();
      } else {
        Log.i("Leanplum", "No valid Google Play Services APK found.");
      }
    } catch (Exception e) {
      Log.e("Leanplum", "There was an error registering for push notifications.", e);
    }
  }

  private static void initPushService() {
    Context context = Leanplum.getContext();
    if (gcmSenderIds == null && gcmRegistrationId == null) {
      return;
    }

    boolean hasPermissions = checkPermission(RECEIVE_PERMISSION, false, true)
        && (checkPermission(context.getPackageName() + ".gcm.permission.C2D_MESSAGE", true, false)
            || checkPermission(context.getPackageName() + ".permission.C2D_MESSAGE", true, true));

    boolean hasReceivers = checkComponent(ApplicationComponent.RECEIVER,
            GCM_RECEIVER, true, SEND_PERMISSION,
            Arrays.asList(RECEIVE_ACTION, REGISTRATION_ACTION), context.getPackageName())
        && checkComponent(ApplicationComponent.RECEIVER,
            LeanplumPushReceiver.class.getName(), false, null,
            Collections.singletonList(PUSH_LISTENER_SERVICE), null);

    boolean hasServices = checkComponent(ApplicationComponent.SERVICE,
            PUSH_LISTENER_SERVICE, false, null,
            Collections.singletonList(RECEIVE_ACTION), null)
        && checkComponent(ApplicationComponent.SERVICE,
            INSTANCE_ID_SERVICE, false, null,
            Collections.singletonList(INSTANCE_ID_ACTION), null)
        && checkComponent(ApplicationComponent.SERVICE,
            LeanplumPushRegistrationService.class.getName(), false, null, null, null);

    if (!hasPermissions || !hasReceivers || !hasServices) {
      return;
    }

    if (needsGcmRegistration(context.getApplicationContext())) {
      registerInBackground();
    }
  }

  private static boolean checkPermission(String permission, boolean definesPermission,
      boolean logError) {
    Context context = Leanplum.getContext();
    int res = context.checkCallingOrSelfPermission(permission);
    if (res != PackageManager.PERMISSION_GRANTED) {
      String definition;
      if (definesPermission) {
        definition = "<permission android:name=\"" + permission +
            "\" android:protectionLevel=\"signature\" />\n";
      } else {
        definition = "";
      }
      if (logError) {
        Log.e("Leanplum", "In order to use push notifications, you need to enable the " +
            permission + " permission in your AndroidManifest.xml file. " +
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
   * @param componentType A receiver or a service.
   * @param name The name of the class.
   * @param exported What the exported option should be.
   * @param permission Whether we need any permission.
   * @param actions What actions we need to check for in the intent-filter.
   * @param packageName The package name for the category tag, if we require one.
   * @return true if the respective component is in the manifest file, and false otherwise.
   */
  private static boolean checkComponent(ApplicationComponent componentType, String name,
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
          if (componentInfo.name.equals(name)) {
            if (packageName == null || componentInfo.packageName.equals(packageName)) {
              foundComponent = true;
            }
          }
        }
        if (!foundComponent) {
          Log.e("Leanplum", getComponentError(componentType, name, exported, permission, actions,
              packageName));
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
      } catch (NameNotFoundException e) {
        Log.e("Leanplum", getComponentError(componentType, name, exported, permission, actions,
            packageName));
        return false;
      }
    }
    return true;
  }

  private static String getComponentError(ApplicationComponent componentType, String name,
      boolean exported, String permission, List<String> actions, String packageName) {
    StringBuffer errorMessage = new StringBuffer("Push notifications requires you to add the " +
        componentType.name().toLowerCase() + " " + name + " to your AndroidManifest.xml file." +
        "Add this code within the <application> section:\n");
    errorMessage.append("<" + componentType.name().toLowerCase() + "\n");
    errorMessage.append("    android:name=\"" + name + "\"\n");
    errorMessage.append("    android:exported=\"" + Boolean.toString(exported) + "\"");
    if (permission != null) {
      errorMessage.append("\n    android:permission=\"" + permission + "\"");
    }
    errorMessage.append(">\n");
    if (actions != null) {
      errorMessage.append("    <intent-filter>\n");
      for (String action : actions) {
        errorMessage.append("        <action android:name=\"" + action + "\" />\n");
      }
      if (packageName != null) {
        errorMessage.append("        <category android:name=\"" + packageName + "\" />\n");
      }
      errorMessage.append("    </intent-filter>\n");
    }
    errorMessage.append("</" + componentType.name().toLowerCase() + ">");
    return errorMessage.toString();
  }
}

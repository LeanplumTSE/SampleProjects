// Copyright 2014, Leanplum, Inc.

package com.leanplum;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.leanplum.callbacks.VariablesChangedCallback;
import com.leanplum.internal.ActionManager;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Constants.Keys;
import com.leanplum.internal.Constants.Methods;
import com.leanplum.internal.Constants.Params;
import com.leanplum.internal.JsonConverter;
import com.leanplum.internal.LeanplumInternal;
import com.leanplum.internal.Log;
import com.leanplum.internal.Request;
import com.leanplum.internal.Util;
import com.leanplum.internal.VarCache;
import com.leanplum.utils.BitmapUtil;
import com.leanplum.utils.SharedPreferencesUtil;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Leanplum push notification service class, handling initialization, opening, showing, integration
 * verification and registration for push notifications.
 *
 * @author Andrew First, Anna Orlova
 */
public class LeanplumPushService {
  /**
   * Leanplum's built-in Google Cloud Messaging sender ID.
   */
  public static final String LEANPLUM_SENDER_ID = "44059457771";

  private static Class<? extends Activity> callbackClass;
  private static LeanplumCloudMessagingProvider provider;
  private static boolean isFirebaseEnabled = false;

  private static final int NOTIFICATION_ID = 1;
  private static LeanplumPushNotificationCustomizer customizer;

  /**
   * Use Firebase Cloud Messaging, instead of the default Google Cloud Messaging.
   */
  public static void enableFirebase() {
    LeanplumPushService.isFirebaseEnabled = true;
  }

  /**
   * Whether Firebase Cloud Messaging is enabled or not.
   *
   * @return Boolean - true if enabled
   */
  static boolean isFirebaseEnabled() {
    return isFirebaseEnabled;
  }

  /**
   * Get Cloud Messaging provider. By default - GCM.
   *
   * @return LeanplumCloudMessagingProvider - current provider
   */
  static LeanplumCloudMessagingProvider getCloudMessagingProvider() {
    return provider;
  }

  /**
   * Changes the default activity to launch if the user opens a push notification.
   *
   * @param callbackClass The activity class.
   */
  public static void setDefaultCallbackClass(Class<? extends Activity> callbackClass) {
    LeanplumPushService.callbackClass = callbackClass;
  }

  /**
   * Sets an object used to customize the appearance of notifications. <p>Call this from your
   * Application class's onCreate method so that the customizer is set when your application starts
   * in the background.
   */
  public static void setCustomizer(LeanplumPushNotificationCustomizer customizer) {
    LeanplumPushService.customizer = customizer;
  }

  /**
   * Sets the Google Cloud Messaging/Firebase Cloud Messaging sender ID. Required for push
   * notifications to work.
   *
   * @param senderId The GCM/FCM sender ID to permit notifications from. Use {@link
   * LeanplumPushService#LEANPLUM_SENDER_ID} to use the built-in sender ID for GCM. If you have
   * multiple sender IDs, use {@link LeanplumPushService#setGcmSenderIds}.
   */
  public static void setGcmSenderId(String senderId) {
    LeanplumGcmProvider.setSenderId(senderId);
  }

  /**
   * Sets the Google Cloud Messaging/Firebase Cloud Messaging sender ID. Required for push
   * notifications to work.
   *
   * @param senderIds The GCM/FCM sender IDs to permit notifications from. Use {@link
   * LeanplumPushService#LEANPLUM_SENDER_ID} to use the built-in sender ID.
   */
  public static void setGcmSenderIds(String... senderIds) {
    StringBuilder joinedSenderIds = new StringBuilder();
    for (String senderId : senderIds) {
      if (joinedSenderIds.length() > 0) {
        joinedSenderIds.append(',');
      }
      joinedSenderIds.append(senderId);
    }
    LeanplumGcmProvider.setSenderId(joinedSenderIds.toString());
  }

  private static Class<? extends Activity> getCallbackClass() {
    return callbackClass;
  }

  private static boolean areActionsEmbedded(final Bundle message) {
    return message.containsKey(Keys.PUSH_MESSAGE_ACTION);
  }

  private static void requireMessageContent(
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
            Map<String, Object> params = new HashMap<>();
            params.put(Params.INCLUDE_DEFAULTS, "" + false);
            params.put(Params.INCLUDE_MESSAGE_ID, messageId);
            Request req = Request.post(Methods.GET_VARS, params);
            req.onResponse(new Request.ResponseCallback() {
              @Override
              public void response(JSONObject response) {
                try {
                  JSONObject getVariablesResponse = Request.getLastResponse(response);
                  if (getVariablesResponse == null) {
                    Log.e("No response received from the server. Please contact us to " +
                        "investigate.");
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
                        VarCache.getDiffs().equals(values)) {
                      values = null;
                    }
                    if (VarCache.getMessageDiffs().equals(messages)) {
                      messages = null;
                    }
                    if (values != null || messages != null) {
                      VarCache.applyVariableDiffs(values, messages, null, null, regions, variants);
                    }
                  }
                  onComplete.variablesChanged();
                } catch (Throwable t) {
                  Util.handleException(t);
                }
              }
            });
            req.onError(new Request.ErrorCallback() {
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

  private static String getMessageId(Bundle message) {
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
    if (messageId == null || !LeanplumInternal.hasCalledStart()) {
      showNotification(context, message);
      return;
    }

    // Can only track displays if we call Leanplum.start explicitly above where it says
    // if (!Leanplum.calledStart). However, this is probably not worth it.
    //
    // Map<String, String> requestArgs = new HashMap<String, String>();
    // requestArgs.put(Constants.Params.MESSAGE_ID, getMessageId);
    // Leanplum.track("Displayed", 0.0, null, null, requestArgs);

    showNotification(context, message);
  }

  /**
   * Put the message into a notification and post it.
   */
  private static void showNotification(Context context, Bundle message) {
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

    String imageUrl = message.getString(Keys.PUSH_MESSAGE_IMAGE_URL);
    // BigPictureStyle support requires API 16 and higher.
    if (!TextUtils.isEmpty(imageUrl) && Build.VERSION.SDK_INT >= 16) {
      Bitmap bigPicture = BitmapUtil.getScaledBitmap(context, imageUrl);
      if (bigPicture != null) {
        builder.setStyle(new NotificationCompat.BigPictureStyle()
            .bigPicture(bigPicture)
            .setBigContentTitle(title)
            .setSummaryText(message.getString(Keys.PUSH_MESSAGE_TEXT)))
            .setPriority(Notification.PRIORITY_MAX);
      } else {
        Log.w(String.format("Image download failed for push notification with big picture. " +
            "No image will be included with the push notification. Image URL: %s.", imageUrl));
      }
    }

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
    Log.i("Opening notification");

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
              Map<String, Object> args = new HashMap<>();
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
                          LeanplumInternal.performTrackedAction(actionName, messageId);
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

  private static Intent getActionIntent(Context context) {
    Class<? extends Activity> callbackClass = LeanplumPushService.getCallbackClass();
    if (callbackClass != null) {
      return new Intent(context, callbackClass);
    } else {
      PackageManager pm = context.getPackageManager();
      return pm.getLaunchIntentForPackage(context.getPackageName());
    }
  }

  /**
   * Unregisters the device from all GCM push notifications. You shouldn't need to call this method
   * in production.
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
   * Stores the registration ID and app versionCode in the application's shared preferences.
   */
  private static void registerInBackground() {
    Context context = Leanplum.getContext();
    if (context == null) {
      Log.e("Failed to register application with GCM/FCM. Your application context is not set.");
      return;
    }
    Intent registerIntent = new Intent(context, LeanplumPushRegistrationService.class);
    context.startService(registerIntent);
  }

  /**
   * Register manually for Google Cloud Messaging services.
   *
   * @param token The registration ID token or the instance ID security token.
   */
  public static void setGcmRegistrationId(String token) {
    new LeanplumManualProvider(Leanplum.getContext().getApplicationContext(), token);
  }

  /**
   * Call this when Leanplum starts.
   */
  static void onStart() {
    try {
      if (Util.hasPlayServices()) {
        initPushService();
      } else {
        Log.i("No valid Google Play Services APK found.");
      }
    } catch (LeanplumException e) {
      Log.e("There was an error registering for push notifications.\n" +
          Log.getStackTraceString(e));
    }
  }

  private static void initPushService() {
    if (isFirebaseEnabled()) {
      provider = new LeanplumFcmProvider();
    } else {
      provider = new LeanplumGcmProvider();
    }
    if (!provider.isInitialized() || !provider.isManifestSetUp()) {
      return;
    }
    if (hasAppIDChanged(Request.appId())) {
      provider.unregister();
    }
    registerInBackground();
  }

  /**
   * Check if current application id is different from stored one.
   *
   * @param currentAppId - Current application id.
   * @return True if application id was stored before and doesn't equal to current.
   */
  private static boolean hasAppIDChanged(String currentAppId) {
    Context context = Leanplum.getContext();
    String storedAppId = SharedPreferencesUtil.getString(context, Constants.Defaults.LEANPLUM_PUSH,
        Constants.Defaults.APP_ID);
    if (!currentAppId.equals(storedAppId)) {
      Log.v("Saving the application id in the shared preferences.");
      SharedPreferencesUtil.setString(context, Constants.Defaults.LEANPLUM_PUSH,
          Constants.Defaults.APP_ID, currentAppId);
      // Check application id was stored before.
      if (!SharedPreferencesUtil.DEFAULT_STRING_VALUE.equals(storedAppId)) {
        return true;
      }
    }
    return false;
  }
}

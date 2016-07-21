// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.leanplum.ActionContext.ContextualValues;
import com.leanplum.ActionManager.MessageMatchResult;
import com.leanplum.Util.DeviceIdInfo;
import com.leanplum.callbacks.ActionCallback;
import com.leanplum.callbacks.RegisterDeviceCallback;
import com.leanplum.callbacks.RegisterDeviceFinishedCallback;
import com.leanplum.callbacks.StartCallback;
import com.leanplum.callbacks.VariablesChangedCallback;
import com.leanplum.messagetemplates.MessageTemplates;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Leanplum Android SDK.
 *
 * @author Andrew First, Ben Marten
 */
public class Leanplum {
  @SuppressWarnings("PointlessBitwiseExpression")
  public static int ACTION_KIND_MESSAGE = 1 << 0;
  public static int ACTION_KIND_ACTION = 1 << 1;

  private static final String TAG = "Leanplum";

  /**
   * Default event name to use for Purchase events.
   */
  public static final String PURCHASE_EVENT_NAME = "Purchase";

  private Leanplum() {}

  private static final ArrayList<StartCallback> startHandlers = new ArrayList<>();
  private static final ArrayList<Runnable> startIssuedHandlers = new ArrayList<>();
  private static final ArrayList<VariablesChangedCallback> variablesChangedHandlers =
      new ArrayList<>();
  private static final ArrayList<VariablesChangedCallback> noDownloadsHandlers =
      new ArrayList<>();
  private static final ArrayList<VariablesChangedCallback> onceNoDownloadsHandlers =
      new ArrayList<>();
  private static final Map<String, List<ActionCallback>> actionHandlers =
      new HashMap<>();
  private static RegisterDeviceCallback registerDeviceHandler;
  private static RegisterDeviceFinishedCallback registerDeviceFinishedHandler;

  private static ActionManager actionManager;

  static boolean calledStart;
  private static boolean issuedStart;
  private static boolean hasStarted;
  private static boolean startSuccessful;
  private static boolean hasStartedAndRegisteredAsDeveloper;
  static boolean isPaused = false;
  private static LeanplumDeviceIdMode deviceIdMode = LeanplumDeviceIdMode.MD5_MAC_ADDRESS;
  private static String customDeviceId;
  private static boolean userSpecifiedDeviceId;
  private static boolean initializedMessageTemplates = false;

  static boolean startedInBackground;
  private static boolean inForeground;
  private static final Object inForegroundLock = new Object();

  private static ScheduledExecutorService heartbeatExecutor;
  private static final Object heartbeatLock = new Object();

  private static Context context;

  private static final Queue<Map<String, ?>> userAttributeChanges =
      new ConcurrentLinkedQueue<>();

  /**
   * Wraps Handler while allowing overriding of methods that are needed for unit testing
   *
   * @author kkafadarov
   */
  static class OsHandler {
    // Posts to UI thread. Visible for testing.
    static OsHandler instance;

    final Handler handler = new Handler(Looper.getMainLooper());

    public Boolean post(Runnable runnable) {
      return handler.post(runnable);
    }

    public Boolean postDelayed(Runnable runnable, long lng) {
      return handler.postDelayed(runnable, lng);
    }

    static OsHandler getInstance() {
      if (instance == null) {
        instance = new OsHandler();
      }
      return instance;
    }
  }

  private static Runnable gcmStartCallback;

  /**
   * Optional. Sets the API server. The API path is of the form http[s]://hostname/servletName
   * @param hostName The name of the API host, such as www.leanplum.com
   * @param servletName The name of the API servlet, such as api
   * @param ssl Whether to use SSL
   */
  public static void setApiConnectionSettings(String hostName, String servletName, boolean ssl) {
    if (TextUtils.isEmpty(hostName)) {
      Log.e(TAG, "setApiConnectionSettings - Empty hostname parameter provided.");
      return;
    }
    if (TextUtils.isEmpty(servletName)) {
      Log.e(TAG, "setApiConnectionSettings - Empty servletName parameter provided.");
      return;
    }

    Constants.API_HOST_NAME = hostName;
    Constants.API_SERVLET = servletName;
    Constants.API_SSL = ssl;
  }

  /**
   * Optional. Sets the socket server path for Development mode. Path is of the form hostName:port
   * @param hostName The host name of the socket server.
   * @param port The port to connect to.
   */
  public static void setSocketConnectionSettings(String hostName, int port) {
    if (TextUtils.isEmpty(hostName)) {
      Log.e(TAG, "setSocketConnectionSettings - Empty hostName parameter provided.");
      return;
    }
    if (port < 1 || port > 65535) {
      Log.e(TAG, "setSocketConnectionSettings - Invalid port parameter provided.");
      return;
    }

    Constants.SOCKET_HOST = hostName;
    Constants.SOCKET_PORT = port;
  }

  /**
   * Optional. By default, Leanplum will hash file variables to determine if they're
   * modified and need to be uploaded to the server.
   * Use this method to override this setting.
   *
   * @param enabled Setting this to false will reduce startup latency in development mode, but it's
   * possible that Leanplum will always have the most up-to-date versions of your resources.
   * (Default: true)
   */
  public static void setFileHashingEnabledInDevelopmentMode(boolean enabled) {
    Constants.hashFilesToDetermineModifications = enabled;
  }

  /**
   * Optional. Whether to enable file uploading in development mode.
   * @param enabled Whether or not files should be uploaded.
   * (Default: true)
   */
  public static void setFileUploadingEnabledInDevelopmentMode(boolean enabled) {
    Constants.enableFileUploadingInDevelopmentMode = enabled;
  }

  /**
   * Optional. Enables verbose logging in development mode.
   */
  public static void enableVerboseLoggingInDevelopmentMode() {
    Constants.enableVerboseLoggingInDevelopmentMode = true;
  }

  /**
   * Optional. Adjusts the network timeouts.
   * The default timeout is 10 seconds for requests, and 15 seconds for file downloads.
   */
  public static void setNetworkTimeout(int seconds, int downloadSeconds) {
    if (seconds < 0) {
      Log.e(TAG, "setNetworkTimeout - Invalid seconds parameter provided.");
      return;
    }
    if (downloadSeconds < 0) {
      Log.e(TAG, "setNetworkTimeout - Invalid downloadSeconds parameter provided.");
      return;
    }

    Constants.NETWORK_TIMEOUT_SECONDS = seconds;
    Constants.NETWORK_TIMEOUT_SECONDS_FOR_DOWNLOADS = downloadSeconds;
  }

  /**
   * Advanced: Whether new variables can be downloaded mid-session.
   * By default, this is disabled.
   * Currently, if this is enabled, new variables can only be downloaded
   * if a push notification is sent while the app is running, and the
   * notification's metadata hasn't be downloaded yet.
   */
  public static void setCanDownloadContentMidSessionInProductionMode(boolean value) {
    Constants.canDownloadContentMidSessionInProduction = value;
  }

  /**
   * Must call either this or {@link Leanplum#setAppIdForProductionMode}
   * before issuing any calls to the API, including start.
   * @param appId Your app ID.
   * @param accessKey Your development key.
   */
  public static void setAppIdForDevelopmentMode(String appId, String accessKey) {
    if (TextUtils.isEmpty(appId)) {
      Log.e(TAG, "setAppIdForDevelopmentMode - Empty appId parameter provided.");
      return;
    }
    if (TextUtils.isEmpty(accessKey)) {
      Log.e(TAG, "setAppIdForDevelopmentMode - Empty accessKey parameter provided.");
      return;
    }

    Constants.isDevelopmentModeEnabled = true;
    LeanplumRequest.setAppId(appId, accessKey);
  }

  /**
   * Must call either this or {@link Leanplum#setAppIdForDevelopmentMode}
   * before issuing any calls to the API, including start.
   * @param appId Your app ID.
   * @param accessKey Your production key.
   */
  public static void setAppIdForProductionMode(String appId, String accessKey) {
    if (TextUtils.isEmpty(appId)) {
      Log.e(TAG, "setAppIdForProductionMode - Empty appId parameter provided.");
      return;
    }
    if (TextUtils.isEmpty(accessKey)) {
      Log.e(TAG, "setAppIdForProductionMode - Empty accessKey parameter provided.");
      return;
    }

    Constants.isDevelopmentModeEnabled = false;
    LeanplumRequest.setAppId(appId, accessKey);
  }

  /**
   * Sets the type of device ID to use. Default: {@link LeanplumDeviceIdMode#MD5_MAC_ADDRESS}
   */
  public static void setDeviceIdMode(LeanplumDeviceIdMode mode) {
    if (mode == null) {
      Log.e(TAG, "setDeviceIdMode - Invalid mode parameter provided.");
      return;
    }

    deviceIdMode = mode;
    userSpecifiedDeviceId = true;
  }

  /**
   * (Advanced) Sets a custom device ID. Normally, you should use setDeviceIdMode to change
   * the type of device ID provided.
   */
  public static void setDeviceId(String deviceId) {
    if (TextUtils.isEmpty(deviceId)) {
      Log.w(TAG, "setDeviceId - Empty deviceId parameter provided.");
    }

    customDeviceId = deviceId;
    userSpecifiedDeviceId = true;
  }

  /**
   * Sets the application context. This should be the first call to Leanplum.
   */
  public static void setApplicationContext(Context context) {
    if (context == null) {
      Log.w(TAG, "setApplicationContext - Null context parameter provided.");
    }

    Leanplum.context = context;
  }

  /**
   * Gets the application context.
   */
  public static Context getContext() {
    if (context == null) {
      Log.e(TAG, "Your application context is not set. "
          + "You should call Leanplum.setApplicationContext(this) or "
          + "LeanplumActivityHelper.enableLifecycleCallbacks(this) in your application's "
          + "onCreate method, or have your application extend LeanplumApplication.");
    }
    return context;
  }

  /**
   * Called when the device needs to be registered in development mode.
   */
  @Deprecated
  public static void setRegisterDeviceHandler(RegisterDeviceCallback handler,
      RegisterDeviceFinishedCallback finishHandler) {
    if (handler == null && finishHandler == null) {
      Log.w(TAG, "setRegisterDeviceHandler - Invalid handler parameter provided.");
    }

    registerDeviceHandler = handler;
    registerDeviceFinishedHandler = finishHandler;
  }

  /**
   * Syncs resources between Leanplum and the current app.
   * You should only call this once, and before {@link Leanplum#start}.
   * syncResourcesAsync should be used instead unless file variables need
   * to be defined early
   */
  public static void syncResources() {
    if (Constants.isNoop()) {
      return;
    }
    try {
      FileManager.enableResourceSyncing(null, null, false);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Syncs resources between Leanplum and the current app.
   * You should only call this once, and before {@link Leanplum#start}.
   */
  public static void syncResourcesAsync() {
    if (Constants.isNoop()) {
      return;
    }
    try {
      FileManager.enableResourceSyncing(null, null, true);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Syncs resources between Leanplum and the current app.
   * You should only call this once, and before {@link Leanplum#start}.
   * syncResourcesAsync should be used instead unless file variables need
   * to be defined early
   * @param patternsToInclude Limit paths
   *     to only those matching at least one pattern in this list.
   *     Supply null to indicate no inclusion patterns.
   *     Paths start with the folder name within the res folder,
   *     e.g. "layout/main.xml".
   * @param patternsToExclude Exclude paths
   *     matching at least one of these patterns.
   *     Supply null to indicate no exclusion patterns.
   */
  public static void syncResources(
      List<String> patternsToInclude,
      List<String> patternsToExclude) {
    try {
      FileManager.enableResourceSyncing(patternsToInclude, patternsToExclude, false);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Syncs resources between Leanplum and the current app.
   * You should only call this once, and before {@link Leanplum#start}.
   * syncResourcesAsync should be used instead unless file variables need
   * to be defined early
   * @param patternsToInclude Limit paths
   *     to only those matching at least one pattern in this list.
   *     Supply null to indicate no inclusion patterns.
   *     Paths start with the folder name within the res folder,
   *     e.g. "layout/main.xml".
   * @param patternsToExclude Exclude paths
   *     matching at least one of these patterns.
   *     Supply null to indicate no exclusion patterns.
   */
  public static void syncResourcesAsync(
      List<String> patternsToInclude,
      List<String> patternsToExclude) {
    try {
      FileManager.enableResourceSyncing(patternsToInclude, patternsToExclude, true);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Returns true if resource syncing is enabled. Resource syncing may not be fully initialized.
   */
  public static boolean isResourceSyncingEnabled() {
    return FileManager.isResourceSyncingEnabled();
  }

  /**
   * Call this when your application starts.
   * This will initiate a call to Leanplum's servers to get the values
   * of the variables used in your app.
   */
  public static void start(Context context) {
    start(context, null, null, null, null);
  }

  /**
   * Call this when your application starts.
   * This will initiate a call to Leanplum's servers to get the values
   * of the variables used in your app.
   */
  public static void start(Context context, StartCallback callback) {
    start(context, null, null, callback, null);
  }

  /**
   * Call this when your application starts.
   * This will initiate a call to Leanplum's servers to get the values
   * of the variables used in your app.
   */
  public static void start(Context context, Map<String, ?> userAttributes) {
    start(context, null, userAttributes, null, null);
  }

  /**
   * Call this when your application starts.
   * This will initiate a call to Leanplum's servers to get the values
   * of the variables used in your app.
   */
  public static void start(Context context, String userId) {
    start(context, userId, null, null, null);
  }

  /**
   * Call this when your application starts.
   * This will initiate a call to Leanplum's servers to get the values
   * of the variables used in your app.
   */
  public static void start(Context context, String userId, StartCallback callback) {
    start(context, userId, null, callback, null);
  }

  /**
   * Call this when your application starts.
   * This will initiate a call to Leanplum's servers to get the values
   * of the variables used in your app.
   */
  public static void start(Context context, String userId, Map<String, ?> userAttributes) {
    start(context, userId, userAttributes, null, null);
  }

  /**
   * Call this when your application starts.
   * This will initiate a call to Leanplum's servers to get the values
   * of the variables used in your app.
   */
  public static synchronized void start(final Context context, String userId,
      Map<String, ?> attributes, StartCallback response) {
    start(context, userId, attributes, response, null);
  }

  static synchronized void start(final Context context, final String userId,
      final Map<String, ?> attributes, StartCallback response, final Boolean isBackground) {
    try {
      OsHandler.getInstance();

      if (context instanceof Activity) {
        LeanplumActivityHelper.currentActivity = (Activity) context;
      }

      // Detect if app is in background automatically if isBackground is not set.
      final boolean actuallyInBackground;
      if (isBackground == null) {
        actuallyInBackground = LeanplumActivityHelper.currentActivity == null ||
            LeanplumActivityHelper.isActivityPaused;
      } else {
        actuallyInBackground = isBackground;
      }

      if (Constants.isNoop()) {
        hasStarted = true;
        startSuccessful = true;
        triggerStartResponse(true);
        triggerVariablesChanged();
        triggerVariablesChangedAndNoDownloadsPending();
        VarCache.applyVariableDiffs(
            new HashMap<String, Object>(),
            new HashMap<String, Object>(),
            new HashMap<String, Object>(),
            new ArrayList<Map<String, Object>>());
        Newsfeed.getInstance().updateNewsfeed(new HashMap<String, NewsfeedMessage>(), 0, false);
        return;
      }

      if (response != null) {
        addStartResponseHandler(response);
      }

      if (context != null) {
        Leanplum.setApplicationContext(context.getApplicationContext());
      }

      if (calledStart) {
        if (!actuallyInBackground && startedInBackground) {
          // Move to foreground.
          startedInBackground = false;
          moveToForeground();
        } else {
          Log.i(TAG, "Already called start");
        }
        return;
      }

      initializedMessageTemplates = true;
      MessageTemplates.register(Leanplum.getContext());

      startedInBackground = actuallyInBackground;

      final Map<String, ?> validAttributes = validateAttributes(attributes, "userAttributes", true);
      calledStart = true;

      if (validAttributes != null) {
        userAttributeChanges.add(validAttributes);
      }

      actionManager = ActionManager.instance();

      LeanplumRequest.loadToken();
      VarCache.setSilent(true);
      VarCache.loadDiffs();
      VarCache.setSilent(false);
      Newsfeed.getInstance().load();

      // Setup class members.
      VarCache.onUpdate(new VarCache.CacheUpdateBlock() {
        @Override
        public void updateCache() {
          triggerVariablesChanged();
          if (LeanplumRequest.numPendingDownloads() == 0) {
            triggerVariablesChangedAndNoDownloadsPending();
          }
        }
      });
      LeanplumRequest.onNoPendingDownloads(new LeanplumRequest.NoPendingDownloadsCallback() {
        @Override
        public void noPendingDownloads() {
          triggerVariablesChangedAndNoDownloadsPending();
        }
      });

      // Reduce latency by running the rest of the start call in a background thread.
      Util.executeAsyncTask(new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          try {
            startHelper(userId, validAttributes, actuallyInBackground);
          } catch (Throwable t) {
            Util.handleException(t);
          }
          return null;
        }
      });
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  private static void startHelper(
      String userId, final Map<String, ?> attributes, final boolean isBackground) {
    LeanplumPushService.onStart();

    Boolean limitAdTracking = null;
    String deviceId = LeanplumRequest.deviceId();
    if (deviceId == null) {
      if (!userSpecifiedDeviceId && Constants.defaultDeviceId != null) {
        deviceId = Constants.defaultDeviceId;
      } else if (customDeviceId != null) {
        deviceId = customDeviceId;
      } else {
        DeviceIdInfo deviceIdInfo = Util.getDeviceId(deviceIdMode);
        deviceId = deviceIdInfo.id;
        limitAdTracking = deviceIdInfo.limitAdTracking;
      }
      LeanplumRequest.setDeviceId(deviceId);
    }

    if (userId == null) {
      userId = LeanplumRequest.userId();
      if (userId == null) {
        userId = LeanplumRequest.deviceId();
      }
    }
    LeanplumRequest.setUserId(userId);

    // Setup parameters.
    String versionName = Util.getVersionName();
    if (versionName == null) {
        versionName = "";
    }

    TimeZone localTimeZone = TimeZone.getDefault();
    Date now = new Date();
    int timezoneOffsetSeconds = localTimeZone.getOffset(now.getTime()) / 1000;

    HashMap<String, Object> params = new HashMap<>();
    params.put(Constants.Params.INCLUDE_DEFAULTS, "" + false);
    if (isBackground) {
      params.put(Constants.Params.BACKGROUND, Boolean.toString(true));
    }
    params.put(Constants.Params.VERSION_NAME, versionName);
    params.put(Constants.Params.DEVICE_NAME, Util.getDeviceName());
    params.put(Constants.Params.DEVICE_MODEL, Util.getDeviceModel());
    params.put(Constants.Params.DEVICE_SYSTEM_NAME, Util.getSystemName());
    params.put(Constants.Params.DEVICE_SYSTEM_VERSION, Util.getSystemVersion());
    params.put(Constants.Keys.TIMEZONE, localTimeZone.getID());
    params.put(Constants.Keys.TIMEZONE_OFFSET_SECONDS, "" + timezoneOffsetSeconds);
    params.put(Constants.Keys.LOCALE, Util.getLocale());
    params.put(Constants.Keys.COUNTRY, Constants.Values.DETECT);
    params.put(Constants.Keys.REGION, Constants.Values.DETECT);
    params.put(Constants.Keys.CITY, Constants.Values.DETECT);
    params.put(Constants.Keys.LOCATION, Constants.Values.DETECT);
    if (Boolean.TRUE.equals(limitAdTracking)) {
      params.put(Constants.Params.LIMIT_TRACKING, limitAdTracking.toString());
    }
    if (attributes != null) {
      params.put(Constants.Params.USER_ATTRIBUTES, JsonConverter.toJson(attributes));
    }
    if (Constants.isDevelopmentModeEnabled) {
      params.put(Constants.Params.DEV_MODE, Boolean.TRUE.toString());
    }

    // Get the current Newsfeed messages on the device.
    params.put(Constants.Params.NEWSFEED_MESSAGES, Newsfeed.getInstance().messagesIds());

    Util.initializePreLeanplumInstall(params);

    // Issue start API call.
    LeanplumRequest req = LeanplumRequest.post(Constants.Methods.START, params);
    req.onApiResponse(new LeanplumRequest.ApiResponseCallback() {
      @Override
      public void response(List<Map<String, Object>> requests, JSONObject response) {
        Leanplum.handleApiResponse(response, requests);
      }
    });

    if (isBackground) {
      req.sendEventually();
    } else {
      req.sendIfConnected();
    }

    triggerStartIssued();
  }

  private static void handleApiResponse(JSONObject response, List<Map<String, Object>> requests) {
    boolean hasStartResponse = false;
    JSONObject lastStartResponse = null;

    // Find and handle the last start response.
    try {
      int numResponses = LeanplumRequest.numResponses(response);
      for (int i = requests.size() - 1; i >= 0; i--) {
        Map<String, Object> request = requests.get(i);
        if (Constants.Methods.START.equals(request.get(Constants.Params.ACTION))) {
          if (i < numResponses) {
            lastStartResponse = LeanplumRequest.getResponseAt(response, i);
          }
          hasStartResponse = true;
          break;
        }
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }

    if (hasStartResponse) {
      if (!hasStarted) {
        Leanplum.handleStartResponse(lastStartResponse);
      }
    }
  }

  private static void handleStartResponse(JSONObject response) {
    boolean success = LeanplumRequest.isResponseSuccess(response);
    if (!success) {
      try {
        hasStarted = true;
        startSuccessful = false;

        // Load the variables that were stored on the device from the last session.
        VarCache.loadDiffs();

        triggerStartResponse(false);
      } catch (Throwable t) {
        Util.handleException(t);
      }
    } else {
      try {
        hasStarted = true;
        startSuccessful = true;

        JSONObject values = response.optJSONObject(Constants.Keys.VARS);
        if (values == null) {
          Log.e(TAG, "No variable values were received from the server. " +
              "Please contact us to investigate.");
          values = new JSONObject();
        }

        JSONObject messages = response.optJSONObject(Constants.Keys.MESSAGES);
        if (messages == null) {
          messages = new JSONObject();
        }

        JSONObject regions = response.optJSONObject(Constants.Keys.REGIONS);
        if (regions == null) {
          regions = new JSONObject();
        }

        JSONArray variants = response.optJSONArray(Constants.Keys.VARIANTS);
        if (variants == null) {
          variants = new JSONArray();
        }

        String token = response.optString(Constants.Keys.TOKEN, null);
        LeanplumRequest.setToken(token);
        LeanplumRequest.saveToken();

        VarCache.applyVariableDiffs(
            JsonConverter.mapFromJson(values),
            JsonConverter.mapFromJson(messages),
            JsonConverter.mapFromJson(regions),
            JsonConverter.<Map<String, Object>>listFromJson(variants));
        VarCache.saveUserAttributes();
        triggerStartResponse(true);

        if (response.optBoolean(Constants.Keys.SYNC_NEWSFEED, false)) {
          Newsfeed.getInstance().downloadMessages();
        }

        // Allow bidirectional realtime variable updates.
        if (Constants.isDevelopmentModeEnabled) {

          final Context currentContext = (
              LeanplumActivityHelper.currentActivity != context &&
                  LeanplumActivityHelper.currentActivity != null) ?
              LeanplumActivityHelper.currentActivity
              : context;

          // Register device.
          if (!response.optBoolean(
              Constants.Keys.IS_REGISTERED) && registerDeviceHandler != null) {
            registerDeviceHandler.setResponseHandler(new RegisterDeviceCallback.EmailCallback() {
              @Override
              public void onResponse(String email) {
                try {
                  if (email != null) {
                    Registration.registerDevice(email, new StartCallback() {
                      @Override
                      public void onResponse(boolean success) {
                        if (registerDeviceFinishedHandler != null) {
                          registerDeviceFinishedHandler.setSuccess(success);
                          OsHandler.getInstance().post(registerDeviceFinishedHandler);
                        }
                        if (success) {
                          try {
                            onHasStartedAndRegisteredAsDeveloper();
                          } catch (Throwable t) {
                            Util.handleException(t);
                          }
                        }
                      }
                    });
                  }
                } catch (Throwable t) {
                  Util.handleException(t);
                }
              }
            });
            OsHandler.getInstance().post(registerDeviceHandler);
          }

          // Show device is already registered.
          if (response.optBoolean(Constants.Keys.IS_REGISTERED_FROM_OTHER_APP)) {
            OsHandler.getInstance().post(new Runnable() {
              @Override
              public void run() {
                try {
                  NotificationCompat.Builder mBuilder =
                      new NotificationCompat.Builder(currentContext)
                          .setSmallIcon(android.R.drawable.star_on)
                          .setContentTitle("Leanplum")
                          .setContentText("Your device is registered.");
                  mBuilder.setContentIntent(PendingIntent.getActivity(
                      currentContext.getApplicationContext(), 0, new Intent(), 0));
                  NotificationManager mNotificationManager =
                      (NotificationManager) currentContext.getSystemService(
                          Context.NOTIFICATION_SERVICE);
                  // mId allows you to update the notification later on.
                  mNotificationManager.notify(0, mBuilder.build());
                } catch (Throwable t) {
                  Log.i(TAG, "Device is registered.");
                }
              }
            });
          }

          boolean isRegistered = response.optBoolean(Constants.Keys.IS_REGISTERED);

          // Check for updates.
          final String latestVersion =  response.optString(Constants.Keys.LATEST_VERSION, null);
          if (isRegistered && latestVersion != null) {
            Log.i(TAG, "An update to Leanplum Android SDK, " + latestVersion +
                ", is available. Go to leanplum.com to download it.");
          }

          JSONObject valuesFromCode = response.optJSONObject(Constants.Keys.VARS_FROM_CODE);
          if (valuesFromCode == null) {
            valuesFromCode = new JSONObject();
          }

          JSONObject actionDefinitions =
              response.optJSONObject(Constants.Params.ACTION_DEFINITIONS);
          if (actionDefinitions == null) {
            actionDefinitions = new JSONObject();
          }

          JSONObject fileAttributes = response.optJSONObject(Constants.Params.FILE_ATTRIBUTES);
          if (fileAttributes == null) {
            fileAttributes = new JSONObject();
          }

          VarCache.setDevModeValuesFromServer(
              JsonConverter.mapFromJson(valuesFromCode),
              JsonConverter.mapFromJson(fileAttributes),
              JsonConverter.mapFromJson(actionDefinitions));

          if (isRegistered) {
            onHasStartedAndRegisteredAsDeveloper();
          }
        }

        moveToForeground();
        startHeartbeat();
      } catch (Throwable t) {
        Util.handleException(t);
      }
    }
  }

  private static void moveToForeground() {
    synchronized (inForegroundLock) {
      if (inForeground) {
        return;
      }
      inForeground = true;
    }
    if (Constants.isDevelopmentModeEnabled && !Constants.isNoop()) {
      new LeanplumSocket();
    }
    maybePerformActions(new String[] {"start", "resume"}, null,
        LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL, null, null);
    recordAttributeChanges();
  }

  /**
   * Allows start to be called again.
   */
  static void reset() {
    calledStart = false;
    hasStarted = false;
    issuedStart = false;
    hasStartedAndRegisteredAsDeveloper = false;
    startSuccessful = false;
    startHandlers.clear();
    startIssuedHandlers.clear();
    variablesChangedHandlers.clear();
    noDownloadsHandlers.clear();
    onceNoDownloadsHandlers.clear();
    actionHandlers.clear();
    userAttributeChanges.clear();

    registerDeviceHandler = null;
    registerDeviceFinishedHandler = null;
    actionManager = null;
    isPaused = false;
    deviceIdMode = LeanplumDeviceIdMode.MD5_MAC_ADDRESS;
    customDeviceId = null;
    userSpecifiedDeviceId = false;
    initializedMessageTemplates = false;
    startedInBackground = false;
    inForeground = false;
    heartbeatExecutor = null;
    context = null;
    gcmStartCallback = null;

    Newsfeed.getInstance().reset();
  }

  /**
   * Used by wrapper SDKs like Unity to override the SDK client name and version.
   */
  static void setClient(String client, String sdkVersion, String defaultDeviceId) {
    Constants.CLIENT = client;
    Constants.LEANPLUM_VERSION = sdkVersion;
    Constants.defaultDeviceId = defaultDeviceId;
  }

  /**
   * Throws a LeanplumException when in development mode.
   * Otherwise, logs the exception to prevent the app from crashing.
   */
  static void maybeThrowException(RuntimeException e) {
    if (Constants.isDevelopmentModeEnabled) {
      throw e;
    } else {
      Log.e(TAG, e.getMessage() + " This error is only thrown in development mode.", e);
    }
  }

  /**
   * Validates that the attributes are of the correct format.
   * User attributes must be Strings, Booleans, or Numbers.
   * @param argName Argument name used for error messages.
   * @param allowLists Whether attribute values can be lists.
   */
  @SuppressWarnings("unchecked")
  static <T>Map<String, T> validateAttributes(Map<String, T> attributes, String argName,
      boolean allowLists) {
    if (attributes == null) {
      return null;
    }
    Map<String, T> validAttributes = new HashMap<>();
    try {
      for (String key : attributes.keySet()) {
        T value = attributes.get(key);

        if (value == null) {
          continue;
        }

        // Validate lists.
        if (allowLists && value instanceof Iterable<?>) {
          boolean valid = true;
          for (Object item : (Iterable<Object>) value) {
            if (!isValidScalarValue(item, argName)) {
              valid = false;
              break;
            }
          }
          if (!valid) {
            continue;
          }

          // Validate scalars.
        } else {
          if (value instanceof Date) {
            value = (T) (Long) ((Date) value).getTime();
          }
          if (!isValidScalarValue(value, argName)) {
            continue;
          }
        }
        validAttributes.put(key, value);
      }
    } catch (ConcurrentModificationException e) {
      maybeThrowException(new LeanplumException("ConcurrentModificationException: You cannot " +
          "modify Map<String, ?> attributes/parameters. Will override with an empty map"));
      validAttributes = new HashMap<>();
    }
    return validAttributes;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private static boolean isValidScalarValue(Object value, String argName) {
    if (!(value instanceof Number) && !(value instanceof String) && !(value instanceof Boolean)) {
      maybeThrowException(new LeanplumException(
          argName + " values must be of type String, Number, or Boolean."));
      return false;
    }
    return true;
  }

  /**
   * Call this when your activity pauses.
   * This is called from LeanplumActivityHelper.
   */
  static void pause() {
    if (Constants.isNoop()) {
      return;
    }
    if (!calledStart) {
      Log.e(TAG, "You cannot call pause before calling start");
      return;
    }
    isPaused = true;

    if (issuedStart) {
      pauseInternal();
    } else {
      Leanplum.addStartIssuedHandler(new Runnable() {
        @Override
        public void run() {
          try {
            pauseInternal();
          } catch (Throwable t) {
            Util.handleException(t);
          }
        }
      });
    }
  }

  private static void pauseInternal() {
    LeanplumRequest.post(Constants.Methods.PAUSE_SESSION, null).sendIfConnected();
    pauseHeartbeat();
  }

  /**
   * Call this when your activity resumes.
   * This is called from LeanplumActivityHelper.
   */
  static void resume() {
    if (Constants.isNoop()) {
      return;
    }
    if (!calledStart) {
      Log.e(TAG, "You cannot call resume before calling start");
      return;
    }
    isPaused = false;

    if (issuedStart) {
      resumeInternal();
    } else {
      Leanplum.addStartIssuedHandler(new Runnable() {
        @Override
        public void run() {
          try {
            resumeInternal();
          } catch (Throwable t) {
            Util.handleException(t);
          }
        }
      });
    }
  }

  private static void resumeInternal() {
    LeanplumRequest request = LeanplumRequest.post(Constants.Methods.RESUME_SESSION, null);
    if (startedInBackground) {
      startedInBackground = false;
      request.sendIfConnected();
    } else {
      request.sendIfDelayed();
      maybePerformActions("resume", null, LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL,
          null, null);
    }
    resumeHeartbeat();
  }

  /**
   * Send a heartbeat every 15 minutes while the app is running.
   */
  private static void startHeartbeat() {
    synchronized (heartbeatLock) {
      heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
      heartbeatExecutor.scheduleAtFixedRate(new Runnable() {
        public void run() {
          try {
            LeanplumRequest.post(Constants.Methods.HEARTBEAT, null).sendIfDelayed();
          } catch (Throwable t) {
            Util.handleException(t);
          }
        }
      }, 15, 15, TimeUnit.MINUTES);
    }
  }

  private static void pauseHeartbeat() {
    synchronized (heartbeatLock) {
      if (heartbeatExecutor != null) {
        heartbeatExecutor.shutdown();
      }
    }
  }

  private static void resumeHeartbeat() {
    startHeartbeat();
  }

  /**
   * Call this to explicitly end the session.
   * This should not be used in most cases, so we won't make it public for now.
   */
  static void stop() {
    if (Constants.isNoop()) {
      return;
    }
    if (!calledStart) {
      Log.e(TAG, "You cannot call stop before calling start");
      return;
    }

    if (issuedStart) {
      stopInternal();
    } else {
      Leanplum.addStartIssuedHandler(new Runnable() {
        @Override
        public void run() {
          try {
            stopInternal();
          } catch (Throwable t) {
            Util.handleException(t);
          }
        }
      });
    }
  }

  private static void stopInternal() {
    LeanplumRequest.post(Constants.Methods.STOP, null).sendIfConnected();
  }

  /**
   * Whether or not Leanplum has finished starting.
   */
  public static boolean hasStarted() {
    return hasStarted;
  }

  /**
   * Returns an instance to the singleton Newsfeed object.
   */
  public static Newsfeed newsfeed() {
    return Newsfeed.getInstance();
  }

  /**
   * Whether or not Leanplum has finished starting and the device is registered
   * as a developer.
   */
  public static boolean hasStartedAndRegisteredAsDeveloper() {
    return hasStartedAndRegisteredAsDeveloper;
  }

  /**
   * Called when the development mode has started and
   * the device is registered successfully.
   */
  static void onHasStartedAndRegisteredAsDeveloper() {
    synchronized (FileManager.initializingLock) {
      if (FileManager.initializing()) {
        FileManager.setResourceSyncFinishBlock(new FileManager.ResourceUpdateCallback() {
          @Override
          public void onResourceSyncFinish() {
            try {
              onHasStartedAndRegisteredAsDeveloperAndFinishedSyncing();
            } catch (Throwable t) {
              Util.handleException(t);
            }
          }
        });
      } else {
        onHasStartedAndRegisteredAsDeveloperAndFinishedSyncing();
      }
    }
  }

  private static void onHasStartedAndRegisteredAsDeveloperAndFinishedSyncing() {
    if (!hasStartedAndRegisteredAsDeveloper) {
      hasStartedAndRegisteredAsDeveloper = true;
    }
  }

  /**
   * Add a callback for when the start call finishes, and variables are
   * returned back from the server.
   */
  public static void addStartResponseHandler(StartCallback handler) {
    if (handler == null) {
      Log.e(TAG, "addStartResponseHandler - Invalid handler parameter provided.");
      return;
    }

    if (hasStarted) {
      if (startSuccessful) {
        handler.setSuccess(true);
      }
      handler.run();
    } else {
      synchronized (startHandlers) {
        if (startHandlers.indexOf(handler) == -1) {
          startHandlers.add(handler);
        }
      }
    }
  }

  /**
   * Removes a start response callback.
   */
  public static void removeStartResponseHandler(StartCallback handler) {
    if (handler == null) {
      Log.e(TAG, "removeStartResponseHandler - Invalid handler parameter provided.");
      return;
    }

    synchronized (startHandlers) {
      startHandlers.remove(handler);
    }
  }

  private static void triggerStartResponse(boolean success) {
    synchronized (startHandlers) {
      for (StartCallback callback : startHandlers) {
        callback.setSuccess(success);
        OsHandler.getInstance().post(callback);
      }
      startHandlers.clear();
    }
  }

  private static void addStartIssuedHandler(Runnable handler) {
    if (handler == null) {
      Log.e(TAG, "addStartIssuedHandler - Invalid handler parameter provided.");
      return;
    }

    synchronized (startIssuedHandlers) {
      if (!issuedStart) {
        startIssuedHandlers.add(handler);
        return;
      }
    }
    handler.run();
  }

  private static void triggerStartIssued() {
    synchronized (startIssuedHandlers) {
      issuedStart = true;
      for (Runnable callback : startIssuedHandlers) {
        OsHandler.getInstance().post(callback);
      }
      startIssuedHandlers.clear();
    }
  }

  /**
   * Add a callback for when the variables receive new values from the server.
   * This will be called on start, and also later on if the user is in an
   * experiment that can updated in realtime.
   */
  public static void addVariablesChangedHandler(VariablesChangedCallback handler) {
    if (handler == null) {
      Log.e(TAG, "addVariablesChangedHandler - Invalid handler parameter provided.");
      return;
    }

    synchronized (variablesChangedHandlers) {
      variablesChangedHandlers.add(handler);
    }
    if (VarCache.hasReceivedDiffs()) {
      handler.variablesChanged();
    }
  }

  /**
   * Removes a variables changed callback.
   */
  public static void removeVariablesChangedHandler(VariablesChangedCallback handler) {
    if (handler == null) {
      Log.e(TAG, "removeVariablesChangedHandler - Invalid handler parameter provided.");
      return;
    }

    synchronized (variablesChangedHandlers) {
      variablesChangedHandlers.remove(handler);
    }
  }

  private static void triggerVariablesChanged() {
    synchronized (variablesChangedHandlers) {
      for (VariablesChangedCallback callback : variablesChangedHandlers) {
        OsHandler.getInstance().post(callback);
      }
    }
  }

  /**
   * Add a callback for when no more file downloads are pending (either when
   * no files needed to be downloaded or all downloads have been completed).
   */
  public static void addVariablesChangedAndNoDownloadsPendingHandler(
      VariablesChangedCallback handler) {
    if (handler == null) {
      Log.e(TAG, "addVariablesChangedAndNoDownloadsPendingHandler - Invalid handler parameter " +
          "provided.");
      return;
    }

    synchronized (noDownloadsHandlers) {
      noDownloadsHandlers.add(handler);
    }
    if (VarCache.hasReceivedDiffs()
        && LeanplumRequest.numPendingDownloads() == 0) {
      handler.variablesChanged();
    }
  }

  /**
   * Removes a variables changed and no downloads pending callback.
   */
  public static void removeVariablesChangedAndNoDownloadsPendingHandler(
      VariablesChangedCallback handler) {
    if (handler == null) {
      Log.e(TAG, "removeVariablesChangedAndNoDownloadsPendingHandler - Invalid handler parameter " +
          "provided.");
      return;
    }

    synchronized (noDownloadsHandlers) {
      noDownloadsHandlers.remove(handler);
    }
  }

  /**
   * Add a callback to call ONCE when no more file downloads are pending (either when
   * no files needed to be downloaded or all downloads have been completed).
   */
  public static void addOnceVariablesChangedAndNoDownloadsPendingHandler(
      VariablesChangedCallback handler) {
    if (handler == null) {
      Log.e(TAG, "addOnceVariablesChangedAndNoDownloadsPendingHandler - Invalid handler parameter" +
          " provided.");
      return;
    }

    if (VarCache.hasReceivedDiffs()
        && LeanplumRequest.numPendingDownloads() == 0) {
      handler.variablesChanged();
    } else {
      synchronized (onceNoDownloadsHandlers) {
        onceNoDownloadsHandlers.add(handler);
      }
    }
  }

  /**
   * Removes a once variables changed and no downloads pending callback.
   */
  public static void removeOnceVariablesChangedAndNoDownloadsPendingHandler(
      VariablesChangedCallback handler) {
    if (handler == null) {
      Log.e(TAG, "removeOnceVariablesChangedAndNoDownloadsPendingHandler - Invalid handler" +
          " parameter provided.");
      return;
    }

    synchronized (onceNoDownloadsHandlers) {
      onceNoDownloadsHandlers.remove(handler);
    }
  }

  static void triggerVariablesChangedAndNoDownloadsPending() {
    synchronized (noDownloadsHandlers) {
      for (VariablesChangedCallback callback : noDownloadsHandlers) {
        OsHandler.getInstance().post(callback);
      }
    }
    synchronized (onceNoDownloadsHandlers) {
      for (VariablesChangedCallback callback : onceNoDownloadsHandlers) {
        OsHandler.getInstance().post(callback);
      }
      onceNoDownloadsHandlers.clear();
    }
  }

  /**
   * Defines an action that is used within Leanplum Marketing Automation.
   * Actions can be set up to get triggered based on app opens, events, and states.
   * Call {@link Leanplum#onAction} to handle the action.
   * @param name The name of the action to register.
   * @param kind Whether to display the action as a message and/or a regular action.
   * @param args User-customizable options for the action.
   */
  public static void defineAction(String name, int kind, ActionArgs args) {
    defineAction(name, kind, args, null, null);
  }

  @Deprecated
  static void defineAction(String name, int kind, ActionArgs args,
      Map<String, Object> options) {
    defineAction(name, kind, args, options, null);
  }

  /**
   * Defines an action that is used within Leanplum Marketing Automation.
   * Actions can be set up to get triggered based on app opens, events, and states.
   * @param name The name of the action to register.
   * @param kind Whether to display the action as a message and/or a regular action.
   * @param args User-customizable options for the action.
   * @param responder Called when the action is triggered with a context object containing the
   * user-specified options.
   */
  public static void defineAction(String name, int kind, ActionArgs args,
      ActionCallback responder) {
    defineAction(name, kind, args, null, responder);
  }

  private static void defineAction(String name, int kind, ActionArgs args,
      Map<String, Object> options, ActionCallback responder) {
    if (TextUtils.isEmpty(name)) {
      Log.e(TAG, "defineAction - Empty name parameter provided.");
      return;
    }
    if (args == null) {
      Log.e(TAG, "defineAction - Invalid args parameter provided.");
      return;
    }

    try {
      Context context = Leanplum.getContext();
      if (!initializedMessageTemplates) {
        initializedMessageTemplates = true;
        MessageTemplates.register(context);
      }

      if (options == null) {
        options = new HashMap<>();
      }
      actionHandlers.remove(name);
      VarCache.registerActionDefinition(name, kind, args.getValue(), options);
      if (responder != null) {
        onAction(name, responder);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Adds a callback that handles an action with the given name.
   * @param actionName The name of the type of action to handle.
   * @param handler The callback that runs when the action is triggered.
   */
  public static void onAction(String actionName, ActionCallback handler) {
    if (actionName == null) {
      Log.e(TAG, "onAction - Invalid actionName parameter provided.");
      return;
    }
    if (handler == null) {
      Log.e(TAG, "onAction - Invalid handler parameter provided.");
      return;
    }

    List<ActionCallback> handlers = actionHandlers.get(actionName);
    if (handlers == null) {
      handlers = new ArrayList<>();
      actionHandlers.put(actionName, handlers);
    }
    handlers.add(handler);
  }

  static void triggerAction(ActionContext context) {
    triggerAction(context, null);
  }

  private static void triggerAction(final ActionContext context,
      final VariablesChangedCallback handledCallback) {
    List<ActionCallback> callbacks;
    synchronized (actionHandlers) {
      List<ActionCallback> handlers = actionHandlers.get(context.actionName());
      if (handlers == null) {
        // Handled by default.
        if (handledCallback != null) {
          handledCallback.variablesChanged();
        }
        return;
      }
      callbacks = new ArrayList<>(handlers);
    }
    final AtomicBoolean handled = new AtomicBoolean(false);
    for (final ActionCallback callback : callbacks) {
      OsHandler.getInstance().post(new Runnable() {
        @Override
        public void run() {
          if (callback.onResponse(context) && handledCallback != null) {
            if (!handled.getAndSet(true)) {
              handledCallback.variablesChanged();
            }
          }
        }
      });
    }
  }

  static void maybePerformActions(String when, String eventName, int filter,
      String fromMessageId, ContextualValues contextualValues) {
    maybePerformActions(new String[] {when}, eventName, filter, fromMessageId, contextualValues);
  }

  @SuppressWarnings("unchecked")
  private static void maybePerformActions(String[] whenConditions, String eventName, int filter,
      String fromMessageId, ContextualValues contextualValues) {
    Map<String, Object> messages = VarCache.messages();
    if (messages == null) {
      return;
    }

    ArrayList<ActionContext> actionContexts = new ArrayList<>();
    for (final String messageId : messages.keySet()) {
      if (messageId != null && messageId.equals(fromMessageId)) {
        continue;
      }

      Map<String, Object> messageConfig = (Map<String, Object>) messages.get(messageId);

      String actionType = (String) messageConfig.get("action");
      if (actionType == null) {
        continue;
      }

      boolean isForeground = !actionType.equals("__Push Notification");
      if (isForeground &&
          ((filter & LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_FOREGROUND) == 0)) {
        continue;
      }

      MessageMatchResult result = new MessageMatchResult();
      for (String when : whenConditions) {
        MessageMatchResult conditionResult = actionManager.shouldShowMessage(
            messageId, messageConfig, when, eventName, contextualValues);
        result.matchedTrigger |= conditionResult.matchedTrigger;
        result.matchedUnlessTrigger |= conditionResult.matchedUnlessTrigger;
        result.matchedLimit |= conditionResult.matchedLimit;
      }

      // Make sure we cancel before matching in case the criteria overlap.
      if (result.matchedUnlessTrigger) {
        String cancelActionName = "__Cancel" + messageConfig.get("action");
        triggerAction(new ActionContext(
                cancelActionName, new HashMap<String, Object>(), messageId),
            new VariablesChangedCallback() {
              @Override
              public void variablesChanged() {
                // Track cancel.
                try {
                  Map<String, String> requestArgs = new HashMap<>();
                  requestArgs.put(Constants.Params.MESSAGE_ID, messageId);
                  Leanplum.track("Cancel", 0.0, null, null, requestArgs);
                } catch (Throwable t) {
                  Util.handleException(t);
                }
              }
            });
      }

      if (result.matchedTrigger) {
        actionManager.recordMessageTrigger(messageId);

        if (result.matchedLimit) {
          int priority;
          if (messageConfig.containsKey("priority")) {
            priority = (int) messageConfig.get("priority");
          } else {
            priority = Constants.Messaging.DEFAULT_PRIORITY;
          }
          ActionContext actionContext = new ActionContext(
              messageConfig.get("action").toString(),
              (Map<String, Object>) messageConfig.get(Constants.Keys.VARS), messageId, priority);
          actionContext.setContextualValues(contextualValues);
          actionContexts.add(actionContext);
        }
      }
    }

    if (!actionContexts.isEmpty()) {
      Collections.sort(actionContexts);
      int priorityThreshold = actionContexts.get(0).priority();
      for (final ActionContext actionContext : actionContexts) {
        int priority = actionContext.priority();
        if (priority <= priorityThreshold) {
          triggerAction(actionContext, new VariablesChangedCallback() {
            @Override
            public void variablesChanged() {
              try {
                actionManager.recordMessageImpression(actionContext.messageId());
              } catch (Throwable t) {
                Util.handleException(t);
              }
            }
          });
        } else {
          break;
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Deprecated
  static void performAction(String actionName, String messageId) {
    Map<String, Object> messages = VarCache.messages();
    if (messages == null) {
      return;
    }
    Map<String, Object> messageConfig = (Map<String, Object>) messages.get(messageId);
    if (messageConfig == null) {
      return;
    }
    ActionContext context = new ActionContext(
        messageConfig.get("action").toString(),
        (Map<String, Object>) messageConfig.get(Constants.Keys.VARS),
        messageId);
    context.runActionNamed(actionName);
  }

  @SuppressWarnings("unchecked")
  static void performTrackedAction(String actionName, String messageId) {
    Map<String, Object> messages = VarCache.messages();
    if (messages == null) {
      return;
    }
    Map<String, Object> messageConfig = (Map<String, Object>) messages.get(messageId);
    if (messageConfig == null) {
      return;
    }
    ActionContext context = new ActionContext(
        messageConfig.get("action").toString(),
        (Map<String, Object>) messageConfig.get(Constants.Keys.VARS),
        messageId);
    context.runTrackedActionNamed(actionName);
  }

  /**
   * Updates the user ID and adds or modifies user attributes.
   */
  public static void setUserAttributes(final String userId, Map<String, ?> userAttributes) {
    if (Constants.isNoop()) {
      return;
    }
    if (!calledStart) {
      Log.e(TAG, "You cannot call setUserAttributes before calling start");
      return;
    }
    try {
      final HashMap<String, Object> params = new HashMap<>();
      if (userId != null) {
        params.put(Constants.Params.NEW_USER_ID, userId);
      }
      if (userAttributes != null) {
        userAttributes = validateAttributes(userAttributes, "userAttributes", true);
        params.put(Constants.Params.USER_ATTRIBUTES, JsonConverter.toJson(userAttributes));
        userAttributeChanges.add(userAttributes);
      }

      if (issuedStart) {
        setUserAttributesInternal(userId, params);
      } else {
        Leanplum.addStartIssuedHandler(new Runnable() {
          @Override
          public void run() {
            try {
              setUserAttributesInternal(userId, params);
            } catch (Throwable t) {
              Util.handleException(t);
            }
          }
        });
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  private static void setUserAttributesInternal(String userId,
      HashMap<String, Object> requestArgs) {
    LeanplumRequest.post(Constants.Methods.SET_USER_ATTRIBUTES, requestArgs).send();
    if (userId != null && userId.length() > 0) {
      LeanplumRequest.setUserId(userId);
      if (hasStarted) {
        VarCache.saveDiffs();
      }
    }
    recordAttributeChanges();
  }

  private static void recordAttributeChanges() {
    boolean madeChanges = false;
    for (Map<String, ?> attributes : userAttributeChanges) {
      Map<String, Object> existingAttributes = VarCache.userAttributes();
      for (String attributeName : attributes.keySet()) {
        Object existingValue = existingAttributes.get(attributeName);
        Object value = attributes.get(attributeName);
        if ((existingValue == null && value != null) ||
            (existingValue != null && !existingValue.equals(value))) {
          ContextualValues contextualValues = new ContextualValues();
          contextualValues.previousAttributeValue = existingValue;
          contextualValues.attributeValue = value;
          existingAttributes.put(attributeName, value);
          maybePerformActions("userAttribute", attributeName,
              LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL, null, contextualValues);
          madeChanges = true;
        }
      }
    }
    userAttributeChanges.clear();
    if (madeChanges) {
      VarCache.saveUserAttributes();
    }
  }

  /**
   * Updates the user ID.
   */
  public static void setUserId(String userId) {
    if (userId == null) {
      Log.e(TAG, "setUserId - Invalid userId parameter provided.");
      return;
    }

    setUserAttributes(userId, null);
  }

  /**
   * Adds or modifies user attributes.
   */
  public static void setUserAttributes(Map<String, Object> userAttributes) {
    if (userAttributes == null || userAttributes.isEmpty()) {
      Log.e(TAG, "setUserAttributes - Invalid userAttributes parameter provided (null or empty).");
      return;
    }

    setUserAttributes(null, userAttributes);
  }

  /**
   * Sets the registration ID used for GCM.
   */
  static void setGcmRegistrationId(final String registrationId) {
    if (Constants.isNoop()) {
      return;
    }
    gcmStartCallback = new Runnable() {
      @Override
      public void run() {
        if (Constants.isNoop()) {
          return;
        }
        try {
          HashMap<String, Object> params = new HashMap<>();
          params.put(Constants.Params.DEVICE_PUSH_TOKEN, registrationId);
          LeanplumRequest.post(Constants.Methods.SET_DEVICE_ATTRIBUTES, params).send();
        } catch (Throwable t) {
          Util.handleException(t);
        }
      }
    };
    Leanplum.addStartIssuedHandler(gcmStartCallback);
  }

  /**
   * Sets the traffic source info for the current user.
   * Keys in info must be one of: publisherId, publisherName, publisherSubPublisher,
   * publisherSubSite, publisherSubCampaign, publisherSubAdGroup, publisherSubAd.
   */
  public static void setTrafficSourceInfo(Map<String, String> info) {
    if (Constants.isNoop()) {
      return;
    }
    if (!calledStart) {
      Log.e(TAG, "You cannot call setTrafficSourceInfo before calling start");
      return;
    }
    if (info == null || info.isEmpty()) {
      Log.e(TAG, "setTrafficSourceInfo - Invalid info parameter provided (null or empty).");
      return;
    }

    try {
      final HashMap<String, Object> params = new HashMap<>();
      info = validateAttributes(info, "info", false);
      params.put(Constants.Params.TRAFFIC_SOURCE, JsonConverter.toJson(info));
      if (issuedStart) {
        setTrafficSourceInfoInternal(params);
      } else {
        Leanplum.addStartIssuedHandler(new Runnable() {
          @Override
          public void run() {
            try {
              setTrafficSourceInfoInternal(params);
            } catch (Throwable t) {
              Util.handleException(t);
            }
          }
        });
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  private static void setTrafficSourceInfoInternal(HashMap<String, Object> params) {
    LeanplumRequest.post(Constants.Methods.SET_TRAFFIC_SOURCE_INFO, params).send();
  }

  /**
   * Logs a particular event in your application. The string can be
   * any value of your choosing, and will show up in the dashboard.
   *
   * <p>To track Purchase events, call {@link Leanplum#trackGooglePlayPurchase} instead for
   * in-app purchases, or use {@link Leanplum#PURCHASE_EVENT_NAME} as the event name for other types
   * of purchases.
   *
   * @param event Name of the event. Event may be empty for message impression events.
   * @param value The value of the event. The value is special in that you can use it for
   * targeting content and messages to users who have a particular lifetime value.
   * For purchase events, the value is the revenue associated with the purchase.
   * @param info Basic context associated with the event, such as the item purchased.
   * info is treated like a default parameter.
   * @param params Key-value pairs with metrics or data associated with the event. Parameters
   * can be strings or numbers. You can use up to 200 different parameter names in your app.
   */
  public static void track(final String event, double value, String info,
      Map<String, ?> params) {
    track(event, value, info, params, null);
  }

  static void track(final String event, double value, String info,
      Map<String, ?> params, Map<String, String> args) {
    if (Constants.isNoop()) {
      return;
    }
    if (!calledStart) {
      Log.e(TAG, "You cannot call track before calling start");
      return;
    }

    try {
      final Map<String, Object> requestParams = new HashMap<>();
      if (args != null) {
        requestParams.putAll(args);
      }
      requestParams.put(Constants.Params.VALUE, "" + value);
      requestParams.put(Constants.Params.INFO, info);
      if (event != null) {
        requestParams.put(Constants.Params.EVENT, event);
      }
      if (params != null) {
        params = validateAttributes(params, "params", false);
        requestParams.put(Constants.Params.PARAMS, JsonConverter.toJson(params));
      }
      if (!inForeground || LeanplumActivityHelper.isActivityPaused) {
        requestParams.put("allowOffline", Boolean.TRUE.toString());
      }

      trackInternalWhenStarted(event, params, requestParams);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Performs the track operation once Leanplum has started. If Leanplum is already started,
   * perform the track immediately.
   * @param event The event name. Event may be empty for message impression events.
   * @param params The event parameters.
   * @param requestArgs The arguments to send with the API request.
   */
  private static void trackInternalWhenStarted(final String event,
      final Map<String, ?> params, final Map<String, Object> requestArgs) {
    if (issuedStart) {
      trackInternal(event, params, requestArgs);
    } else {
      Leanplum.addStartIssuedHandler(new Runnable() {
        @Override
        public void run() {
          try {
            trackInternal(event, params, requestArgs);
          } catch (Throwable t) {
            Util.handleException(t);
          }
        }
      });
    }
  }

  /**
   * Performs the track API and any actions that are associated with the event.
   * @param event The event name. Event may be empty for message impression events.
   * @param params The event parameters.
   * @param requestArgs The arguments to send with the API request.
   */
  private static void trackInternal(String event, Map<String, ?> params,
      Map<String, Object> requestArgs) {
    LeanplumRequest.post(Constants.Methods.TRACK, requestArgs).send();

    String eventTriggerName = event;
    String messageId = null;
    if (requestArgs.get(Constants.Params.MESSAGE_ID) != null) {
      messageId = requestArgs.get(Constants.Params.MESSAGE_ID).toString();
      eventTriggerName = ".m" + messageId;
      if (event != null && event.length() > 0) {
        eventTriggerName += " " + event;
      }
    }

    ContextualValues contextualValues = new ContextualValues();
    contextualValues.parameters = params;

    maybePerformActions("event", eventTriggerName,
        LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL, messageId, contextualValues);
  }

  /**
   * Tracks an in-app purchase as a Purchase event.
   * @param item The name of the item that was purchased.
   * @param priceMicros The price in micros in the user's local currency.
   * @param currencyCode The currency code corresponding to the price.
   * @param purchaseData Purchase data from purchase.getOriginalJson().
   * @param dataSignature Signature from purchase.getSignature().
   */
  public static void trackGooglePlayPurchase(String item, long priceMicros, String currencyCode,
      String purchaseData, String dataSignature) {
    trackGooglePlayPurchase(PURCHASE_EVENT_NAME, item, priceMicros, currencyCode, purchaseData,
        dataSignature, null);
  }

  /**
   * Tracks an in-app purchase as a Purchase event.
   * @param item The name of the item that was purchased.
   * @param priceMicros The price in micros in the user's local currency.
   * @param currencyCode The currency code corresponding to the price.
   * @param purchaseData Purchase data from purchase.getOriginalJson().
   * @param dataSignature Signature from purchase.getSignature().
   * @param params Any additional parameters to track with the event.
   */
  public static void trackGooglePlayPurchase(String item, long priceMicros, String currencyCode,
      String purchaseData, String dataSignature, Map<String, ?> params) {
    trackGooglePlayPurchase(PURCHASE_EVENT_NAME, item, priceMicros, currencyCode,
        purchaseData, dataSignature, params);
  }

  /**
   * Tracks an in-app purchase.
   * @param eventName The name of the event to record the purchase under. Normally, this would be
   *     {@link Leanplum#PURCHASE_EVENT_NAME}.
   * @param item The name of the item that was purchased.
   * @param priceMicros The price in micros in the user's local currency.
   * @param currencyCode The currency code corresponding to the price.
   * @param purchaseData Purchase data from purchase.getOriginalJson().
   * @param dataSignature Signature from purchase.getSignature().
   * @param params Any additional parameters to track with the event.
   */
  @SuppressWarnings("SameParameterValue")
  public static void trackGooglePlayPurchase(String eventName, String item, long priceMicros,
      String currencyCode, String purchaseData, String dataSignature, Map<String, ?> params) {
    if (TextUtils.isEmpty(eventName)) {
      Log.w(TAG, "trackGooglePlayPurchase - Empty eventName parameter provided.");
    }

    final Map<String, String> requestArgs = new HashMap<>();
    requestArgs.put(Constants.Params.GOOGLE_PLAY_PURCHASE_DATA, purchaseData);
    requestArgs.put(Constants.Params.GOOGLE_PLAY_PURCHASE_DATA_SIGNATURE, dataSignature);
    requestArgs.put(Constants.Params.IAP_CURRENCY_CODE, currencyCode);

    Map<String, Object> modifiedParams;
    if (params == null) {
      modifiedParams = new HashMap<>();
    } else {
      modifiedParams = new HashMap<>(params);
    }
    modifiedParams.put(Constants.Params.IAP_ITEM, item);

    track(eventName, priceMicros / 1000000.0, null, modifiedParams, requestArgs);
  }

  /**
   * Logs a particular event in your application. The string can be
   * any value of your choosing, and will show up in the dashboard.
   *
   * <p>To track Purchase events, use {@link Leanplum#PURCHASE_EVENT_NAME}.
   *
   * @param event Name of the event.
   */
  public static void track(String event) {
    track(event, 0.0, "", null);
  }

  /**
   * Logs a particular event in your application. The string can be
   * any value of your choosing, and will show up in the dashboard.
   *
   * <p>To track Purchase events, use {@link Leanplum#PURCHASE_EVENT_NAME}.
   *
   * @param event Name of the event.
   * @param value The value of the event. The value is special in that you can use it for
   * targeting content and messages to users who have a particular lifetime value.
   * For purchase events, the value is the revenue associated with the purchase.
   */
  public static void track(String event, double value) {
    track(event, value, "", null);
  }

  /**
   * Logs a particular event in your application. The string can be
   * any value of your choosing, and will show up in the dashboard.
   *
   * <p>To track Purchase events, use {@link Leanplum#PURCHASE_EVENT_NAME}.
   *
   * @param event Name of the event.
   * @param info Basic context associated with the event, such as the item purchased.
   * info is treated like a default parameter.
   */
  public static void track(String event, String info) {
    track(event, 0.0, info, null);
  }

  /**
   * Logs a particular event in your application. The string can be
   * any value of your choosing, and will show up in the dashboard.
   *
   * <p>To track Purchase events, use {@link Leanplum#PURCHASE_EVENT_NAME}.
   *
   * @param event Name of the event.
   * @param params Key-value pairs with metrics or data associated with the event. Parameters
   * can be strings or numbers. You can use up to 200 different parameter names in your app.
   */
  public static void track(String event, Map<String, ?> params) {
    track(event, 0.0, "", params);
  }

  /**
   * Logs a particular event in your application. The string can be
   * any value of your choosing, and will show up in the dashboard.
   *
   * <p>To track Purchase events, use {@link Leanplum#PURCHASE_EVENT_NAME}.
   *
   * @param event Name of the event.
   * @param value The value of the event. The value is special in that you can use it for
   * targeting content and messages to users who have a particular lifetime value.
   * For purchase events, the value is the revenue associated with the purchase.
   * @param params Key-value pairs with metrics or data associated with the event. Parameters
   * can be strings or numbers. You can use up to 200 different parameter names in your app.
   */
  public static void track(String event, double value, Map<String, ?> params) {
    track(event, value, "", params);
  }

  /**
   * Logs a particular event in your application. The string can be
   * any value of your choosing, and will show up in the dashboard.
   *
   * <p>To track Purchase events, use {@link Leanplum#PURCHASE_EVENT_NAME}.
   *
   * @param event Name of the event.
   * @param value The value of the event. The value is special in that you can use it for
   * targeting content and messages to users who have a particular lifetime value.
   * For purchase events, the value is the revenue associated with the purchase.
   * @param info Basic context associated with the event, such as the item purchased.
   * info is treated like a default parameter.
   */
  public static void track(String event, double value, String info) {
    track(event, value, info, null);
  }

  /**
   * Advances to a particular state in your application. The string can be
   * any value of your choosing, and will show up in the dashboard.
   * A state is a section of your app that the user is currently in.
   *
   * @param state Name of the state. State may be empty for message impression events.
   * @param info Basic context associated with the state, such as the item purchased.
   * info is treated like a default parameter.
   * @param params Key-value pairs with metrics or data associated with the state. Parameters
   * can be strings or numbers. You can use up to 200 different parameter names in your app.
   */
  public static void advanceTo(final String state, String info, final Map<String, ?> params) {
    if (Constants.isNoop()) {
      return;
    }
    if (!calledStart) {
      Log.e(TAG, "You cannot call advanceTo before calling start");
      return;
    }

    try {
      final Map<String, Object> requestParams = new HashMap<>();
      requestParams.put(Constants.Params.INFO, info);
      requestParams.put(Constants.Params.STATE, state);
      final Map<String, ?> validatedParams;
      if (params != null) {
        validatedParams = validateAttributes(params, "params", false);
        requestParams.put(Constants.Params.PARAMS, JsonConverter.toJson(validatedParams));
      } else {
        validatedParams = null;
      }

      if (issuedStart) {
        advanceToInternal(state, validatedParams, requestParams);
      } else {
        Leanplum.addStartIssuedHandler(new Runnable() {
          @Override
          public void run() {
            try {
              advanceToInternal(state, validatedParams, requestParams);
            } catch (Throwable t) {
              Util.handleException(t);
            }
          }
        });
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Performs the advance API and any actions that are associated with the state.
   * @param state The state name. State may be empty for message impression events.
   * @param params The state parameters.
   * @param requestParams The arguments to send with the API request.
   */
  private static void advanceToInternal(String state, Map<String, ?> params,
      Map<String, Object> requestParams) {
    LeanplumRequest.post(Constants.Methods.ADVANCE, requestParams).send();

    ContextualValues contextualValues = new ContextualValues();
    contextualValues.parameters = params;

    maybePerformActions("state", state, LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL,
        null, contextualValues);
  }

  /**
   * Advances to a particular state in your application. The string can be
   * any value of your choosing, and will show up in the dashboard.
   * A state is a section of your app that the user is currently in.
   *
   * @param state Name of the state. State may be empty for message impression events.
   */
  public static void advanceTo(String state) {
    advanceTo(state, "", null);
  }

  /**
   * Advances to a particular state in your application. The string can be
   * any value of your choosing, and will show up in the dashboard.
   * A state is a section of your app that the user is currently in.
   *
   * @param state Name of the state. State may be empty for message impression events.
   * @param info Basic context associated with the state, such as the item purchased.
   * info is treated like a default parameter.
   */
  public static void advanceTo(String state, String info) {
    advanceTo(state, info, null);
  }

  /**
   * Advances to a particular state in your application. The string can be
   * any value of your choosing, and will show up in the dashboard.
   * A state is a section of your app that the user is currently in.
   *
   * @param state Name of the state. State may be empty for message impression events.
   * @param params Key-value pairs with metrics or data associated with the state. Parameters
   * can be strings or numbers. You can use up to 200 different parameter names in your app.
   */
  public static void advanceTo(String state, Map<String, ?> params) {
    advanceTo(state, "", params);
  }

  /**
   * Pauses the current state.
   * You can use this if your game has a "pause" mode. You shouldn't call it
   * when someone switches out of your app because that's done automatically.
   */
  public static void pauseState() {
    if (Constants.isNoop()) {
      return;
    }
    if (!calledStart) {
      Log.e(TAG, "You cannot call pauseState before calling start");
      return;
    }

    try {
      if (issuedStart) {
        pauseStateInternal();
      } else {
        Leanplum.addStartIssuedHandler(new Runnable() {
          @Override
          public void run() {
            try {
              pauseStateInternal();
            } catch (Throwable t) {
              Util.handleException(t);
            }
          }
        });
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  private static void pauseStateInternal() {
    LeanplumRequest.post(Constants.Methods.PAUSE_STATE, new HashMap<String, Object>()).send();
  }

  /**
   * Resumes the current state.
   */
  public static void resumeState() {
    if (Constants.isNoop()) {
      return;
    }
    if (!calledStart) {
      Log.e(TAG, "You cannot call resumeState before calling start");
      return;
    }

    try {
      if (issuedStart) {
        resumeStateInternal();
      } else {
        Leanplum.addStartIssuedHandler(new Runnable() {
          @Override
          public void run() {
            try {
              resumeStateInternal();
            } catch (Throwable t) {
              Util.handleException(t);
            }
          }
        });
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  private static void resumeStateInternal() {
    LeanplumRequest.post(Constants.Methods.RESUME_STATE, new HashMap<String, Object>()).send();
  }

  /**
   * Forces content to update from the server. If variables have changed, the
   * appropriate callbacks will fire. Use sparingly as if the app is updated,
   * you'll have to deal with potentially inconsistent state or user experience.
   */
  public static void forceContentUpdate() {
    forceContentUpdate(null);
  }

  /**
   * Forces content to update from the server. If variables have changed, the
   * appropriate callbacks will fire. Use sparingly as if the app is updated,
   * you'll have to deal with potentially inconsistent state or user experience.
   *
   * @param callback The callback to invoke when the call completes from the server.
   *        The callback will fire regardless of whether the variables have changed.
   */
  @SuppressWarnings("SameParameterValue")
  public static void forceContentUpdate(final VariablesChangedCallback callback) {
    if (Constants.isNoop()) {
      if (callback != null) {
        OsHandler.getInstance().post(callback);
      }
      return;
    }
    try {
      Map<String, Object> params = new HashMap<>();
      params.put(Constants.Params.INCLUDE_DEFAULTS, "" + false);
      params.put(Constants.Params.NEWSFEED_MESSAGES, Newsfeed.getInstance().messagesIds());
      LeanplumRequest req = LeanplumRequest.post(Constants.Methods.GET_VARS, params);
      req.onResponse(new LeanplumRequest.ResponseCallback() {
        @Override
        public void response(JSONObject response) {
          try {
            JSONObject lastResponse = LeanplumRequest.getLastResponse(response);
            if (lastResponse == null) {
              Log.e(TAG,
                  "No response received from the server. Please contact us to investigate.");
            } else {
              Map<String, Object> values = JsonConverter.mapFromJson(
                  lastResponse.optJSONObject(Constants.Keys.VARS));
              Map<String, Object> messages = JsonConverter.mapFromJson(
                  lastResponse.optJSONObject(Constants.Keys.MESSAGES));
              Map<String, Object> regions = JsonConverter.mapFromJson(
                  lastResponse.optJSONObject(Constants.Keys.REGIONS));
              List<Map<String, Object>> variants = JsonConverter.listFromJson(
                  lastResponse.optJSONArray(Constants.Keys.VARIANTS));
              if (!VarCache.diffs().equals(values)
                  || !VarCache.messageDiffs().equals(messages)) {
                VarCache.applyVariableDiffs(values, messages, regions, variants);
              }
              if (lastResponse.optBoolean(Constants.Keys.SYNC_NEWSFEED, false)) {
                Newsfeed.getInstance().downloadMessages();
              }
            }
            if (callback != null) {
              OsHandler.getInstance().post(callback);
            }
          } catch (Throwable t) {
            Util.handleException(t);
          }
        }
      });
      req.onError(new LeanplumRequest.ErrorCallback() {
        @Override
        public void error(Exception e) {
          if (callback != null) {
            OsHandler.getInstance().post(callback);
          }
        }
      });
      req.sendIfConnected();
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * This should be your first statement in a unit test. This prevents
   * Leanplum from communicating with the server.
   */
  public static void enableTestMode() {
    Constants.isTestMode = true;
  }

  public static boolean isTestModeEnabled() {
    return Constants.isTestMode;
  }

  /**
   * This should be your first statement in a unit test. This prevents
   * Leanplum from communicating with the server.
   */
  public static void setIsTestModeEnabled(boolean isTestModeEnabled) {
    Constants.isTestMode = isTestModeEnabled;
  }

  /**
   * Gets the path for a particular resource.
   * The resource can be overridden by the server.
   */
  public static String pathForResource(String filename) {
    if (TextUtils.isEmpty(filename)) {
      Log.e(TAG, "pathForResource - Empty filename parameter provided.");
      return null;
    }

    Var fileVar = Var.defineFile(filename, filename);
    return (fileVar != null) ? fileVar.fileValue() : null;
  }

  /**
   * Traverses the variable structure with the specified path.
   * Path components can be either strings representing keys in a dictionary,
   * or integers representing indices in a list.
   */
  public static Object objectForKeyPath(Object... components) {
    if (components == null || components.length == 0) {
      Log.e(TAG, "objectForKeyPath - No components parameter provided.");
      return null;
    }
    try {
      return VarCache.getMergedValueFromComponentArray(components);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * Traverses the variable structure with the specified path.
   * Path components can be either strings representing keys in a dictionary,
   * or integers representing indices in a list.
   */
  public static Object objectForKeyPathComponents(Object[] pathComponents) {
    if (pathComponents == null || pathComponents.length == 0) {
      Log.e(TAG, "objectForKeyPathComponents - Invalid pathComponents parameter provided (null or" +
          " empty).");
      return null;
    }
    try {
      return VarCache.getMergedValueFromComponentArray(pathComponents);
    } catch (Throwable t) {
      Util.handleException(t);
    }
    return null;
  }

  /**
   * Returns information about the active variants for the current user.
   * Each variant will contain an "id" key mapping to the numeric ID of the variant.
   */
  public static List<Map<String, Object>> variants() {
    List<Map<String, Object>> variants = VarCache.variants();
    if (variants == null) {
      return new ArrayList<>();
    }
    return variants;
  }
}

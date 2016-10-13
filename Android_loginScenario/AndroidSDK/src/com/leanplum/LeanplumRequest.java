// Copyright 2013, Leanplum, Inc.

package com.leanplum;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.leanplum.Leanplum.OsHandler;

import org.apache.http.HttpException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

/**
 * Leanplum request class.
 *
 * @author Andrew First
 */
class LeanplumRequest {
  private static final long DEVELOPMENT_MIN_DELAY_MS = 100;
  static long DEVELOPMENT_MAX_DELAY_MS = 5000;
  private static final long PRODUCTION_DELAY = 60000;

  private static String appId;
  private static String accessKey;
  private static String deviceId;
  private static String userId;
  private static Map<String, Boolean> fileTransferStatus = new HashMap<String, Boolean>();
  private static int pendingDownloads;
  private static NoPendingDownloadsCallback noPendingDownloadsBlock;

  // The token is saved primarily for legacy SharedPreferences decryption. This could
  // likely be removed in the future.
  private static String token = null;
  private static final Object lock = LeanplumRequest.class;
  private static Map<File, Long> fileUploadSize = new HashMap<File, Long>();
  private static Map<File, Double> fileUploadProgress = new HashMap<File, Double>();
  private static String fileUploadProgressString = "";
  private static long lastSendTimeMs;
  private static final Object uploadFileLock = new Object();

  public static void setAppId(String appId, String accessKey) {
    LeanplumRequest.appId = appId;
    LeanplumRequest.accessKey = accessKey;
  }

  public static void setDeviceId(String deviceId) {
    LeanplumRequest.deviceId = deviceId;
  }

  public static void setUserId(String userId) {
    LeanplumRequest.userId = userId;
  }

  public static void setToken(String token) {
    LeanplumRequest.token = token;
  }

  public static String token() {
    return token;
  }

  public static void loadToken() {
    Context context = Leanplum.getContext();
    SharedPreferences defaults = context.getSharedPreferences(
        "__leanplum__", Context.MODE_PRIVATE);
    String token = defaults.getString(Constants.Defaults.TOKEN_KEY, null);
    if (token == null) {
      return;
    }
    setToken(token);
  }

  public static void saveToken() {
    Context context = Leanplum.getContext();
    SharedPreferences defaults = context.getSharedPreferences(
        "__leanplum__", Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = defaults.edit();
    editor.putString(Constants.Defaults.TOKEN_KEY, LeanplumRequest.token());
    try {
      editor.apply();
    } catch (NoSuchMethodError e) {
      editor.commit();
    }
  }

  public static String appId() {
    return appId;
  }

  public static String deviceId() {
    return deviceId;
  }

  public static String userId() {
    return LeanplumRequest.userId;
  }

  private String httpMethod;
  private String apiMethod;
  private Map<String, Object> params;
  private ResponseCallback response;
  private ErrorCallback error;
  private boolean sent;

  static ApiResponseCallback apiResponse;

  LeanplumRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
    this.httpMethod = httpMethod;
    this.apiMethod = apiMethod;
    this.params = params != null ? params : new HashMap<String, Object>();

    // Make sure the Handler is initialized on the main thread.
    OsHandler.getInstance();
  }

  public static LeanplumRequest get(String apiMethod, Map<String, Object> params) {
    return RequestFactory.getInstance().createRequest("GET", apiMethod, params);
  }

  public static LeanplumRequest post(String apiMethod, Map<String, Object> params) {
    return RequestFactory.getInstance().createRequest("POST", apiMethod, params);
  }

  public void onResponse(ResponseCallback response) {
    this.response = response;
  }

  public void onError(ErrorCallback error) {
    this.error = error;
  }

  public void onApiResponse(ApiResponseCallback apiResponse) {
    LeanplumRequest.apiResponse = apiResponse;
  }

  public Map<String, Object> createArgsDictionary() {
    Map<String, Object> args = new HashMap<String, Object>();
    args.put(Constants.Params.DEVICE_ID, deviceId);
    args.put(Constants.Params.USER_ID, userId);
    args.put(Constants.Params.ACTION, apiMethod);
    args.put(Constants.Params.SDK_VERSION, Constants.LEANPLUM_VERSION);
    args.put(Constants.Params.DEV_MODE, "" + Constants.isDevelopmentModeEnabled);
    args.put(Constants.Params.TIME, "" + (new Date().getTime() / 1000.0));
    if (token != null) {
      args.put(Constants.Params.TOKEN, token);
    }
    args.putAll(params);
    return args;
  }

  static void saveRequestForLater(Map<String, Object> args) {
    synchronized (lock) {
      Context context = Leanplum.getContext();
      SharedPreferences preferences = context.getSharedPreferences(
          "__leanplum__", Context.MODE_PRIVATE);
      SharedPreferences.Editor editor = preferences.edit();
      int count = preferences.getInt(Constants.Defaults.COUNT_KEY, 0);
      String itemKey = String.format(Locale.US, Constants.Defaults.ITEM_KEY, count);
      editor.putString(itemKey, JsonConverter.toJson(args));
      count++;
      editor.putInt(Constants.Defaults.COUNT_KEY, count);
      try {
        editor.apply();
      } catch (NoSuchMethodError e) {
        editor.commit();
      }
    }
  }

  void send() {
    this.sendEventually();
    if (Constants.isDevelopmentModeEnabled) {
      long currentTimeMs = System.currentTimeMillis();
      long delayMs;
      if (lastSendTimeMs == 0 || currentTimeMs - lastSendTimeMs > DEVELOPMENT_MAX_DELAY_MS) {
        delayMs = DEVELOPMENT_MIN_DELAY_MS;
      } else {
        delayMs = (lastSendTimeMs + DEVELOPMENT_MAX_DELAY_MS) - currentTimeMs;
      }
      OsHandler.getInstance().postDelayed(new Runnable() {
        @Override
        public void run() {
          try {
            sendIfConnected();
          } catch (Throwable t) {
            Util.handleException(t);
          }
        }
      }, delayMs);
    }
  }

  /**
   * Wait 1 second for potential other API calls, and then sends the call synchronously
   * if no other call has been sent within 1 minute.
   */
  void sendIfDelayed() {
    sendEventually();
    OsHandler.getInstance().postDelayed(new Runnable() {
      @Override
      public void run() {
        try {
          sendIfDelayedHelper();
        } catch (Throwable t) {
          Util.handleException(t);
        }
      }
    }, 1000);
  }

  /**
   * Sends the call synchronously if no other call has been sent within 1 minute.
   */
  void sendIfDelayedHelper() {
    if (Constants.isDevelopmentModeEnabled) {
      send();
    } else {
      long currentTimeMs = System.currentTimeMillis();
      if (lastSendTimeMs == 0 || currentTimeMs - lastSendTimeMs > PRODUCTION_DELAY) {
        sendIfConnected();
      }
    }
  }

  void sendIfConnected() {
    if (Util.isConnected()) {
      this.sendNow();
    } else {
      this.sendEventually();
      Log.i("Leanplum", "Device is offline, will send later");
      triggerErrorCallback(new Exception("Not connected to the Internet"));
    }
  }

  void triggerErrorCallback(Exception e) {
    if (error != null) {
      error.error(e);
    }
    if (apiResponse != null) {
      List<Map<String, Object>> requests = getUnsentRequests();
      apiResponse.response(requests, null);
    }
  }

  boolean attachApiKeys(Map<String, Object> dict) {
    if (appId == null || accessKey == null) {
      Log.e("Leanplum", "API keys are not set. Please use "
          + "Leanplum.setAppIdForDevelopmentMode or "
          + "Leanplum.setAppIdForProductionMode");
      return false;
    }
    dict.put(Constants.Params.APP_ID, appId);
    dict.put(Constants.Params.CLIENT_KEY, accessKey);
    dict.put(Constants.Params.CLIENT, Constants.CLIENT);
    return true;
  }

  interface ResponseCallback {
    void response(JSONObject response);
  }

  interface ApiResponseCallback {
    void response(List<Map<String, Object>> requests, JSONObject response);
  }

  interface ErrorCallback {
    void error(Exception e);
  }

  interface NoPendingDownloadsCallback {
    void noPendingDownloads();
  }

  private void parseResponseJson(JSONObject responseJson, List<Map<String, Object>> requestsToSend,
      Exception error) {
    if (apiResponse != null) {
      apiResponse.response(requestsToSend, responseJson);
    }

    if (responseJson != null) {
      Exception lastResponseError = null;
      int numResponses = LeanplumRequest.numResponses(responseJson);
      for (int i = 0; i < numResponses; i++) {
        JSONObject response = LeanplumRequest.getResponseAt(responseJson, i);
        if (!LeanplumRequest.isResponseSuccess(response)) {
          String errorMessage = LeanplumRequest.getResponseError(response);
          if (errorMessage == null || errorMessage.length() == 0) {
            errorMessage = "API error";
          } else if (errorMessage.startsWith("App not found")) {
            errorMessage = "No app matching the provided app ID was found.";
            Constants.isInPermanentFailureState = true;
          } else if (errorMessage.startsWith("Invalid access key")) {
            errorMessage = "The access key you provided is not valid for this app.";
            Constants.isInPermanentFailureState = true;
          } else if (errorMessage.startsWith("Development mode requested but not permitted")) {
            errorMessage = "A call to Leanplum.setAppIdForDevelopmentMode "
                + "with your production key was made, which is not permitted.";
            Constants.isInPermanentFailureState = true;
          } else {
            errorMessage = "API error: " + errorMessage;
          }
          Log.e("Leanplum", errorMessage);
          if (i == numResponses - 1) {
            lastResponseError = new Exception(errorMessage);
          }
        }
      }

      if (lastResponseError == null) {
        lastResponseError = error;
      }

      if (lastResponseError != null && this.error != null) {
        this.error.error(lastResponseError);
      } else if (this.response != null) {
        this.response.response(responseJson);
      }
    } else if (error != null && this.error != null) {
        this.error.error(error);
    }
  }

  private void sendNow() {
    if (Constants.isTestMode) {
      return;
    }
    if (appId == null) {
      Log.e("Leanplum", "Cannot send request. appId is not set");
      return;
    }
    if (accessKey == null) {
      Log.e("Leanplum", "Cannot send request. accessKey is not set");
      return;
    }

    this.sendEventually();

    final List<Map<String, Object>> requestsToSend = popUnsentRequests();
    if (requestsToSend.size() == 0) {
      return;
    }

    final Map<String, Object> multiRequestArgs = new HashMap<String, Object>();
    multiRequestArgs.put(Constants.Params.DATA, jsonEncodeUnsentRequests(requestsToSend));
    multiRequestArgs.put(Constants.Params.SDK_VERSION, Constants.LEANPLUM_VERSION);
    multiRequestArgs.put(Constants.Params.ACTION, Constants.Methods.MULTI);
    multiRequestArgs.put(Constants.Params.TIME, "" + (new Date().getTime() / 1000.0));
    if (!this.attachApiKeys(multiRequestArgs)) {
      return;
    }

    Util.executeAsyncTask(new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        JSONObject result = null;
        HttpURLConnection op = null;
        try {
          try {
            op = Util.operation(
                Constants.API_HOST_NAME,
                Constants.API_SERVLET,
                multiRequestArgs,
                httpMethod,
                Constants.API_SSL,
                Constants.NETWORK_TIMEOUT_SECONDS);

            result = Util.getJsonResponse(op);
            int statusCode = op.getResponseCode();

            Exception errorException = null;
            if (statusCode >= 400) {
              errorException = new Exception("HTTP error " + statusCode);
              if (statusCode == 408 || (statusCode >= 500 && statusCode <= 599)) {
                pushUnsentRequests(requestsToSend);
              }
            } else {
              if (result != null) {
                int numResponses = LeanplumRequest.numResponses(result);
                if (numResponses != requestsToSend.size()) {
                  Log.w("Leanplum", "Sent " + requestsToSend.size() + " requests but only"
                      + " received " + numResponses);
                }
              } else {
                errorException = new Exception("Response JSON is null.");
              }
            }
            parseResponseJson(result, requestsToSend, errorException);
          } catch (JSONException e) {
            Log.e("Leanplum", "Error parsing JSON response: " + e.toString(), e);
            parseResponseJson(result, requestsToSend, e);
          } catch (Exception e) {
            pushUnsentRequests(requestsToSend);
            Log.e("Leanplum", "Unable to send request: " + e.toString(), e);
            parseResponseJson(result, requestsToSend, e);
          } finally {
            if (op != null) {
              op.disconnect();
            }
          }
        } catch (Throwable t) {
          Util.handleException(t);
        }
        return null;
      }
    });
  }

  void sendEventually() {
    if (Constants.isTestMode) {
      return;
    }
    if (!sent) {
      sent = true;
      Map<String, Object> args = createArgsDictionary();
      saveRequestForLater(args);
    }
  }

  static List<Map<String, Object>> popUnsentRequests() {
    return getUnsentRequests(true);
  }

  static List<Map<String, Object>> getUnsentRequests() {
    return getUnsentRequests(false);
  }

  private static List<Map<String, Object>> getUnsentRequests(boolean remove) {
    List<Map<String, Object>> requestData = new ArrayList<Map<String, Object>>();

    synchronized (lock) {
      lastSendTimeMs = System.currentTimeMillis();

      Context context = Leanplum.getContext();
      SharedPreferences preferences = context.getSharedPreferences(
          "__leanplum__", Context.MODE_PRIVATE);
      SharedPreferences.Editor editor = preferences.edit();

      int count = preferences.getInt(Constants.Defaults.COUNT_KEY, 0);
      if (count == 0) {
        return new ArrayList<Map<String, Object>>();
      }
      if (remove) {
        editor.remove(Constants.Defaults.COUNT_KEY);
      }

      for (int i = 0; i < count; i++) {
        String itemKey = String.format(Locale.US, Constants.Defaults.ITEM_KEY, i);
        Map<String, Object> requestArgs;
        try {
          requestArgs = JsonConverter.mapFromJson(new JSONObject(preferences.getString(itemKey, "{}")));
          requestData.add(requestArgs);
        } catch (JSONException e) {
          e.printStackTrace();
        }
        if (remove) {
          editor.remove(itemKey);
        }
      }
      if (remove) {
        try {
          editor.apply();
        } catch (NoSuchMethodError e) {
          editor.commit();
        }
      }
    }

    return requestData;
  }

  static String jsonEncodeUnsentRequests(List<Map<String, Object>> requestData) {
    Map<String, Object> data = new HashMap<String, Object>();
    data.put(Constants.Params.DATA, requestData);
    return JsonConverter.toJson(data);
  }

  static void pushUnsentRequests(List<Map<String, Object>> requestData) {
    for (Map<String, Object> args : requestData) {
      Object retryCountString = args.get("retryCount");
      int retryCount;
      if (retryCountString != null) {
        retryCount = Integer.parseInt(retryCountString.toString()) + 1;
      } else {
        retryCount = 1;
      }
      args.put("retryCount", Integer.toString(retryCount));
      saveRequestForLater(args);
    }
  }

  private static String getSizeAsString(int bytes) {
    if (bytes < (1 << 10)) {
      return bytes + " B";
    } else if (bytes < (1 << 20)) {
      return (bytes >> 10) + " KB";
    }  else {
      return (bytes >> 20) + " MB";
    }
  }

  private static void printUploadProgress() {
    int totalFiles = fileUploadSize.size();
    int sentFiles = 0;
    int totalBytes = 0;
    int sentBytes = 0;
    for (File file : fileUploadSize.keySet()) {
      long fileSize = fileUploadSize.get(file);
      double fileProgress = fileUploadProgress.get(file);
      if (fileProgress == 1) {
        sentFiles++;
      }
      sentBytes += (int)(fileSize * fileProgress);
      totalBytes += fileSize;
    }
    String progressString = "Uploading resources. " +
        sentFiles + '/' + totalFiles + " files completed; " +
        getSizeAsString(sentBytes) + '/' + getSizeAsString(totalBytes) + " transferred.";
    if (!fileUploadProgressString.equals(progressString)) {
      fileUploadProgressString = progressString;
      Log.i("Leanplum", progressString);
    }
  }

  public void sendFilesNow(final List<String> filenames, final List<InputStream> streams) {
    if (Constants.isTestMode) {
      return;
    }
    final Map<String, Object> dict = createArgsDictionary();
    if (!attachApiKeys(dict)) {
      return;
    }
    final List<File> filesToUpload = new ArrayList<File>();

    // First set up the files for upload
    for (int i = 0; i < filenames.size(); i++) {
      String filename = filenames.get(i);
      if (Boolean.TRUE.equals(fileTransferStatus.get(filename))) {
        continue;
      }
      File file = new File(filename);
      long size;
      try {
        size = streams.get(i).available();
      } catch (IOException e) {
        size = file.length();
      } catch (NullPointerException e) {
        // Not good. Can't read asset.
        Log.e("Leanplum", "Unable to read file " + filename);
        continue;
      }
      fileTransferStatus.put(filename, true);
      filesToUpload.add(file);
      fileUploadSize.put(file, size);
      fileUploadProgress.put(file, 0.0);
    }
    if (filesToUpload.size() == 0) {
      return;
    }

    printUploadProgress();

    // Now upload the files
    Util.executeAsyncTask(new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        synchronized (uploadFileLock) {  // Don't overload app and server with many upload tasks
          JSONObject result = null;
          HttpURLConnection op = null;

          try {
            op = Util.uploadFilesOperation(
                Constants.Params.FILE,
                filesToUpload,
                streams,
                Constants.API_HOST_NAME,
                Constants.API_SERVLET,
                dict,
                httpMethod,
                Constants.API_SSL,
                60);

            if (op != null) {
              result = Util.getJsonResponse(op);
              int statusCode = op.getResponseCode();
              if (statusCode != 200) {
                throw new HttpException("Leanplum: Error sending request: " + statusCode);
              }
              if (LeanplumRequest.this.response != null) {
                LeanplumRequest.this.response.response(result);
              }
            } else {
              if (error != null) {
                error.error(new HttpException("Leanplum: Unable to read file"));
              }
            }
          } catch (HttpException e) {
            Log.e("Leanplum", "Unable to connect", e);
            if (error != null) {
              error.error(e);
            }
          } catch (JSONException e) {
            Log.e("Leanplum", "Unable to convert to JSON", e);
            if (error != null) {
              error.error(e);
            }
          } catch (SocketTimeoutException e) {
            Log.e("Leanplum", "Timeout uploading files. Try again or limit the number of files to upload with parameters to syncResourcesAsync");
            if (error != null) {
              error.error(e);
            }
          } catch (Exception e) {
            Log.e("Leanplum", "Unable to send file", e);
            if (error != null) {
              error.error(e);
            }
          } finally {
            if (op != null) {
              op.disconnect();
            }
          }

          for (File file : filesToUpload) {
            fileUploadProgress.put(file, 1.0);
          }
          printUploadProgress();

          return null;
        }
      }
    });

    // TODO: Upload progress
  }

  void downloadFile(final String path) {
    if (Constants.isTestMode) {
      return;
    }
    if (Boolean.TRUE.equals(fileTransferStatus.get(path))) {
      return;
    }
    pendingDownloads++;
    Log.i("Leanplum", "Downloading resource " + path);
    fileTransferStatus.put(path, true);
    final Map<String, Object> dict = createArgsDictionary();
    dict.put(Constants.Keys.FILENAME, path);
    if (!attachApiKeys(dict)) {
      return;
    }

    Util.executeAsyncTask(new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        try {
          downloadHelper(Constants.API_HOST_NAME, Constants.API_SERVLET, path, dict);
        } catch (Throwable t) {
          Util.handleException(t);
        }
        return null;
      }
    });
    // TODO: Download progress
  }

  private void downloadHelper(String hostName, String servlet, final String path,
      final Map<String, Object> dict) {
    HttpURLConnection op = null;
    URL originalURL = null;
    try {
      op = Util.operation(
          hostName,
          servlet,
          dict,
          httpMethod,
          Constants.API_SSL,
          Constants.NETWORK_TIMEOUT_SECONDS_FOR_DOWNLOADS);
      originalURL = op.getURL();
      op.connect();
      int statusCode = op.getResponseCode();
      if (statusCode != 200) {
        throw new Exception("Leanplum: Error sending request to: " + hostName + ", HTTP status code: " + statusCode);
      }
      Stack<String> dirs = new Stack<String>();
      String currentDir = path;
      while ((currentDir = new File(currentDir).getParent()) != null) {
        dirs.push(currentDir);
      }
      while (!dirs.isEmpty()) {
        new File(FileManager.fileRelativeToDocuments(dirs.pop())).mkdir();
      }

      FileOutputStream out = new FileOutputStream(new File(FileManager.fileRelativeToDocuments(path)));
      Util.saveResponse(op, out);
      pendingDownloads--;
      if (LeanplumRequest.this.response != null) {
        LeanplumRequest.this.response.response(null);
      }
      if (pendingDownloads == 0 && noPendingDownloadsBlock != null) {
        noPendingDownloadsBlock.noPendingDownloads();
      }
    } catch (Exception e) {
      if (e instanceof EOFException) {
        if (!op.getURL().equals(originalURL)) {
          downloadHelper(null, op.getURL().toString(), path, new HashMap<String, Object>());
          return;
        }
      }
      Log.e("Leanplum", "Error downloading resource:" + path, e);
      pendingDownloads--;
      if (error != null) {
        error.error(e);
      }
      if (pendingDownloads == 0 && noPendingDownloadsBlock != null) {
        noPendingDownloadsBlock.noPendingDownloads();
      }
    } finally {
      if (op != null) {
        op.disconnect();
      }
    }
  }

  static int numPendingDownloads() {
    return pendingDownloads;
  }

  static void onNoPendingDownloads(NoPendingDownloadsCallback block) {
    noPendingDownloadsBlock = block;
  }


  static int numResponses(JSONObject response) {
    if (response == null) {
      return 0;
    }
    try {
      return response.getJSONArray("response").length();
    } catch (JSONException e) {
      Log.e("Leanplum", "Could not parse JSON response", e);
      return 0;
    }
  }

  static JSONObject getResponseAt(JSONObject response, int index) {
    try {
      return response.getJSONArray("response").getJSONObject(index);
    } catch (JSONException e) {
      Log.e("Leanplum", "Could not parse JSON response", e);
      return null;
    }
  }

  static JSONObject getLastResponse(JSONObject response) {
    int numResponses = numResponses(response);
    if (numResponses > 0) {
      return getResponseAt(response, numResponses - 1);
    } else {
      return null;
    }
  }

  static boolean isResponseSuccess(JSONObject response) {
    if (response == null) {
      return false;
    }
    try {
    return response.getBoolean("success");
    } catch (JSONException e) {
      Log.e("Leanplum", "Could not parse JSON response", e);
      return false;
    }
  }

  static String getResponseError(JSONObject response) {
    if (response == null) {
      return null;
    }
    try {
      JSONObject error = response.getJSONObject("error");
      if (error == null) {
        return null;
      }
      return error.getString("message");
    } catch (JSONException e) {
      Log.e("Leanplum", "Could not parse JSON response", e);
      return null;
    }
  }
}

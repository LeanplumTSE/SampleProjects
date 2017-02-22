// Copyright 2013, Leanplum, Inc.

package com.leanplum.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.TypedValue;
import android.support.annotation.RequiresPermission;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumDeviceIdMode;
import com.leanplum.LeanplumException;
import com.leanplum.internal.Constants.Methods;
import com.leanplum.internal.Constants.Params;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Leanplum utilities.
 *
 * @author Andrew First
 */
public class Util {
  private static final Executor asyncExecutor = Executors.newCachedThreadPool();

  private static final String ACCESS_WIFI_STATE_PERMISSION = "android.permission.ACCESS_WIFI_STATE";

  private static String appName = null;

  private static boolean hasPlayServicesCalled = false;
  private static boolean hasPlayServices = false;

  public static class DeviceIdInfo {
    public String id;
    public boolean limitAdTracking;

    public DeviceIdInfo(String id) {
      this.id = id;
    }

    public DeviceIdInfo(String id, boolean limitAdTracking) {
      this.id = id;
      this.limitAdTracking = limitAdTracking;
    }
  }

  public static <T> Iterable<T> iterable(final Iterator<T> iterator) {
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return iterator;
      }
    };
  }

  private static String md5(String string) throws Exception {
    MessageDigest messageDigest = MessageDigest.getInstance("MD5");
    messageDigest.update(string.getBytes());
    byte digest[] = messageDigest.digest();

    StringBuilder result = new StringBuilder();
    for (int i = 0; i < digest.length; i++) {
      result.append(String.format("%02x", digest[i]));
    }
    return result.toString();
  }

  private static String checkDeviceId(String deviceIdMethod, String deviceId) {
    if (deviceId != null) {
      if (!isValidDeviceId(deviceId)) {
        Log.e("Invalid device id generated (" + deviceIdMethod + "): " + deviceId);
        return null;
      }
    }
    return deviceId;
  }

  @RequiresPermission(ACCESS_WIFI_STATE_PERMISSION)
  private static String getWifiMacAddressHash(Context context) {
    String logPrefix = "Skipping wifi device id; ";
    if (context.checkCallingOrSelfPermission(ACCESS_WIFI_STATE_PERMISSION) !=
        PackageManager.PERMISSION_GRANTED) {
      Log.v(logPrefix + "no wifi state permissions.");
      return null;
    }
    try {
      WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
      WifiInfo wifiInfo = manager.getConnectionInfo();
      if (wifiInfo == null) {
        Log.i(logPrefix + "null WifiInfo.");
        return null;
      }
      String macAddress = wifiInfo.getMacAddress();
      if (macAddress == null || macAddress.isEmpty()) {
        Log.i(logPrefix + "no mac address returned.");
        return null;
      }
      if (Constants.INVALID_MAC_ADDRESS.equals(macAddress)) {
        // Note(ed): this is the expected case for Marshmallow and later, as they return
        // INVALID_MAC_ADDRESS; we intend to fall back to the Android id for Marshmallow devices.
        Log.v(logPrefix + "Marshmallow and later returns a fake MAC address.");
        return null;
      }
      String deviceId = md5(wifiInfo.getMacAddress());
      Log.v("Using wifi device id: " + deviceId);
      return checkDeviceId("mac address", deviceId);
    } catch (Exception e) {
      Log.w("Error getting wifi MAC address.");
    }
    return null;
  }

  /**
   * Retrieves the advertising ID. Requires Google Play Services. Note: This method must not run on
   * the main thread.
   */
  private static DeviceIdInfo getAdvertisingId(Context caller) throws Exception {
    try {
      // Using reflection because the app will either crash or print warnings
      // if the app doesn't link to Google Play Services, even if this method is not called.
      Object adInfo = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
          .getMethod("getAdvertisingIdInfo", Context.class).invoke(null, caller);
      String id = checkDeviceId(
          "advertising id", (String) adInfo.getClass().getMethod("getId").invoke(adInfo));
      if (id != null) {
        boolean limitTracking = (Boolean) adInfo.getClass()
            .getMethod("isLimitAdTrackingEnabled").invoke(adInfo);
        Log.v("Using advertising device id: " + id);
        return new DeviceIdInfo(id, limitTracking);
      }
    } catch (Exception e) {
      if (e.getClass().getName().equals("GooglePlayServicesNotAvailableException")) {
        Log.w("Error getting advertising ID. Google Play services are not available.");
      } else {
        throw e;
      }
    }
    return null;
  }

  private static String getAndroidId(Context context) {
    String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
    if (androidId == null || androidId.isEmpty()) {
      Log.i("Skipping Android device id; no id returned.");
      return null;
    }
    if (Constants.INVALID_ANDROID_ID.equals(androidId)) {
      Log.v("Skipping Android device id; got invalid " + "device id: " + androidId);
      return null;
    }
    Log.v("Using Android device id: " + androidId);
    return checkDeviceId("android id", androidId);
  }

  /**
   * Final fallback device id -- generate a random device id.
   */
  private static String generateRandomDeviceId() {
    // Mark random IDs to be able to identify them.
    String randomId = UUID.randomUUID().toString() + "-LP";
    Log.v("Using generated device id: " + randomId);
    return randomId;
  }

  private static boolean isValidForCharset(String id, String charsetName) {
    CharsetEncoder encoder = null;
    try {
      Charset charset = Charset.forName(charsetName);
      encoder = charset.newEncoder();
    } catch (UnsupportedCharsetException e) {
      Log.w("Unsupported charset: " + charsetName);
    }
    if (encoder != null && !encoder.canEncode(id)) {
      Log.v("Invalid id (contains invalid characters): " + id);
      return false;
    }
    return true;
  }

  public static boolean isValidUserId(String userId) {
    String logPrefix = "Invalid user id ";
    if (userId == null || userId.isEmpty()) {
      Log.v(logPrefix + "(sentinel): " + userId);
      return false;
    }
    if (userId.length() > Constants.MAX_USER_ID_LENGTH) {
      Log.v(logPrefix + "(too long): " + userId);
      return false;
    }
    if (userId.contains("\n")) {
      Log.v(logPrefix + "(contains newline): " + userId);
      return false;
    }
    if (userId.contains("\"") || userId.contains("\'")) {
      Log.v(logPrefix + "(contains quotes): " + userId);
      return false;
    }
    return isValidForCharset(userId, "UTF-8");
  }

  public static boolean isValidDeviceId(String deviceId) {
    String logPrefix = "Invalid device id ";
    if (deviceId == null || deviceId.isEmpty() ||
        Constants.INVALID_ANDROID_ID.equals(deviceId) ||
        Constants.INVALID_MAC_ADDRESS_HASH.equals(deviceId) ||
        Constants.OLD_INVALID_MAC_ADDRESS_HASH.equals(deviceId)) {
      Log.v(logPrefix + "(sentinel): " + deviceId);
      return false;
    }
    if (deviceId.length() > Constants.MAX_DEVICE_ID_LENGTH) {
      Log.v(logPrefix + "(too long): " + deviceId);
      return false;
    }
    if (deviceId.contains("[")) {
      Log.v(logPrefix + "(contains brackets): " + deviceId);
      return false;
    }
    if (deviceId.contains("\n")) {
      Log.v(logPrefix + "(contains newline): " + deviceId);
      return false;
    }
    if (deviceId.contains(",")) {
      Log.v(logPrefix + "(contains comma): " + deviceId);
      return false;
    }
    if (deviceId.contains("\"") || deviceId.contains("\'")) {
      Log.v(logPrefix + "(contains quotes): " + deviceId);
      return false;
    }
    return isValidForCharset(deviceId, "US-ASCII");
  }

  @RequiresPermission(ACCESS_WIFI_STATE_PERMISSION)
  public static DeviceIdInfo getDeviceId(LeanplumDeviceIdMode mode) {
    Context context = Leanplum.getContext();

    if (mode.equals(LeanplumDeviceIdMode.ADVERTISING_ID)) {
      try {
        DeviceIdInfo info = getAdvertisingId(context);
        if (info != null) {
          return info;
        }
      } catch (Exception e) {
        Log.e("Error getting advertising ID", e);
      }
    }

    if (isSimulator() || mode.equals(LeanplumDeviceIdMode.ANDROID_ID)) {
      String androidId = getAndroidId(context);
      if (androidId != null) {
        return new DeviceIdInfo(getAndroidId(context));
      }
    }

    String macAddressHash = getWifiMacAddressHash(context);
    if (macAddressHash != null) {
      return new DeviceIdInfo(macAddressHash);
    }

    String androidId = getAndroidId(context);
    if (androidId != null) {
      return new DeviceIdInfo(androidId);
    }

    return new DeviceIdInfo(generateRandomDeviceId());
  }

  public static String getVersionName() {
    Context context = Leanplum.getContext();
    String versionName = "";
    try {
      PackageInfo pInfo = context.getPackageManager().getPackageInfo(
          context.getPackageName(), 0);
      versionName = pInfo.versionName;
    } catch (NameNotFoundException e) {
      Log.w("Could not extract versionName from PackageInfo.");
    }
    return versionName;
  }

  public static String getDeviceModel() {
    if (isSimulator()) {
      return "Android Emulator";
    }
    String manufacturer = Build.MANUFACTURER;
    String model = Build.MODEL;
    if (model.startsWith(manufacturer)) {
      return capitalize(model);
    } else {
      return capitalize(manufacturer) + " " + model;
    }
  }

  public static String getApplicationName(Context context) {
    if (appName != null) {
      return appName;
    }
    int stringId = context.getApplicationInfo().labelRes;
    if (stringId == 0) {
      appName = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
    } else {
      appName = context.getString(stringId);
    }
    return appName;
  }

  private static String capitalize(String s) {
    if (s == null || s.length() == 0) {
      return "";
    }
    char first = s.charAt(0);
    if (Character.isUpperCase(first)) {
      return s;
    } else {
      return Character.toUpperCase(first) + s.substring(1);
    }
  }

  public static String getSystemName() {
    return "Android OS";
  }

  public static String getSystemVersion() {
    return Build.VERSION.RELEASE;
  }

  public static boolean isSimulator() {
    String model = android.os.Build.MODEL.toLowerCase(Locale.getDefault());
    return model.contains("google_sdk")
        || model.contains("emulator")
        || model.contains("sdk");
  }

  public static String getDeviceName() {
    if (isSimulator()) {
      return "Android Emulator";
    }
    return getDeviceModel();
  }

  public static String getLocale() {
    String language = Locale.getDefault().getLanguage();
    if (language.equals("")) {
      language = "xx";
    }
    String country = Locale.getDefault().getCountry();
    if (country.equals("")) {
      country = "XX";
    }
    return language + "_" + country;
  }

  private static String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException {
    StringBuilder result = new StringBuilder();
    boolean first = true;
    for (NameValuePair pair : params) {
      if (pair.getValue() == null) {
        Log.w("Request param " + pair.getName() + " is null.");
        continue;
      }
      if (first) {
        first = false;
      } else {
        result.append("&");
      }
      result.append(pair.getName());
      result.append("=");
      result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
    }
    return result.toString();
  }

  public static HttpURLConnection operation(
      String hostName,
      String path,
      Map<String, Object> params,
      String httpMethod,
      boolean ssl,
      int timeoutSeconds) throws IOException {
    if (httpMethod.equals("GET")) {
      path = attachGetParameters(path, params);
    }
    HttpURLConnection urlConnection = createHttpUrlConnection(hostName, path,
        httpMethod, ssl, timeoutSeconds);

    if (!httpMethod.equals("GET")) {
      attachPostParameters(params, urlConnection);
    }

    if (Constants.enableVerboseLoggingInDevelopmentMode
        && Constants.isDevelopmentModeEnabled) {
      Log.d("Sending request at path " + path + " with parameters " + params);
    }

    //urlConnection.connect();

    return urlConnection;
  }

  private static String attachGetParameters(String path,
      Map<String, Object> params) throws UnsupportedEncodingException {
    String queryParams = "";
    for (String key : params.keySet()) {
      Object value = params.get(key);
      if (value == null) {
        Log.w("Request param " + key + " is null");
        continue;
      }
      queryParams += queryParams.length() == 0 ? '?' : '&';
      queryParams += key + "=" + URLEncoder.encode(value.toString(), "utf-8");
    }
    path += queryParams;
    return path;
  }

  private static void attachPostParameters(Map<String, Object> params,
      HttpURLConnection urlConnection) throws IOException {
    List<NameValuePair> paramPairs = new ArrayList<>();
    for (String key : params.keySet()) {
      String value = null;
      if (params.get(key) != null) {
        value = params.get(key).toString();
      }
      paramPairs.add(new BasicNameValuePair(key, value));
    }

    OutputStream os = urlConnection.getOutputStream();
    BufferedWriter writer = new BufferedWriter(
        new OutputStreamWriter(os, "UTF-8"));
    writer.write(getQuery(paramPairs));
    writer.close();
    os.close();
  }

  public static HttpURLConnection createHttpUrlConnection(String hostName,
      String path, String httpMethod, boolean ssl, int timeoutSeconds)
      throws IOException {
    String fullPath;
    if (path.startsWith("http")) {
      fullPath = path;
    } else {
      fullPath = (ssl ? "https://" : "http://") + hostName + "/" + path;
    }
    return createHttpUrlConnection(fullPath, httpMethod, ssl, timeoutSeconds);
  }

  public static HttpURLConnection createHttpUrlConnection(
      String fullPath, String httpMethod, boolean ssl, int timeoutSeconds)
      throws IOException {
    URL url = new URL(fullPath);
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    if (ssl) {
      SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
      ((HttpsURLConnection) urlConnection).setSSLSocketFactory(socketFactory);
    }
    urlConnection.setReadTimeout(timeoutSeconds * 1000);
    urlConnection.setConnectTimeout(timeoutSeconds * 1000);
    urlConnection.setRequestMethod(httpMethod);
    urlConnection.setDoOutput(!httpMethod.equals("GET"));
    urlConnection.setDoInput(true);
    urlConnection.setUseCaches(false);
    urlConnection.setInstanceFollowRedirects(true);
    Context context = Leanplum.getContext();
    urlConnection.setRequestProperty("User-Agent",
        getApplicationName(context) + "/" + getVersionName() + "/" + Request.appId() + "/" +
            Constants.CLIENT + "/" + Constants.LEANPLUM_VERSION + "/" + getSystemName() + "/" +
            getSystemVersion());
    return urlConnection;
  }

  /**
   * Writes the filesToUpload to a new HttpURLConnection using the multipart form data format.
   *
   * @return the connection that the files were uploaded using
   */
  public static HttpURLConnection uploadFilesOperation(
      String key,
      List<File> filesToUpload,
      List<InputStream> streams,
      String hostName,
      String path,
      Map<String, Object> params,
      String httpMethod,
      boolean ssl,
      int timeoutSeconds) throws IOException {

    HttpURLConnection urlConnection = createHttpUrlConnection(hostName, path,
        httpMethod, ssl, timeoutSeconds);

    final String BOUNDARY = "==================================leanplum";
    final String LINE_END = "\r\n";
    final String TWO_HYPHENS = "--";
    final String CONTENT_TYPE = "Content-Type: application/octet-stream";

    // Make a connection to the server
    urlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
    urlConnection.setRequestProperty("Connection", "Keep-Alive");

    DataOutputStream outputStream = new DataOutputStream(urlConnection.getOutputStream());

    // Create the header for the request with the parameters
    for (String arg : params.keySet()) {
      String paramData = TWO_HYPHENS + BOUNDARY + LINE_END
          + "Content-Disposition: form-data; name=\"" + arg + "\"" + LINE_END
          + LINE_END
          + params.get(arg) + LINE_END;
      outputStream.writeBytes(paramData);
    }

    // Main file writing loop
    for (int i = 0; i < filesToUpload.size(); i++) {
      File fileToUpload = filesToUpload.get(i);
      String contentDisposition = String.format(Locale.getDefault(), "Content-Disposition: " +
              "form-data; name=\"%s%d\";filename=\"%s\"",
              key, i, fileToUpload.getName());

      // Create the header for the file
      String fileHeader = TWO_HYPHENS + BOUNDARY + LINE_END
          + contentDisposition + LINE_END
          + CONTENT_TYPE + LINE_END
          + LINE_END;
      outputStream.writeBytes(fileHeader);

      // Read in the actual file
      InputStream is = (i < streams.size()) ? streams.get(i) : new FileInputStream(fileToUpload);
      byte[] buffer = new byte[4096];
      int bytesRead;
      try {
        while ((bytesRead = is.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
        }
      } catch (NullPointerException e) {
        Log.e("Unable to read file while uploading " + filesToUpload.get(i));
        return null;
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (IOException e) {
            Log.w("Failed to close InputStream: " + e);
          }
        }
      }

      // End the output for this file
      outputStream.writeBytes(LINE_END);
    }

    // End the output for the request
    String endOfRequest = TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END;
    outputStream.writeBytes(endOfRequest);

    outputStream.flush();
    outputStream.close();
    return urlConnection;
  }

  public static void saveResponse(URLConnection op, OutputStream outputStream) throws JSONException, IOException {
    InputStream is = op.getInputStream();
    byte[] buffer = new byte[4096];
    int bytesRead;
    while ((bytesRead = is.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }
    outputStream.close();
  }

  public static String getResponse(HttpURLConnection op) throws JSONException, IOException {
    InputStream inputStream;
    if (op.getResponseCode() < 400) {
      inputStream = op.getInputStream();
    } else {
      inputStream = op.getErrorStream();
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    StringBuilder builder = new StringBuilder();
    for (String line; (line = reader.readLine()) != null; ) {
      builder.append(line).append("\n");
    }
    return builder.toString();
  }

  public static JSONObject getJsonResponse(HttpURLConnection op)
      throws JSONException, IOException {
    String response = getResponse(op);
    if (Constants.enableVerboseLoggingInDevelopmentMode
        && Constants.isDevelopmentModeEnabled) {
      Log.d("Received response " + response);
    }
    JSONTokener tokener = new JSONTokener(response);
    return new JSONObject(tokener);
  }

  /**
   * Check whether the device has a network connection. WARNING: Does not check for available
   * internet connection! use isOnline()
   *
   * @return Whether a network connection is available or not.
   */
  public static boolean isConnected() {
    try {
      Context context = Leanplum.getContext();
      ConnectivityManager manager = (ConnectivityManager) context.getSystemService(
          Context.CONNECTIVITY_SERVICE);
      if (manager == null) {
        return false;
      }
      NetworkInfo netInfo = manager.getActiveNetworkInfo();
      if (netInfo == null || !netInfo.isConnectedOrConnecting()) {
        return false;
      }
      return true;
    } catch (Exception e) {
      Log.e("Error getting connectivity info", e);
      return false;
    }
  }

  public static <T> T multiIndex(Map<?, ?> map, Object... indices) {
    if (map == null) {
      return null;
    }
    Object current = map;
    for (Object index : indices) {
      if (!((Map<?, ?>) current).containsKey(index)) {
        return null;
      }
      current = ((Map<?, ?>) current).get(index);
    }
    return CollectionUtil.uncheckedCast(current);
  }

  public static <T> void executeAsyncTask(AsyncTask<T, ?, ?> task, T... params) {
    if (Build.VERSION.SDK_INT >= 11) {
      task.executeOnExecutor(asyncExecutor, params);
    } else {
      task.execute(params);
    }
  }

  /**
   * Check the device to make sure it has the Google Play Services APK. If it doesn't, display a
   * dialog that allows users to download the APK from the Google Play Store or enable it in the
   * device's system settings.
   */
  public static boolean hasPlayServices() {
    if (hasPlayServicesCalled) {
      return hasPlayServices;
    }
    Context context = Leanplum.getContext();
    PackageManager packageManager = context.getPackageManager();
    PackageInfo packageInfo;
    try {
      packageInfo = packageManager.getPackageInfo("com.google.android.gms", 64);
    } catch (PackageManager.NameNotFoundException e) {
      hasPlayServicesCalled = true;
      hasPlayServices = false;
      return false;
    }
    if (packageInfo.versionCode < 4242000) {
      Log.i("Google Play services version is too old: " + packageInfo.versionCode);
      hasPlayServicesCalled = true;
      hasPlayServices = false;
      return false;
    }
    ApplicationInfo info;
    try {
      info = packageManager.getApplicationInfo("com.google.android.gms", 0);
    } catch (PackageManager.NameNotFoundException e) {
      hasPlayServicesCalled = true;
      hasPlayServices = false;
      return false;
    }
    hasPlayServicesCalled = true;
    hasPlayServices = info.enabled;
    return info.enabled;
  }

  public static boolean isInBackground() {
    return (LeanplumActivityHelper.getCurrentActivity() == null ||
        LeanplumActivityHelper.isActivityPaused());
  }

  /**
   * Include install time and last app update time in start API params the first time that the app
   * runs with Leanplum.
   */
  public static void initializePreLeanplumInstall(Map<String, Object> params) {
    Context context = Leanplum.getContext();
    SharedPreferences preferences = context.getSharedPreferences("__leanplum__", Context.MODE_PRIVATE);
    if (preferences.getBoolean(Constants.Keys.INSTALL_TIME_INITIALIZED, false)) {
      return;
    }

    PackageManager packageManager = context.getPackageManager();
    String packageName = context.getPackageName();
    setInstallTime(params, packageManager, packageName);
    setUpdateTime(params, packageManager, packageName);

    SharedPreferences.Editor editor = preferences.edit();
    editor.putBoolean(Constants.Keys.INSTALL_TIME_INITIALIZED, true);
    try {
      editor.apply();
    } catch (NoSuchMethodError e) {
      editor.commit();
    }
  }

  /**
   * Set install time from package manager and update time from apk file modification time.
   */
  private static void setInstallTime(Map<String, Object> params, PackageManager packageManager,
      String packageName) {
    if (Build.VERSION.SDK_INT >= 9) {
      try {
        PackageInfo info = packageManager.getPackageInfo(packageName, 0);
        params.put(Constants.Params.INSTALL_DATE, "" + (info.firstInstallTime / 1000.0));
      } catch (NameNotFoundException e) {
        Log.w("Failed to find package info: " + e);
      }
    }
  }

  /**
   * Set update time from apk file modification time.
   */
  private static void setUpdateTime(Map<String, Object> params, PackageManager packageManager,
      String packageName) {
    try {
      ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
      File apkFile = new File(info.sourceDir);
      if (apkFile.exists()) {
        params.put(Constants.Params.UPDATE_DATE, "" + (apkFile.lastModified() / 1000.0));
      }
    } catch (NameNotFoundException e) {
      Log.w("Failed to find package info: " + e);
    }
  }

  /**
   * Handles uncaught exceptions in the SDK.
   */
  public static void handleException(Throwable t) {
    if (t instanceof OutOfMemoryError) {
      if (Constants.isDevelopmentModeEnabled) {
        throw (OutOfMemoryError) t;
      }
      return;
    }

    // Propagate Leanplum generated exceptions.
    if (t instanceof LeanplumException) {
      if (Constants.isDevelopmentModeEnabled) {
        throw (LeanplumException) t;
      }
      return;
    }

    Log.e("INTERNAL ERROR", t);

    String versionName;
    try {
      versionName = getVersionName();
    } catch (Throwable t2) {
      versionName = "(Unknown)";
    }

    try {
      Map<String, Object> params = new HashMap<>();
      params.put(Params.TYPE, Constants.Values.SDK_ERROR);

      String message = t.getMessage();
      if (message != null) {
        message = t.toString() + " (" + message + ')';
      } else {
        message = t.toString();
      }
      params.put(Params.MESSAGE, message);

      StringWriter stringWriter = new StringWriter();
      PrintWriter writer = new PrintWriter(stringWriter);
      t.printStackTrace(writer);
      params.put("stackTrace", stringWriter.toString());

      params.put(Params.VERSION_NAME, versionName);
      Request.post(Methods.LOG, params).send();
    } catch (Throwable t2) {
      Log.e("Unable to send error report.", t2);
    }
  }

  /**
   * Constructs a {@link HashMap} with the given keys and values.
   */
  public static <K, V> Map<K, V> newMap(K firstKey, V firstValue, Object... otherValues) {
    if (otherValues.length % 2 == 1) {
      throw new IllegalArgumentException("Must supply an even number of values.");
    }

    Map<K, V> map = new HashMap<>();
    map.put(firstKey, firstValue);
    for (int i = 0; i < otherValues.length; i += 2) {
      K otherKey = CollectionUtil.uncheckedCast(otherValues[i]);
      V otherValue = CollectionUtil.uncheckedCast(otherValues[i + 1]);
      map.put(otherKey, otherValue);
    }
    return map;
  }

  /**
   * Generates a Resource name from resourceId located in res/ folder.
   *
   * @param resourceId id of the resource, must be greater then 0.
   * @return resourceName in format folder/file.extension.
   */
  public static String generateResourceNameFromId(int resourceId) {
    try {
      if (resourceId <= 0) {
        Log.w("Provided resource id is invalid.");
        return null;
      }
      Resources resources = Leanplum.getContext().getResources();
      // Get entryName from resourceId, which represents a file name in res/ directory.
      String entryName = resources.getResourceEntryName(resourceId);
      // Get typeName from resourceId, which represents a folder where file is located in
      // res/ directory.
      String typeName = resources.getResourceTypeName(resourceId);

      // By using TypedValue we can get full path of a file with extension.
      TypedValue value = new TypedValue();
      resources.getValue(resourceId, value, true);

      // Regex matching to find real file extension, "image.img.png" will produce "png".
      String[] fullFileName = value.string.toString().split("\\.(?=[^\\.]+$)");
      String extension = "";
      // If extension is found, we will append dot before it.
      if (fullFileName.length == 2) {
        extension = "." + fullFileName[1];
      }

      // Return full resource name in format: drawable/image.png
      return typeName + "/" + entryName + extension;
    } catch (Exception e) {
      Log.w("Failed to generate resource name from provided resource id: ", e);
      Util.handleException(e);
    }
    return null;
  }

  /**
   * Generates resource Id based on Resource name.
   *
   * @param resourceName name of the resource including folder and file extension.
   * @return id of the resource if found, 0 otherwise.
   */
  public static int generateIdFromResourceName(String resourceName) {
    // Split resource name to extract folder and file name.
    String[] parts = resourceName.split("/");
    if (parts.length == 2) {
      Resources resources = Leanplum.getContext().getResources();
      // Type name represents folder where file is contained.
      String typeName = parts[0];
      String fileName = parts[1];
      String entryName = fileName;
      // Since fileName contains extension we have to remove it,
      // to be able to get resource id.
      String[] fileParts = fileName.split("\\.(?=[^\\.]+$)");
      if (fileParts.length == 2) {
        entryName = fileParts[0];
      }
      // Get identifier for a file in specified directory
      if (!TextUtils.isEmpty(typeName) && !TextUtils.isEmpty(entryName)) {
        return resources.getIdentifier(entryName, typeName, Leanplum.getContext().getPackageName());
      }
    }
    Log.w("Could not extract resource id from provided resource name: ", resourceName);
    return 0;
  }
}
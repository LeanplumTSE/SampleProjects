// Copyright 2013, Leanplum, Inc.

package com.leanplum;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Leanplum file manager.
 * @author Andrew First
 */
class FileManager {
  interface ResourceUpdateCallback {
    void onResourceSyncFinish();
  }
  private static ResourceUpdateCallback updateCallback;

  static Var<HashMap<String, Object>> resources = null;

  private static boolean hasInited = false;
  private static boolean initializing = false;
  public static final Object initializingLock = new Object();

  public enum DownloadFileResult {
    NONE,
    EXISTS_IN_ASSETS,
    DOWNLOADING
  }

  static class HashResults {
    String hash;
    int size;

    public HashResults(String hash, int size) {
      this.hash = hash;
      this.size = size;
    }
  }

  public static DownloadFileResult maybeDownloadFile(boolean isResource,
      String stringValue, String defaultValue,
      final Runnable onComplete) {
    if (stringValue != null && !stringValue.equals(defaultValue) &&
        (!isResource || VarCache.getResIdFromPath(stringValue) == 0)) {
      try {
        // TODO: how to find these files? maybe through apk
        Context context = Leanplum.getContext();
        if (context.getResources().getAssets().open((String) stringValue) != null) {
          return DownloadFileResult.EXISTS_IN_ASSETS;
        }
      } catch (IOException e1) {
      }
      String realPath = FileManager.fileRelativeToAppBundle(stringValue);
      if (!FileManager.fileExistsAtPath(realPath)) {
        realPath = FileManager.fileRelativeToDocuments(stringValue);
        if (!FileManager.fileExistsAtPath(realPath)) {
          LeanplumRequest downloadRequest = LeanplumRequest.get(Constants.Methods.DOWNLOAD_FILE, null);
          downloadRequest.onResponse(new LeanplumRequest.ResponseCallback() {
            @Override
            public void response(JSONObject response) {
              if (onComplete != null) {
                onComplete.run();
              }
            }
          });
          downloadRequest.onError(new LeanplumRequest.ErrorCallback() {
            @Override
            public void error(Exception e) {
              if (onComplete != null) {
                onComplete.run();
              }
            }
          });
          downloadRequest.downloadFile(stringValue);
          return DownloadFileResult.DOWNLOADING;
        }
      }
    }
    return DownloadFileResult.NONE;
  }

  @SuppressWarnings("resource")
  public static HashResults fileMD5HashCreateWithPath(InputStream is) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      int size = 0;
      try {
        is = new DigestInputStream(is, md);
        byte[] buffer = new byte[8192];
        int bytesRead = 0;
        while ((bytesRead = is.read(buffer)) != -1) {
          size += bytesRead;
        }
      }
      finally {
        is.close();
      }
      byte[] digest = md.digest();

      StringBuffer hexString = new StringBuffer();
      for (int i= 0; i < digest.length;i ++) {
        String hex = Integer.toHexString(0xFF & digest[i]);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return new HashResults(hexString.toString(), size);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static int getFileSize(String path) {
    return (int) new File(path).length();
  }

  public static boolean fileExistsAtPath(String path) {
    return new File(path).exists();
  }

  public static String appBundlePath() {
    return "";
  }

  public static String documentsPath() {
    Context context = Leanplum.getContext();
    return context.getDir("Leanplum_Documents", Context.MODE_PRIVATE).getAbsolutePath();
  }

  public static String resourcesPath() {
    Context context = Leanplum.getContext();
    return context.getDir("Leanplum_Resources", Context.MODE_PRIVATE).getAbsolutePath();
  }

  public static String bundlePath() {
    Context context = Leanplum.getContext();
    return context.getDir("Leanplum_Bundle", Context.MODE_PRIVATE).getAbsolutePath();
  }

  private static String fileRelativeTo(String root, String path) {
    return root + "/" + path;
  }
  
  public static String fileRelativeToAppBundle(String path) {
    if (path == null) {
      return null;
    }
    return fileRelativeTo(appBundlePath(), path);
  }

  public static String fileRelativeToLPBundle(String path) {
    return fileRelativeTo(bundlePath(), path);
  }

  public static String fileRelativeToDocuments(String path) {
    return fileRelativeTo(documentsPath(), path);
  }

  public static boolean isNewerLocally(
      Map<String, Object> localAttributes,
      Map<String,Object> serverAttributes) {
    if (serverAttributes == null) {
        return true;
    }
    String localHash = (String) localAttributes.get(Constants.Keys.HASH);
    String serverHash = (String) serverAttributes.get(Constants.Keys.HASH);
    Integer localSize = (Integer) localAttributes.get(Constants.Keys.SIZE);
    Integer serverSize = (Integer) serverAttributes.get(Constants.Keys.SIZE);
    if (serverSize == null || !localSize.equals(serverSize)) {
        return true;
    }
    return localHash != null && (serverHash == null || !localHash.equals(serverHash));
  }

  public static void setResourceSyncFinishBlock(ResourceUpdateCallback callback) {
    updateCallback = callback;
  }

  public static boolean hasInited() {
    return hasInited;
  }

  public static boolean initializing() {
    return initializing;
  }

  static boolean isResourceSyncingEnabled() {
    return initializing || hasInited;
  }

  private static void enableResourceSyncing(List<Pattern> patternsToInclude,
      List<Pattern> patternsToExclude) {
    resources = Var.define(Constants.Values.RESOURCES_VARIABLE, new HashMap<String, Object>());

    // This is from http://stackoverflow.com/questions/3052964/is-there-any-way-to-tell-what-folder-a-drawable-resource-was-inflated-from.
    String drawableDirPrefix = "res/drawable";
    String layoutDirPrefix = "res/layout";
    ZipInputStream apk = null;
    Context context = Leanplum.getContext();
    try {
      apk = new ZipInputStream(new FileInputStream(context.getPackageResourcePath()));
      ZipEntry entry = null;
      while ((entry = apk.getNextEntry()) != null) {
        String resourcePath = entry.getName();
        if (resourcePath.startsWith(drawableDirPrefix) ||
            resourcePath.startsWith(layoutDirPrefix)) {
          String unprefixedResourcePath = resourcePath.substring(4);

          if (patternsToInclude != null &&
              patternsToInclude.size() > 0) {
            boolean included = false;
            for (Pattern pattern : patternsToInclude) {
              if (pattern.matcher(unprefixedResourcePath).matches()) {
                included = true;
                break;
              }
            }
            if (!included) {
              continue;
            }
          }
          if (patternsToExclude != null) {
            boolean excluded = false;
            for (Pattern pattern : patternsToExclude) {
              if (pattern.matcher(unprefixedResourcePath).matches()) {
                excluded = true;
                break;
              }
            }
            if (excluded) {
              continue;
            }
          }

          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

          int bytesRead;
          int size = 0;
          byte[] buffer = new byte[8192];
          while ((bytesRead = apk.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            size += bytesRead;
          }
          apk.closeEntry();

          String hash = ("" + entry.getTime()) + ("" + size);

          Var.defineResource(
              Constants.Values.RESOURCES_VARIABLE
                  + "." + unprefixedResourcePath.replace(".", "\\.").replace('/', '.'),
              resourcePath, size, hash, outputStream.toByteArray());
        }
      }
    } catch (IOException e) {
      Log.e("tag", "Error", e);
    } finally {
      if (apk != null) {
        try {
          apk.close();
        } catch (IOException e) {
          Log.e("tag", "Error", e);
        }
      }
    }
    hasInited = true;
    synchronized (initializingLock) {
      initializing = false;
      if (updateCallback != null) {
        updateCallback.onResourceSyncFinish();
      }
    }
  }

  private static List<Pattern> compilePatterns(List<String> patterns) {
    if (patterns == null) {
      return new ArrayList<Pattern>(0);
    }
    List<Pattern> compiledPatterns = new ArrayList<Pattern>(patterns.size());
    for (String pattern : patterns) {
      try {
        compiledPatterns.add(Pattern.compile(pattern));
      } catch (PatternSyntaxException e) {
        Log.e("Leanplum", "Ignoring malformed resource syncing pattern: \"" + pattern +
            "\". Patterns must be regular expressions.");
      }
    }
    return compiledPatterns;
  }

  public static void enableResourceSyncing(final List<String> patternsToInclude,
      final List<String> patternsToExclude, boolean isAsync) {
    initializing = true;
    if (hasInited) {
      return;
    }

    try {
      final List<Pattern> compiledIncludePatterns = compilePatterns(patternsToInclude);
      final List<Pattern> compiledExcludePatterns = compilePatterns(patternsToExclude);

      if (isAsync) {
        Util.executeAsyncTask(new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
            try {
              enableResourceSyncing(compiledIncludePatterns, compiledExcludePatterns);
            } catch (Throwable t) {
              Util.handleException(t);
            }
            return null;
          }
        });
      } else {
        enableResourceSyncing(compiledIncludePatterns, compiledExcludePatterns);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  public static String fileValue(String stringValue, String defaultValue,
      Boolean valueIsInAssets) {
    String result;
    if (stringValue.equals(defaultValue)) {
      result = FileManager.fileRelativeToAppBundle(defaultValue);
      if (FileManager.fileExistsAtPath(result)) {
        return result;
      }
    }

    if (valueIsInAssets == null) {
      try {
        Context context = Leanplum.getContext();
        context.getAssets().open(stringValue);
        return stringValue;
      } catch (IOException e) {
      }
    } else if (valueIsInAssets) {
      return stringValue;
    }

    result = FileManager.fileRelativeToLPBundle(stringValue);
    if (!FileManager.fileExistsAtPath(result)) {
      result = FileManager.fileRelativeToDocuments(stringValue);
      if (!FileManager.fileExistsAtPath(result)) {
        result = FileManager.fileRelativeToAppBundle(stringValue);
        if (!FileManager.fileExistsAtPath(result)) {
          result = FileManager.fileRelativeToLPBundle(defaultValue);
          if (!FileManager.fileExistsAtPath(result)) {
            result = FileManager.fileRelativeToAppBundle(defaultValue);
            if (!FileManager.fileExistsAtPath(result)) {
              return defaultValue;
            }
          }
        }
      }
    }
    return result;
  }

  static InputStream stream(boolean isResource, Boolean isAsset,
      Boolean valueIsInAssets, String value, String defaultValue, byte[] resourceData) {
    if (value == null || value.equals("")) {
      return null;
    }
    try {
      if (isResource && value.equals(defaultValue)) {
        return new ByteArrayInputStream(resourceData);
      }
      Context context = Leanplum.getContext();
      if (valueIsInAssets == null) {
        try {
          return context.getAssets().open(value);
        } catch (IOException e) {
        }
      } else if (valueIsInAssets ||
          (isAsset && value.equals(defaultValue))) {
        return context.getAssets().open(value);
      }
      return new FileInputStream(new File(value));
    } catch (IOException e) {
      Log.e("Leanplum", "Error loading stream", e);
      return null;
    }
  }
}

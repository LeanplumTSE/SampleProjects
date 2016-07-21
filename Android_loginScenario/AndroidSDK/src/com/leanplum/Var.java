// Copyright 2013, Leanplum, Inc.

package com.leanplum;

import android.text.TextUtils;
import android.util.Log;

import com.leanplum.FileManager.DownloadFileResult;
import com.leanplum.Leanplum.OsHandler;
import com.leanplum.callbacks.VariableCallback;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Leanplum variable.
 *
 * @author Andrew First
 *
 * @param <T> Type of the variable.
 * Can be Boolean, Byte, Short, Integer, Long, Float, Double, Character, String,
 * List, or Map. You may nest lists and maps arbitrarily.
 */
public class Var<T> {
  private static final String TAG = "Leanplum [Var]";

  private String name;
  private String[] nameComponents;
  String stringValue;
  private Double numberValue;
  private T defaultValue;
  private T value;
  private String kind;
  private final List<VariableCallback<T>> fileReadyHandlers = new ArrayList<>();
  private final List<VariableCallback<T>> valueChangedHandlers = new ArrayList<>();
  private boolean fileIsPending;
  private boolean hadStarted;
  private boolean isAsset;
  boolean isResource;
  private int size;
  private String hash;
  private byte[] data;
  private boolean valueIsInAssets = false;
  private boolean isInternal;
  private int overrideResId;
  private static boolean printedCallbackWarning;

  private void warnIfNotStarted() {
    if (!isInternal && !Leanplum.hasStarted() && !printedCallbackWarning) {
      Log.w("Leanplum", "Leanplum hasn't finished retrieving values from the server. "
          + "You should use a callback to make sure the value for '" + name +
          "' is ready. Otherwise, your app may not use the most up-to-date value.");
      printedCallbackWarning = true;
    }
  }

  private interface VarInitializer<T> {
    void init(Var<T> var);
  }

  private static <T> Var<T> define(
      String name, T defaultValue, String kind, VarInitializer<T> initializer) {
    if (TextUtils.isEmpty(name)) {
      Log.e(TAG, "define - Empty name parameter provided.");
      return null;
    }
    Var<T> existing = VarCache.getVariable(name);
    if (existing != null) {
      return existing;
    }
    if (Leanplum.calledStart && !name.startsWith(Constants.Values.RESOURCES_VARIABLE)) {
      Log.w("Leanplum", "You should not create new variables after calling start "
          + "(name=" + name + ")");
    }
    Var<T> var = new Var<>();
    try {
      var.name = name;
      var.nameComponents = VarCache.getNameComponents(name);
      var.defaultValue = defaultValue;
      var.value = defaultValue;
      var.kind = kind;
      if (name.startsWith(Constants.Values.RESOURCES_VARIABLE)) {
        var.isInternal = true;
      }
      if (initializer != null) {
        initializer.init(var);
      }
      var.cacheComputedValues();
      VarCache.registerVariable(var);
      if (var.kind.equals(Constants.Kinds.FILE)) {
        VarCache.registerFile(var.stringValue,
            var.defaultValue() == null ? null : var.defaultValue().toString(),
            var.defaultStream(), var.isResource, var.hash, var.size);
      }
      var.update();
    } catch (Throwable t) {
      Util.handleException(t);
    }
    return var;
  }

  /**
   * Defines a new variable with a default value.
   * @param name Name of the variable.
   * @param defaultValue Default value of the variable. Can't be null.
   */
  public static <T> Var<T> define(String name, T defaultValue) {
    return define(name, defaultValue, VarCache.kindFromValue(defaultValue), null);
  }

  public static <T> Var<T> define(String name, T defaultValue, String kind) {
    return define(name, defaultValue, kind, null);
  }

  public static Var<Integer> defineColor(String name, int defaultValue) {
    return define(name, defaultValue, Constants.Kinds.COLOR, null);
  }

  public static Var<String> defineFile(String name, String defaultFilename) {
    return define(name, defaultFilename, Constants.Kinds.FILE, null);
  }

  public static Var<String> defineAsset(String name, String defaultFilename) {
    return define(name, defaultFilename, Constants.Kinds.FILE, new VarInitializer<String>() {
      @Override
      public void init(Var<String> var) {
        var.isAsset = true;
      }
    });
  }

  static Var<String> defineResource(String name, String defaultFilename,
      final int size, final String hash, final byte[] data) {
    return define(name, defaultFilename, Constants.Kinds.FILE, new VarInitializer<String>() {
      @Override
      public void init(Var<String> var) {
        var.isResource = true;
        var.size = size;
        var.hash = hash;
        var.data = data;
      }
    });
  }

  protected Var() {}

  /**
   * Gets the name of the variable.
   */
  public String name() {
    return name;
  }

  public String[] nameComponents() {
    return nameComponents;
  }

  /**
   * Gets the kind of the variable.
   */
  public String kind() {
    return kind;
  }

  public T defaultValue() {
    return defaultValue;
  }

  public T value() {
    warnIfNotStarted();
    return value;
  }

  public int overrideResId() {
    return overrideResId;
  }

  public void setOverrideResId(int resId) {
    overrideResId = resId;
  }

  @SuppressWarnings("unchecked")
  private void cacheComputedValues() {
    if (value instanceof String) {
      stringValue = (String) value;
      try {
        numberValue = Double.valueOf(stringValue);
      } catch (NumberFormatException e) {
        numberValue = null;
      }
    } else if (value instanceof Number) {
      stringValue = "" + value;
      numberValue = ((Number)value).doubleValue();
      if (defaultValue instanceof Byte) {
        value = (T) (Byte) ((Number) value).byteValue();
      } else if (defaultValue instanceof Short) {
        value = (T) (Short) ((Number) value).shortValue();
      } else if (defaultValue instanceof Integer) {
        value = (T) (Integer) ((Number) value).intValue();
      } else if (defaultValue instanceof Long) {
        value = (T) (Long) ((Number) value).longValue();
      } else if (defaultValue instanceof Float) {
        value = (T) (Float) ((Number) value).floatValue();
      } else if (defaultValue instanceof Double) {
        value = (T) (Double) ((Number) value).doubleValue();
      } else if (defaultValue instanceof Character) {
        value = (T) (Character) (char) ((Number) value).intValue();
      }
    } else if (value != null &&
        !(value instanceof Iterable<?>) && !(value instanceof Map<?, ?>)) {
      stringValue = value.toString();
      numberValue = null;
    } else {
      stringValue = null;
      numberValue = null;
    }
  }

  void update() {
    // TODO: Clean up memory for resource variables.
    //data = null;

    T oldValue = value;
    value = VarCache.getMergedValueFromComponentArray(nameComponents);
    if (value == null && oldValue == null) {
      return;
    }
    if (value != null && oldValue != null && value.equals(oldValue) && hadStarted) {
      return;
    }
    cacheComputedValues();

    if (VarCache.silent() && name.startsWith(Constants.Values.RESOURCES_VARIABLE)
        && kind.equals(Constants.Kinds.FILE) && !fileIsPending) {
      triggerFileIsReady();
    }

    if (VarCache.silent()) {
      return;
    }

    if (Leanplum.hasStarted()) {
      triggerValueChanged();
    }

    // Check if file exists, otherwise we need to download it.
    if (kind.equals(Constants.Kinds.FILE)) {
      if (!Constants.isNoop()) {
        DownloadFileResult result = FileManager.maybeDownloadFile(
            isResource, stringValue, (String) defaultValue,
            new Runnable() {
              @Override
              public void run() {
                triggerFileIsReady();
              }
            });
        valueIsInAssets = false;
        if (result == DownloadFileResult.DOWNLOADING) {
          fileIsPending = true;
        } else if (result == DownloadFileResult.EXISTS_IN_ASSETS) {
          valueIsInAssets = true;
        }
      }
      if (Leanplum.hasStarted() && !fileIsPending) {
        triggerFileIsReady();
      }
    }

    if (Leanplum.hasStarted()) {
      hadStarted = true;
    }
  }

  private void triggerValueChanged() {
    synchronized (valueChangedHandlers) {
      for (VariableCallback<T> callback : valueChangedHandlers) {
        callback.setVariable(this);
        OsHandler.getInstance().post(callback);
      }
    }
  }

  public void addValueChangedHandler(VariableCallback<T> handler) {
    if (handler == null) {
      Log.e(TAG, "addValueChangedHandler - Invalid handler parameter provided.");
      return;
    }

    synchronized (valueChangedHandlers) {
      valueChangedHandlers.add(handler);
    }
    if (Leanplum.hasStarted()) {
      handler.handle(this);
    }
  }

  public void removeValueChangedHandler(VariableCallback<T> handler) {
    synchronized (valueChangedHandlers) {
      valueChangedHandlers.remove(handler);
    }
  }

  private void triggerFileIsReady() {
    synchronized (fileReadyHandlers) {
      fileIsPending = false;
      for (VariableCallback<T> callback : fileReadyHandlers) {
        callback.setVariable(this);
        OsHandler.getInstance().post(callback);
      }
    }
  }

  public void addFileReadyHandler(VariableCallback<T> handler) {
    if (handler == null) {
      Log.e(TAG, "addFileReadyHandler - Invalid handler parameter provided.");
      return;
    }
    synchronized (fileReadyHandlers) {
      fileReadyHandlers.add(handler);
    }
    if (Leanplum.hasStarted() && !fileIsPending) {
      handler.handle(this);
    }
  }

  public void removeFileReadyHandler(VariableCallback<T> handler) {
    if (handler == null) {
      Log.e(TAG, "removeFileReadyHandler - Invalid handler parameter provided.");
      return;
    }
    synchronized (fileReadyHandlers) {
      fileReadyHandlers.remove(handler);
    }
  }

  public String fileValue() {
    try {
      warnIfNotStarted();
      if (kind.equals(Constants.Kinds.FILE)) {
        return FileManager.fileValue(stringValue, (String) defaultValue, valueIsInAssets);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
    return null;
  }

  public Object objectForKeyPath(Object... keys) {
    if (keys == null || keys.length == 0) {
      Log.e(TAG, "objectForKeyPath - Invalid keys parameter provided (null or empty).");
      return null;
    }
    try {
      warnIfNotStarted();
      List<Object> components = new ArrayList<>();
      Collections.addAll(components, nameComponents);
      Collections.addAll(components, keys);
      return VarCache.getMergedValueFromComponentArray(
          components.toArray(new Object[components.size()]));
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  public int count() {
    try {
      warnIfNotStarted();
      Object result = VarCache.getMergedValueFromComponentArray(nameComponents);
      if (result instanceof List) {
        return ((List<?>)result).size();
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return 0;
    }
    Leanplum.maybeThrowException(new UnsupportedOperationException("This variable is not a list."));
    return 0;
  }

  public Number numberValue() {
    warnIfNotStarted();
    return numberValue;
  }

  public String stringValue() {
    warnIfNotStarted();
    return stringValue;
  }

  public InputStream stream() {
    try {
      if (!kind.equals(Constants.Kinds.FILE)) {
        return null;
      }
      warnIfNotStarted();
      InputStream stream = FileManager.stream(isResource, isAsset, valueIsInAssets,
          fileValue(), (String) defaultValue, data);
      if (stream == null) {
        return defaultStream();
      }
      return stream;
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  public InputStream defaultStream() {
    try {
      if (!kind.equals(Constants.Kinds.FILE)) {
        return null;
      }
      return FileManager.stream(isResource, isAsset, valueIsInAssets,
          (String) defaultValue, (String) defaultValue, data);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  @Override
  public String toString() {
    return "Var(" + name + ")=" + value;
  }
}

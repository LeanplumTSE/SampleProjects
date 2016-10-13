// Copyright 2013, Leanplum, Inc.

package com.leanplum;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.leanplum.FileManager.HashResults;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Variable cache.
 * @author Andrew First.
 */
class VarCache {
  interface CacheUpdateBlock {
    void updateCache();
  }

  private static Map<String, Var<?>> vars = new ConcurrentHashMap<String, Var<?>>();
  private static Map<String, Object> fileAttributes = new HashMap<String, Object>();
  private static Map<String, InputStream> fileStreams = new HashMap<String, InputStream>();
  
  /**
   * The default values set by the client.
   * This is not thread-safe so traversals should be synchronized.
   */
  static final Map<String, Object> valuesFromClient = new HashMap<String, Object>();

  private static Map<String, String> defaultKinds = new HashMap<String, String>();
  private static Map<String, Object> actionDefinitions = new HashMap<String, Object>();
  private static Map<String, Object> diffs = new HashMap<String, Object>();
  private static Map<String, Object> regions = new HashMap<String, Object>();
  private static Map<String, Object> messageDiffs = new HashMap<String, Object>();
  private static Map<String, Object> devModeValuesFromServer;
  private static Map<String, Object> devModeFileAttributesFromServer;
  private static Map<String, Object> devModeActionDefinitionsFromServer;
  private static List<Map<String, Object>> variants = new ArrayList<Map<String, Object>>();
  private static CacheUpdateBlock updateBlock;
  private static boolean hasReceivedDiffs = false;
  private static Map<String, Object> messages = new HashMap<String, Object>();
  private static Object merged;
  private static boolean silent;
  private static int contentVersion;
  private static Map<String, Object> userAttributes;

  private static final String NAME_COMPONENT_REGEX = "(?:[^\\.\\[.(\\\\]+|\\\\.)+";
  private static final Pattern NAME_COMPONENT_PATTERN = Pattern.compile(NAME_COMPONENT_REGEX);

  public static String[] getNameComponents(String name) {
    Matcher matcher = NAME_COMPONENT_PATTERN.matcher(name);
    List<String> components = new ArrayList<String>();
    while (matcher.find()) {
      components.add(name.substring(matcher.start(), matcher.end()));
    }
    return components.toArray(new String[0]);
  }
  
  @SuppressWarnings("unchecked")
  private static Object traverse(Object collection, Object key, boolean autoInsert) {
    if (collection == null) {
      return null;
    }
    if (collection instanceof Map) {
      Object result = ((Map<?, ?>)collection).get(key);
      if (autoInsert && result == null && key instanceof String) {
        result = new HashMap<String, Object>();
        ((Map<String, Object>) collection).put((String) key, result);
      }
      return result;
    } else if (collection instanceof List) {
      Object result = ((List<?>)collection).get((Integer)key);
      if (autoInsert && result == null && key instanceof String) {
        result = new HashMap<String, Object>();
        ((List<Object>) collection).set((Integer)key, result);
      }
      return result;
    }
    return null;
  }

  public static boolean registerFile(
      String stringValue, String defaultValue,
      InputStream defaultStream, boolean isResource, String resourceHash, int resourceSize) {
    if (Constants.isDevelopmentModeEnabled) {
      if (!Constants.isNoop()) {
        InputStream stream = defaultStream;
        if (stream == null) {
          return false;
        }
        Map<String, Object> variationAttributes = new HashMap<String, Object>();
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (isResource) {
          attributes.put(Constants.Keys.HASH, resourceHash);
          attributes.put(Constants.Keys.SIZE, resourceSize);
        } else {
          if (Constants.hashFilesToDetermineModifications && Util.isSimulator()) {
            HashResults result = FileManager.fileMD5HashCreateWithPath(stream);
            attributes.put(Constants.Keys.HASH, result.hash);
            attributes.put(Constants.Keys.SIZE, result.size);
          } else {
            int size = FileManager.getFileSize(
                FileManager.fileValue(stringValue, defaultValue, null));
            attributes.put(Constants.Keys.SIZE, size);
          }
        }
        variationAttributes.put("", attributes);
        String fileName = stringValue;
        fileAttributes.put(fileName, variationAttributes);
        fileStreams.put(fileName, defaultStream);
        maybeUploadNewFiles();
      }
      return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  public static void updateValues(
      String name, String[] nameComponents, Object value,
      String kind, Map<String, Object> values, Map<String, String> kinds) {
    Object valuesPtr = values;
    for (int i = 0; i < nameComponents.length - 1; i++) {
      valuesPtr = traverse(valuesPtr, nameComponents[i], true);
    }
    ((Map<String, Object>)valuesPtr).put(
        nameComponents[nameComponents.length - 1], value);
    kinds.put(name, kind);
  }

  public static void registerVariable(Var<?> var) {
    vars.put(var.name(), var);
    synchronized (valuesFromClient) {
      updateValues(
          var.name(), var.nameComponents(), var.defaultValue(),
          var.kind(), valuesFromClient, defaultKinds);
    }
  }
  
  @SuppressWarnings("unchecked")
  public static <T> Var<T> getVariable(String name) {
    return (Var<T>) vars.get(name);
  }

  private static void computeMergedDictionary() {
    synchronized (valuesFromClient) {
      merged = mergeHelper(valuesFromClient, diffs);
    }
  }

  static Object mergeHelper(Object vars, Object diff) {
    if (diff == null) {
      return vars;
    }
    if (diff instanceof Number
        || diff instanceof Boolean
        || diff instanceof String
        || diff instanceof Character
        || vars instanceof Number
        || vars instanceof Boolean
        || vars instanceof String
        || vars instanceof Character) {
      return diff;
    }

    Iterable<?> diffKeys = (diff instanceof Map) ? ((Map<?, ?>)diff).keySet() : (Iterable<?>)diff;
    Iterable<?> varsKeys = (vars instanceof Map) ? ((Map<?, ?>)vars).keySet() : (Iterable<?>)vars;
    Map<?, ?> diffMap = (diff instanceof Map) ? ((Map<?, ?>)diff) : null;
    Map<?, ?> varsMap = (vars instanceof Map) ? ((Map<?, ?>)vars) : null;

    // Infer that the diffs is an array if the vars value doesn't exist to tell us the type.
    boolean isArray = false;
    if (vars == null) {
      if (diff instanceof Map && ((Map<?, ?>)diff).size() > 0) {
        isArray = true;
        for (Object var : diffKeys) {
          if (!(var instanceof String)) {
            isArray = false;
            break;
          }
          String str = ((String)var);
          if (str.length() < 3 || str.charAt(0) != '[' || str.charAt(str.length() - 1) != ']') {
            isArray = false;
            break;
          }
          String varSubscript = str.substring(1, str.length() - 1);
          if (!("" + Integer.getInteger(varSubscript)).equals(varSubscript)) {
            isArray = false;
            break;
          }
        }
      }
    }
  
    // Merge arrays.
    if (vars instanceof List || isArray) {
      ArrayList<Object> merged = new ArrayList<Object>();
      for (Object var : varsKeys) {
        merged.add(var);
      }
      for (Object varSubscript : diffKeys) {
        String strSubscript = (String) varSubscript;
        int subscript = Integer.parseInt(strSubscript.substring(1, strSubscript.length() - 1));
        Object var = diffMap.get(strSubscript);
        while (subscript >= merged.size()) {
          merged.add(null);
        }
        merged.set(subscript, mergeHelper(merged.get(subscript), var));
      }
      return merged;
    }
  
    // Merge dictionaries.
    if (vars instanceof Map || diff instanceof Map) {
      HashMap<Object, Object> merged = new HashMap<Object, Object>();
      if (varsKeys != null) {
        for (Object var : varsKeys) {
          if (diffMap.get(var) == null) {
            merged.put(var, varsMap.get(var));
          }
        }
      }
      for (Object var : diffKeys) {
        merged.put(var, (mergeHelper(varsMap != null ? varsMap.get(var) : null, diffMap.get(var))));
      }
      return merged;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public static <T> T getMergedValueFromComponentArray(Object[] components, Object values) {
    Object mergedPtr = values;
    for (Object component : components) {
      mergedPtr = traverse(mergedPtr, component, false);
    }
    return (T) mergedPtr;
  }

  public static <T> T getMergedValueFromComponentArray(Object[] components) {
    return getMergedValueFromComponentArray(components,
        merged != null ? merged : valuesFromClient);
  }

  public static Map<String, Object> diffs() {
    return diffs;
  }

  public static Map<String, Object> messageDiffs() {
    return messageDiffs;
  }
  
  public static Map<String, Object> regions() {
    return regions;
  }

  public static boolean hasReceivedDiffs() {
    return hasReceivedDiffs;
  }

  public static void loadDiffs() {
    if (Constants.isNoop()) {
      return;
    }
    Context context = Leanplum.getContext();
    SharedPreferences defaults = context.getSharedPreferences(
        "__leanplum__", Context.MODE_PRIVATE);
    if (LeanplumRequest.token() == null) {
      applyVariableDiffs(
          new HashMap<String, Object>(),
          new HashMap<String, Object>(),
          new HashMap<String, Object>(),
          new ArrayList<Map<String,Object>>());
      return;
    }
    try {
      // Crypt functions return input text if there was a problem.
      AESCrypt aesContext = new AESCrypt(LeanplumRequest.appId(), LeanplumRequest.token());
      String variables = aesContext.decodePreference(
          defaults, Constants.Defaults.VARIABLES_KEY, "{}");
      String messages = aesContext.decodePreference(
          defaults, Constants.Defaults.MESSAGES_KEY, "{}");
      String regions = aesContext.decodePreference(defaults, Constants.Defaults.REGIONS_KEY, "{}");
      String variants = aesContext.decodePreference(defaults, Constants.Keys.VARIANTS, "[]");
      applyVariableDiffs(
          JsonConverter.fromJson(variables),
          JsonConverter.fromJson(messages),
          JsonConverter.fromJson(regions),
          JsonConverter.<Map<String, Object>>listFromJson(new JSONArray(variants)));
      String deviceId = aesContext.decodePreference(defaults, Constants.Params.DEVICE_ID, null);
      if (deviceId != null) {
        if (Util.isValidDeviceId(deviceId)) {
          LeanplumRequest.setDeviceId(deviceId);
        } else {
          Log.w("Leanplum", "Invalid stored device id found: \"" + deviceId + "\"; discarding.");
        }
      }
      String userId = aesContext.decodePreference(defaults, Constants.Params.USER_ID, null);
      if (userId != null) {
        if (Util.isValidUserId(userId)) {
          LeanplumRequest.setUserId(userId);
        } else {
          Log.w("Leanplum", "Invalid stored user id found: \"" + userId + "\"; discarding.");
        }
      }
    } catch (Exception e) {
      Log.e("Leanplum", "Could not load variable diffs", e);
    }
    userAttributes();
  }

  public static void saveDiffs() {
    if (Constants.isNoop()) {
      return;
    }
    if (LeanplumRequest.token() == null) {
      return;
    }
    Context context = Leanplum.getContext();
    SharedPreferences defaults = context.getSharedPreferences(
            "__leanplum__", Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = defaults.edit();
    // Crypt functions return null if there was a problem.
    AESCrypt aesContext = new AESCrypt(LeanplumRequest.appId(), LeanplumRequest.token());
    String variablesJson = JsonConverter.toJson(diffs);
    editor.putString(Constants.Defaults.VARIABLES_KEY, aesContext.encrypt(variablesJson));
    String messagesJson = JsonConverter.toJson(messages);
    editor.putString(Constants.Defaults.MESSAGES_KEY, aesContext.encrypt(messagesJson));
    String regionsJson = JsonConverter.toJson(regions);
    editor.putString(Constants.Defaults.REGIONS_KEY, aesContext.encrypt(regionsJson));
    try {
      String variantsJson = JsonConverter.listToJsonArray(variants).toString();
      editor.putString(Constants.Keys.VARIANTS, aesContext.encrypt(variantsJson));
    } catch (JSONException e1) {
      Log.e("Leanplum", "Error converting " + variants + " to JSON", e1);
    }
    editor.putString(Constants.Params.DEVICE_ID, aesContext.encrypt(LeanplumRequest.deviceId()));
    editor.putString(Constants.Params.USER_ID, aesContext.encrypt(LeanplumRequest.userId()));
    try {
      editor.apply();
    } catch (NoSuchMethodError e) {
      editor.commit();
    }
  }

  /**
   * Convert a resId to a resPath.
   */
  public static int getResIdFromPath(String resPath) {
    int resId = 0;
    try {
      String path = resPath.replace("res/", "");
      path = path.substring(0, path.lastIndexOf('.'));  // remove file extension
      String name = path.substring(path.lastIndexOf('/') + 1);
      String type = path.substring(0, path.lastIndexOf('/'));
      type = type.replace('/', '.');
      Context context = Leanplum.getContext();
      resId = context.getResources().getIdentifier(name, type, context.getPackageName());
    } catch (Exception e) {
      // Fall back to 0 on any exception
    }
    return resId;
  }

  /**
   * Update file variables stream info with override info, so that override files don't require
   * downloads if they're already available.
   */
  @SuppressWarnings("unchecked")
  private static void fileVariableFinish() {
    for (String name : new HashMap<String, Var<?>>(vars).keySet()) {
      Var<?> var = vars.get(name);
      String overrideFile = var.stringValue;
      if (var.isResource && (var.kind().equals(Constants.Kinds.FILE)) && overrideFile != null &&
          !var.defaultValue().equals(overrideFile)) {
        Map<String, Object> variationAttributes = (Map<String, Object>)
            fileAttributes.get(overrideFile);
        InputStream stream = fileStreams.get(overrideFile);
        if (variationAttributes != null && stream != null) {
          var.setOverrideResId(getResIdFromPath(var.stringValue()));
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static void applyVariableDiffs(
      Map<String, Object> diffs,
      Map<String, Object> messages,
      Map<String, Object> regions,
      List<Map<String, Object>> variants) {
    if (diffs != null) {
      VarCache.diffs = diffs;
      computeMergedDictionary();
      
      // Update variables with new values.
      // Have to copy the dictionary because a dictionary variable may add a new sub-variable,
      // modifying the variable dictionary.
      for (String name : new HashMap<String, Var<?>>(vars).keySet()) {
        vars.get(name).update();
      }
      fileVariableFinish();
    }

    if (messages != null) {
      // Store messages.
      messageDiffs = messages;
      Map<String, Object> newMessages = new HashMap<String, Object>();
      for (String name : messages.keySet()) {
        Map<String, Object> messageConfig = (Map<String, Object>) messages.get(name);
        Map<String, Object> newConfig = new HashMap<String, Object>(messageConfig);
        Map<String, Object> actionArgs =
            (Map<String, Object>) messageConfig.get(Constants.Keys.VARS);
        Map<String, Object> defaultArgs = Util.multiIndex(actionDefinitions,
            newConfig.get(Constants.Params.ACTION), "values");
        Map<String, Object> vars = (Map<String, Object>) mergeHelper(defaultArgs, actionArgs);
        newMessages.put(name, newConfig);
        newConfig.put(Constants.Keys.VARS, vars);
      }
      VarCache.messages = newMessages;
      for (String name : VarCache.messages.keySet()) {
        Map<String, Object> messageConfig = (Map<String, Object>) VarCache.messages.get(name);
        if (messageConfig.get("action") != null) {
          Map<String, Object> actionArgs =
              (Map<String, Object>) messageConfig.get(Constants.Keys.VARS);
          new ActionContext(
              messageConfig.get("action").toString(), actionArgs, name).update();
        }
      }
    }

    if (regions != null) {
      VarCache.regions = regions;
    }

    if (messages != null || regions != null) {
      Set<String> foregroundRegionNames = new HashSet<String>();
      Set<String> backgroundRegionNames = new HashSet<String>();
      ActionManager.getForegroundandBackgroundRegionNames(foregroundRegionNames,
          backgroundRegionNames);
      LocationManager locationManager = ActionManager.getLocationManager();
      if (locationManager != null) {
        locationManager.setRegionsData(regions, foregroundRegionNames, backgroundRegionNames);
      }
    }

    if (variants != null) {
      VarCache.variants = variants;
    }

    contentVersion++;

    if (!silent) {
      saveDiffs();
      triggerHasReceivedDiffs();
    }
  }

  public static int contentVersion() {
    return contentVersion;
  }

  @SuppressWarnings("unchecked")
  public static boolean areActionDefinitionsEqual(
      Map<String, Object> a, Map<String, Object> b) {
    if (a.size() != b.size()) {
      return false;
    }
    for (String key : a.keySet()) {
      Map<String, Object> aItem = (Map<String, Object>) a.get(key);
      Map<String, Object> bItem = (Map<String, Object>) b.get(key);
      if (bItem == null) {
        return false;
      }
      if (!aItem.get("kind").equals(bItem.get("kind")) ||
          !aItem.get("values").equals(bItem.get("values")) ||
          !aItem.get("kinds").equals(bItem.get("kinds")) ||
          (aItem.get("options") == null) != (bItem.get("options") == null) ||
          aItem.get("options").equals(bItem.get("options"))) {
        return false;
      }
    }
    return true;
  }
  
  private static void triggerHasReceivedDiffs() {
    hasReceivedDiffs = true;
    if (updateBlock != null) {
      updateBlock.updateCache();
    }
  }

  static boolean sendVariablesIfChanged() {
    return sendContentIfChanged(true, false);
  }

  static boolean sendActionsIfChanged() {
    return sendContentIfChanged(false, true);
  }

  private static boolean sendContentIfChanged(boolean variables, boolean actions) {
    boolean changed = false;
    if (variables && devModeValuesFromServer != null
        && !valuesFromClient.equals(devModeValuesFromServer)) {
      changed = true;
    }
    if (actions && !areActionDefinitionsEqual(
        actionDefinitions, devModeActionDefinitionsFromServer)) {
      changed = true;
    }

    if (changed) {
      HashMap<String, Object> params = new HashMap<String, Object>();
      if (variables) {
        params.put(Constants.Params.VARS, JsonConverter.toJson(valuesFromClient));
        params.put(Constants.Params.KINDS, JsonConverter.toJson(defaultKinds));
      }
      if (actions) {
        params.put(Constants.Params.ACTION_DEFINITIONS, JsonConverter.toJson(actionDefinitions));
      }
      params.put(Constants.Params.FILE_ATTRIBUTES, JsonConverter.toJson(fileAttributes));
      LeanplumRequest.post(Constants.Methods.SET_VARS, params).sendIfConnected();
    }

    return changed;
  }

  @SuppressWarnings("unchecked")
  static void maybeUploadNewFiles() {
    // First check to make sure we have all the data we need
    if (Constants.isNoop()
        || devModeFileAttributesFromServer == null
        || !Leanplum.hasStartedAndRegisteredAsDeveloper()
        || !Constants.enableFileUploadingInDevelopmentMode) {
      return;
    }
    
    List<String> filenames = new ArrayList<String>();
    List<JSONObject> fileData = new ArrayList<JSONObject>();
    List<InputStream> streams = new ArrayList<InputStream>();
    int totalSize = 0;

    for (String name : fileAttributes.keySet()) {
      Map<String, Object> variationAttributes = (Map<String, Object>) fileAttributes.get(name);
      Map<String, Object> serverVariationAttributes =
          (Map<String, Object>) devModeFileAttributesFromServer.get(name);
      Map<String, Object> localAttributes = (Map<String, Object>) variationAttributes.get("");
      Map<String, Object> serverAttributes = (Map<String, Object>)
          (serverVariationAttributes != null ? serverVariationAttributes.get("") : null);
      if (FileManager.isNewerLocally(localAttributes, serverAttributes)) {
        if (Constants.enableVerboseLoggingInDevelopmentMode) {
          Log.d("Leanplum", "Will upload file " + name + ". Local attributes: " + localAttributes +
              "; server attributes: " + serverAttributes);
        }

        String hash = (String) localAttributes.get(Constants.Keys.HASH);
        if (hash == null) {
          hash = "";
        }

        String variationPath = FileManager.fileRelativeToAppBundle(name);

        // Upload in batch if we can't put any more files in
        if ((totalSize > Constants.Files.MAX_UPLOAD_BATCH_SIZES && filenames.size() > 0)
            || filenames.size() >= Constants.Files.MAX_UPLOAD_BATCH_FILES) {
          Map<String, Object> params = new HashMap<String, Object>();
          params.put(Constants.Params.DATA, fileData.toString());

          LeanplumRequest.post(Constants.Methods.UPLOAD_FILE, params).sendFilesNow(filenames,
              streams);

          filenames = new ArrayList<String>();
          fileData = new ArrayList<JSONObject>();
          streams = new ArrayList<InputStream>();
          totalSize = 0;
        }

        // Add the current file to the lists and update size
        Object size = localAttributes.get(Constants.Keys.SIZE);
        totalSize += ((Integer)size).intValue();
        filenames.add(variationPath);
        JSONObject fileDatum = new JSONObject();          
        try {
          fileDatum.put(Constants.Keys.HASH, hash);
          fileDatum.put(Constants.Keys.SIZE, localAttributes.get(Constants.Keys.SIZE) + "");
          fileDatum.put(Constants.Keys.FILENAME, name);
          fileData.add(fileDatum);
        } catch (JSONException e) {
          // HASH, SIZE, or FILENAME are null, which they never should be (they're constants).
          Log.e("Leanplum", "Unable to upload files", e);
          fileData.add(new JSONObject());
        }
        streams.add(fileStreams.get(name));
      }
    }

    if (filenames.size() > 0) {
      Map<String, Object> params = new HashMap<String, Object>();
      params.put(Constants.Params.DATA, fileData.toString());
      LeanplumRequest.post(Constants.Methods.UPLOAD_FILE, params).sendFilesNow(filenames, streams);
    }
  }

  /**
   * Sets whether values should be saved and callbacks triggered
   * when the variable values get updated.
   */
  public static void setSilent(boolean silent) {
    VarCache.silent = silent;
  }
  
  public static boolean silent() {
    return silent;
  }

  public static void setDevModeValuesFromServer(Map<String, Object> values,
      Map<String, Object> fileAttributes, Map<String, Object> actionDefinitions) {
    devModeValuesFromServer = values;
    devModeActionDefinitionsFromServer = actionDefinitions;
    devModeFileAttributesFromServer = fileAttributes;
  }

  public static void onUpdate(CacheUpdateBlock block) {
    updateBlock = block;
  }

  public static List<Map<String, Object>> variants() {
    return variants;
  }

  public static Map<String, Object> actionDefinitions() {
    return actionDefinitions;
  }

  public static Map<String, Object> messages() {
    return messages;
  }

  public static void registerActionDefinition(
      String name, int kind, List<ActionArg<?>> args,
      Map<String, Object> options) {
    Map<String, Object> values = new HashMap<String, Object>();
    Map<String, String> kinds = new HashMap<String, String>();
    List<String> order = new ArrayList<String>();
    for (ActionArg<?> arg : args) {
      updateValues(arg.name(), getNameComponents(arg.name()),
          arg.defaultValue(), arg.kind(), values, kinds);
      order.add(arg.name());
    }
    Map<String, Object> definition = new HashMap<String, Object>();
    definition.put("kind", kind);
    definition.put("values", values);
    definition.put("kinds", kinds);
    definition.put("order", order);
    definition.put("options", options);
    actionDefinitions.put(name, definition);
  }

  public static <T> String kindFromValue(T defaultValue) {
    String kind = null;
    if (defaultValue instanceof Integer
        || defaultValue instanceof Long
        || defaultValue instanceof Short
        || defaultValue instanceof Character
        || defaultValue instanceof Byte
        || defaultValue instanceof BigInteger) {
      kind = Constants.Kinds.INT;
    } else if (defaultValue instanceof Float
        || defaultValue instanceof Double
        || defaultValue instanceof BigDecimal) {
      kind = Constants.Kinds.FLOAT;
    } else if (defaultValue instanceof String) {
      kind = Constants.Kinds.STRING;
    } else if (defaultValue instanceof List
        || defaultValue instanceof Array) {
      kind = Constants.Kinds.ARRAY;
    } else if (defaultValue instanceof Map) {
      kind = Constants.Kinds.DICTIONARY;
    } else if (defaultValue instanceof Boolean) {
      kind = Constants.Kinds.BOOLEAN;
    }
    return kind;
  }
  
  public static Map<String, Object> userAttributes() {
    if (userAttributes == null) {
      Context context = Leanplum.getContext();
      SharedPreferences defaults = context.getSharedPreferences(
          "__leanplum__", Context.MODE_PRIVATE);
      AESCrypt aesContext = new AESCrypt(LeanplumRequest.appId(), LeanplumRequest.token());
      try {
        userAttributes = JsonConverter.fromJson(
            aesContext.decodePreference(defaults, Constants.Defaults.ATTRIBUTES_KEY, "{}"));
      } catch (Exception e) {
        Log.e("Leanplum", "Could not load user attributes", e);
        userAttributes = new HashMap<String, Object>();
      }
    }
    return userAttributes;
  }

  public static void saveUserAttributes() {
    if (Constants.isNoop() || LeanplumRequest.appId() == null || userAttributes == null) {
      return;
    }
    Context context = Leanplum.getContext();
    SharedPreferences defaults = context.getSharedPreferences(
        "__leanplum__", Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = defaults.edit();
    // Crypt functions return input text if there was a problem.
    String plaintext = JsonConverter.toJson(userAttributes);
    AESCrypt aesContext = new AESCrypt(LeanplumRequest.appId(), LeanplumRequest.token());
    editor.putString(Constants.Defaults.ATTRIBUTES_KEY, aesContext.encrypt(plaintext));
    try {
      editor.apply();
    } catch (NoSuchMethodError e) {
      editor.commit();
    }
  }

  /**
   * Resets the VarCache to stock state.
   */
  public static void reset() {
    vars.clear();
    fileAttributes.clear();
    fileStreams.clear();
    valuesFromClient.clear();
    defaultKinds.clear();
    actionDefinitions.clear();
    diffs.clear();
    messageDiffs.clear();
    regions.clear();
    devModeValuesFromServer = null;
    devModeFileAttributesFromServer = null;
    devModeActionDefinitionsFromServer = null;
    variants.clear();
    updateBlock = null;
    hasReceivedDiffs = false;
    messages = null;
    merged = null;
    silent = false;
    contentVersion = 0;
    userAttributes = null;
  }
}

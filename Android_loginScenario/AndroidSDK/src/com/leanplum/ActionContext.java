// Copyright 2014, Leanplum, Inc.

package com.leanplum;

import android.text.TextUtils;
import android.util.Log;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The context in which an action or message is executed.
 * @author Andrew First
 */
public class ActionContext implements Comparable<ActionContext> {
  // Abbreviating context here, because the logging tag can be at most 23 characters.
  private static final String TAG = "Leanplum [ActionCtx]";
  private final String name;
  private final String messageId;
  private Map<String, Object> args;
  private ActionContext parentContext;
  private final int contentVersion;
  private String key;
  private boolean preventRealtimeUpdating = false;
  private boolean isRooted = true;
  private boolean isPreview = false;
  private ContextualValues contextualValues;
  private int priority;

  static class ContextualValues {
    /**
     * Parameters from the triggering event or state.
     */
    public Map<String, ?> parameters;
    
    /**
     * The previous user attribute value.
     */
    public Object previousAttributeValue;

    /**
     * The current user attribute value.
     */
    public Object attributeValue;
  }

  ActionContext(String name, Map<String, Object> args, String messageId) {
    this(name, args, messageId, Constants.Messaging.DEFAULT_PRIORITY);
  }

  ActionContext(String name, Map<String, Object> args, String messageId, int priority) {
    this.name = name;
    this.args = args;
    this.messageId = messageId;
    this.contentVersion = VarCache.contentVersion();
    this.priority = priority;
  }

  void preventRealtimeUpdating() {
    preventRealtimeUpdating = true;
  }
  
  void setIsRooted(boolean value) {
    isRooted = value;
  }

  void setContextualValues(ContextualValues values) {
    contextualValues = values;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> getDefinition(String name) {
    Map<String, Object> definition = (Map<String, Object>)
         VarCache.actionDefinitions().get(name);
     if (definition == null) {
       return new HashMap<>();
     }
    return definition;
  }

  private Map<String, Object> getDefinition() {
    return getDefinition(name);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> defaultValues() {
   Map<String, Object> values = (Map<String, Object>) getDefinition().get("values");
   if (values == null) {
     return new HashMap<>();
   }
   return values;
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> kinds() {
   Map<String, String> kinds = (Map<String, String>) getDefinition().get("kinds");
   if (kinds == null) {
     return new HashMap<>();
   }
   return kinds;
  }

  void update() {
    this.updateArgs(args, "", defaultValues());
  }

  @SuppressWarnings("unchecked")
  private void updateArgs(Map<String, Object> args,
      String prefix, Map<String, Object> defaultValues) {
    Map<String, String> kinds = kinds();
    for (String arg : args.keySet()) {
      Object value = args.get(arg);
      Object defaultValue = defaultValues != null ? defaultValues.get(arg) : null;
      String kind = kinds.get(prefix + arg);
      if ((kind == null || !kind.equals(Constants.Kinds.ACTION)) && value instanceof Map &&
          !((Map<String, ?>) value).containsKey(Constants.Values.ACTION_ARG)) {
        Map<String, Object> defaultValueMap = (defaultValue instanceof Map) ?
            (Map<String, Object>) defaultValue : null;
        this.updateArgs((Map<String, Object>) value, prefix + arg + ".", defaultValueMap);
      } else {
        if (kind != null && kind.equals(Constants.Kinds.FILE)) {
          FileManager.maybeDownloadFile(false, value.toString(),
              defaultValue != null ? defaultValue.toString() : null, null);

          // Need to check for null because server actions like push notifications aren't
          // defined in the SDK, and so there's no associated metadata.
        } else if (kind == null || kind.equals(Constants.Kinds.ACTION)) {
          Object actionArgsObj = objectNamed(prefix + arg);
          if (!(actionArgsObj instanceof Map)) {
            continue;
          }
          Map<String, Object> actionArgs = (Map<String, Object>) actionArgsObj;
          ActionContext context = new ActionContext(
              (String) actionArgs.get(Constants.Values.ACTION_ARG),
              actionArgs, messageId);
          context.update();
        }
      }
    }
  }

  public String actionName() {
    return name;
  }
  
  @SuppressWarnings("unchecked")
  public <T>T objectNamed(String name) {
    if (TextUtils.isEmpty(name)) {
      Log.e(TAG, "objectNamed - Invalid name parameter provided.");
      return null;
    }
    try {
      if (!preventRealtimeUpdating && VarCache.contentVersion() > contentVersion) {
        ActionContext parent = parentContext;
        if (parent != null) {
          args = parent.getChildArgs(key);
        } else if (messageId != null) {
          // This is sort of a best effort to display the most recent version of the message, if
          // this happens to be null, it probably means that it got changed somehow in between the
          // time when it was activated and displayed (e.g. by forceContentUpdate), in which case
          // we just ignore it and display the latest stable version.
          Map<String, Object> message = (Map<String, Object>) VarCache.messages().get(messageId);
          if (message != null) {
            args = (Map<String, Object>) message.get(Constants.Keys.VARS);
          }
        }
      }
      return VarCache.getMergedValueFromComponentArray(
          VarCache.getNameComponents(name), args);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  public String stringNamed(String name) {
    if (TextUtils.isEmpty(name)) {
      Log.e(TAG, "stringNamed - Invalid name parameter provided.");
      return null;
    }
    Object object = objectNamed(name);
    if (object == null) {
      Log.e(TAG, "stringNamed - Could not create named object.");
      return null;
    }
    try {
      return fillTemplate(object.toString());
    } catch (Throwable t) {
      Util.handleException(t);
      return object.toString();
    }
  }

  private String fillTemplate(String value) {
    if (contextualValues == null || value == null || !value.contains("##")) {
      return value;
    }
    if (contextualValues.parameters != null) {
      Map<String, ?> parameters = contextualValues.parameters;
      for (String parameterName : parameters.keySet()) {
        String placeholder = "##Parameter " + parameterName + "##";
        value = value.replace(placeholder, "" + parameters.get(parameterName));
      }
    }
    if (contextualValues.previousAttributeValue != null) {
      value = value.replace("##Previous Value##",
          contextualValues.previousAttributeValue.toString());
    }
    if (contextualValues.attributeValue != null) {
      value = value.replace("##Value##", contextualValues.attributeValue.toString());
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  private String getDefaultValue(String name) {
    String[] components = name.split("\\.");
    Map<String, Object> defaultValues = defaultValues();
    for (int i = 0; i < components.length; i++) {
      if (defaultValues == null) {
        return null;
      }
      if (i == components.length - 1) {
        Object value = defaultValues.get(components[i]);
        return value == null ? null : value.toString();
      }
      defaultValues = (Map<String, Object>) defaultValues.get(components[i]);
    }
    return null;
  }

  public InputStream streamNamed(String name) {
    try {
      if (TextUtils.isEmpty(name)) {
        Log.e(TAG, "streamNamed - Invalid name parameter provided.");
        return null;
      }
      String stringValue = stringNamed(name);
      String defaultValue = getDefaultValue(name);
      if ((stringValue == null || stringValue.length() == 0) &&
          (defaultValue == null || defaultValue.length() == 0)) {
        return null;
      }
      InputStream stream = FileManager.stream(false, null, null,
          FileManager.fileValue(stringValue, defaultValue, null), defaultValue, null);
      if (stream == null) {
        Log.e("Leanplum", "Could not open stream named " + name);
      }
      return stream;
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }
  
  public boolean booleanNamed(String name) {
    if (TextUtils.isEmpty(name)) {
      Log.e(TAG, "booleanNamed - Invalid name parameter provided.");
      return false;
    }
    Object object = objectNamed(name);
    try {
      if (object instanceof Boolean) {
        return (Boolean) object;
      }
      return Boolean.valueOf(object.toString());
    } catch (Throwable t) {
      Util.handleException(t);
      return false;
    }
  }

  public Number numberNamed(String name) {
    if (TextUtils.isEmpty(name)) {
      Log.e(TAG, "numberNamed - Invalid name parameter provided.");
      return null;
    }
    Object object = objectNamed(name);
    try {
      if (object instanceof Number) {
        return (Number) object;
      }
      return Double.valueOf(object.toString());
    } catch (Throwable t) {
      Util.handleException(t);
      return 0.0;
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getChildArgs(String name) {
    Object actionArgsObj = objectNamed(name);
    if (!(actionArgsObj instanceof Map)) {
      return null;
    }
    Map<String, Object> actionArgs = (Map<String, Object>) actionArgsObj;
    Map<String, Object> defaultArgs = (Map<String, Object>) getDefinition(
        (String) actionArgs.get(Constants.Values.ACTION_ARG)).get("values");
    actionArgs = (Map<String, Object>) VarCache.mergeHelper(defaultArgs, actionArgs);
    return actionArgs;
  }

  public void runActionNamed(String name) {
    if (TextUtils.isEmpty(name)) {
      Log.e(TAG, "runActionNamed - Invalid name parameter provided.");
      return;
    }
    Map<String, Object> args = getChildArgs(name);
    if (args == null) {
      return;
    }
    try {
      ActionContext childContext = new ActionContext(
          args.get(Constants.Values.ACTION_ARG).toString(),
          args, messageId);
      childContext.contextualValues = contextualValues;
      childContext.preventRealtimeUpdating = preventRealtimeUpdating;
      childContext.isRooted = isRooted;
      childContext.parentContext = this;
      childContext.key = name;
      Leanplum.triggerAction(childContext);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  public void runTrackedActionNamed(String name) {
    try {
      if (!Constants.isNoop() && messageId != null && isRooted) {
        if (TextUtils.isEmpty(name)) {
          Log.e(TAG, "runTrackedActionNamed - Invalid name parameter provided.");
          return;
        }
        ActionContext context = this;
        List<ActionContext> parents = new ArrayList<>();
        while (context.parentContext != null) {
          parents.add(context);
          context = context.parentContext;
        }
        StringBuilder eventName = new StringBuilder();
        boolean ok = true;
        for (int i = parents.size() - 1; i >= -1; i--) {
          if (eventName.length() > 0) {
            eventName.append(' ');
          }
          String actionName;
          if (i > -1) {
            actionName = parents.get(i).key;
          } else {
            actionName = name;
          }
          if (actionName == null) {
            ok = false;
            break;
          }
          actionName = actionName.replace(" action", "");
          eventName.append(actionName);
        }
        if (ok) {
          Map<String, String> requestArgs = new HashMap<>();
          requestArgs.put(Constants.Params.MESSAGE_ID, messageId);
          Leanplum.track(eventName.toString(), 0.0, null, null, requestArgs);
        }
      }
      runActionNamed(name);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  public void track(String event, double value, Map<String, Object> params) {
    try {
      if (!Constants.isNoop() && messageId != null) {
        if (TextUtils.isEmpty(event)) {
          Log.e(TAG, "track - Invalid event parameter provided.");
          return;
        }
        Map<String, String> requestArgs = new HashMap<>();
        requestArgs.put(Constants.Params.MESSAGE_ID, messageId);
        Leanplum.track(event, 0.0, null, params, requestArgs);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  public void muteFutureMessagesOfSameKind() {
    try {
      ActionManager.instance().muteFutureMessagesOfKind(messageId);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  public int compareTo(ActionContext other) {
    return this.priority() - other.priority();
  }

  String messageId() {
    return messageId;
  }

  int priority() {
    return priority;
  }

  Map<String, Object> args() {
    return args;
  }
  
  void setIsPreview(boolean isPreview) {
    this.isPreview = isPreview;
  }

  boolean isPreview() {
    return isPreview;
  }
}

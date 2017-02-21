// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal;

import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumException;
import com.leanplum.callbacks.ActionCallback;
import com.leanplum.callbacks.StartCallback;
import com.leanplum.callbacks.VariablesChangedCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Internal leanplum methods that are very generic but should not be exposed to the public.
 *
 * @author Ben Marten
 */
public class LeanplumInternal {
  private static boolean hasStartedAndRegisteredAsDeveloper;
  private static final Map<String, List<ActionCallback>> actionHandlers = new HashMap<>();
  private static boolean issuedStart;
  private static boolean hasStarted;
  private static boolean startSuccessful;
  private static boolean calledStart;
  private static boolean isPaused;

  private static boolean startedInBackground;
  private static boolean inForeground;
  private static final Object inForegroundLock = new Object();
  private static final Queue<Map<String, ?>> userAttributeChanges = new ConcurrentLinkedQueue<>();
  private static final ArrayList<Runnable> startIssuedHandlers = new ArrayList<>();

  private static void onHasStartedAndRegisteredAsDeveloperAndFinishedSyncing() {
    if (!hasStartedAndRegisteredAsDeveloper) {
      hasStartedAndRegisteredAsDeveloper = true;
    }
  }

  /**
   * Called when the development mode has started and the device is registered successfully.
   */
  public static void onHasStartedAndRegisteredAsDeveloper() {
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

  public static void triggerAction(ActionContext context) {
    triggerAction(context, null);
  }

  public static void triggerAction(final ActionContext context,
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

  public static void maybePerformActions(String when, String eventName, int filter,
      String fromMessageId, ActionContext.ContextualValues contextualValues) {
    maybePerformActions(new String[] {when}, eventName, filter, fromMessageId, contextualValues);
  }

  public static void maybePerformActions(String[] whenConditions, String eventName, int filter,
      String fromMessageId, ActionContext.ContextualValues contextualValues) {
    Map<String, Object> messages = VarCache.messages();
    if (messages == null) {
      return;
    }

    ArrayList<ActionContext> actionContexts = new ArrayList<>();
    for (final String messageId : messages.keySet()) {
      if (messageId != null && messageId.equals(fromMessageId)) {
        continue;
      }

      Map<String, Object> messageConfig = CollectionUtil.uncheckedCast(messages.get(messageId));

      String actionType = (String) messageConfig.get("action");
      if (actionType == null) {
        continue;
      }

      final String internalMessageId;
      if (actionType.equals(ActionManager.HELD_BACK_ACTION_NAME)) {
        // Spoof the message ID for held back messages,
        // and store the original ID for the track event.
        internalMessageId = Constants.HELD_BACK_MESSAGE_PREFIX + messageId;
      } else {
        // If this is not a holdback, the internal message ID is the same as the original.
        internalMessageId = messageId;
      }

      boolean isForeground = !actionType.equals(ActionManager.PUSH_NOTIFICATION_ACTION_NAME);
      if (isForeground &&
          ((filter & LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_FOREGROUND) == 0)) {
        continue;
      }

      ActionManager.MessageMatchResult result = new ActionManager.MessageMatchResult();
      for (String when : whenConditions) {
        ActionManager.MessageMatchResult conditionResult = ActionManager.getInstance()
            .shouldShowMessage(internalMessageId, messageConfig, when, eventName, contextualValues);
        result.matchedTrigger |= conditionResult.matchedTrigger;
        result.matchedUnlessTrigger |= conditionResult.matchedUnlessTrigger;
        result.matchedLimit |= conditionResult.matchedLimit;
      }

      // Make sure we cancel before matching in case the criteria overlap.
      if (result.matchedUnlessTrigger) {
        String cancelActionName = "__Cancel" + messageConfig.get("action");
        LeanplumInternal.triggerAction(new ActionContext(
                cancelActionName, new HashMap<String, Object>(), messageId),
            new VariablesChangedCallback() {
              @Override
              public void variablesChanged() {
                // Track cancel.
                try {
                  Map<String, String> requestArgs = new HashMap<>();
                  requestArgs.put(Constants.Params.MESSAGE_ID, messageId);
                  track("Cancel", 0.0, null, null, requestArgs);
                } catch (Throwable t) {
                  Util.handleException(t);
                }
              }
            });
      }

      if (result.matchedTrigger) {
        ActionManager.getInstance().recordMessageTrigger(internalMessageId);

        if (result.matchedLimit) {
          int priority;
          if (messageConfig.containsKey("priority")) {
            priority = (int) messageConfig.get("priority");
          } else {
            priority = Constants.Messaging.DEFAULT_PRIORITY;
          }
          Map<String, Object> vars = CollectionUtil.uncheckedCast(messageConfig.get(Constants
                  .Keys.VARS));
          ActionContext actionContext = new ActionContext(
                  messageConfig.get("action").toString(),
                  vars,
                  internalMessageId,
                  messageId,
                  priority);
          actionContext.setContextualValues(contextualValues);
          actionContexts.add(actionContext);
        }
      }
    }

    if (!actionContexts.isEmpty()) {
      Collections.sort(actionContexts);
      int priorityThreshold = actionContexts.get(0).getPriority();
      for (final ActionContext actionContext : actionContexts) {
        if (actionContext.getPriority() <= priorityThreshold) {
          if (actionContext.actionName().equals(ActionManager.HELD_BACK_ACTION_NAME)) {
            ActionManager.getInstance().recordHeldBackImpression(
                actionContext.getMessageId(), actionContext.getOriginalMessageId());
          } else {
            LeanplumInternal.triggerAction(actionContext, new VariablesChangedCallback() {
              @Override
              public void variablesChanged() {
                try {
                  ActionManager.getInstance().recordMessageImpression(actionContext.getMessageId());
                } catch (Throwable t) {
                  Util.handleException(t);
                }
              }
            });
          }
        } else {
          break;
        }
      }
    }
  }

  public static void track(final String event, double value, String info,
      Map<String, ?> params, Map<String, String> args) {
    if (Constants.isNoop()) {
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
      if (!inForeground || LeanplumActivityHelper.isActivityPaused()) {
        requestParams.put("allowOffline", Boolean.TRUE.toString());
      }

      trackInternalWhenStarted(event, params, requestParams);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Performs the track operation once Leanplum has started. If Leanplum is already started, perform
   * the track immediately.
   *
   * @param event The event name. Event may be empty for message impression events.
   * @param params The event parameters.
   * @param requestArgs The arguments to send with the API request.
   */
  private static void trackInternalWhenStarted(final String event,
      final Map<String, ?> params, final Map<String, Object> requestArgs) {
    if (issuedStart) {
      trackInternal(event, params, requestArgs);
    } else {
      addStartIssuedHandler(new Runnable() {
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
   *
   * @param event The event name. Event may be empty for message impression events.
   * @param params The event parameters.
   * @param requestArgs The arguments to send with the API request.
   */
  private static void trackInternal(String event, Map<String, ?> params,
      Map<String, Object> requestArgs) {
    Request.post(Constants.Methods.TRACK, requestArgs).send();

    String eventTriggerName = event;
    String messageId = null;
    if (requestArgs.get(Constants.Params.MESSAGE_ID) != null) {
      messageId = requestArgs.get(Constants.Params.MESSAGE_ID).toString();
      eventTriggerName = ".m" + messageId;
      if (event != null && event.length() > 0) {
        eventTriggerName += " " + event;
      }
    }

    ActionContext.ContextualValues contextualValues = new ActionContext.ContextualValues();
    contextualValues.parameters = params;

    LeanplumInternal.maybePerformActions("event", eventTriggerName,
        LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL, messageId, contextualValues);
  }

  public static void performTrackedAction(String actionName, String messageId) {
    Map<String, Object> messages = VarCache.messages();
    if (messages == null) {
      return;
    }
    Map<String, Object> messageConfig = CollectionUtil.uncheckedCast(messages.get(messageId));
    if (messageConfig == null) {
      return;
    }
    Map<String, Object> vars = CollectionUtil.uncheckedCast(messageConfig.get(Constants.Keys.VARS));
    ActionContext context = new ActionContext(
        messageConfig.get("action").toString(),
        vars,
        messageId);
    context.runTrackedActionNamed(actionName);
  }

  public static void recordAttributeChanges() {
    boolean madeChanges = false;
    for (Map<String, ?> attributes : userAttributeChanges) {
      Map<String, Object> existingAttributes = VarCache.userAttributes();
      if (existingAttributes == null) {
        existingAttributes = new HashMap<>();
      }
      for (String attributeName : attributes.keySet()) {
        Object existingValue = existingAttributes.get(attributeName);
        Object value = attributes.get(attributeName);
        if ((existingValue == null && value != null) ||
            (existingValue != null && !existingValue.equals(value))) {
          ActionContext.ContextualValues contextualValues = new ActionContext.ContextualValues();
          contextualValues.previousAttributeValue = existingValue;
          contextualValues.attributeValue = value;
          existingAttributes.put(attributeName, value);
          LeanplumInternal.maybePerformActions("userAttribute", attributeName,
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

  public static void moveToForeground() {
    synchronized (inForegroundLock) {
      if (inForeground) {
        return;
      }
      inForeground = true;
    }

    Leanplum.addStartResponseHandler(new StartCallback() {
      @Override
      public void onResponse(boolean success) {
        if (!success) {
          return;
        }
        if (Constants.isDevelopmentModeEnabled && !Constants.isNoop()) {
          Socket.getInstance();
        }
        maybePerformActions(new String[] {"start", "resume"}, null,
            LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL, null, null);
        recordAttributeChanges();
      }
    });
  }

  public static void addStartIssuedHandler(Runnable handler) {
    if (handler == null) {
      Log.e("addStartIssuedHandler - Invalid handler parameter provided.");
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

  public static void triggerStartIssued() {
    synchronized (startIssuedHandlers) {
      LeanplumInternal.setIssuedStart(true);
      for (Runnable callback : startIssuedHandlers) {
        OsHandler.getInstance().post(callback);
      }
      startIssuedHandlers.clear();
    }
  }

  /**
   * Validates that the attributes are of the correct format. User attributes must be Strings,
   * Booleans, or Numbers.
   *
   * @param argName Argument name used for error messages.
   * @param allowLists Whether attribute values can be lists.
   */
  public static <T> Map<String, T> validateAttributes(Map<String, T> attributes, String argName,
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
          Iterable<Object> iterable = CollectionUtil.uncheckedCast(value);
          for (Object item : iterable) {
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
            Date date = CollectionUtil.uncheckedCast(value);
            value = CollectionUtil.uncheckedCast(date.getTime());
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

  public static boolean isValidScalarValue(Object value, String argName) {
    if (!(value instanceof Number) && !(value instanceof String) && !(value instanceof Boolean)) {
      maybeThrowException(new LeanplumException(
          argName + " values must be of type String, Number, or Boolean."));
      return false;
    }
    return true;
  }

  /**
   * Throws a LeanplumException when in development mode. Otherwise, logs the exception to prevent
   * the app from crashing.
   */
  public static void maybeThrowException(RuntimeException e) {
    if (Constants.isDevelopmentModeEnabled) {
      throw e;
    } else {
      Log.e(e.getMessage() + " This error is only thrown in development mode.", e);
    }
  }

  /***
   * Getters & Setters
   ***/
  public static boolean hasStartedAndRegisteredAsDeveloper() {
    return hasStartedAndRegisteredAsDeveloper;
  }

  public static void setHasStartedAndRegisteredAsDeveloper(
      boolean hasStartedAndRegisteredAsDeveloper) {
    LeanplumInternal.hasStartedAndRegisteredAsDeveloper = hasStartedAndRegisteredAsDeveloper;
  }

  public static Map<String, List<ActionCallback>> getActionHandlers() {
    return actionHandlers;
  }

  public static boolean issuedStart() {
    return issuedStart;
  }

  public static void setIssuedStart(boolean issuedStart) {
    LeanplumInternal.issuedStart = issuedStart;
  }

  public static boolean hasStarted() {
    return hasStarted;
  }

  public static void setHasStarted(boolean hasStarted) {
    LeanplumInternal.hasStarted = hasStarted;
  }

  public static boolean isStartSuccessful() {
    return startSuccessful;
  }

  public static void setStartSuccessful(boolean startSuccessful) {
    LeanplumInternal.startSuccessful = startSuccessful;
  }

  public static boolean hasCalledStart() {
    return calledStart;
  }

  public static void setCalledStart(boolean calledStart) {
    LeanplumInternal.calledStart = calledStart;
  }

  public static boolean isPaused() {
    return isPaused;
  }

  public static void setIsPaused(boolean isPaused) {
    LeanplumInternal.isPaused = isPaused;
  }

  public static boolean hasStartedInBackground() {
    return startedInBackground;
  }

  public static void setStartedInBackground(boolean startedInBackground) {
    LeanplumInternal.startedInBackground = startedInBackground;
  }

  public static void setInForeground(boolean inForeground) {
    LeanplumInternal.inForeground = inForeground;
  }

  public static Queue<Map<String, ?>> getUserAttributeChanges() {
    return userAttributeChanges;
  }

  public static ArrayList<Runnable> getStartIssuedHandlers() {
    return startIssuedHandlers;
  }
}

// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

import com.leanplum.Leanplum.OsHandler;
import com.leanplum.callbacks.VariablesChangedCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Leanplum socket class, that handles connections to the Leanplum remote socket.
 *
 * @author Andrew First, Ben Marten
 */
class LeanplumSocket {
  private static final String TAG = "Leanplum";
  private static final String EVENT_CONTENT_RESPONSE = "getContentResponse";
  private static final String EVENT_UPDATE_VARS = "updateVars";
  private static final String EVENT_TRIGGER = "trigger";
  private static final String EVENT_GET_VARIABLES = "getVariables";
  private static final String EVENT_GET_ACTIONS = "getActions";
  private static final String EVENT_REGISTER_DEVICE = "registerDevice";

  private SocketIOClient sio;
  private boolean authSent;
  private boolean connected = false;
  private boolean connecting = false;

  /**
   * Creates a new socket connection to leanplum server as defined in the Constants.
   */
  public LeanplumSocket() {
    SocketIOClient.Handler socketIOClientHandler = new SocketIOClient.Handler() {
      @Override
      public void onError(Exception error) {
        Log.e(TAG, "Development socket error", error);
      }

      @Override
      public void onDisconnect(int code, String reason) {
        Log.i(TAG, "Disconnected from development server");
        connected = false;
        connecting = false;
        authSent = false;
      }

      @Override
      public void onConnect() {
        if (!authSent) {
          Log.i(TAG, "Connected to development server");
          try {
            Map<String, String> args = Util.newMap(
                Constants.Params.APP_ID, LeanplumRequest.appId(),
                Constants.Params.DEVICE_ID, LeanplumRequest.deviceId());
            try {
              sio.emit("auth", new JSONArray(Collections.singletonList(new JSONObject(args))));
            } catch (JSONException e) {
              e.printStackTrace();
            }
          } catch (Throwable t) {
            Util.handleException(t);
          }
          authSent = true;
          connected = true;
          connecting = false;
        }
      }

      @Override
      public void on(String event, JSONArray arguments) {
        try {
          switch (event) {
            case EVENT_UPDATE_VARS:
              Leanplum.forceContentUpdate();
              break;
            case EVENT_TRIGGER:
              handleTriggerEvent(arguments);
              break;
            case EVENT_GET_VARIABLES:
              handleGetVariablesEvent();
              break;
            case EVENT_GET_ACTIONS:
              handleGetActionsEvent();
              break;
            case EVENT_REGISTER_DEVICE:
              handleRegisterDeviceEvent(arguments);
              break;
          }
        } catch (Throwable t) {
          Util.handleException(t);
        }
      }
    };

    try {
      sio = new SocketIOClient(new URI("http://" + Constants.SOCKET_HOST + ":" +
          Constants.SOCKET_PORT), socketIOClientHandler);
    } catch (URISyntaxException e) {
      Log.e(TAG, e.getMessage());
    }
    connect();
    Timer reconnectTimer = new Timer();
    reconnectTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          reconnect();
        } catch (Throwable t) {
          Util.handleException(t);
        }
      }
    }, 0, 5000);
  }

  /**
   * Connect to the remote socket.
   */
  private void connect() {
    connecting = true;
    sio.connect();
  }

  /**
   * Disconnect from the remote socket.
   */
  private void reconnect() {
    if (!connected && !connecting) {
      connect();
    }
  }

  /**
   * Send a given event and data to the remote socket server.
   *
   * @param eventName The name of the event.
   * @param data The data to be sent to the remote server.
   */
  private <T> void sendEvent(String eventName, Map<String, T> data) {
    try {
      sio.emit(eventName,
          new JSONArray(Collections.singletonList(JsonConverter.mapToJsonObject(data))));
    } catch (JSONException e) {
      Log.e("Leanplum", "Failed to create JSON data object: " + e.getMessage());
    }
  }

  /**
   * Handles the "trigger" event received from server.
   *
   * @param arguments The arguments received from server.
   */
  @SuppressWarnings("unchecked")
  void handleTriggerEvent(JSONArray arguments) {
    // Trigger a custom action.
    try {
      JSONObject payload = arguments.getJSONObject(0);
      JSONObject actionJson = payload.getJSONObject(Constants.Params.ACTION);
      if (actionJson != null) {
        String messageId = payload.getString(Constants.Params.MESSAGE_ID);
        boolean isRooted = payload.getBoolean("isRooted");
        String actionType = actionJson.getString(Constants.Values.ACTION_ARG);
        Map<String, Object> defaultDefinition = (Map<String, Object>)
            VarCache.actionDefinitions().get(actionType);
        Map<String, Object> defaultArgs = defaultDefinition != null ?
            ((Map<String, Object>) defaultDefinition.get("values")) : null;
        Map<String, Object> action = JsonConverter.mapFromJson(actionJson);
        action = (Map<String, Object>) VarCache.mergeHelper(defaultArgs, action);
        ActionContext context = new ActionContext(actionType, action, messageId);
        context.preventRealtimeUpdating();
        context.setIsRooted(isRooted);
        context.setIsPreview(true);
        context.update();
        Leanplum.triggerAction(context);
        ActionManager.instance().recordMessageImpression(messageId);
      }
    } catch (JSONException e) {
      Log.e(TAG, "Error getting action info", e);
    }
  }

  /**
   * Handles the "getVariables" event received from server.
   */
  void handleGetVariablesEvent() {
    boolean sentValues = VarCache.sendVariablesIfChanged();
    VarCache.maybeUploadNewFiles();
    sendEvent(EVENT_CONTENT_RESPONSE, Util.newMap("updated", sentValues));
  }

  /**
   * Handles the "getActions" event received from server.
   */
  void handleGetActionsEvent() {
    boolean sentValues = VarCache.sendActionsIfChanged();
    VarCache.maybeUploadNewFiles();
    sendEvent(EVENT_CONTENT_RESPONSE, Util.newMap("updated", sentValues));
  }

  /**
   * Handles the "registerDevice" event received from server.
   *
   * @param arguments The arguments received from server.
   */
  void handleRegisterDeviceEvent(JSONArray arguments) {
    Leanplum.onHasStartedAndRegisteredAsDeveloper();
    String emailArg = null;
    try {
      emailArg = arguments.getJSONObject(0).getString("email");
    } catch (JSONException e) {
      Log.v(TAG, "LeanplumSocket - No developer e-mail provided.");
    }
    final String email = (emailArg == null) ? "a Leanplum account" : emailArg;
    OsHandler.getInstance().post(new Runnable() {
      @Override
      public void run() {
        LeanplumActivityHelper.queueActionUponActive(new VariablesChangedCallback() {
          @Override
          public void variablesChanged() {
            Activity activity = LeanplumActivityHelper.getCurrentActivity();
            AlertDialog.Builder alert = new AlertDialog.Builder(activity);
            alert.setTitle(TAG);
            alert.setMessage("Your device is registered to " + email + ".");
            alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
              }
            });
            alert.show();
          }
        });
      }
    });
  }
}

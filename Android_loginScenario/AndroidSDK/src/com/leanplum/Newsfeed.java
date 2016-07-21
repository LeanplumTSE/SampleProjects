// Copyright 2015, Leanplum, Inc. All rights reserved.

package com.leanplum;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.leanplum.callbacks.NewsfeedChangedCallback;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Newsfeed class.
 *
 * @author Aleksandar Gyorev
 */
public class Newsfeed {
  private static Newsfeed instance = new Newsfeed();

  // Newsfeed properties.
  private int unreadCount;
  private Map<String, NewsfeedMessage> messages;
  private boolean didLoad = false;
  private final List<NewsfeedChangedCallback> newsfeedChangedHandlers;
  private final Object updatingLock = new Object();

  /**
   * A private constructor, which prevents any other class from instantiating.
   */
  private Newsfeed() {
    this.unreadCount = 0;
    this.messages = new HashMap<String, NewsfeedMessage>();
    this.didLoad = false;
    this.newsfeedChangedHandlers = new ArrayList<NewsfeedChangedCallback>();
  }

  /**
   * Static 'instance' method.
   */
  static Newsfeed getInstance() {
    return instance;
  }

  void updateUnreadCount(int unreadCount) {
    this.unreadCount = unreadCount;
    save();
    triggerNewsfeedChanged();
  }

  void updateNewsfeed(Map<String, NewsfeedMessage> messages, int unreadCount, boolean shouldSave) {
    try {
      synchronized (updatingLock) {
        this.unreadCount = unreadCount;
        if (messages != null) {
          this.messages = messages;
        }
      }
      this.didLoad = true;
      if (shouldSave) {
        save();
      }
      triggerNewsfeedChanged();
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  void removeMessage(String messageId) {
    int unreadCount = this.unreadCount;
    if (!messageForId(messageId).isRead()) {
      unreadCount--;
    }

    messages.remove(messageId);
    updateNewsfeed(messages, unreadCount, true);

    if (Constants.isNoop()) {
      return;
    }

    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Constants.Params.NEWSFEED_MESSAGE_ID, messageId);
    LeanplumRequest req = LeanplumRequest.post(Constants.Methods.DELETE_NEWSFEED_MESSAGE, params);
    req.send();
  }

  void triggerNewsfeedChanged() {
    synchronized (newsfeedChangedHandlers) {
      for (NewsfeedChangedCallback callback : newsfeedChangedHandlers) {
        Leanplum.OsHandler.getInstance().post(callback);
      }
    }
  }

  @SuppressWarnings("unchecked")
  void load() {
    if (Constants.isNoop()) {
      return;
    }
    Context context = Leanplum.getContext();
    SharedPreferences defaults = context.getSharedPreferences(
        "__leanplum__", Context.MODE_PRIVATE);
    if (LeanplumRequest.token() == null) {
      updateNewsfeed(new HashMap<String, NewsfeedMessage>(), 0, false);
      return;
    }
    int unreadCount = 0;
    AESCrypt aesContext = new AESCrypt(LeanplumRequest.appId(), LeanplumRequest.token());
    String newsfeedString = aesContext.decodePreference(
        defaults, Constants.Defaults.NEWSFEED_KEY, "{}");
    Map<String, Object> newsfeed = JsonConverter.fromJson(newsfeedString);

    Map<String, NewsfeedMessage> messages = new HashMap<String, NewsfeedMessage>();
    for (Map.Entry<String, Object> entry : newsfeed.entrySet()) {
      String messageId = entry.getKey();
      Map<String, Object> data = (Map<String, Object>) entry.getValue();
      NewsfeedMessage message = NewsfeedMessage.createFromJsonMap(messageId, data);

      if (message.isActive()) {
        messages.put(messageId, message);
        if (!message.isRead()) {
          unreadCount++;
        }
      }
    }
    updateNewsfeed(messages, unreadCount, false);
  }

  void save() {
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
    Map<String, Object> messages = new HashMap<String, Object>();
    for (Map.Entry<String, NewsfeedMessage> entry : this.messages.entrySet()) {
      String messageId = entry.getKey();
      NewsfeedMessage newsfeedMessage = entry.getValue();
      Map<String, Object> data = newsfeedMessage.toJsonMap();
      messages.put(messageId, data);
    }
    String messagesJson = JsonConverter.toJson(messages);
    AESCrypt aesContext = new AESCrypt(LeanplumRequest.appId(), LeanplumRequest.token());
    editor.putString(Constants.Defaults.NEWSFEED_KEY, aesContext.encrypt(messagesJson));
    try {
      editor.apply();
    } catch (NoSuchMethodError e) {
      editor.commit();
    }
  }

  void downloadMessages() {
    if (Constants.isNoop()) {
      return;
    }

    LeanplumRequest req = LeanplumRequest.post(Constants.Methods.GET_NEWSFEED_MESSAGES, null);
    req.onResponse(new LeanplumRequest.ResponseCallback() {
      @Override
      public void response(JSONObject responses) {
        try {
          JSONObject response = LeanplumRequest.getLastResponse(responses);
          if (response == null) {
            Log.e("Leanplum", "No newsfeed response received from the server.");
            return;
          }

          JSONObject messagesDict = response.optJSONObject(Constants.Keys.NEWSFEED_MESSAGES);
          if (messagesDict == null) {
            Log.e("Leanplum", "No newsfeed messages found in the response from the server.");
            return;
          }
          int unreadCount = 0;
          Map<String, NewsfeedMessage> messages = new HashMap<String, NewsfeedMessage>();

          for (Iterator iterator = messagesDict.keys(); iterator.hasNext();) {
            String messageId = (String) iterator.next();
            JSONObject messageDict = messagesDict.getJSONObject(messageId);

            Map<String, Object> actionArgs = JsonConverter.mapFromJson(
                messageDict.getJSONObject(Constants.Keys.MESSAGE_DATA).getJSONObject(Constants.Keys.VARS)
            );
            Long deliveryTimestamp = messageDict.getLong(Constants.Keys.DELIVERY_TIMESTAMP);
            Long expirationTimestamp = null;
            if (messageDict.opt(Constants.Keys.EXPIRATION_TIMESTAMP) != null) {
              expirationTimestamp = messageDict.getLong(Constants.Keys.EXPIRATION_TIMESTAMP);
            }
            boolean isRead = messageDict.getBoolean(Constants.Keys.IS_READ);

            NewsfeedMessage message = NewsfeedMessage.constructNewsfeedMessage(messageId,
                deliveryTimestamp, expirationTimestamp, isRead, actionArgs);
            if (message != null) {
              if (!isRead) {
                unreadCount++;
              }
              messages.put(messageId, message);
            }
          }
          updateNewsfeed(messages, unreadCount, true);
        } catch (Throwable t) {
          Util.handleException(t);
        }
      }
    });
    req.sendIfConnected();
  }

  /**
   * Returns the number of all newsfeed messages on the device.
   */
  public int count() {
    return messages.size();
  }

  /**
   * Returns the number of the unread newsfeed messages on the device.
   */
  public int unreadCount() {
    return unreadCount;
  }

  /**
   * Returns the identifiers of all newsfeed messages on the device sorted in ascending
   * chronological order, i.e. the id of the oldest message is the first one, and the most
   * recent one is the last one in the array.
   */
  public List<String> messagesIds() {
    List<String> messageIds = new ArrayList<String>(messages.keySet());
    try {
      Collections.sort(messageIds, new Comparator<String>() {
        @Override
        public int compare(String firstMessage, String secondMessage) {
          Date firstDate = messageForId(firstMessage).deliveryTimestamp();
          Date secondDate = messageForId(secondMessage).deliveryTimestamp();
          return firstDate.compareTo(secondDate);
        }
      });
    } catch (Throwable t) {
      Util.handleException(t);
    }
    return messageIds;
  }

  /**
   * Returns a List containing all of the newsfeed messages sorted chronologically ascending
   * (i.e. the oldest first and the newest last).
   */
  public List<NewsfeedMessage> allMessages() {
    List<NewsfeedMessage> messages = new ArrayList<NewsfeedMessage>();
    try {
      for (String messageId : messagesIds()) {
        messages.add(messageForId(messageId));
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
    return messages;
  }

  /**
   * Returns a List containing all of the unread newsfeed messages sorted chronologically ascending
   * (i.e. the oldest first and the newest last).
   */
  public List<NewsfeedMessage> unreadMessages() {
    List<NewsfeedMessage> unreadMessages = new ArrayList<NewsfeedMessage>();
    List<NewsfeedMessage> messages = allMessages();
    for (NewsfeedMessage message : messages) {
      if (!message.isRead()) {
        unreadMessages.add(message);
      }
    }
    return unreadMessages;
  }

  /**
   * Returns the newsfeed messages associated with the given messageId identifier.
   */
  public NewsfeedMessage messageForId(String messageId) {
    return messages.get(messageId);
  }

  /**
   * Add a callback for when the newsfeed receives new values from the server.
   */
  public void addNewsfeedChangedHandler(NewsfeedChangedCallback handler) {
    synchronized (newsfeedChangedHandlers) {
      newsfeedChangedHandlers.add(handler);
    }
    if (this.didLoad) {
      handler.newsfeedChanged();
    }
  }

  /**
   * Removes a newsfeed changed callback.
   */
  public void removeNewsfeedChangedHandler(NewsfeedChangedCallback handler) {
    synchronized (newsfeedChangedHandlers) {
      newsfeedChangedHandlers.remove(handler);
    }
  }

  void reset() {
    this.unreadCount = 0;
    this.messages.clear();
    this.newsfeedChangedHandlers.clear();
    this.didLoad = false;
  }
}

// Copyright 2015, Leanplum, Inc. All rights reserved.

package com.leanplum;

import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * NewsfeedMessage class.
 *
 * @author Aleksandar Gyorev
 */
public class NewsfeedMessage {
  private String messageId;
  private Long deliveryTimestamp;
  private Long expirationTimestamp;
  private boolean isRead;
  private ActionContext context;

  private NewsfeedMessage(String messageId, Long deliveryTimestamp, Long expirationTimestamp,
      boolean isRead, ActionContext context) {
    this.messageId = messageId;
    this.deliveryTimestamp = deliveryTimestamp;
    this.expirationTimestamp = expirationTimestamp;
    this.isRead = isRead;
    this.context = context;
  }

  static NewsfeedMessage constructNewsfeedMessage(String messageId, Long deliveryTimestamp,
        Long expirationTimestamp, boolean isRead, Map<String, Object> actionArgs) {
    if (!isValidMessageId(messageId)) {
      Log.e("Leanplum", "Malformed newsfeed messageId: " + messageId);
      return null;
    }

    String[] messageIdParts = messageId.split("##");
    ActionContext context = new ActionContext((String) actionArgs.get(Constants.Values.ACTION_ARG),
        actionArgs, messageIdParts[0]);
    context.preventRealtimeUpdating();
    context.update();
    return new NewsfeedMessage(messageId, deliveryTimestamp, expirationTimestamp, isRead, context);
  }

  Map<String, Object> toJsonMap() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put(Constants.Keys.DELIVERY_TIMESTAMP, this.deliveryTimestamp);
    map.put(Constants.Keys.EXPIRATION_TIMESTAMP, this.expirationTimestamp);
    map.put(Constants.Keys.MESSAGE_DATA, this.actionArgs());
    map.put(Constants.Keys.IS_READ, this.isRead());
    return map;
  }

  @SuppressWarnings("unchecked")
  static NewsfeedMessage createFromJsonMap(String messageId, Map<String, Object> map) {
    return constructNewsfeedMessage(messageId,
        (Long) map.get(Constants.Keys.DELIVERY_TIMESTAMP),
        (Long) map.get(Constants.Keys.EXPIRATION_TIMESTAMP),
        (Boolean) map.get(Constants.Keys.IS_READ),
        (Map<String, Object>) map.get(Constants.Keys.MESSAGE_DATA));
  }

  Map<String, Object> actionArgs() {
    return context.args();
  }

  void setIsRead(boolean isRead) {
    this.isRead = isRead;
  }

  boolean isActive() {
    if (expirationTimestamp == null) {
      return true;
    }

    Date now = new Date();
    return now.before(new Date(expirationTimestamp));
  }

  static boolean isValidMessageId(String messageId) {
    return messageId.split("##").length == 2;
  }

  /**
   * Returns the message identifier of the newsfeed message.
   */
  public String messageId() {
    return messageId;
  }

  /**
   * Returns the title of the newsfeed message.
   */
  public String title() {
    return context.stringNamed(Constants.Keys.TITLE);
  }

  /**
   * Returns the subtitle of the newsfeed message.
   */
  public String subtitle() {
    return context.stringNamed(Constants.Keys.SUBTITLE);
  }

  /**
   * Returns the delivery timestamp of the newsfeed message.
   */
  public Date deliveryTimestamp() {
    return new Date(deliveryTimestamp);
  }

  /**
   * Return the expiration timestamp of the newsfeed message.
   */
  public Date expirationTimestamp() {
    if (expirationTimestamp == null) {
      return null;
    }
    return new Date(expirationTimestamp);
  }

  /**
   * Returns 'true' if the newsfeed message is read.
   */
  public boolean isRead() {
    return isRead;
  }

  /**
   * Read the newsfeed message, marking it as read and invoking its open action.
   */
  public void read() {
    try {
      if (Constants.isNoop()) {
        return;
      }

      if (!this.isRead) {
        setIsRead(true);

        int unreadCount = Newsfeed.getInstance().unreadCount() - 1;
        Newsfeed.getInstance().updateUnreadCount(unreadCount);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(Constants.Params.NEWSFEED_MESSAGE_ID, messageId);
        LeanplumRequest req = LeanplumRequest.post(Constants.Methods.MARK_NEWSFEED_MESSAGE_AS_READ,
            params);
        req.send();
      }
      this.context.runTrackedActionNamed(Constants.Values.DEFAULT_PUSH_ACTION);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Remove the newsfeed message from the newsfeed.
   */
  public void remove() {
    try {
      Newsfeed.getInstance().removeMessage(messageId);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }
}

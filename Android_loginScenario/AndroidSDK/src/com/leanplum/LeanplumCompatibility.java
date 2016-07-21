// All rights reserved. Leanplum. 2013.
// Author: Atanas Dobrev (dobrev@leanplum.com)

package com.leanplum;

import android.text.TextUtils;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

/**
 * Leanplum Android Compatibility SDK.
 * 
 * @author Atanas Dobrev
 */
public class LeanplumCompatibility {
  public static final String TYPE = "&t";
  public static final String EVENT_CATEGORY = "&ec";
  public static final String EVENT_ACTION = "&ea";
  public static final String EVENT_LABEL = "&el";
  public static final String EVENT_VALUE = "&ev";
  public static final String EXCEPTION_DESCRIPTION = "&exd";
  public static final String TRANSACTION_AFFILIATION = "&ta";
  public static final String ITEM_NAME = "&in";
  public static final String ITEM_CATEGORY = "&iv";
  public static final String SOCIAL_NETWORK = "&sn";
  public static final String SOCIAL_ACTION = "&sa";
  public static final String SOCIAL_TARGET = "&st";
  public static final String TIMING_NAME = "&utv";
  public static final String TIMING_CATEGORY = "&utc";
  public static final String TIMING_LABEL = "&utl";
  public static final String TIMING_VALUE = "&utt";
  public static final String CAMPAIGN_SOURCE = "&cs";
  public static final String CAMPAIGN_NAME = "&cn";
  public static final String CAMPAIGN_MEDUIM = "&cm";
  public static final String CAMPAIGN_CONTENT = "&cc";

  /**
   * This method is for compatibility with Flurry.
   */
  public static void fTrack(String event) {
    Leanplum.track(event);
  }

  /**
   * This method is for compatibility with Flurry.
   */
  public static void fTrack(String event, boolean timed) {
    if (timed) {
      Leanplum.track(event + " begin", 0.0, "", new HashMap<String, Object>());
    } else {
      Leanplum.track(event, 0.0, "", new HashMap<String, Object>());
    }
  }

  /**
   * This method is for compatibility with Flurry.
   */
  public static void fTrack(String event, HashMap<String, ?> params) {
    Leanplum.track(event, params);
  }

  /**
   * This method is for compatibility with Flurry.
   */
  public static void fTrack(String event, HashMap<String, ?> params,
      boolean timed) {
    if (timed) {
      Leanplum.track(event + " begin", 0.0, "", params);
    } else {
      Leanplum.track(event, 0.0, "", params);
    }
  }

  /**
   * This method is for compatibility with Flurry.
   */
  public static void fTrack(String event, String errorMessage, String errorClass) {
    HashMap<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("errorMessage", errorMessage);
    attributes.put("errorClass", errorClass);
    Leanplum.track(event + " error", 0.0, "", attributes);
  }

  /**
   * This method is for compatibility with Google Analytics.
   */
  public static void gaTrack(Map<String, String> inputParams) {
    try {
      String event = "";
      String stringValue = null;
      HashMap<String, String> params = new HashMap<String, String>(inputParams);
  
      // Event.
      if (params.get(LeanplumCompatibility.EVENT_CATEGORY) != null
          || params.get(LeanplumCompatibility.EVENT_ACTION) != null
          || params.get(LeanplumCompatibility.EVENT_LABEL) != null) {
        event = LeanplumCompatibility.getEventName(params, Arrays
            .asList(LeanplumCompatibility.EVENT_CATEGORY,
                LeanplumCompatibility.EVENT_ACTION,
                LeanplumCompatibility.EVENT_LABEL));
        stringValue = params.get(LeanplumCompatibility.EVENT_VALUE);
        if (stringValue != null) {
          params.remove(LeanplumCompatibility.EVENT_VALUE);
        }
        // Exception.
      } else if (params.get(LeanplumCompatibility.EXCEPTION_DESCRIPTION) != null) {
        event = LeanplumCompatibility.getEventName(params, Arrays.asList(
            LeanplumCompatibility.EXCEPTION_DESCRIPTION,
            LeanplumCompatibility.TYPE));
        // Transaction.
      } else if (params.get(LeanplumCompatibility.TRANSACTION_AFFILIATION) != null) {
        event = LeanplumCompatibility.getEventName(params, Arrays.asList(
            LeanplumCompatibility.TRANSACTION_AFFILIATION,
            LeanplumCompatibility.TYPE));
        // Item.
      } else if (params.get(LeanplumCompatibility.ITEM_CATEGORY) != null
          || params.get(LeanplumCompatibility.ITEM_NAME) != null) {
        event = LeanplumCompatibility.getEventName(params, Arrays.asList(
            LeanplumCompatibility.ITEM_CATEGORY, LeanplumCompatibility.ITEM_NAME,
            LeanplumCompatibility.TYPE));
        // Social.
      } else if (params.get(LeanplumCompatibility.SOCIAL_NETWORK) != null
          || params.get(LeanplumCompatibility.SOCIAL_ACTION) != null
          || params.get(LeanplumCompatibility.SOCIAL_TARGET) != null) {
        event = LeanplumCompatibility.getEventName(params, Arrays.asList(
            LeanplumCompatibility.SOCIAL_NETWORK,
            LeanplumCompatibility.SOCIAL_ACTION,
            LeanplumCompatibility.SOCIAL_TARGET));
        // Timing.
      } else if (params.get(LeanplumCompatibility.TIMING_CATEGORY) != null
          || params.get(LeanplumCompatibility.TIMING_NAME) != null
          || params.get(LeanplumCompatibility.TIMING_LABEL) != null) {
        event = LeanplumCompatibility.getEventName(params, Arrays.asList(
            LeanplumCompatibility.TIMING_CATEGORY,
            LeanplumCompatibility.TIMING_NAME,
            LeanplumCompatibility.TIMING_LABEL, LeanplumCompatibility.TYPE));
        stringValue = params.get(LeanplumCompatibility.TIMING_VALUE);
        if (stringValue != null) {
          params.remove(LeanplumCompatibility.TIMING_VALUE);
        }
        // We are skipping traffic source events.
      } else if (params.get(LeanplumCompatibility.CAMPAIGN_MEDUIM) != null
          || params.get(LeanplumCompatibility.CAMPAIGN_CONTENT) != null
          || params.get(LeanplumCompatibility.CAMPAIGN_NAME) != null
          || params.get(LeanplumCompatibility.CAMPAIGN_SOURCE) != null) {
        return;
        // Can't decide just write as event the type of the object.
      } else {
        return;
      }
  
      for (String key : params.keySet()) {
        if (params.get(key) == null) {
          params.remove(key);
        }
      }
  
      // Event value.
      if (stringValue != null) {
        double value = Double.parseDouble(stringValue);
        Leanplum.track(event, value, params);
      } else {
        Leanplum.track(event, params);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  private static String getEventName(HashMap<String, String> params,
      List<String> keys) {
    List<String> resultValues = new LinkedList<String>();
    for (String key : keys) {
      if (params.get(key) != null) {
        resultValues.add(params.get(key));
        params.remove(key);
      }
    }
    return TextUtils.join(" ", resultValues);
  }
}

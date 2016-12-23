package com.divenire;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.leanplum.ActionContext;
import com.leanplum.LeanplumPushListenerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by fede on 11/14/16.
 */

public class Custom_PushListener extends LeanplumPushListenerService {
    //    @Override
    public void onMessageReceived(String var, Bundle notificationPayload) {
        if (ApplicationClass.isActive) {
            if(!TextUtils.isEmpty(notificationPayload.getString("_lpm"))){
                Map<String, Object> args = new HashMap<String, Object>();
                args.put("Open action", fromJson(
                        notificationPayload.getString("_lpx")));
                ActionContext context = new ActionContext(
                        "__Push Notification", args, notificationPayload.getString("_lpm"));
                context.preventRealtimeUpdating();
                context.update();
                context.runTrackedActionNamed("Open action");
            }
            if(TextUtils.isEmpty(notificationPayload.getString("_lpn"))){
                return;
            }
            Map<String, String> arg = new HashMap<>();
            arg.put("Message", notificationPayload.getString("lp_message"));
            arg.put("__name__", "Alert");
            Map<String, Object> args = new HashMap<>();
            args.put("Open action", arg);
            ActionContext context = new ActionContext("__Push Notification", args, notificationPayload.getString("_lpn"));
            context.preventRealtimeUpdating();
            context.update();
            context.runTrackedActionNamed("Open action");
        } else {
            super.onMessageReceived(var, notificationPayload);
            Log.i("### " , "isActive false");
            Log.i("### ", "1 received");
        }
    }
    public static Map<String, Object> fromJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return mapFromJson(new JSONObject(json));
        } catch (JSONException e) {
            Log.e("Leanplum", "Error converting " + json + " from JSON", e);
            return null;
        }
    }
    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> mapFromJson(JSONObject object) {
        if (object == null) {
            return null;
        }
        Map<String, T> result = new HashMap<>();
        Iterator<?> keysIterator = object.keys();
        while (keysIterator.hasNext()) {
            String key = (String) keysIterator.next();
            Object value = object.opt(key);
            if (value == null || value == JSONObject.NULL) {
                value = null;
            } else if (value instanceof JSONObject) {
                value = mapFromJson((JSONObject) value);
            } else if (value instanceof JSONArray) {
                value = listFromJson((JSONArray) value);
            } else if (JSONObject.NULL.equals(value)) {
                value = null;
            }
            result.put(key, (T) value);
        }
        return result;
    }
    @SuppressWarnings("unchecked")
    public static <T> List<T> listFromJson(JSONArray json) {
        if (json == null) {
            return null;
        }
        List<Object> result = new ArrayList<>(json.length());
        for (int i = 0; i < json.length(); i++) {
            Object value = json.opt(i);
            if (value == null || value == JSONObject.NULL) {
                value = null;
            } else if (value instanceof JSONObject) {
                value = mapFromJson((JSONObject) value);
            } else if (value instanceof JSONArray) {
                value = listFromJson((JSONArray) value);
            } else if (JSONObject.NULL.equals(value)) {
                value = null;
            }
            result.add(value);
        }
        return (List<T>) result;
    }
}


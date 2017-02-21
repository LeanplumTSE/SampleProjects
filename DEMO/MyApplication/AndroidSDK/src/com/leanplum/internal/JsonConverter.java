// Copyright 2013, Leanplum, Inc.

package com.leanplum.internal;

import android.text.Editable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Converts objects to/from JSON.
 *
 * @author Andrew First
 */
public class JsonConverter {
  public static String toJson(Map<String, ?> map) {
    if (map == null) {
      return null;
    }
    try {
      return mapToJsonObject(map).toString();
    } catch (JSONException e) {
      Log.e("Error converting " + map + " to JSON", e);
      return null;
    }
  }

  public static Map<String, Object> fromJson(String json) {
    if (json == null) {
      return null;
    }
    try {
      return mapFromJson(new JSONObject(json));
    } catch (JSONException e) {
      Log.e("Error converting " + json + " from JSON", e);
      return null;
    }
  }

  public static JSONObject mapToJsonObject(Map<String, ?> map) throws JSONException {
    if (map == null) {
      return null;
    }
    JSONObject obj = new JSONObject();
    for (String key : map.keySet()) {
      Object value = map.get(key);
      if (value instanceof Map) {
        Map<String, ?> mappedValue = CollectionUtil.uncheckedCast(value);
        value = mapToJsonObject(mappedValue);
      } else if (value instanceof Iterable) {
        value = listToJsonArray((Iterable<?>) value);
      } else if (value instanceof Editable) {
        value = value.toString();
      } else if (value == null) {
        value = JSONObject.NULL;
      }
      obj.put(key, value);
    }
    return obj;
  }


  public static JSONArray listToJsonArray(Iterable<?> list) throws JSONException {
    if (list == null) {
      return null;
    }
    JSONArray obj = new JSONArray();
    for (Object value : list) {
      if (value instanceof Map) {
        Map<String, ?> mappedValue = CollectionUtil.uncheckedCast(value);
        value = mapToJsonObject(mappedValue);
      } else if (value instanceof Iterable) {
        value = listToJsonArray((Iterable<?>) value);
      } else if (value == null) {
        value = JSONObject.NULL;
      }
      obj.put(value);
    }
    return obj;
  }

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
      T castedValue = CollectionUtil.uncheckedCast(value);
      result.put(key, castedValue);
    }
    return result;
  }

  public static <T> Map<String, T> mapFromJsonOrDefault(JSONObject object) {
    if (object == null) {
      return new HashMap<>();
    }
    return mapFromJson(object);
  }

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
    return CollectionUtil.uncheckedCast(result);
  }

  public static <T> List<T> listFromJsonOrDefault(JSONArray json) {
    if (json == null) {
      return new ArrayList<>();
    }
    return listFromJson(json);
  }
}

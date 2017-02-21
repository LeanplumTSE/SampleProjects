// Copyright 2016, Leanplum, Inc

package com.leanplum.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to easily create new list, map or set objects containing provided parameters.
 *
 * @author Ben Marten
 */
public class CollectionUtil {
  /**
   * Creates a new ArrayList and adds the passed arguments to it.
   *
   * @param items The items to add to the list.
   * @param <T> The type of the list to be created.
   * @return A typed list that contains the passed arguments.
   */
  @SafeVarargs
  public static <T> ArrayList<T> newArrayList(T... items) {
    ArrayList<T> result = new ArrayList<>(items.length);
    Collections.addAll(result, items);
    return result;
  }

  /**
   * Creates a new HashSet and adds the passed arguments to it.
   *
   * @param items The items to add to the set.
   * @param <T> The type of the set to be created.
   * @return A typed set that contains the passed arguments.
   */
  @SafeVarargs
  public static <T> HashSet<T> newHashSet(T... items) {
    HashSet<T> result = new HashSet<>(items.length);
    Collections.addAll(result, items);
    return result;
  }

  /**
   * Creates a new HashMap and adds the passed arguments to it in pairs.
   *
   * @param items The keys and values, to add to the map in pairs.
   * @param <T> The type of the map to be created.
   * @return A typed map that contains the passed arguments.
   * @throws IllegalArgumentException Throws an exception when an uneven number of arguments are
   * passed.
   */
  @SuppressWarnings("unchecked")
  public static <T, U> HashMap<T, U> newHashMap(Object... items) {
    return (HashMap<T, U>) newMap(new HashMap(items.length), items);
  }

  /**
   * Creates a new HashMap and adds the passed arguments to it in pairs.
   *
   * @param items The keys and values, to add to the map in pairs.
   * @param <T> The type of the map to be created.
   * @return A typed map that contains the passed arguments.
   * @throws IllegalArgumentException Throws an exception when an uneven number of arguments are
   * passed.
   */
  @SuppressWarnings("unchecked")
  public static <T, U> LinkedHashMap<T, U> newLinkedHashMap(Object... items) {
    return (LinkedHashMap<T, U>) newMap(new LinkedHashMap(items.length), items);
  }

  /**
   * Creates a new Map and adds the passed arguments to it in pairs.
   *
   * @param items The keys and values, to add to the map in pairs.
   * @param <T> The type of the map to be created.
   * @return A typed map that contains the passed arguments.
   * @throws IllegalArgumentException Throws an exception when an even number of arguments are
   * passed, or the type parameter is not a subclass of map.
   */
  @SuppressWarnings("unchecked")
  private static <T, U> Map<T, U> newMap(Map<T, U> map, T[] items) {
    if (items.length % 2 != 0) {
      throw new IllegalArgumentException("newMap requires an even number of items.");
    }

    for (int i = 0; i < items.length; i += 2) {
      map.put(items[i], (U) items[i + 1]);
    }
    return map;
  }

  /**
   * Returns the components of an array as concatenated String by calling toString() on each item.
   *
   * @param array The array to be concatenated.
   * @param separator The separator between elements.
   * @return A concatenated string of the items in list.
   */
  public static <T> String concatenateArray(T[] array, String separator) {
    if (array == null) {
      return null;
    }
    return concatenateList(Arrays.asList(array), separator);
  }

  /**
   * Returns the components of a list as concatenated String by calling toString() on each item.
   *
   * @param list The list to be concatenated.
   * @param separator The separator between elements.
   * @return A concatenated string of the items in list.
   */
  public static String concatenateList(List<?> list, String separator) {
    if (list == null) {
      return null;
    }
    if (separator == null) {
      separator = "";
    }
    StringBuilder stringBuilder = new StringBuilder();
    for (Object item : list) {
      stringBuilder.append(item.toString());
      stringBuilder.append(separator);
    }
    String result = stringBuilder.toString();

    if (result.length() > 0) {
      return result.substring(0, result.length() - separator.length());
    } else {
      return result;
    }
  }

  @SuppressWarnings({"unchecked"})
  public static <T> T uncheckedCast(Object obj) {
    return (T) obj;
  }
}

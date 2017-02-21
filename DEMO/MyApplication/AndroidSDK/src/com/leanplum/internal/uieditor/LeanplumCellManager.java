// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.app.Activity;
import android.view.View;

import com.leanplum.internal.CollectionUtil;
import com.leanplum.internal.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps track of cells reverse rules.
 *
 * @author Ben Marten
 */
public class LeanplumCellManager {
  private static final Map<String, Map<View, List<Map<String, Object>>>> cellReverseUpdateRules =
      new HashMap<>();

  /**
   * Adds a reverse rule to internal storage.
   *
   * @param reverseRule The reverse rule.
   */
  public static void saveCellReverseRule(String activityName, View cell,
      final Map<String, Object> reverseRule) {
    Log.p("Adding cell reverse rule: " + reverseRule.toString() + " for cell: " +
        (cell != null ? cell.hashCode() : ""));

    Map<View, List<Map<String, Object>>> reverseUpdateRulesForActivity =
        cellReverseUpdateRules.get(activityName);
    if (reverseUpdateRulesForActivity == null) {
      reverseUpdateRulesForActivity = new WeakHashMap<>();
      reverseUpdateRulesForActivity.put(cell,
          CollectionUtil.newArrayList(reverseRule));
      cellReverseUpdateRules.put(activityName, reverseUpdateRulesForActivity);
    } else {
      List<Map<String, Object>> reverseUpdateRulesForCell = reverseUpdateRulesForActivity.get(cell);
      if (reverseUpdateRulesForCell == null) {
        reverseUpdateRulesForActivity.put(cell,
            CollectionUtil.newArrayList(reverseRule));
      } else {
        reverseUpdateRulesForCell.add(reverseRule);
      }
    }
  }

  /**
   * Get the cell reverse rule for a given activity name and cell and remove it.
   *
   * @param activityName The name of the activity.
   * @param cell The cell for the reverse rule.
   * @return The list of reverse rules.
   */
  public static List<Map<String, Object>> popCellReverseRules(String activityName, View cell) {
    Log.p("Getting reverse rule for cell: " + cell.hashCode());
    Map<View, List<Map<String, Object>>> cellReverseUpdateRulesForActivity =
        cellReverseUpdateRules.get(activityName);
    if (cellReverseUpdateRulesForActivity == null) {
      return null;
    }
    List<Map<String, Object>> cellReverseRules = cellReverseUpdateRulesForActivity.get(cell);
    if (cellReverseRules != null) {
      cellReverseUpdateRulesForActivity.remove(cell);
    }
    return cellReverseRules;
  }

  /**
   * Clears the applied rules for an activity.
   *
   * @param activity The activity to clear the cells for.
   */
  public static void resetCellsForActivity(Activity activity) {
    Map<View, List<Map<String, Object>>> reverseRulesForActivity =
        cellReverseUpdateRules.get(activity.getClass().getName());
    if (reverseRulesForActivity == null) {
      return;
    }

    Log.p("Resetting cells for activity: " + activity.getClass());
    for (Map.Entry entry : reverseRulesForActivity.entrySet()) {
      View cell = (View) entry.getKey();

      // Pitching idea on unchecked cast to reduce code cluttering with annotation.
      List<Map<String, Object>> reverseRules = CollectionUtil.uncheckedCast(entry.getValue());
      LeanplumViewManager.applyUpdateRules(activity, reverseRules, false, true, cell, null);
    }

    cellReverseUpdateRules.remove(activity.getClass().getName());
  }
}

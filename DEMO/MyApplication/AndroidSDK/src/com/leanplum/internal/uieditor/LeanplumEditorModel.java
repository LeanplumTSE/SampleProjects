// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.app.Activity;
import android.view.View;

import com.leanplum.internal.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EditorModel class that is holding the reverseUpdateRules and the appliedEvents.
 *
 * @author Ben Marten
 */
public class LeanplumEditorModel {
  private static final Map<String, List<Map<String, Object>>> reverseUpdateRules = new HashMap<>();
  private static final Map<String, Map<String, LeanplumEditorEvent>> appliedEvents =
      new HashMap<>();
  private static final LeanplumCellManager listViewCellManager = new LeanplumCellManager();
  private static final LeanplumCellManager viewPagerCellManager = new LeanplumCellManager();
  private static final LeanplumCellManager recycleViewCellManager = new LeanplumCellManager();

  /**
   * Adds a reverse rule to internal storage.
   *
   * @param reverseRule The reverse rule.
   * @param activityName The activity name of the reverse rule.
   */
  public static void addReverseUpdateRule(Map<String, Object> reverseRule, String activityName) {
    Log.p("Adding reverse update rule: " + reverseRule.toString());
    List<Map<String, Object>> reverseRulesForActivity = reverseUpdateRules.get(activityName);
    if (reverseRulesForActivity == null) {
      reverseRulesForActivity = new ArrayList<>();
    }
    reverseRulesForActivity.add(reverseRule);
    reverseUpdateRules.put(activityName, reverseRulesForActivity);
  }

  /**
   * Returns the reverse rules for an activity.
   *
   * @param activityName The name of the activity.
   * @return The list of reverse rules.
   */
  public static List<Map<String, Object>> getReverseRules(String activityName) {
    return reverseUpdateRules.get(activityName);
  }

  /**
   * Returns the applied events for an activity.
   *
   * @param activityName The name of the activity.
   * @return Returns a map of events.
   */
  public static Map<String, LeanplumEditorEvent> getAppliedEventsForActivity(String activityName) {
    if (appliedEvents.get(activityName) == null) {
      appliedEvents.put(activityName, new HashMap<String, LeanplumEditorEvent>());
    }
    return appliedEvents.get(activityName);
  }

  /**
   * Saves an applied event for an activity.
   *
   * @param activityName The name of the activity.
   * @param event The event to store.
   */
  public static void addAppliedEventForActivity(
      String activityName, final LeanplumEditorEvent event
  ) {
    Log.p("Adding event: " + event.getEventName() + " for activity: " + activityName);
    Map<String, LeanplumEditorEvent> eventsForActivity = appliedEvents.get(activityName);
    if (eventsForActivity == null) {
      eventsForActivity = new HashMap<>();
    }
    eventsForActivity.put(event.getEventName(), event);
  }

  /**
   * Clears all applied events for an activity.
   *
   * @param activityName The name of the activity.
   */
  public static void clearAppliedEventForActivity(String activityName) {
    Log.p("Clearing all events for activity: " + activityName);
    Map<String, LeanplumEditorEvent> events = appliedEvents.get(activityName);
    if (events != null) {
      for (Map.Entry<String, LeanplumEditorEvent> eventEntry : events.entrySet()) {
        eventEntry.getValue().remove();
      }
      appliedEvents.remove(activityName);
    }
  }

  /**
   * Retrieves the listViewCellManager that manages the list view cells.
   *
   * @return The cell manager.
   */
  public static LeanplumCellManager getListViewCellManager() {
    return listViewCellManager;
  }

  /**
   * Retrieves the viewPagerCellManager that manages the list view cells.
   *
   * @return The cell manager.
   */
  public static LeanplumCellManager getViewPagerCellManager() {
    return viewPagerCellManager;
  }

  /**
   * Retrieves the recycleViewCellManager that manages the list view cells.
   *
   * @return The cell manager.
   */
  public static LeanplumCellManager getRecycleViewCellManager() {
    return recycleViewCellManager;
  }

  /**
   * Clears the reverse rules for the given activity.
   *
   * @param activity The activity.
   */
  public static void clearReverseRulesForActivity(Activity activity) {
    reverseUpdateRules.remove(activity.getClass().getName());
  }

  /**
   * Resets the views of an activity to its original state.
   *
   * @param activity The activity to be reset.
   */
  public static void resetViews(Activity activity, View cell, Number row) {
    LeanplumCellManager.resetCellsForActivity(activity);
    final List<Map<String, Object>> reverseRules =
        getReverseRules(activity.getClass().getName());
    LeanplumViewManager.applyUpdateRules(activity, reverseRules, false, true, cell, row);
    LeanplumEditorModel.clearReverseRulesForActivity(activity);
  }
}

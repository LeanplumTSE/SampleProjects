// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import com.leanplum.LeanplumActivityHelper;
import com.leanplum.internal.ClassUtil;
import com.leanplum.internal.CollectionUtil;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;
import com.leanplum.internal.VarCache;
import com.leanplum.internal.ViewPagerUtil;
import com.leanplum.internal.uieditor.model.ViewRootData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.leanplum.internal.uieditor.LeanplumEditorModel.addAppliedEventForActivity;
import static com.leanplum.internal.uieditor.LeanplumEditorModel.clearAppliedEventForActivity;
import static com.leanplum.internal.uieditor.LeanplumEditorModel.getAppliedEventsForActivity;

/**
 * Manages the updates to and from the activities/fragments.
 *
 * @author Ben Marten
 */
public class LeanplumViewManager {
  private static final int APPLY_UPDATES_DELAY = 100;
  private static final int UPDATE_IN_PROGRESS_DELAY = 200;

  private static boolean isInterfaceEditingEnabled = false;
  private static boolean isScreenTrackingEnabled = false;
  private static boolean isUpdateInProgress = false;
  private static Handler delayedUiUpdateHandler;
  private static Runnable delayedUiUpdateRunnable;
  private static Handler delayedEventUpdateHandler;
  private static Runnable delayedEventUpdateRunnable;

  public static boolean getIsInterfaceEditingEnabled() {
    return isInterfaceEditingEnabled;
  }

  public static void enableInterfaceEditor() {
    isInterfaceEditingEnabled = true;
  }

  public static boolean getIsScreenTrackingEnabled() {
    return isScreenTrackingEnabled;
  }

  public static void enableAutomaticScreenTracking() {
    isScreenTrackingEnabled = true;
  }

  /**
   * Whether or not we are in the process of applying updates to the UI.
   *
   * @return true if updates are in progress, otherwise false.
   */
  public static boolean getIsUpdateInProgress() {
    return isUpdateInProgress;
  }

  public static void setIsUpdateInProgress(final Boolean isUpdateInProgress) {
    if (isUpdateInProgress) {
      LeanplumViewManager.isUpdateInProgress = true;
    } else {
      // Disable update flag delayed to be sure, not to run in to a endless loop.
      new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
        @Override
        public void run() {
          try {
            LeanplumViewManager.isUpdateInProgress = false;
          } catch (Throwable t) {
            Util.handleException(t);
          }
        }
      }, UPDATE_IN_PROGRESS_DELAY);
    }
  }

  /**
   * Triggers the application of the update rules on the views.
   */
  public static void showInterfacePreviewForVisibleViewControllers() {
    if (!Constants.isDevelopmentModeEnabled || isUpdateInProgress) {
      return;
    }
    isUpdateInProgress = true;
    final Activity currentActivity = LeanplumActivityHelper.getCurrentActivity();

    if (currentActivity == null) {
      Log.p("Can't get activity, not showing interface preview...");
      return;
    }

    final List<Map<String, Object>> updateRules = VarCache.getUpdateRuleDiffs();


    currentActivity.runOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            try {
              LeanplumEditorModel.resetViews(currentActivity, null, null);
              applyUpdateRules(currentActivity, updateRules, false, false, null, null);
              isUpdateInProgress = false;
            } catch (Throwable t) {
              Util.handleException(t);
            }
          }
        });
  }

  /**
   * Shows the event preview for the visible view controllers.
   */
  public static void showEventPreviewForVisibleViewControllers() {
    if (!Constants.isDevelopmentModeEnabled) {
      return;
    }
    final Activity currentActivity = LeanplumActivityHelper.getCurrentActivity();
    clearAppliedEventForActivity(currentActivity.getClass().getName());

    List<Map<String, Object>> updateRules = VarCache.getEventRuleDiffs();
    if (updateRules != null && updateRules.size() > 0) {
      LeanplumCellManager.resetCellsForActivity(currentActivity);
      applyUpdateRules(currentActivity, updateRules, true, false, null, null);
    }
  }

  /**
   * Applies the current rules for the given cell or row to a cell.
   *
   * @param cell The cell to apply the rules to.
   * @param rowIndex The row to apply the rules to.
   * @param isRevert Whether or not its a revert rule.
   */
  public static void applyListViewRulesInCell(View cell, int rowIndex, Boolean isRevert) {
    final Activity currentActivity = LeanplumActivityHelper.getCurrentActivity();
    LeanplumViewManager.setIsUpdateInProgress(true);
    LeanplumViewManager.applyUpdateRules(
        currentActivity, VarCache.getUpdateRuleDiffs(), false, isRevert, cell, rowIndex);
    LeanplumViewManager.setIsUpdateInProgress(false);
  }

  /**
   * Applies the given rules for the given cell or row to a cell.
   *
   * @param cell The cell to apply the rules to.
   * @param rowIndex The row to apply the rules to.
   * @param isRevert Whether or not its a revert rule.
   * @param rules The rules to apply
   */
  public static void applyListViewRulesInCell(View cell, int rowIndex, Boolean isRevert,
      List<Map<String, Object>> rules) {
    final Activity currentActivity = LeanplumActivityHelper.getCurrentActivity();
    LeanplumViewManager.setIsUpdateInProgress(true);
    LeanplumViewManager.applyUpdateRules(
        currentActivity, rules, false, isRevert, cell, rowIndex);
    LeanplumViewManager.setIsUpdateInProgress(false);
  }

  /**
   * Apply interface and event update rules to the given activity.
   *
   * @param activity The activity to apply the rules on.
   */
  public static void applyInterfaceAndEventUpdateRules(Activity activity) {
    LeanplumViewManager.applyUiUpdateRulesDelayed(activity, VarCache.getUpdateRuleDiffs(),
        null, null, 0);
    LeanplumViewManager.applyEventUpdateRulesDelayed(activity, VarCache.getEventRuleDiffs(),
        false, null, null, 0);
  }

  /**
   * Apply interface and event update rules to the given activity delayed.
   *
   * @param activity The activity to apply the rules on.
   */
  public static void applyInterfaceAndEventUpdateRulesDelayed(Activity activity) {
    LeanplumViewManager.applyUiUpdateRulesDelayed(activity, VarCache.getUpdateRuleDiffs(),
        null, null, APPLY_UPDATES_DELAY);
    LeanplumViewManager.applyEventUpdateRulesDelayed(activity, VarCache.getEventRuleDiffs(),
        false, null, null, APPLY_UPDATES_DELAY);
  }

  /**
   * Apply Updated Rules delayed.
   */
  private static void applyUiUpdateRulesDelayed(final Activity activity,
      final List<Map<String, Object>> updateRules, final View cell, final Number row, int delay) {
    if (delayedUiUpdateHandler == null) {
      delayedUiUpdateHandler = new Handler(Looper.getMainLooper());
    }

    if (delayedUiUpdateRunnable != null) {
      Log.p("Canceled existing scheduled update thread.");
      delayedUiUpdateHandler.removeCallbacks(delayedUiUpdateRunnable);
    }

    // Post a new update runnable.
    delayedUiUpdateRunnable = new Runnable() {
      @Override
      public void run() {
        try {
          LeanplumViewManager.setIsUpdateInProgress(true);
          LeanplumEditorModel.resetViews(activity, cell, row);
          LeanplumViewManager.applyUpdateRules(activity, updateRules, false, false, cell, row);
          LeanplumViewManager.setIsUpdateInProgress(false);
        } catch (Throwable t) {
          Util.handleException(t);
        }
      }
    };
    boolean success = delayedUiUpdateHandler.postDelayed(delayedUiUpdateRunnable, delay);
    if (!success) {
      Log.p("Could not post update delayed runnable.");
    }
  }

  /**
   * Send an update with given delay of the UI to the LP server.
   */
  private static void applyEventUpdateRulesDelayed(final Activity activity,
      final List<Map<String, Object>> updateRules, final Boolean isRevert, final View cell,
      final Number row, int delay) {
    if (delayedEventUpdateHandler == null) {
      Log.p("Canceled existing scheduled update thread.");
      delayedEventUpdateHandler = new Handler(Looper.getMainLooper());
    }

    if (delayedEventUpdateRunnable != null) {
      Log.p("Canceled existing scheduled update thread.");
      delayedEventUpdateHandler.removeCallbacks(delayedEventUpdateRunnable);
    }

    // Post a new update runnable.
    delayedEventUpdateRunnable = new Runnable() {
      @Override
      public void run() {
        try {
          LeanplumViewManager.setIsUpdateInProgress(true);
          LeanplumViewManager.applyUpdateRules(activity, updateRules, true, isRevert, cell, row);
          LeanplumViewManager.setIsUpdateInProgress(false);
        } catch (Throwable t) {
          Util.handleException(t);
        }
      }
    };
    boolean success = delayedEventUpdateHandler.postDelayed(delayedEventUpdateRunnable, delay);
    if (!success) {
      Log.p("Could not post update delayed runnable.");
    }
  }

  /**
   * Apply the update rules to an activities views.
   *
   * @param activity The activity.
   * @param updateRules The update rules.
   * @param isEvent True if its event update rules.
   * @param isRevert True if its revert rules.
   */
  public static void applyUpdateRules(Activity activity,
      final List<Map<String, Object>> updateRules, final boolean isEvent, final boolean isRevert,
      final View cell, final Number row) {
    if (updateRules == null || updateRules.isEmpty()) {
      return;
    }

    // Get first child of rootView.
    List<ViewRootData> viewRootDataList = null;
    try {
      viewRootDataList = ClassUtil.getRootViews(activity);
    } catch (Exception e) {
      Log.e("Can not get viewRootData list.");
    }

    for (Map<String, Object> changeRule : updateRules) {
      LeanplumActivityInfo activityIndex = LeanplumViewHelper.
          getActivityIndex(changeRule.get("viewController").toString());
      ViewRootData rootViewData = null;
      if (viewRootDataList != null && activityIndex.getActivityIndex() < viewRootDataList.size()) {
        rootViewData = viewRootDataList.get(activityIndex.getActivityIndex());
      }

      if (!activityIndex.getName().equals(activity.getClass().getName()) || rootViewData == null) {
        Log.p("Rules do not apply to this view controller or subview of that activity not found.");
        continue;
      }

      ViewGroup rootViewGroup = ClassUtil.getViewGroup(rootViewData.getView());

      List<Map<String, Object>> rulePath = CollectionUtil.uncheckedCast(changeRule.get("viewPath"));

      LeanplumCellViewHolder cellViewHolder =
          traverseCells(rulePath, isRevert, cell, row, rootViewGroup);

      if (cellViewHolder.getTargetView() == null) {
        cellViewHolder.setTargetView(
            LeanplumViewHelper.getTargetViewForViewPath(rootViewGroup, rulePath));
        if (cellViewHolder.getTargetView() == null) {
          continue;
        }
      }

      if (!isRevert && !isEvent) {
        Map<String, Object> reverseRule =
            makeReverseRule(cellViewHolder.getTargetView(), changeRule);
        // Take the provided cell or if none provided the found cell (if scroll view found).
        if (cellViewHolder.getCellView() != null) {
          if (cellViewHolder.getAbsListView() != null) {
            LeanplumCellManager.saveCellReverseRule(
                activityIndex.getName(), cellViewHolder.getCellView(), reverseRule);
          } else if (cellViewHolder.getRecyclerView() != null) {
            LeanplumCellManager.saveCellReverseRule(
                activityIndex.getName(), cellViewHolder.getCellView(), reverseRule);
          } else if (cellViewHolder.getViewPager() != null) {
            LeanplumCellManager.saveCellReverseRule(
                activityIndex.getName(), cellViewHolder.getCellView(), reverseRule);
          }
        } else {
          LeanplumEditorModel.addReverseUpdateRule(reverseRule, activity.getClass().getName());
        }
      }
      if (isEvent) {
        applyEvents(activity, cellViewHolder.getTargetView(), changeRule);
      } else {
        applyChanges(cellViewHolder.getTargetView(), changeRule);
      }
    }
  }

  /**
   * Handles finding the correct target view for a given rulePath when cells are detected.
   *
   * @param rulePath The rule path.
   * @param isRevert Whether the rule is a revert rule.
   * @param cell The cell.
   * @param row The row.
   * @param rootViewGroup The rootview group
   * @return LeanplumCellViewHolder containing the information about the found view.
   */
  private static LeanplumCellViewHolder traverseCells(List<Map<String, Object>> rulePath,
      Boolean isRevert, View cell, Number row, ViewGroup rootViewGroup) {
    LeanplumCellViewHolder cellViewHolder = new LeanplumCellViewHolder(rootViewGroup, rulePath);
    cellViewHolder.setCellView(cell);
    for (Map rulePart : rulePath) {
      Number ruleCellRow = (Number) rulePart.get("row");
      if (ruleCellRow == null) {
        Log.p("Not a cell rule part.");
        continue;
      }
      // If revert rule, skip the next validation, because the row of the cell has already changed.
      if (!isRevert && cell != null && row != null && ruleCellRow.intValue() != row.intValue()) {
        Log.p("Rule does not apply to this cell. rule row: " + ruleCellRow);
        continue;
      }

      // TODO: Extract to separate method, after unit tests are added.
      if (cellViewHolder.getCellView() == null) {
        // If no cell and row is provided, try to find it.
        if (cellViewHolder.getAbsListView() != null) {
          if (ruleCellRow.intValue() < cellViewHolder.getAbsListView().getFirstVisiblePosition() ||
              ruleCellRow.intValue() > cellViewHolder.getAbsListView().getLastVisiblePosition()) {
            Log.p("Cell not visible, not applying updates");
            continue;
          }
          int start = cellViewHolder.getAbsListView().getFirstVisiblePosition();
          cellViewHolder.setCellView(
              cellViewHolder.getAbsListView().getChildAt(ruleCellRow.intValue() - start));
        } else if (cellViewHolder.getRecyclerView() != null) {
          Object layoutManager = ClassUtil
              .invokeMethod(cellViewHolder.getRecyclerView(), "getLayoutManager", null, null);

          if (layoutManager != null) {
            Number firstVisibleItemPosition = (Number) ClassUtil.
                invokeMethod(layoutManager, "findFirstVisibleItemPosition", null, null);
            Number lastVisibleItemPosition = (Number) ClassUtil.
                invokeMethod(layoutManager, "findLastVisibleItemPosition", null, null);
            if ((firstVisibleItemPosition !=
                null && ruleCellRow.intValue() < firstVisibleItemPosition.intValue()) ||
                (lastVisibleItemPosition != null &&
                    ruleCellRow.intValue() > lastVisibleItemPosition.intValue())) {
              Log.p("Cell not visible, not applying updates");
              continue;
            }
            if (firstVisibleItemPosition != null) {
              final int index = ruleCellRow.intValue() - firstVisibleItemPosition.intValue();
              cellViewHolder.setCellView((View) ClassUtil
                  .invokeMethod(layoutManager, "getChildAt",
                      CollectionUtil.<Class>newArrayList(int.class),
                      CollectionUtil.newArrayList(index)
                  )
              );
            }
          }
        } else if (cellViewHolder.getViewPager() != null) {
          if (cellViewHolder.getViewPager().getCurrentItem() != ruleCellRow.intValue()) {
            Log.p("Page: not visible, not applying updates");
            continue;
          }
          cellViewHolder.setCellView(ViewPagerUtil
              .getViewAtPosition(cellViewHolder.getViewPager(), ruleCellRow.intValue()));
        }
      }
      List<Map<String, Object>> cellRulePath =
          rulePath.subList(rulePath.indexOf(rulePart) + 1, rulePath.size());
      // Retrieve the subview to apply the updates to from the rest of the rule path.
      if (cellRulePath.size() > 0 && cellViewHolder.getCellView() != null) {
        ViewGroup viewGroup = ClassUtil.getViewGroup(cellViewHolder.getCellView());
        View view = LeanplumViewHelper.getTargetViewForViewPath(viewGroup, cellRulePath);
        if (view != null) { // We have found target view.
          cellViewHolder.setTargetView(view);
        } else { // Target view not found, dig deeper, starting from the current cell.
          cellViewHolder = traverseCells(cellRulePath, isRevert, null, row, viewGroup);
        }
      } else if (cellViewHolder.getCellView() != null) {
        // We already have the correct subview, use that.
        cellViewHolder.setTargetView(cellViewHolder.getCellView());
      }
      // Stop looping through the rest of the rules because we have already found the desired path.
      break;
    }
    return cellViewHolder;
  }

  /**
   * Creates reverse rules for a view.
   *
   * @param view The view to create the reverse rules for.
   * @param changeRule The actual update rule.
   * @return A map of reverse rules.
   */
  private static Map<String, Object> makeReverseRule(View view, Map<String, Object> changeRule) {
    Log.p("Generating Reverse rules for activity: " + view.getClass().toString());
    List<Map<String, Object>> previousChanges = new ArrayList<>();
    List<Map<String, String>> changes = CollectionUtil.uncheckedCast(changeRule.get("changes"));
    if (Constants.isDevelopmentModeEnabled) {
      for (final Map<String, String> change : changes) {
        final String key = change.get("key");
        final Object value = LeanplumInterfaceProperty.getValueForProperty(key, view);
        previousChanges.add(CollectionUtil.<String, Object>newHashMap(
                "key", key,
                "value", value,
                "ifValueIs", change.get("value")
        ));
      }
    }
    Log.p("Generated reverse rule: \n" + previousChanges.toString());

    Map<String, Object> revertUpdateRule = new HashMap<>(changeRule);
    revertUpdateRule.put("changes", previousChanges);
    return revertUpdateRule;
  }

  /**
   * Apply the ui update rules to a view.
   *
   * @param view The view to update.
   * @param updateRule The update rules.
   */
  private static void applyChanges(final View view, Map<String, Object> updateRule) {
    Log.p("Applying updates: \n" + updateRule.get("changes").toString() +
            "\n to view: " + view.getClass().getName());
    List<Map<String, String>> changes = CollectionUtil.uncheckedCast(updateRule.get("changes"));
    for (Map<String, String> change : changes) {
      String propertyName = change.get("key");
      final Object newValue = change.get("value");
      final LeanplumProperty property = LeanplumInterfaceProperty.getProperty(propertyName, view);
      if (property == null) {
        Log.p("Property named '" + propertyName + "' not found!");
        continue;
      }

      Object ifValueIs = change.get("ifValueIs");
      if (ifValueIs != null) {
        // Only make the change if the current value is equal to "ifValueIs".
        // Used by reverse rules.
        if (!property.isValueInViewEquivalent(view, ifValueIs)) {
          continue;
        }
      }

      try {
        // Run on UI thread.
        if (Looper.myLooper() == Looper.getMainLooper()) {
          property.setPropertyValue(view, newValue);
        } else {
          new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              try {
                property.setPropertyValue(view, newValue);
              } catch (Throwable t) {
                Util.handleException(t);
              }
            }
          });
        }
      } catch (Throwable t) {
        Util.handleException(t);
      }
    }
  }

  /**
   * Apply the event update rules to a view.
   *
   * @param view The view to update.
   * @param updateRule The event update rules.
   */
  private static void applyEvents(Activity activity, View view, Map<String, Object> updateRule) {
    List<Map<String, String>> changes = CollectionUtil.uncheckedCast(updateRule.get("changes"));
    Map<String, LeanplumEditorEvent> appliedEvents =
            getAppliedEventsForActivity(activity.getClass().getName());
    for (Map<String, String> change : changes) {
      String propertyName = change.get("key");
      if (!propertyName.equals("tapEvent")) {
        Log.p("Ignoring unrecognized event.");
        continue;
      }
      String userEventName = change.get("value");
      LeanplumEditorEvent oldEvent = appliedEvents.get(userEventName);
      if (oldEvent != null && oldEvent.getView() == view) {
        Log.p("View is already listening for event: " + userEventName);
        continue;
      }
      Log.p("Adding event: " + userEventName + " to view: " + view.getClass().getName());
      LeanplumEditorEvent editorEvent = new LeanplumEditorEvent(userEventName, view);
      addAppliedEventForActivity(activity.getClass().getName(), editorEvent);
    }
  }
}

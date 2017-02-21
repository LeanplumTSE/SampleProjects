// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.app.Activity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.leanplum.internal.ClassUtil;
import com.leanplum.internal.CollectionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper class for handling views and activities.
 *
 * @author Ben Marten
 */
public class LeanplumViewHelper {
  private static final Class<?> recyclerViewClass = ClassUtil.recyclerViewClass();
  private static final String ACTIVITY_INDEX_IDENTIFIER = "_-_";

  /**
   * Returns the first target view for a given view path.
   *
   * @param viewGroup The view group to search.
   * @param viewPath The required view path.
   * @return The found view.
   */
  public static Object getFirstTargetViewForViewPath(ViewGroup viewGroup,
      List<Map<String, Object>> viewPath) {
    List<Class> classList = CollectionUtil.<Class>newArrayList(ViewPager.class, AbsListView.class);
    if (recyclerViewClass != null) {
      classList.add(recyclerViewClass);
    }
    return getTargetAbstractViewForViewPath(viewGroup, viewPath, classList);
  }

  /**
   * Scans the given viewGroup for the first occurrence of any of the given classes along view tree
   *
   * @param viewGroup The view group to search.
   * @param viewPath The required view path.
   * @param classes The given classes to be checked against
   * @return The found view.
   */
  private static Object getTargetAbstractViewForViewPath(
      ViewGroup viewGroup, List<Map<String, Object>> viewPath, List<Class> classes) {
    if (viewGroup == null || viewPath == null || viewPath.size() == 0) {
      return null;
    }
    Map<String, Object> viewDescription = viewPath.get(0);

    if (viewDescription == null || viewDescription.get("index") == null) {
      return null;
    }
    int index = Integer.parseInt(viewDescription.get("index").toString());
    String className = viewDescription.get("class").toString();

    // Loop through children and find correct next node
    int sameChildInLevelCounter = 0;
    for (int i = 0; i < viewGroup.getChildCount(); i++) {
      View subview = viewGroup.getChildAt(i);
      String subviewClassName = subview.getClass().getName();
      if (subviewClassName.equals(className)) {
        if (index > sameChildInLevelCounter) {
          sameChildInLevelCounter++;
        } else {
          for (Class clazz : classes) {
            //noinspection unchecked
            if (clazz.isAssignableFrom(subview.getClass())) {
              return subview;
            }
          }
          // pop current view and dig deeper
          return getTargetAbstractViewForViewPath(
              ClassUtil.getViewGroup(subview), viewPath.subList(1, viewPath.size()), classes);
        }
      }
    }

    return null;
  }

  /**
   * Returns the target view for a given view path.
   *
   * @param viewGroup The view group to search.
   * @param viewPath The required view path.
   * @return The found view.
   */
  public static View getTargetViewForViewPath(ViewGroup viewGroup, List<Map<String,
      Object>> viewPath) {
    if (viewGroup == null) {
      return null;
    }
    Map<String, Object> viewDescription = viewPath.get(0);
    if (viewDescription.get("index") == null) {
      return null;
    }
    int index = Integer.parseInt(viewDescription.get("index").toString());
    String className = viewDescription.get("class").toString();

    // Loop through children and find correct next node.
    int sameChildInLevelCounter = 0;
    for (int i = 0; i < viewGroup.getChildCount(); i++) {
      View subview = viewGroup.getChildAt(i);
      String subviewClassName = subview.getClass().getName();
      if (subviewClassName.equals(className)) {
        if (index > sameChildInLevelCounter) {
          sameChildInLevelCounter++;
        } else {
          if (viewPath.size() > 1) {
            // Pop the current view and dig deeper.
            return getTargetViewForViewPath(
                ClassUtil.getViewGroup(subview), viewPath.subList(1, viewPath.size())
            );
          } else {
            return subview;
          }
        }
      }
    }

    return null;
  }

  /**
   * Holder class for the activity name and subview index.
   *
   * @param activityName The name of the activity.
   * @return An instance of an ActivityInfo class
   */
  public static LeanplumActivityInfo getActivityIndex(String activityName) {
    int subViewPos = activityName.indexOf(ACTIVITY_INDEX_IDENTIFIER);
    int activitySubViewIndex = 0;
    if (subViewPos > -1) {
      activitySubViewIndex = Integer.parseInt(activityName.substring(subViewPos +
          ACTIVITY_INDEX_IDENTIFIER.length(), activityName.length()));
      activityName = activityName.substring(0, subViewPos);
    }

    return new LeanplumActivityInfo(activityName, activitySubViewIndex);
  }

  /**
   * Retrieves the activity path for a given activity.
   *
   * @param activity The activity.
   * @return A list containing the name of the activities class.
   */
  public static ArrayList<String> getActivityPath(final Activity activity, final Number index) {
    return CollectionUtil.newArrayList(activity.getClass().getName() +
        ((index.intValue() > 0) ? ACTIVITY_INDEX_IDENTIFIER +
            index.toString() : ""));
  }
}

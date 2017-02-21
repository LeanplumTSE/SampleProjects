// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.app.ActionBar;
import android.app.Activity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.leanplum.internal.ClassUtil;
import com.leanplum.internal.CollectionUtil;
import com.leanplum.internal.JsonConverter;
import com.leanplum.internal.Log;
import com.leanplum.internal.uieditor.model.ViewRootData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hierarchy Scanner, scans the views recursive to create a structure description.
 *
 * @author Ben Marten
 */
class LeanplumHierarchyScanner {
  /**
   * Classes for which we should not read subviews.
   */
  @SuppressWarnings("deprecation")
  private static final List<Class> terminalViewClasses = CollectionUtil.<Class>newArrayList(
      Button.class, // UIButton
      TextView.class, // UITextField
      Switch.class, // UISwitch
      ImageView.class, // UISwitch
      SeekBar.class, // UISlider
      ProgressBar.class, // UIProgressView
      DatePicker.class, // UIDatePicker
      WebView.class, // UIWebView
      SearchView.class, // UISearchBar
      ActionBar.Tab.class, // UITabBarItem
      ActionBar.class // UITabBar
  );

  private static final Class<?> recyclerViewClass = ClassUtil.recyclerViewClass();

  /**
   * Scan the view tree of an activity.
   *
   * @param activity The activity to be scanned.
   * @return A map containing the view tree.
   * @throws IllegalArgumentException
   */
  public static Map<String, List> scanViewTree(final Activity activity) {
    if (activity == null ||
        activity.getWindow() == null ||
        activity.getWindow().getDecorView() == null) {
      throw new IllegalArgumentException("Can't scan view tree, activity or its window/view is " +
          "null.");
    }

    long startTime = System.currentTimeMillis();

    List<ViewRootData> viewRootDataList;
    try {
      viewRootDataList = ClassUtil.getRootViews(activity);
    } catch (Exception e) {
      return null;
    }

    if (viewRootDataList == null) {
      return null;
    }

    final List<Map> children = new ArrayList<>();

    for (ViewRootData viewRootData : viewRootDataList) {
      final List<String> viewControllerPath = LeanplumViewHelper
          .getActivityPath(activity, viewRootData.getIndex());
      Log.p("Scanning views of activity: ", viewControllerPath);
      children.add(scanPropertiesRecursive(viewRootData.getView(), viewControllerPath, null));
    }

    long endTime = System.currentTimeMillis();
    Log.p("Done scanning views of activity in " + (endTime - startTime) + "ms");

    Map<String, List> viewHierarchy = CollectionUtil.newHashMap("children", children);
    Log.p("ViewHierarchy:");
    Log.logLargeString(JsonConverter.toJson(viewHierarchy));
    return viewHierarchy;
  }

  /**
   * Scan the properties of a view recursive.
   *
   * @param view The view to be scanned.
   * @param activityPath The current activityPath.
   * @return A map containing the property description.
   */
  private static Map<String, Object> scanPropertiesRecursive(
      final View view, List<String> activityPath, Object scrollViewParent) {
    Log.p("Scanning properties of view: " + view.getClass().getName());
    Map<String, Object> viewHierarchy = new HashMap<>();

    // Set class type
    viewHierarchy.put("type", view.getClass().getName());

    // Set view controller path
    viewHierarchy.put("viewControllerPath", activityPath);

    // Add properties of current view
    viewHierarchy.putAll(LeanplumInterfaceProperty.getPropertiesForView(view));

    // swizzle view specific listeners
    LeanplumListenerSwizzler.swizzleViewListeners(view);

    // If parent was a listview or gridview
    if (scrollViewParent != null) {
      viewHierarchy.put("section", 0); // Hardcode section, as there is no sections by default.
      if (scrollViewParent instanceof ListView) {
        // Retrieve cells rowindex in the listview
        viewHierarchy.put("row", ((ListView) scrollViewParent).getPositionForView(view));
      } else if (scrollViewParent instanceof GridView) {
        // Retrieve cells rowindex in the GridView
        viewHierarchy.put("row", ((GridView) scrollViewParent).getPositionForView(view));
      } else if (recyclerViewClass != null &&
          scrollViewParent.getClass().isAssignableFrom(recyclerViewClass)) {
        // Retrieve cells rowindex in the RecyclerView
        int row = (int) ClassUtil.invokeMethod(scrollViewParent, "getChildAdapterPosition",
            CollectionUtil.<Class>newArrayList(View.class),
            CollectionUtil.newArrayList(view));
        viewHierarchy.put("row", row);
      } else if (scrollViewParent instanceof ViewPager) {
        // Retrieve cells rowindex in the viewPager
        viewHierarchy.put("row", ((ViewPager) scrollViewParent).getCurrentItem());
      }
      scrollViewParent = null; // clear scrollViewParent, so we only process current level of cells.
    }

    // Handle ListView or Gridview differently
    if (view instanceof ListView || view instanceof GridView ||
        (recyclerViewClass != null && view.getClass().isAssignableFrom(recyclerViewClass)) ||
        view instanceof ViewPager) {
      scrollViewParent = view;
    }

    if (getIncludeChildren(view)) {
      // Include children of view
      List<Map<String, Object>> subviews = getSubviewsOfView(view, activityPath, scrollViewParent);
      if (subviews != null && subviews.size() > 0) {
        viewHierarchy.put("children", subviews);
      }
    }

    return viewHierarchy;
  }

  /**
   * Returns whether or not the children of a view should be included in the view scanning.
   *
   * @param currentView The view to be checked.
   * @return true if the children should be included, otherwise false.
   */
  private static boolean getIncludeChildren(View currentView) {
    Class<?> viewClass = currentView.getClass();
    boolean includeChildren = true;
    for (Class<?> baseClass : terminalViewClasses) {
      if (baseClass.isAssignableFrom(viewClass)) {
        includeChildren = false;
        break;
      }
    }
    Log.p(((!includeChildren) ? "Not i" : "I") +
        "ncluding children of view: " + currentView.getClass().getName());
    return includeChildren;
  }

  /**
   * Retrieves the subviews of a given view.
   *
   * @param view The given view.
   * @param controllerPath The controllerpath.
   * @return The subviews of a view.
   */
  private static List<Map<String, Object>> getSubviewsOfView(
      View view, List<String> controllerPath, Object scrollViewParent) {
    if (!(view instanceof ViewGroup)) {
      return null;
    }
    List<Map<String, Object>> result = new ArrayList<>();
    ViewGroup group = (ViewGroup) view;
    Map<Class, Integer> nameIndexes = new HashMap<>();

    Log.p("Found " + group.getChildCount() + " subview(s)");

    for (int i = 0; i < group.getChildCount(); i++) {
      View subview = group.getChildAt(i);

      // Calculate index - counter for each identical class within this view hierarchy
      Integer classIndex = nameIndexes.get(subview.getClass());
      int index = (classIndex == null) ? 0 : classIndex + 1;
      nameIndexes.put(subview.getClass(), index);

      Map<String, Object> subviewMap =
          scanPropertiesRecursive(subview, controllerPath, scrollViewParent);
      subviewMap.put("index", index);
      result.add(subviewMap);
    }
    return result;
  }

  /**
   * Scans the given activity subviews for a list view.
   *
   * @param activity The activity to scan for.
   * @return The AbsListView object or null.
   */
  public static AbsListView getAbsListView(Activity activity) {
    return (AbsListView) getTargetViewClassRecursive(
        activity.getWindow().getDecorView(), AbsListView.class
    );
  }

  /**
   * Scans the given activity subviews for a recycler view.
   *
   * @param activity The activity to scan for.
   * @return The RecyclerView or null.
   */
  public static Object getRecyclerView(Activity activity) {
    Class<?> recyclerViewClass = ClassUtil.recyclerViewClass();
    if (recyclerViewClass != null) {
      return getTargetViewClassRecursive(
          activity.getWindow().getDecorView(), recyclerViewClass
      );
    } else {
      return null;
    }
  }

  /**
   * Retrieves the target view class recursively by scanning the views subviews.
   *
   * @param view The view to scan recursively.
   * @param targetClass The desired target class.
   * @return The target class or null.
   */
  private static View getTargetViewClassRecursive(View view, Class targetClass) {
    Log.p("Scanning view for " +
        targetClass.getClass().getSimpleName() + ": " + view.getClass().getName());
    ViewGroup viewGroup = ClassUtil.getViewGroup(view);
    if (viewGroup == null) {
      Log.p("Last child in tree reached: " + view.getClass().getName());
      return null;
    }
    View result = null;
    for (int i = 0; i < viewGroup.getChildCount(); i++) {
      View childView = viewGroup.getChildAt(i);
      if (targetClass.isInstance(childView)) {
        result = childView;
        Log.p(targetClass.getSimpleName() + " found: " + childView.getClass().getName());
      } else if (getIncludeChildren(childView)) {
        result = getTargetViewClassRecursive(childView, targetClass);
      }
      if (result != null) {
        break;
      }
    }
    return result;
  }
}

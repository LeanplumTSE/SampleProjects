// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewPager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;

import com.leanplum.LeanplumActivityHelper;
import com.leanplum.internal.ClassUtil;
import com.leanplum.internal.CollectionUtil;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

interface LeanplumOnTouchListener extends View.OnTouchListener {
}

interface LeanplumOnClickListener extends View.OnClickListener {
}

/**
 * Helper class to swizzle out the original listeners of a view class.
 *
 * @author Ben Marten
 */
public class LeanplumListenerSwizzler {
  private static final int SWIZZLE_DELAY = 100;

  private static final Map<View, Void> alreadySwizzledViewPager = new WeakHashMap<>();
  private static final Class<?> recyclerViewClass = ClassUtil.recyclerViewClass();
  private static Handler delayedUpdateHandler;
  private static Runnable delayedUpdateRunnable;

  /**
   * Swizzles the existing list adapter with a leanplum proxy adapter.
   *
   * @param activity The activity that holds the listview
   * @param listView The listview
   */
  @TargetApi(11)
  public static void swizzleListAdapter(Activity activity, AbsListView listView) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      ListAdapter existingListAdapter = listView.getAdapter();
      if (!(existingListAdapter instanceof LeanplumAdapter)) {
        listView.setAdapter(new LeanplumAdapter(activity, listView));
      }
    } else {
      Log.w("Unable to swizzle ListAdapter, API level is lower then 11.");
    }
  }

  /**
   * Swizzles the existing recyclerview adapter with a leanplum proxy adapter.
   *
   * @param activity The activity that holds the recyclerview
   * @param recyclerView The recyclerview
   */
  public static void swizzleRecyclerViewAdapter(Activity activity, Object recyclerView) {
    if (recyclerViewClass == null) {
      return;
    }

    Object existingAdapter = ClassUtil.invokeMethod(recyclerView, "getAdapter", null, null);
    if (existingAdapter != null &&
        !(existingAdapter.getClass().isAssignableFrom(LeanplumRecyclerViewAdapter.class))) {
      final Object proxyAdapter = new LeanplumRecyclerViewAdapter(activity, existingAdapter);
      ClassUtil.invokeMethod(recyclerView, "setAdapter",
          CollectionUtil.<Class>newArrayList(ClassUtil.recyclerViewAdapterClass()),
          CollectionUtil.newArrayList(proxyAdapter));
    }
  }

  /**
   * Swizzles the existing touch listener with a leanplum proxy listener.
   *
   * @param view The view that contains the touch listener.
   */
  public static void swizzleTouchListener(View view) {
    final View.OnTouchListener originalOnTouchListener = ClassUtil.getOnTouchListener(view);
    if (originalOnTouchListener instanceof LeanplumOnTouchListener) {
      // Do nothing, since we have already registered our custom listener.
      return;
    }
    Log.p("Swizzling touchListener for view: " + view.getClass().getName());
    view.setOnTouchListener(new LeanplumOnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent event) {
        try {
          if (event.getAction() == MotionEvent.ACTION_MOVE) {
            LeanplumInterfaceEditor.sendUpdateDelayed(LeanplumInterfaceEditor.UPDATE_DELAY_SCROLL);
          }
        } catch (Throwable t) {
          Util.handleException(t);
        }
        return originalOnTouchListener != null && originalOnTouchListener.onTouch(view, event);
      }
    });

  }

  /**
   * Swizzles the existing click listener with a leanplum proxy listener.
   *
   * @param view The view that contains the click listener.
   */
  public static void swizzleClickListener(View view) {
    final View.OnClickListener originalOnClickListener = ClassUtil.getOnClickListener(view);
    if (originalOnClickListener == null ||
        originalOnClickListener instanceof LeanplumOnClickListener) {
      // If no existing click listener found, do nothing, since we have already registered our
      // custom listener.
      return;
    }
    Log.p("Swizzling clickListener for view: " + view.getClass().getName());
    view.setOnClickListener(new LeanplumOnClickListener() {
      @Override
      public void onClick(View view) {
        try {
          LeanplumInterfaceEditor.sendUpdateDelayed(LeanplumInterfaceEditor.UPDATE_DELAY_SCROLL);
          LeanplumViewManager.applyInterfaceAndEventUpdateRulesDelayed(
              LeanplumActivityHelper.getCurrentActivity());
        } catch (Throwable t) {
          Util.handleException(t);
        } finally {
          originalOnClickListener.onClick(view);
        }
      }
    });
  }

  /**
   * Swizzles the existing onpagechange listener with a leanplum proxy listener.
   *
   * @param view The viw that contains the onpagechange listener.
   */
  public static void addOnPageChangeListener(ViewPager view) {
    if (!alreadySwizzledViewPager.containsKey(view)) {
      Log.p("Swizzling onPageChangeListener for view: " + view.getClass().getName());
      view.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
          try {
            LeanplumViewManager
                .applyInterfaceAndEventUpdateRules(LeanplumActivityHelper.getCurrentActivity());
            LeanplumInterfaceEditor.sendUpdateDelayed(LeanplumInterfaceEditor.UPDATE_DELAY_SCROLL);
          } catch (Throwable t) {
            Util.handleException(t);
          }
        }

        @Override
        public void onPageSelected(int position) {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
      });
      alreadySwizzledViewPager.put(view, null);
    }
  }

  /**
   * Swizzles the existing onGlobalChangeListener with a leanplum proxy listener.
   *
   * @param activity The activity to change the listener from.
   */
  public static void addOnGlobalChangeListener(final Activity activity) {
    Boolean alreadyAdded = false;

    ViewTreeObserver viewTreeObserver = activity.getWindow().getDecorView().getViewTreeObserver();
    Object onGlobalLayoutListeners;
    try {
      onGlobalLayoutListeners = ClassUtil.getFieldValue("mOnGlobalLayoutListeners",
          viewTreeObserver);
    } catch (Exception e) {
      Log.e("Can not get onGlobalLayoutListeners field value.");
      return;
    }
    if (onGlobalLayoutListeners != null) {
      List onGlobalLayoutListenersList;
      try {
        onGlobalLayoutListenersList = (List) ClassUtil
            .getFieldValue("mData", onGlobalLayoutListeners);
      } catch (Exception e) {
        Log.e("Can not add onGlobalChangeListener for activity: ", activity);
        return;
      }

      if (onGlobalLayoutListenersList != null) {
        for (Object o : onGlobalLayoutListenersList) {
          if (o instanceof LeanplumOnGlobalLayoutListener) {
            alreadyAdded = true;
            break;
          }
        }
      }
    }
    if (!alreadyAdded) {
      activity.getWindow().getDecorView().getViewTreeObserver()
          .addOnGlobalLayoutListener(new LeanplumOnGlobalLayoutListener(activity));
    }
  }

  /**
   * Send an update with given delay of the UI to the LP server.
   */
  public static void swizzleScrollViewListenersDelayed(final Activity activity) {
    if (delayedUpdateHandler == null) {
      delayedUpdateHandler = new Handler(Looper.getMainLooper());
    }

    if (delayedUpdateRunnable != null) {
      Log.p("Canceled existing scheduled update thread.");
      delayedUpdateHandler.removeCallbacks(delayedUpdateRunnable);
    }

    try {
      // Post a new update runnable.
      delayedUpdateRunnable = new Runnable() {
        @Override
        public void run() {
          try {
            swizzleScrollViewListeners(activity);
          } catch (Throwable t) {
            Util.handleException(t);
          }
        }
      };
      boolean success = delayedUpdateHandler.postDelayed(delayedUpdateRunnable, SWIZZLE_DELAY);
      if (!success) {
        Log.p("Could not post update delayed runnable.");
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Swizzle the scroll view listeners of an activity to apply changes to the UI.
   *
   * @param activity The target activity.
   */
  public static void swizzleScrollViewListeners(Activity activity) {
    AbsListView absListView = LeanplumHierarchyScanner.getAbsListView(activity);

    Object recyclerView = LeanplumHierarchyScanner.getRecyclerView(activity);
    if (absListView != null) {
      // Register TableView listener, update will be sent from listener on cell create.
      LeanplumListenerSwizzler.swizzleListAdapter(activity, absListView);
    } else if (recyclerView != null) {
      // Register TableView listener, update will be sent from listener on cell create.
      LeanplumListenerSwizzler.swizzleRecyclerViewAdapter(activity, recyclerView);
    } else {
      // Send UI update.
      LeanplumInterfaceEditor.
          sendUpdateDelayed(LeanplumInterfaceEditor.UPDATE_DELAY_ACTIVITY_CHANGE);
    }
  }

  /**
   * Takes care of swizzling the respective listeners for a specific view class to send updates to
   * the visual editor website.
   *
   * @param view a subview of an activity.
   */
  public static void swizzleViewListeners(View view) {
    // Register scroll listener, to send updates on scroll
    if (view instanceof ScrollView || view instanceof WebView ||
        (view instanceof ScrollingView && (recyclerViewClass != null &&
            !(view.getClass().isAssignableFrom(recyclerViewClass))) &&
            !(view instanceof ListView))) {
      LeanplumListenerSwizzler.swizzleTouchListener(view);
    }

    LeanplumListenerSwizzler.swizzleClickListener(view);
    if (view instanceof ViewPager) {
      LeanplumListenerSwizzler.addOnPageChangeListener((ViewPager) view);
    }
  }
}

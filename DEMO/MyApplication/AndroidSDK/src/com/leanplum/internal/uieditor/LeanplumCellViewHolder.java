// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.leanplum.internal.ClassUtil;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

/**
 * Cell view holder class that contains information about a view.
 */
public class LeanplumCellViewHolder {
  private static final Class<?> recyclerViewClass = ClassUtil.recyclerViewClass();

  private WeakReference<View> cellView;
  private WeakReference<View> targetView;
  private WeakReference<AbsListView> absListView;
  private WeakReference<Object> recyclerView;
  private WeakReference<ViewPager> viewPager;

  public LeanplumCellViewHolder(ViewGroup rootViewGroup, List<Map<String, Object>> rulePath) {
    Object firstTargetView =
        LeanplumViewHelper.getFirstTargetViewForViewPath(rootViewGroup, rulePath);

    if (firstTargetView instanceof ViewPager) {
      viewPager = new WeakReference<>((ViewPager) firstTargetView);
    } else if (firstTargetView instanceof AbsListView) {
      absListView = new WeakReference<>((AbsListView) firstTargetView);
    } else if (recyclerViewClass != null && firstTargetView != null &&
        firstTargetView.getClass().isAssignableFrom(recyclerViewClass)) {
      recyclerView = new WeakReference<>(firstTargetView);
    }

  }

  // Getter & Setter
  public AbsListView getAbsListView() {
    if (absListView == null) {
      return null;
    }
    return absListView.get();
  }

  public Object getRecyclerView() {
    if (recyclerView == null) {
      return null;
    }
    return recyclerView.get();
  }

  public ViewPager getViewPager() {
    if (viewPager == null) {
      return null;
    }
    return viewPager.get();
  }

  public View getTargetView() {
    if (targetView == null) {
      return null;
    }
    return targetView.get();
  }

  public void setTargetView(View targetView) {
    this.targetView = new WeakReference<>(targetView);
  }

  public View getCellView() {
    if (cellView == null) {
      return null;
    }
    return cellView.get();
  }

  public void setCellView(View cellView) {
    this.cellView = new WeakReference<>(cellView);
  }
}

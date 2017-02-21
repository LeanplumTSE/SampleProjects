// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor.model;

import android.graphics.Rect;
import android.view.View;
import android.view.WindowManager;

/**
 * Holds information about a view.
 *
 * @author Ben Marten
 */
public class ViewRootData {
  private final Number index;
  private final View view;
  private final Rect rect;
  private final WindowManager.LayoutParams mLayoutParams;

  /**
   * Creates a new viewRootData object holding information about a view.
   *
   * @param index The index of the view within its parent.
   * @param view The actual view.
   * @param winFrame The frame of the view.
   * @param layoutParams The layout parameters.
   */
  public ViewRootData(Number index, View view, Rect winFrame,
      WindowManager.LayoutParams layoutParams) {
    this.index = index;
    this.view = view;
    this.rect = winFrame;
    this.mLayoutParams = layoutParams;
  }

  public Number getIndex() {
    return index;
  }

  public View getView() {
    return view;
  }

  public Rect getWinFrame() {
    return rect;
  }

  public WindowManager.LayoutParams getLayoutParams() {
    return mLayoutParams;
  }
}

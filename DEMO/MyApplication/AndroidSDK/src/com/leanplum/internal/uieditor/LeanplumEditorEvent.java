// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.view.View;

import com.leanplum.Leanplum;
import com.leanplum.internal.ClassUtil;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

import java.lang.ref.WeakReference;

/**
 * Editor event class represents an event created by the visual editor.
 *
 * @author Ben Marten
 */
public class LeanplumEditorEvent {
  private final String eventName;
  private WeakReference<View> view;
  private WeakReference<View.OnClickListener> originalOnClickListener;

  /**
   * Creates a new new editor event.
   *
   * @param eventName The name of the event as defined in the visual editor.
   * @param view The name of the view
   */
  public LeanplumEditorEvent(String eventName, View view) {
    this.eventName = eventName;
    this.view = new WeakReference<>(view);
    addControlEventToView();
  }

  /**
   * Send the tracking event to the leanplum backend server.
   */
  private void sendLeanplumEvent() {
    Log.p("Tracking Editor Event: " + eventName);
    Leanplum.track(this.eventName);
  }

  /**
   * Sets the onclickListener with provided view which enables event tracking.
   */
  private void addControlEventToView() {
    try {
      final View view = getView();
      if (view != null) {
        originalOnClickListener = new WeakReference<>(ClassUtil.getOnClickListener(view));
        final LeanplumEditorEvent currentEditorEvent = this;
        view.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                try {
                  currentEditorEvent.sendLeanplumEvent();
                  if (originalOnClickListener.get() != null) {
                    originalOnClickListener.get().onClick(view);
                  }
                } catch (Throwable t) {
                  Util.handleException(t);
                }
              }
            }
        );
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Restores the original OnClickListener and removes references to the view, so this Event can be
   * garbage collected.
   */
  public void remove() {
    View view = this.getView();
    if (view != null) {
      view.setOnClickListener(originalOnClickListener.get());
    }
    this.view = null;
  }

  /**
   * Returns the name of the tracked event.
   *
   * @return The name of the event.
   */
  public String getEventName() {
    return eventName;
  }

  /**
   * Returns the view that is being tracked. Might be null.
   *
   * @return The view object.
   */
  public View getView() {
    return view.get();
  }
}

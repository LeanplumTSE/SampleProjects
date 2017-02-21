// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import com.leanplum.LeanplumActivityHelper;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ListAdapter class that acts as proxy between the original ListAdapter and the listView.
 *
 * @author Ben Marten
 */
class LeanplumAdapter extends BaseAdapter {
  private final Activity activity;
  private final AbsListView listView;
  private ListAdapter originalListAdapter;

  /**
   * Constructor setting the list view & original adapter.
   *
   * @param listView The actual listView.
   */
  LeanplumAdapter(Activity activity, AbsListView listView) {
    this.activity = activity;
    this.listView = listView;

    // If list view has already an adapter assigned, save reference, if not we'll be notified in
    // unregisterDataSetObserver
    if (this.listView.getAdapter() != null) {
      originalListAdapter = this.listView.getAdapter();
    }
  }

  @Override
  @TargetApi(11)
  public void unregisterDataSetObserver(DataSetObserver observer) {
    super.unregisterDataSetObserver(observer);
    if (LeanplumActivityHelper.getCurrentActivity() == null ||
        !LeanplumActivityHelper.getCurrentActivity().equals(activity)) {
      return;
    }

    Log.p("LeanplumAdapter just got unregistered from the listView!");
    // TODO: Find a better solution, than just a simple timeout...
    try {
      (new Timer()).schedule(new TimerTask() {
        @Override
        public void run() {
          Runnable runnable = new Runnable() {
            @Override
            public void run() {
              try {
                ListAdapter adapter = listView.getAdapter();
                if (adapter != null && !adapter.equals(LeanplumAdapter.this)) {
                  Log.p("Reswizzling newly assigned adapter!");
                  LeanplumAdapter.this.setOriginalListAdapter(adapter);
                  listView.setAdapter(LeanplumAdapter.this);
                }
              } catch (Throwable t) {
                Util.handleException(t);
              }
            }
          };
          new Handler(Looper.getMainLooper()).post(runnable);
        }
      }, 100);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  @Override
  public int getCount() {
    if (originalListAdapter == null) {
      return 0;
    }
    return originalListAdapter.getCount();
  }

  @Override
  public Object getItem(int position) {
    if (originalListAdapter == null) {
      return null;
    }
    return originalListAdapter.getItem(position);
  }

  @Override
  public long getItemId(int position) {
    if (originalListAdapter == null) {
      return -1;
    }
    return originalListAdapter.getItemId(position);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    if (originalListAdapter == null) {
      return null;
    }
    View cell;
    try {
      List<Map<String, Object>> cellReverseRule;
      LeanplumViewManager.setIsUpdateInProgress(true);
      if (convertView != null) {
        //noinspection AccessStaticViaInstance
        cellReverseRule = LeanplumEditorModel.getListViewCellManager()
            .popCellReverseRules(activity.getClass().getName(), convertView);
        // Apply reverse rules, if they exists, and then new updates.
        if (cellReverseRule != null) {
          LeanplumViewManager.applyListViewRulesInCell(convertView, position, true,
              cellReverseRule);
        }
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }

    cell = originalListAdapter.getView(position, convertView, parent);

    try {
      LeanplumViewManager.applyListViewRulesInCell(cell, position, false);
      LeanplumViewManager.setIsUpdateInProgress(false);

      // Need to send an update again on cell drawing, delay send, will only result in one request.
      LeanplumInterfaceEditor.sendUpdateDelayed(LeanplumInterfaceEditor.UPDATE_DELAY_SCROLL);
    } catch (Throwable t) {
      Util.handleException(t);
    }

    return cell;
  }

  /**
   * Saves a reference of the original ListAdapter.
   *
   * @param mOriginalListAdapter The original list adapter.
   */
  private void setOriginalListAdapter(ListAdapter mOriginalListAdapter) {
    this.originalListAdapter = mOriginalListAdapter;
  }
}

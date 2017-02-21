// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

import java.util.List;
import java.util.Map;

/**
 * RecyclerViewAdapter class that acts as proxy between the original recyclerView and the listView.
 *
 * @author Ben Marten
 */
public class LeanplumRecyclerViewAdapter extends RecyclerView.Adapter {
  private final Activity activity;
  private final RecyclerView.Adapter originalAdapter;

  /**
   * RecyclerView Adapter
   *
   * @param activity the activity containing the listview
   * @param originalAdapter the original adapter
   */
  public LeanplumRecyclerViewAdapter(Activity activity, Object originalAdapter) {
    this.activity = activity;
    this.originalAdapter = (RecyclerView.Adapter) originalAdapter;
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    Log.p("Creating view holder for viewType: " + viewType);
    return originalAdapter.onCreateViewHolder(parent, viewType);
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    try {
      List<Map<String, Object>> cellReverseRule;
      LeanplumViewManager.setIsUpdateInProgress(true);
      if (holder != null) {
        Log.p("Cell being reused.");
        //noinspection AccessStaticViaInstance
        cellReverseRule = LeanplumEditorModel.getListViewCellManager()
            .popCellReverseRules(activity.getClass().getName(), holder.itemView);
        // Apply reverse rules, if they exists, and then new updates.
        if (cellReverseRule != null) {
          LeanplumViewManager.applyListViewRulesInCell(holder.itemView, position, true,
              cellReverseRule);
        }
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }

    //noinspection unchecked
    originalAdapter.onBindViewHolder(holder, position);

    try {
      if (holder != null) {
        LeanplumViewManager.applyListViewRulesInCell(holder.itemView, position, false);
      }
      LeanplumViewManager.setIsUpdateInProgress(false);

      // Need to send an update again on cell drawing, delay send, will only result in one request.
      LeanplumInterfaceEditor.sendUpdateDelayed(LeanplumInterfaceEditor.UPDATE_DELAY_SCROLL);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  @Override
  public int getItemCount() {
    return originalAdapter.getItemCount();
  }
}

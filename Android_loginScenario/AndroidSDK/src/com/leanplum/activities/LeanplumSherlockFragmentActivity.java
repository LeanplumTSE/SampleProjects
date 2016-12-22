package com.leanplum.activities;

// Copyright 2013, Leanplum, Inc.

import android.content.res.Resources;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;

public class LeanplumSherlockFragmentActivity extends SherlockFragmentActivity {
  private LeanplumActivityHelper helper;

  private LeanplumActivityHelper getHelper() {
    if (helper == null) {
      helper = new LeanplumActivityHelper(this);
    }
    return helper;
  }

  @Override
  protected void onPause() {
    super.onPause();
    getHelper().onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    getHelper().onStop();
  }

  @Override
  protected void onResume() {
    super.onResume();
    getHelper().onResume();
  }

  @Override
  public Resources getResources() {
    if (Leanplum.isTestModeEnabled() || !Leanplum.isResourceSyncingEnabled()) {
      return super.getResources();
    }
    return getHelper().getLeanplumResources(super.getResources());
  }

  @Override
  public void setContentView(final int layoutResID) {
    if (Leanplum.isTestModeEnabled() || !Leanplum.isResourceSyncingEnabled()) {
      super.setContentView(layoutResID);
      return;
    }
    getHelper().setContentView(layoutResID);
  }
}
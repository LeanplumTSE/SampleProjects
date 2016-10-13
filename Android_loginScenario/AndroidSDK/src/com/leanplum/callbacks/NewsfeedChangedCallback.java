// Copyright 2015, Leanplum, Inc. All rights reserved.

package com.leanplum.callbacks;

/**
 * Newsfeed changed callback.
 *
 * @author Aleksandar Gyorev
 */
public abstract class NewsfeedChangedCallback implements Runnable {
  public void run() {
    this.newsfeedChanged();
  }

  public abstract void newsfeedChanged();
}

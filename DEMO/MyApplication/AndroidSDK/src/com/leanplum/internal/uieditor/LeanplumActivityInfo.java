// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

/**
 * Holds activity information.
 *
 * @author Ben Marten
 */
public class LeanplumActivityInfo {
  private final String name;
  private final int activityIndex;

  /**
   * Creates a new Activity Info class.
   *
   * @param name The name of the activity.
   * @param activityIndex The index of the activity in the current activity stack.
   */
  public LeanplumActivityInfo(String name, int activityIndex) {
    this.name = name;
    this.activityIndex = activityIndex;
  }

  /**
   * @return The name of the activity.
   */
  public String getName() {
    return name;
  }

  /**
   * @return The index of the activity in the current activity stack.
   */
  public int getActivityIndex() {
    return activityIndex;
  }
}

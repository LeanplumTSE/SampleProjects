// Copyright 2014, Leanplum, Inc.

package com.leanplum.customtemplates;

import android.app.Activity;
import android.content.Context;

import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.callbacks.ActionCallback;
import com.leanplum.callbacks.VariablesChangedCallback;

/**
 * Registers a Leanplum action that displays a custom center popup dialog.
 * @author Andrew First
 */
public class CenterPopupCustom extends BaseMessageDialog {
  private static final String NAME = "Center Popup Custom";

  public CenterPopupCustom(Activity activity, CenterPopupCustomOptions options) {
    super(activity, false, options, null);
    this.options = options;
  }

  public static void register(Context currentContext) {
    Leanplum.defineAction(NAME, Leanplum.ACTION_KIND_MESSAGE | Leanplum.ACTION_KIND_ACTION,
            CenterPopupCustomOptions.toArgs(currentContext), new ActionCallback() {
          @Override
          public boolean onResponse(final ActionContext context) {
            Leanplum.addOnceVariablesChangedAndNoDownloadsPendingHandler(
                new VariablesChangedCallback() {
              @Override
              public void variablesChanged() {
                LeanplumActivityHelper.queueActionUponActive(new VariablesChangedCallback() {
                  @Override
                  public void variablesChanged() {
                    CenterPopupCustom popup = new CenterPopupCustom(
                        LeanplumActivityHelper.getCurrentActivity(),
                        new CenterPopupCustomOptions(context));
                    popup.show();
                  }
                });
              }
            });
            return true;
          }
        });
  }
}
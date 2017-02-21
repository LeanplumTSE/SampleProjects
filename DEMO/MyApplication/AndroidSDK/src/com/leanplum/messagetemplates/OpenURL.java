// Copyright 2014, Leanplum, Inc.

package com.leanplum.messagetemplates;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.callbacks.ActionCallback;
import com.leanplum.callbacks.PostponableAction;
import com.leanplum.internal.Log;
import com.leanplum.messagetemplates.MessageTemplates.Args;
import com.leanplum.messagetemplates.MessageTemplates.Values;

/**
 * Registers a Leanplum action that opens a particular URL. If the URL cannot be handled by the
 * system URL handler, you can add your own action responder using {@link Leanplum#onAction} that
 * handles the URL how you want.
 *
 * @author Andrew First
 */
class OpenURL {
  private static final String NAME = "Open URL";

  public static void register() {
    Leanplum.defineAction(NAME, Leanplum.ACTION_KIND_ACTION,
        new ActionArgs().with(Args.URL, Values.DEFAULT_URL), new ActionCallback() {
          @Override
          public boolean onResponse(ActionContext context) {
            String url = context.stringNamed(Args.URL);
            final Intent uriIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            // Calling startActivity() from outside of an Activity context requires the
            // FLAG_ACTIVITY_NEW_TASK flag.
            if (!(Leanplum.getContext() instanceof Activity)) {
              uriIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            try {
              if (Leanplum.getContext() != null) {
                LeanplumActivityHelper.queueActionUponActive(
                    new PostponableAction() {
                      @Override
                      public void run() {
                        Leanplum.getContext().startActivity(uriIntent);
                      }
                    });
                return true;
              } else {
                return false;
              }
            } catch (ActivityNotFoundException e) {
              Log.e("Unable to handle URL " + url);
              return false;
            }
          }
        });
  }
}

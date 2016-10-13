// Copyright 2013, Leanplum, Inc.

package com.leanplum;

import android.util.Log;

import com.leanplum.Leanplum.OsHandler;
import com.leanplum.callbacks.StartCallback;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

class Registration {
  public static void registerDevice(String email, final StartCallback callback) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Constants.Params.EMAIL, email);
    LeanplumRequest request = LeanplumRequest.post(Constants.Methods.REGISTER_FOR_DEVELOPMENT, params);
    request.onResponse(new LeanplumRequest.ResponseCallback() {
      @Override
      public void response(final JSONObject response) {
        OsHandler.getInstance().post(new Runnable() {
          @Override
          public void run() {
            try {
              JSONObject registerResponse = LeanplumRequest.getLastResponse(response);
              boolean isSuccess = LeanplumRequest.isResponseSuccess(registerResponse);
              if (isSuccess) {
                if (callback != null) {
                  callback.onResponse(true);
                }
              } else {
                Log.e("Leanplum", LeanplumRequest.getResponseError(registerResponse));
                if (callback != null) {
                  callback.onResponse(false);
                }
              }
            } catch (Throwable t) {
              Util.handleException(t);
            }
          }
        });
      }
    });
    request.onError(new LeanplumRequest.ErrorCallback() {
      @Override
      public void error(final Exception e) {
        OsHandler.getInstance().post(new Runnable() {
          @Override
          public void run() {
            if (callback != null) {
              callback.onResponse(false);
            }
          }
        });
      }
    });
    request.sendIfConnected();
  }
}

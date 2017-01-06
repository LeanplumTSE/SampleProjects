package com.android_playground;

import android.os.Bundle;
import android.util.Log;

import com.leanplum.LeanplumPushListenerService;

/**
 * Created by fede on 1/6/17.
 */

public class CustomPushReceiverService extends LeanplumPushListenerService {

    @Override
    public void onMessageReceived(String var, Bundle notificationPayload) {

        if (notificationPayload.containsKey("lp_message")){
            Log.d(CustomPushReceiverService.class.getSimpleName(), "PRE");
            super.onMessageReceived(var, notificationPayload);
            Log.d(CustomPushReceiverService.class.getSimpleName(), "POST");
            Log.i("### ", "1 received");
        } else {
            Log.i("### ", "not a leanplum notification" );
        }

    }
}

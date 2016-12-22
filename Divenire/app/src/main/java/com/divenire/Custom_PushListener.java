package com.divenire;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.leanplum.LeanplumPushListenerService;
import com.leanplum.LeanplumPushService;

/**
 * Created by fede on 11/14/16.
 */

public class Custom_PushListener extends LeanplumPushListenerService {

    public static String payloadText;

//    @Override
    public void onMessageReceived(String var, Bundle notificationPayload) {

        if (ApplicationClass.isActive) {

            super.onMessageReceived(var, notificationPayload);
            Log.i("### " , "isActive true");

            payloadText = notificationPayload.getString("lp_message");

            Intent intnt = new Intent("customID");
//            intnt.putExtra("message", geofenceTransitionDetails);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intnt);

        } else {
            super.onMessageReceived(var, notificationPayload);
            Log.i("### " , "isActive false");
            Log.i("### ", "1 received");
        }

    }
}

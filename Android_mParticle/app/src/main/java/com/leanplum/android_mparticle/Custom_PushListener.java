package com.leanplum.android_mparticle;

import android.app.Service;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.leanplum.LeanplumPushListenerService;
import com.leanplum.LeanplumPushNotificationCustomizer;
import com.leanplum.LeanplumPushService;

/**
 * Created by fede on 2/15/18.
 */

public class Custom_PushListener extends LeanplumPushListenerService {

    boolean isLeanPlumPushNotification;

    // Optional:
    // Customizing the Push Notification placing the Customizer inside onCreate()
    @Override
    public void onCreate(){
        super.onCreate();

        LeanplumPushService.setCustomizer(new LeanplumPushNotificationCustomizer() {
            @Override
            public void customize(NotificationCompat.Builder builder, Bundle notificationPayload) {
//              Setting a custom smallIcon included in the Drawable folder
                builder.setSmallIcon(R.drawable.atest);
            }
        });
    }

    @Override
    public void onMessageReceived(String var, Bundle notificationPayload) {

        // This would ensure the Leanplum code is executed once a Push Notification message is received
//        super.onMessageReceived(var, notificationPayload);

        // Following code would be executed other than the Leanplum default behavior
        Log.i("### ", "Push received - checking if this is from Leanplum or not");
        Log.i("### ", "Here is the Notification Payload: " + notificationPayload.toString());

        // Checking if is a Leanplum Notification
        // If the "lp_version" string is not null, is a Leanplum notification
        isLeanPlumPushNotification = (notificationPayload.getString("lp_version") != null);

        if (isLeanPlumPushNotification) {
            Log.i("##### ", "LP notification!");
            // Code to be executed in case of a Leanplum Notification
//            Log.i("### ", "Here is the Notification Payload: " + notificationPayload.toString());
            // This code is executed when the Leanplum Notification is received.
            super.onMessageReceived(var, notificationPayload);

            try {
                // With the following the Advanced Data can be retrieved from the Push Notification
                // Be sure the Variable name match - in this sample I'm assuming to set a String variable in the Advanced Data on Dashboard
                String dataString = notificationPayload.getString("String_name");
                // Printing to console the String value
                Log.i("#### ", dataString);
            } catch (NullPointerException e) {
                Log.i("#### ", "No dataString is being specified");
            }

        }
        else {
            Log.i("##### ", "Not a LP notification");
//          // Code to be executed in case of a non-Leanplum Notification
        }

    }
}
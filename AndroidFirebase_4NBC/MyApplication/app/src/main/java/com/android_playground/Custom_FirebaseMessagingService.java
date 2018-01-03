package com.android_playground;
import android.app.Notification;
import android.app.NotificationManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.google.firebase.messaging.RemoteMessage;
import com.leanplum.LeanplumPushFirebaseMessagingService;
import com.leanplum.LeanplumPushNotificationCustomizer;
import com.leanplum.LeanplumPushService;

import java.io.EOFException;

/**
 * Created by fede on 11/22/17.
 */

public class Custom_FirebaseMessagingService extends LeanplumPushFirebaseMessagingService {

    String ExtraDataString;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.i("### ", "message received");

        // Check if message contains a notification payload.
//        if ((remoteMessage.getData()).containsKey("stringa")) {
////            Log.i("### ", "Message Notification Body: " + remoteMessage.getNotification().getBody());
//            Log.i("### " , remoteMessage.getData().toString());
//        } else {
//            Log.i("###", "Message does not contain any payload");
//        }


//        LeanplumPushService.setCustomizer(new LeanplumPushNotificationCustomizer() {
//            @Override
//            public void customize(NotificationCompat.Builder builder, Bundle notificationPayload) {
//                Log.i("#### " , "Icon customized from Firebase");
//                // Setting a custom smallIcon included in the Drawable folder
//                builder.setSmallIcon(R.drawable.androidbnw);
//            }
//        });

//        try {
//            ExtraDataString = remoteMessage.getData().get("string1");
//
//            LeanplumPushService.setCustomizer(new LeanplumPushNotificationCustomizer() {
//                @Override
//                public void customize(NotificationCompat.Builder builder, Bundle notificationPayload) {
//                    Log.i("#### " , "notification customized");
//                    // Setting a custom smallIcon included in the Drawable folder
//                    Log.i("####", ExtraDataString);
////                    builder.setSmallIcon(R.drawable.androidbnw);
//
//                    builder.setSmallIcon(R.drawable.catbnw);
//
//                }
//            });
//
//        } catch (Throwable e){
//            LeanplumPushService.setCustomizer(new LeanplumPushNotificationCustomizer() {
//                @Override
//                public void customize(NotificationCompat.Builder builder, Bundle notificationPayload) {
//                    Log.i("#### " , "notification customized");
//                    // Setting a custom smallIcon included in the Drawable folder
//
////                    builder.setSmallIcon(R.drawable.androidbnw);
//
//                    builder.setSmallIcon(R.drawable.catbnw);
//
//                }
//            });
//        }

    }
}

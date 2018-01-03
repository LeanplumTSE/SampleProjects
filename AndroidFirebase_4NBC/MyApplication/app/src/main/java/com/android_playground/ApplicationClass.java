package com.android_playground;
import android.app.Application;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumDeviceIdMode;
import com.leanplum.LeanplumPushNotificationCustomizer;
import com.leanplum.LeanplumPushService;
import com.leanplum.annotations.Parser;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.StartCallback;
import com.leanplum.callbacks.VariablesChangedCallback;
import static com.android_playground.Credentials.AppID;
import static com.android_playground.Credentials.DevKey;
import static com.android_playground.Credentials.ProdKey;


public class ApplicationClass extends Application {


    @Override
    public void onCreate() {
        super.onCreate();

        Leanplum.setApplicationContext(this);
        LeanplumActivityHelper.enableLifecycleCallbacks(this);

        LeanplumActivityHelper.deferMessagesForActivities(SplashscreenActivity.class);

        Parser.parseVariablesForClasses(GlobalVariables.class, MainActivity.class, LoggedInActivity.class);

        if (BuildConfig.DEBUG) {
//                Leanplum.enableVerboseLoggingInDevelopmentMode();
                Leanplum.setAppIdForDevelopmentMode(AppID, DevKey);
        } else {
            Leanplum.setAppIdForProductionMode(AppID, ProdKey);
        }

//         Leanplum Customizer being used to change the App notification icon
        LeanplumPushService.setCustomizer(new LeanplumPushNotificationCustomizer() {
            @Override
            public void customize(NotificationCompat.Builder builder, Bundle notificationPayload) {
                Log.i("#### " , "Icon customized");
                // Setting a custom smallIcon included in the Drawable folder
                builder.setSmallIcon(R.drawable.androidbnw);
            }
        });
        // End Customizer

        // Firebase Push Notification Initialization
//        SenderID: 709867216442
//        Server key
//        AAAApUdiHjo:APA91bEG6GFKWoW7XRZe93PoooYkIL7aLjOKrq2uNcjzdzkUO0EWpsAy21Lqg-mNniNP_nFIAXA_bmUKNdHdQtHvHyq6FSC_5HHbRXUBPadLMsm0xDex1DqV-cWX0oazo-4DQgI3aptP
//        Legacy server key help_outline
//        AIzaSyA7n40b_4AuJNXfFZgxj2Cl65Bfjur5UuQ

        LeanplumPushService.enableFirebase();

        // End Firebase


        Leanplum.addVariablesChangedHandler(new VariablesChangedCallback() {
            @Override
            public void variablesChanged() {
                Log.i("### variants: ", String.valueOf(Leanplum.variants()));
            }
        });

        Leanplum.start(this);

    }
}
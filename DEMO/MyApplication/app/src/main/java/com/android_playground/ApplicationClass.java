package com.android_playground;

import android.app.Application;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumPushService;
import com.leanplum.annotations.Parser;
import com.leanplum.callbacks.StartCallback;
import com.leanplum.callbacks.VariablesChangedCallback;

import static com.android_playground.Credentials.AppID;
import static com.android_playground.Credentials.DevKey;
import static com.android_playground.Credentials.ProdKey;
import static com.android_playground.Credentials.SenderID;


public class ApplicationClass extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Leanplum.setApplicationContext(this);
        LeanplumActivityHelper.enableLifecycleCallbacks(this);

        LeanplumActivityHelper.deferMessagesForActivities(SplashscreenActivity.class, MainActivity.class);

        Parser.parseVariablesForClasses(GlobalVariables.class);


        if (BuildConfig.DEBUG) {
            //    Leanplum.enableVerboseLoggingInDevelopmentMode();
            Leanplum.setAppIdForDevelopmentMode(AppID, DevKey);
        } else {
            Leanplum.setAppIdForProductionMode(AppID, ProdKey);
        }

        // Registering for Push with Leanplum
        // Here is where the SenderID is passed. In this case I'm using the Leanplum bundle SenderID,
        // no need in this case to specify any specific Google API key in the Settings in the Leanplum Dashboard.
//        LeanplumPushService.setGcmSenderId(SenderID);

//        LeanplumPushService.setGcmSenderId("709867216442"); // Google API key: AIzaSyA7n40b_4AuJNXfFZgxj2Cl65Bfjur5UuQ

        // However, a specific SenderID (or SenderIDs) can be passed for the registration.
            // The SenderID correspond to the Google Cloud Porject number (is a 12 digits number) and needs to be passed as a string.
            // For example:
    //        LeanplumPushService.setGcmSenderId("123456789012");
        // In this case, the Google Cloud Project specific API key needs to be inserted in the Google API key field in the Settings in the Leanplum Dashboard.

        // If using multiple Push services with different SenderIDs, they need to be all passed also to Leanplum, using the following, for example:
        // LeanplumPushService.setGcmSenderIds(LeanplumPushService.LEANPLUM_SENDER_ID, "123456789012", "some other SenderID in string format...");


//    Leanplum.trackAllAppScreens();


        // Firebase!
//        LeanplumPushService.enableFirebase();

        Leanplum.start(this);
    }
}
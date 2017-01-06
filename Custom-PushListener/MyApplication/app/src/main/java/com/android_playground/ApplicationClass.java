package com.android_playground;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumPushRegistrationService;
import com.leanplum.LeanplumPushService;
import com.leanplum.annotations.Parser;
import com.leanplum.callbacks.StartCallback;

/**
 * Created by fede on 12/7/16.
 */

public class ApplicationClass extends Application {


    public static boolean useTest = false;

    private static String test_AppID = "app_VX99FQmTngx3HQG1pwKeI36MGPlFZayfiW2xhhNAclQ";
    private static String test_devKey = "dev_8favpED4MXyGLj5d61DyGvQwlA2uA1E7YD684estApY";
    private static String test_prodKey =  "prod_VIroylLak7tYkWub7NgVTJcJvHpr9AAQYXDtfmDn1W4";

    private static String store_AppID = "app_CWa0CcxPpZD2QaR4IT7gQihLUyCJh6FF4I2lLBQgHok";
    private static String store_devKey = "dev_v4n2fDHDzAEV79DygT79zyc5zAsDquumlVFfcLMAjAY";
    private static String store_prodKey = "prod_oiRyVYqodd6d0tobY7qFeXidZJamhEUGQDfvASf6614";


    @Override
    public void onCreate(){
        super.onCreate();

        double randNumber = Math.random() *100;
        int randomInt = (int)randNumber;
        Log.i("### ", String.valueOf(randomInt));

        Leanplum.setApplicationContext(this);
        LeanplumActivityHelper.enableLifecycleCallbacks(this);

        LeanplumActivityHelper.deferMessagesForActivities(SplashscreenActivity.class);

        Parser.parseVariables(this);
        Parser.parseVariablesForClasses(GlobalVariables.class);

//        if ((randomInt % 2) == 0) {
//            useTest = true;
//        } else {
//            useTest = false;
//        }

        Log.i("### ", "test env is: " + String.valueOf(useTest));

        String appId = useTest ? test_AppID : store_AppID;
        String devKey = useTest ? test_devKey : store_devKey;
        String prodKey = useTest ? test_prodKey : store_prodKey;

        if (BuildConfig.DEBUG) {
            Leanplum.setAppIdForDevelopmentMode(appId, devKey);
        } else {
            Leanplum.setAppIdForProductionMode(appId, prodKey);
        }

        // Registering for Push with Leanplum
        // Here is where the SenderID is passed. In this case I'm using the Leanplum bundle SenderID,
        // no need in this case to specify any specific Google API key in the Settings in the Leanplum Dashboard.
//        LeanplumPushService.setGcmSenderId(LeanplumPushService.LEANPLUM_SENDER_ID);

        LeanplumPushService.setGcmSenderId("709867216442"); // Google API key: AIzaSyA7n40b_4AuJNXfFZgxj2Cl65Bfjur5UuQ

        // However, a specific SenderID (or SenderIDs) can be passed for the registration.
        // The SenderID correspond to the Google Cloud Porject number (is a 12 digits number) and needs to be passed as a string.
        // For example:
//              LeanplumPushService.setGcmSenderId("123456789012");
        // In this case, the Google Cloud Project specific API key needs to be inserted in the Google API key field in the Settings in the Leanplum Dashboard.

        // If using multiple Push services with different SenderIDs, they need to be all passed also to Leanplum, using the following, for example:
        // LeanplumPushService.setGcmSenderIds(LeanplumPushService.LEANPLUM_SENDER_ID, "123456789012", "some other SenderID in string format...");

        Leanplum.setDeviceId("FedeTest_" + System.currentTimeMillis());

        Leanplum.start(this);

        registerLeanplumGCM(Leanplum.getContext());

    }

    private void registerLeanplumGCM(Context context) {

        try{
            Intent registerIntent = new Intent(context, LeanplumPushRegistrationService.class);
            context.startService(registerIntent);
        }catch (Exception e){
            Log.e("Leanplum", "Error completing GCM Registration.");
        }
    }
}

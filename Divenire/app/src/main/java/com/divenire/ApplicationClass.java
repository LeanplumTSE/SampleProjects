package com.divenire;

import android.app.Application;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumPushService;
import com.leanplum.annotations.Parser;

/**
 * Created by fede on 11/14/16.
 */

public class ApplicationClass extends Application {

    // isActive boolean being updated from the BaseActivity whether onPause/onResume are executed
    public static boolean isActive = false;

    private static String appId = "app_VX99FQmTngx3HQG1pwKeI36MGPlFZayfiW2xhhNAclQ";
    private static String devKey = "dev_8favpED4MXyGLj5d61DyGvQwlA2uA1E7YD684estApY";
    private static String prodKey =  "prod_VIroylLak7tYkWub7NgVTJcJvHpr9AAQYXDtfmDn1W4";

    @Override
    public void onCreate() {
        super.onCreate();

        Leanplum.setApplicationContext(this);
        LeanplumActivityHelper.enableLifecycleCallbacks(this);
        Parser.parseVariables(this);


        if (BuildConfig.DEBUG) {
            Leanplum.setAppIdForDevelopmentMode(appId, devKey);
        } else {
            Leanplum.setAppIdForProductionMode(appId, prodKey);
        }

        LeanplumPushService.setGcmSenderId(LeanplumPushService.LEANPLUM_SENDER_ID);

        Leanplum.start(this);
    }
}

package com.divenire;

import android.app.Application;
import android.util.Log;

import com.appboy.support.AppboyLogger;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumPushService;
import com.appboy.AppboyLifecycleCallbackListener;
import com.leanplum.annotations.Parser;

/**
 * Created by fede on 11/14/16.
 */

public class ApplicationClass extends Application {

    // Setting it manually at the moment
    // Should be true if app is foregrounded - false if backgrounded
    public static boolean isActive = true;

    @Override
    public void onCreate() {
        super.onCreate();

        Leanplum.setApplicationContext(this);
        LeanplumActivityHelper.enableLifecycleCallbacks(this);
        Parser.parseVariables(this);


        if (BuildConfig.DEBUG) {
            Leanplum.setAppIdForDevelopmentMode("app_CWa0CcxPpZD2QaR4IT7gQihLUyCJh6FF4I2lLBQgHok", "dev_v4n2fDHDzAEV79DygT79zyc5zAsDquumlVFfcLMAjAY");
        } else {
            Leanplum.setAppIdForProductionMode("app_CWa0CcxPpZD2QaR4IT7gQihLUyCJh6FF4I2lLBQgHok", "prod_oiRyVYqodd6d0tobY7qFeXidZJamhEUGQDfvASf6614");
        }

        LeanplumPushService.setGcmSenderId(LeanplumPushService.LEANPLUM_SENDER_ID);

//        LeanplumPushService.setGcmSenderId("709867216442");

        Leanplum.start(this);
    }
}

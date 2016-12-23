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

    @Override
    public void onCreate() {
        super.onCreate();

        Leanplum.setApplicationContext(this);
        LeanplumActivityHelper.enableLifecycleCallbacks(this);
        Parser.parseVariables(this);


        if (BuildConfig.DEBUG) {
            Leanplum.setAppIdForDevelopmentMode("APP_KEY", "DEV_KEY");
        } else {
            Leanplum.setAppIdForProductionMode("APP_KEY", "PROD_KEY");
        }

        LeanplumPushService.setGcmSenderId(LeanplumPushService.LEANPLUM_SENDER_ID);

        Leanplum.start(this);
    }
}

package com.leanplum.myapplication;

import android.app.Application;
import android.os.Bundle;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumPushService;
import com.leanplum.annotations.Parser;

/**
 * Created by fede on 2/6/17.
 */

public class ApplicationClass extends Application {

    @Override
    public void onCreate() {

        super.onCreate();

        Leanplum.setApplicationContext(this);
        Parser.parseVariables(this);
        //  For session lifecyle tracking.
        LeanplumActivityHelper.enableLifecycleCallbacks(this);

        // We've inserted your Unity_debug API keys here for you :)
        if (BuildConfig.DEBUG) {
            Leanplum.setAppIdForDevelopmentMode(Credentials.AppID, Credentials.DevKey);
        } else {
            Leanplum.setAppIdForProductionMode(Credentials.AppID, Credentials.ProdKey);
        }

        LeanplumPushService.setGcmSenderId(LeanplumPushService.LEANPLUM_SENDER_ID);

        // This will only run once per session, even if the activity is restarted.
        Leanplum.start(this);
    }
}

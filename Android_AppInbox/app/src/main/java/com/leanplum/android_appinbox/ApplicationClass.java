package com.leanplum.android_appinbox;

import android.app.Application;
import android.util.Log;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumPushService;
import com.leanplum.annotations.Parser;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fede on 1/30/17.
 */

public class ApplicationClass extends Application {

    Map<String, Object> attributes = new HashMap<String, Object>();

    @Override
    public void onCreate(){
        super.onCreate();

        Leanplum.setApplicationContext(this);
        LeanplumActivityHelper.enableLifecycleCallbacks(this);

        String appId = Credentials.AppID;
        String devKey = Credentials.DevKey;
        String prodKey = Credentials.ProdKey;

        if (!BuildConfig.DEBUG) {
            Leanplum.setAppIdForDevelopmentMode(appId, devKey);
        } else {
            Leanplum.setAppIdForProductionMode(appId, prodKey);
        }

        LeanplumPushService.setGcmSenderId(Credentials.GCMsenderID);

        Leanplum.trackAllAppScreens();

        attributes.put("name", "federico");

        Leanplum.start(this, "fede_nexus5_001", attributes);
    }
}

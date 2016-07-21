package com.leanplum.myapplication;

import android.app.Application;
import android.util.Log;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumApplication;
import com.leanplum.LeanplumPushService;
import com.leanplum.annotations.Parser;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.VariablesChangedCallback;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ApplicationClass extends Application {

    @Override
    public void onCreate() {

        Leanplum.setApplicationContext(this);
        Parser.parseVariables(this);
        Parser.parseVariablesForClasses(LoginWindow.class);
        LeanplumActivityHelper.enableLifecycleCallbacks(this);

        super.onCreate();

        if (BuildConfig.DEBUG) {
            Leanplum.setAppIdForDevelopmentMode("", "");
        } else {
            Leanplum.setAppIdForProductionMode("", "");
        }


        LeanplumPushService.setGcmSenderId(LeanplumPushService.LEANPLUM_SENDER_ID);

//        Leanplum.setDeviceId("newAndroidDevice_" + System.currentTimeMillis());

        com.leanplum.customtemplates.MessageTemplates.register(getApplicationContext());



        Leanplum.start(this);
    }
}

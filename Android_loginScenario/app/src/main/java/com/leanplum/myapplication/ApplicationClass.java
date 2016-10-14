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
        LeanplumActivityHelper.enableLifecycleCallbacks(this);

        Parser.parseVariables(this);
        Parser.parseVariablesForClasses(LPvariables.class);

        super.onCreate();

        if (BuildConfig.DEBUG) {
            Leanplum.setAppIdForDevelopmentMode("app_A9pFafRjGoHqxZtjxsqpwTUla1NXTR0LI8KPlNrSDRU", "dev_N1vM5UrYZnqg4OGoeNLcXsUO7rnpjagpuWhDtQXU6ME");
        } else {
            Leanplum.setAppIdForProductionMode("app_A9pFafRjGoHqxZtjxsqpwTUla1NXTR0LI8KPlNrSDRU", "prod_SI71aoflPA9GHws6jB9qv4QoMYsaazHwvCW3qpuUIag");
        }

        LeanplumPushService.setGcmSenderId(LeanplumPushService.LEANPLUM_SENDER_ID);

        Leanplum.setDeviceId("newAndroidDevice_" + System.currentTimeMillis());

        Leanplum.start(this);
    }
}

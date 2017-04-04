package com.leanplum.myapplication;

import android.app.Application;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumApplication;
import com.leanplum.LeanplumPushNotificationCustomizer;
import com.leanplum.LeanplumPushService;
import com.leanplum.annotations.Parser;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.StartCallback;
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

        LeanplumPushService.setCustomizer(new LeanplumPushNotificationCustomizer() {
            @Override
            public void customize(NotificationCompat.Builder builder, Bundle bundle) {
                LeanplumPushService.setDefaultCallbackClass(Window3.class);
            }
        });

        LeanplumPushService.setGcmSenderId(LeanplumPushService.LEANPLUM_SENDER_ID);

        Leanplum.setDeviceId("newAndroidDevice_" + System.currentTimeMillis());

        Leanplum.addStartResponseHandler(new StartCallback() {
            @Override
            public void onResponse(boolean b) {
                Log.i("### ", String.valueOf(Leanplum.variants()));
            }
        });
        Leanplum.start(this);
    }
}

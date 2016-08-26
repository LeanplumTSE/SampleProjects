package com.leanplum.sample;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumPushService;
import com.leanplum.annotations.Parser;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.StartCallback;
import com.leanplum.callbacks.VariablesChangedCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fede on 4/12/16.
 */
public class ApplicationClass extends Application {


    public static Map<String, Object> attributes = new HashMap<String, Object>();

    @Override
    public void onCreate() {
        super.onCreate();

        Leanplum.setApplicationContext(this);
        Parser.parseVariables(this);
        LeanplumActivityHelper.enableLifecycleCallbacks(this);

        if (BuildConfig.DEBUG) {
            Leanplum.setAppIdForDevelopmentMode("", "");
        } else {
            Leanplum.setAppIdForProductionMode("", "");
        }


        Leanplum.addStartResponseHandler(new StartCallback() {
            @Override
            public void onResponse(boolean b) {
                Log.i("### ", "Leanplum is started - Variants are: " + Leanplum.variants());
            }
        });


        attributes.put("Username", "0001");
        Leanplum.start(this, attributes);

    }
}

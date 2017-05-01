package com.fede.androidfragmentsample;

import android.app.Application;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;

/**
 * Created by fede on 5/1/17.
 */

public class ApplicationClass extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Leanplum.setApplicationContext(this);
        LeanplumActivityHelper.enableLifecycleCallbacks(this);

        if (BuildConfig.DEBUG) {
            Leanplum.setAppIdForDevelopmentMode("appkey", "devkey");
        } else {
            Leanplum.setAppIdForProductionMode("appkey", "prodkey");
        }

        Leanplum.start(this);
    }
}

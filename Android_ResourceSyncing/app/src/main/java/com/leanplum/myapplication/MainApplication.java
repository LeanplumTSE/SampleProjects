package com.leanplum.myapplication;

import android.app.Application;
import android.content.res.Resources;
import android.util.Log;
import android.widget.ImageView;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumApplication;
import com.leanplum.LeanplumPushService;
import com.leanplum.LeanplumResources;
import com.leanplum.Var;
import com.leanplum.annotations.Parser;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.StartCallback;
import com.leanplum.callbacks.VariablesChangedCallback;

import java.util.Arrays;
import java.util.List;

/**
 * Created by fede on 4/4/16.
 */
public class MainApplication extends Application {

    @Variable
    public static String welcomeMessage = "welcome!";

    @Override
    public void onCreate() {
        super.onCreate();

        Leanplum.setApplicationContext(this);
        Parser.parseVariables(this);
        LeanplumActivityHelper.enableLifecycleCallbacks(this);

//        // Registering Leanplum - Fill in your APP_ID and KEYs\
        // bResourcesSync app
        if (BuildConfig.DEBUG) {
            Leanplum.setAppIdForDevelopmentMode("", "");
        } else {
            Leanplum.setAppIdForProductionMode("", "");
        }


        Leanplum.syncResourcesAsync();
//        Leanplum.syncResourcesAsync(Arrays.asList("drawable/s.*", "drawable/p.*"), null);
//        Leanplum.syncResourcesAsync(Arrays.asList("*.png"), null);

        Leanplum.start(this, new StartCallback() {
            @Override
            public void onResponse(boolean b) {
                Log.i("### ", "Leanplum started - welcome message: " + welcomeMessage);
            }
        });
    }

    @Override
    public Resources getResources() {
        return new LeanplumResources(super.getResources());
    }
}

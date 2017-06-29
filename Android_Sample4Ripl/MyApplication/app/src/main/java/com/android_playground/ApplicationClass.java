package com.android_playground;

import android.app.Application;
import android.util.Log;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumDeviceIdMode;
import com.leanplum.LeanplumPushService;
import com.leanplum.annotations.Parser;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.StartCallback;
import com.leanplum.callbacks.VariablesChangedCallback;

import static com.android_playground.Credentials.AppID;
import static com.android_playground.Credentials.DevKey;
import static com.android_playground.Credentials.ProdKey;

public class ApplicationClass extends Application {

    @Variable
    public static boolean boolTest = false;

    private boolean isEnabled(){
        return boolTest;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Leanplum.setApplicationContext(this);
        LeanplumActivityHelper.enableLifecycleCallbacks(this);

        LeanplumActivityHelper.deferMessagesForActivities(SplashscreenActivity.class);

        Parser.parseVariables(this);
        Parser.parseVariablesForClasses(GlobalVariables.class, MainActivity.class, LoggedInActivity.class);

        Leanplum.setDeviceIdMode(LeanplumDeviceIdMode.ADVERTISING_ID);

        if (BuildConfig.DEBUG) {
                Leanplum.enableVerboseLoggingInDevelopmentMode();
            Leanplum.setAppIdForDevelopmentMode(AppID, DevKey);
        } else {
            Leanplum.setAppIdForProductionMode(AppID, ProdKey);
        }

        // Conceptually this should be put BEFORE Leanplum start, but even just after shouldn't change much in the behavior.
        Leanplum.addVariablesChangedHandler(new VariablesChangedCallback() {
            @Override
            public void variablesChanged() {
                Log.i("### variants: ", String.valueOf(Leanplum.variants()));
                Log.i("### isEnabled: ", String.valueOf(isEnabled()));
            }
        });

        Leanplum.start(this);



    }
}
package com.android_playground;

import android.app.Application;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumDeviceIdMode;
import com.leanplum.LeanplumPushService;
import com.leanplum.annotations.Parser;
import static com.android_playground.Credentials.AppID;
import static com.android_playground.Credentials.DevKey;
import static com.android_playground.Credentials.ProdKey;


public class ApplicationClass extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Leanplum.setApplicationContext(this);
        LeanplumActivityHelper.enableLifecycleCallbacks(this);

        LeanplumActivityHelper.deferMessagesForActivities(SplashscreenActivity.class);

        Leanplum.setDeviceIdMode(LeanplumDeviceIdMode.ANDROID_ID);
        if (BuildConfig.DEBUG) {
                Leanplum.enableVerboseLoggingInDevelopmentMode();
            Leanplum.setAppIdForDevelopmentMode(AppID, DevKey);
        } else {
            Leanplum.setAppIdForProductionMode(AppID, ProdKey);
        }


        Leanplum.trackAllAppScreens();

        Leanplum.start(this);

    }
}
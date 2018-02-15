package com.leanplum.android_mparticle;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumPushNotificationCustomizer;
import com.leanplum.LeanplumPushService;
import com.leanplum.annotations.Parser;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.StartCallback;
import com.leanplum.callbacks.VariablesChangedCallback;
import com.mparticle.MParticle;

/**
 * Created by fede on 9/19/16.
 */

public class ApplicationClass extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Parsing for Variable to be registered in Leanplum Dashboard
        // This has to be done BEFORE starting Leanplum
        // Variables are defined in this case into LPvariables class
        Parser.parseVariables(this);
        Parser.parseVariablesForClasses(LPvariables.class);

        Leanplum.setDeviceId("fedePippo");

//        LeanplumPushService.enableFirebase();

//        LeanplumPushService.setGcmSenderId("537826669266");

        // Starting mParticle and Leanplum in Development mode
        MParticle.start(this, MParticle.InstallType.AutoDetect, MParticle.Environment.Development);

        MParticle.getInstance().Messaging().enablePushNotifications("537826669266");

        // mParticle keys are stored in res/values/string.xml
//        MParticle.start(this);
    }
}
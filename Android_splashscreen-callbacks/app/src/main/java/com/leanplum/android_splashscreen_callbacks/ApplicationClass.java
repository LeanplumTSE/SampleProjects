package com.leanplum.android_splashscreen_callbacks;

import android.app.Application;
import android.util.Log;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumPushService;
import com.leanplum.annotations.Parser;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.StartCallback;
import com.leanplum.callbacks.VariablesChangedCallback;

/**
 * Created by fede on 8/23/16.
 */
public class ApplicationClass extends Application {


    @Override
    public void onCreate() {
        super.onCreate();

        Leanplum.setApplicationContext(this);

        Parser.parseVariables(this);
        Parser.parseVariablesForClasses(GlobalVariables.class);

        LeanplumActivityHelper.enableLifecycleCallbacks(this);


        if (BuildConfig.DEBUG) {
            Leanplum.setAppIdForDevelopmentMode("app_KBA1FumxGvuftZQQanpt8qNv1eLJcLHffBF38qwVNcw", "dev_rXc2Io45NCYWFC01MhhurVhd1caBKrwKXWQXOmzzdqA");
        } else {
            Leanplum.setAppIdForProductionMode("app_KBA1FumxGvuftZQQanpt8qNv1eLJcLHffBF38qwVNcw", "prod_JJ7oaM1zXgv0QKxIxNtauBeSD3Wa7ghdaAXWTgYp32o");
        }


        // Registering for Push with Leanplum
        // Here is where the SenderID is passed. In this case I'm using the Leanplum bundle SenderID,
        // no need in this case to specify any specific Google API key in the Settings in the Leanplum Dashboard.
        LeanplumPushService.setGcmSenderId(LeanplumPushService.LEANPLUM_SENDER_ID);

        // However, a specific SenderID (or SenderIDs) can be passed for the registration.
        // The SenderID correspond to the Google Cloud Porject number (is a 12 digits number) and needs to be passed as a string.
        // For example:
        //      LeanplumPushService.setGcmSenderId("123456789012");
        // In this case, the Google Cloud Project specific API key needs to be inserted in the Google API key field in the Settings in the Leanplum Dashboard.

        // If using multiple Push services with different SenderIDs, they need to be all passed also to Leanplum, using the following, for example:
        // LeanplumPushService.setGcmSenderIds(LeanplumPushService.LEANPLUM_SENDER_ID, "123456789012", "some other SenderID in string format...");

        Leanplum.addStartResponseHandler(new StartCallback() {
            @Override
            public void onResponse(boolean b) {
                Log.i("#### ", "Leanplum started - " + Leanplum.variants());
            }
        });

        Leanplum.start(this);



    }
}

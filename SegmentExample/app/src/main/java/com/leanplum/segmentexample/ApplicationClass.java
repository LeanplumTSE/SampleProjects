package com.leanplum.segmentexample;

import android.app.Application;
import android.util.Log;
import android.widget.EditText;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumPushService;
import com.leanplum.callbacks.VariablesChangedCallback;
import com.leanplum.segment.LeanplumIntegration;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fede on 1/18/17.
 */

public class ApplicationClass extends Application {

    private static final String ANALYTICS_WRITE_KEY_DEV = "your key associated with App in Development";
    private static final String ANALYTICS_WRITE_KEY_PROD = "your key associated with App in Production";

    Map<String, Object> params = new HashMap<String, Object>();


    @Override
    public void onCreate(){
        super.onCreate();

        Leanplum.setApplicationContext(this);
        LeanplumActivityHelper.enableLifecycleCallbacks(this);

        // Enabling Push
        // GCM
//        LeanplumPushService.setGcmSenderId(LeanplumPushService.LEANPLUM_SENDER_ID);
        //Firebase
        LeanplumPushService.enableFirebase();


        Analytics.Builder builder;

        if (BuildConfig.DEBUG) {
            builder =
                    new Analytics.Builder(this, ANALYTICS_WRITE_KEY_DEV)
                            .trackApplicationLifecycleEvents()
                            .trackAttributionInformation()
                            .recordScreenViews()
                            .use(LeanplumIntegration.FACTORY);

        } else {
            builder = new Analytics.Builder(this, ANALYTICS_WRITE_KEY_PROD)
                            .trackApplicationLifecycleEvents()
                            .trackAttributionInformation()
                            .recordScreenViews()
                            .use(LeanplumIntegration.FACTORY);
        }


        // Set the initialized instance as a globally accessible instance.
        Analytics.setSingletonInstance(builder.build());
        // Now anytime you call Analytics.with, the custom instance will be returned.
        final Analytics analytics = Analytics.with(this);

        analytics.onIntegrationReady(LeanplumIntegration.LEANPLUM_SEGMENT_KEY,
                new Analytics.Callback() {
                    @Override
                    public void onReady(Object instance) {
                        Leanplum.addVariablesChangedHandler(new VariablesChangedCallback() {
                            @Override
                            public void variablesChanged() {
                                Log.i("### ", "Leanplum started");
                                Log.i("### ", "Logging in with User 'FedeTest2'");


                                // --
//                                params.put("name", "federico");
//                                params.put("age", 99);
//                                Analytics.with(getApplicationContext()).track("testEvent", new Properties().putValue("testKey", params));

                                // --
                                Properties myProps = new Properties();
                                myProps.putValue("param1","value1");
                                myProps.putValue("params2", "value2");
                                Analytics.with(getApplicationContext()).track("testEvent", myProps);


                                Leanplum.forceContentUpdate(new VariablesChangedCallback() {
                                    @Override
                                    public void variablesChanged() {
                                        Log.i("### ","Login and Content updated");
                                    }
                                });
                            }
                        });
                    }
                });
    }
}
package com.leanplum.android_segment;

import android.app.Application;
import android.util.Log;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumPushService;
import com.leanplum.segment.LeanplumIntegration;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;

/**
 * Created by fede on 10/4/16.
 */

public class ApplicationClass extends Application {

    private static final String SEGMENT_WRITE_KEY = "WV0IGPMEneSe74l0lRoLaOAaGj7S4pbM";


    @Override
    public void onCreate(){
        super.onCreate();

        LeanplumPushService.setGcmSenderId(LeanplumPushService.LEANPLUM_SENDER_ID);

        Analytics analytics = new Analytics
                .Builder(getApplicationContext(), SEGMENT_WRITE_KEY)
                .use(LeanplumIntegration.FACTORY)
                .build();

        analytics.onIntegrationReady(LeanplumIntegration.LEANPLUM_SEGMENT_KEY,
                new Analytics.Callback() {
                    @Override
                    public void onReady(Object instance) {
                        Log.i("### ", "Leanplum started");
                        Leanplum.track("test");
                    }
                });

        Analytics.setSingletonInstance(analytics);

        Traits traits = new Traits()
                .putName("First Last")
                .putEmail("first@last.com");

        Analytics.with(getApplicationContext())
                .identify("f4ca124298", traits, null);

        Properties properties = new Properties()
                .putValue("plan", "Enterprise");

        Analytics.with(getApplicationContext()).track("Signed up", properties);
        Analytics.with(getApplicationContext()).screen("Main", "Start");
    }
}

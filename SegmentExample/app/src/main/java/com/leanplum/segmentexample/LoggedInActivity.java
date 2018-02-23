package com.leanplum.segmentexample;

import android.app.Activity;
import android.os.Bundle;

import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;

/**
 * Created by fede on 2/23/18.
 */

public class LoggedInActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loggedin);

        Traits Traits_UA = new Traits().putValue("isLoggedIn","True");

        Analytics.with(this).identify(Traits_UA);
    }
}

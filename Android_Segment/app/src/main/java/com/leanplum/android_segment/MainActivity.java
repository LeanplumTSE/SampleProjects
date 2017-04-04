package com.leanplum.android_segment;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;

public class MainActivity extends AppCompatActivity {

    public void segmentDoThings(View view){
        Properties properties = new Properties()
                .putPrice(0.99)
                .putCurrency("USD")
                .putTitle("InApp Purchase");
        Analytics.with(getApplicationContext())
                .track("Track Button Clicked!", properties);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




    }
}

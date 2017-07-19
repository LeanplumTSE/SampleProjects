package com.android_playground;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.leanplum.Leanplum;
import com.leanplum.callbacks.StartCallback;

/**
 * Created by fede on 12/7/16.
 */

public class SplashscreenActivity extends AppCompatActivity {

    long startTime, endTime, elapsedNanos, elapsedMS, elapsedMSRoundedTo30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Leanplum.track("splashstart");


        Leanplum.addStartResponseHandler(new StartCallback() {
            @Override
            public void onResponse(boolean b) {
                Log.i("### ", "Splashscreen  - Leanplum has started and variables are retrieved");

                Intent intent = new Intent(SplashscreenActivity.this, MainActivity.class);
                startActivity(intent);

                finish();
            }
        });

    }
}

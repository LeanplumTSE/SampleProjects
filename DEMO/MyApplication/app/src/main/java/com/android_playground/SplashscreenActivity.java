package com.android_playground;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.leanplum.Leanplum;
import com.leanplum.callbacks.StartCallback;
import com.leanplum.callbacks.VariablesChangedCallback;

/**
 * Created by fede on 12/7/16.
 */

public class SplashscreenActivity extends AppCompatActivity {

    long startTime, endTime, elapsedNanos, elapsedMS, elapsedMSRoundedTo30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Leanplum.addStartResponseHandler(new StartCallback() {
            @Override
            public void onResponse(boolean b) {
                Log.i("### ", "Splashscreen  - Leanplum has started and All variables changed");

                long endStartTime = System.nanoTime();

                long elapsedStartNanos = endStartTime - startTime;
                long elapsedStartMS = elapsedStartNanos / 1000000;
                long elapsedStartMSRoundedTo30 = (elapsedStartMS / 30) * 30;
                Log.i("#### ", "Leanplum started");

                String waitTimeString = String.valueOf(elapsedStartMSRoundedTo30);
                Log.i("### ", "Leanplum start Time: " + waitTimeString);

                Intent intent = new Intent(SplashscreenActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }
}

package com.android_playground;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumPushListenerService;

/**
 * Created by fede on 12/7/16.
 */

public class DeepLinkedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_deeplinked);

        Leanplum.track("DeepLinked activity");
    }
}

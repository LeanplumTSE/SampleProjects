package com.leanplum.android_mparticle;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.leanplum.Leanplum;
import com.leanplum.callbacks.StartCallback;
import com.mparticle.kits.LeanplumKit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Leanplum.addStartResponseHandler(new StartCallback() {
            @Override
            public void onResponse(boolean b) {
                Log.i("### ", "Leanplum started");
                Leanplum.track("Leanplum started");
            }
        });



    }
}

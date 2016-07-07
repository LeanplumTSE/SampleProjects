package com.leanplum.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.VariablesChangedCallback;

/**
 * Created by fede on 7/6/16.
 */
public class AnotherActivity extends Activity {

    @Variable
    public static String welcomeAnother = "welcome!";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_another);

        Leanplum.track("testEvent");

        Leanplum.addVariablesChangedHandler(new VariablesChangedCallback() {
            @Override
            public void variablesChanged() {
                Log.i("### ", welcomeAnother);
            }
        });

    }
}

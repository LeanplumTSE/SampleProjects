package com.leanplum.android_mparticle;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.leanplum.Leanplum;
import com.leanplum.callbacks.VariablesChangedCallback;

import static com.leanplum.android_mparticle.LPvariables.LPstring;

/**
 * Created by fede on 12/4/17.
 */

public class LoggedinActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loggedin);

        Leanplum.addVariablesChangedHandler(new VariablesChangedCallback() {
            @Override
            public void variablesChanged() {
                TextView tv = (TextView) findViewById (R.id.textView);
                tv.setText(LPstring);
            }
        });
    }
}

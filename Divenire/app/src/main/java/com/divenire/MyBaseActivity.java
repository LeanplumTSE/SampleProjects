package com.divenire;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by fede on 12/23/16.
 */

public abstract class MyBaseActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ApplicationClass.isActive = false;
        Log.i("### ", String.valueOf(ApplicationClass.isActive));
    }
    @Override
    protected void onResume() {
        super.onResume();
        ApplicationClass.isActive = true;
        Log.i("### ", String.valueOf(ApplicationClass.isActive));

    }
}

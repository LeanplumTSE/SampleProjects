package com.leanplum.myapplication;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by fede on 11/7/16.
 */
public class Window3 extends Activity{

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_win3);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.i("### ", "win3 destroyed");
    }
}

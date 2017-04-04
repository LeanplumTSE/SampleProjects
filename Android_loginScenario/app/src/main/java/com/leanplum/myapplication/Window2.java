package com.leanplum.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

/**
 * Created by fede on 11/7/16.
 */

public class Window2 extends Activity {

    public void openWindow3activity(View view){
        Intent intent = new Intent(Window2.this, Window3.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_win2);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.i("### ", "win2 destroyed");
    }
}

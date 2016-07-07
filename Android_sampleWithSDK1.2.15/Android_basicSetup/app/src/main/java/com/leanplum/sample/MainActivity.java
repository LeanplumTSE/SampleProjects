package com.leanplum.sample;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;

public class MainActivity extends Activity {

    public void openAnotherActivity(View view){
        Intent intent = new Intent(this, AnotherActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Leanplum.start(this);
    }
}

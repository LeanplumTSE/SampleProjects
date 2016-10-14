package com.leanplum.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.leanplum.Leanplum;
import com.leanplum.callbacks.VariablesChangedCallback;

/**
 * Created by fede on 10/12/16.
 */

public class Splashcreen extends AppCompatActivity {

    Thread background = new Thread() {
        public void run() {

            try {
                // Thread will sleep for 5 seconds
                sleep(5 * 1000);

                Log.i("### ", "time passed");

                Intent intent = new Intent(Splashcreen.this, MainActivity.class);
                startActivity(intent);
                finish();

            } catch (Exception e) {

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Leanplum.addVariablesChangedAndNoDownloadsPendingHandler(new VariablesChangedCallback() {
            @Override
            public void variablesChanged() {
                Log.i("### ", "Variables changed callback triggered - opening MainActivity");

                Log.i("### ", LPvariables.welcomeMessage);

                // start thread
//                background.start();

                Intent intent = new Intent(Splashcreen.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }
}

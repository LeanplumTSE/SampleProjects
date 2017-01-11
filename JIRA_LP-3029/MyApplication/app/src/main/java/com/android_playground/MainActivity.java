package com.android_playground;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumPushRegistrationService;
import com.leanplum.LeanplumPushService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "MyPrefsFile";

    String token;
    Map<String, Object> loggedoutAttribute = new HashMap<String, Object>();

    public static EditText mEdit;

    public static EditText getmEdit() {
        return mEdit;
    }

    public void login(View view) {
        mEdit = (EditText) findViewById(R.id.loginField);
        Leanplum.setUserId(mEdit.getText().toString());
        Log.i("####", "Logging in with Username: " + mEdit.getText().toString());

        Intent intent = new Intent(this, LoggedInActivity.class);
        startActivity(intent);
    }

    public void doSomething(View view) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean silent = settings.getBoolean("silentMode", false);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("### ", String.valueOf(Leanplum.variants()));

        loggedoutAttribute.put("isLoggedIn:", false);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Setting welcome Text from Leanplum
        // I'm not putting this code inside a Callback since I'm closing the Splashscreen when Leanplum start callback is triggered
        TextView welcomeText = (TextView) findViewById(R.id.welcomeString);
        welcomeText.setText(GlobalVariables.welcomeString);
//

//        final String authorizedEntity = "709867216442"; // Project id from Google Developers Console
//        String scope = “GCM”; // e.g. communicating using GCM, but you can use any
        // URL-safe characters up to a maximum of 1000, or
        // you can also leave it blank.

        if (ApplicationClass.useTest) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    InstanceID instanceID = InstanceID.getInstance(getApplicationContext());
                    try {

                        // retrieving the token value
                        String authorizedEntity = "709867216442";
                        token = instanceID.getToken(authorizedEntity, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                        Log.i("### ", "token: " + token);

                        // workaround to set the Registration ID - first set an empty one and then the one with the retrieved token
                        LeanplumPushService.setGcmRegistrationId("");
                        LeanplumPushService.setGcmRegistrationId(token);

                    } catch (IOException e) {
                        Log.i("### ", "bla bla" + e.toString());
                    }

                }
            });
            thread.start();
        }

//
//        InstanceID instanceID = InstanceID.getInstance(this);
//        try {
//            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
//            Log.i("### ", token);
//        } catch (IOException e) {
//            Log.i("### ", "bla bla" + e.toString());
//        }

//        InstanceID instanceID = InstanceID.getInstance(LeanplumPushRegistrationService.this);
//        try {
//            String registrationId = instanceID.getToken(LeanplumPushService.LEANPLUM_SENDER_ID,
//                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        InstanceID iid = InstanceID.getInstance(this);
//
//        try {
//            registrationId = iid.getToken(ApplicationClass.SenderID1, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
//        } catch (IOException e) {
////            e.printStackTrace();
//            Log.i("###", "blabla");
//        }
//
//        Log.i("### reg:", registrationId);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("####", "User is logged out");
        Leanplum.setUserAttributes(loggedoutAttribute);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

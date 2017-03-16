package com.android_playground;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
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
import com.leanplum.callbacks.VariablesChangedCallback;

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
//
        // Purchase event Test
//        String orderSummary = "35.99";
//        Map<String, Object> params = new HashMap<String, Object>();;
//        params.put("payment-type", "credit_Card");
//        Leanplum.track("purchase_test", Double.parseDouble(orderSummary), params);
//        Leanplum.track("logPurchase", 19.90);

        // UserAttribute test
//        Map<String, Object> attributes = new HashMap<String, Object>();
//        attributes.put("Name", "Federico");
//        attributes.put("ZIPcode", 94107);
//        attributes.put("email", "federico@leanplum.com");
//        Leanplum.setUserAttributes(attributes);

        Leanplum.track("triggerWebhook");

//        Leanplum.forceContentUpdate();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("### ", String.valueOf(Leanplum.variants()));

        loggedoutAttribute.put("isLoggedIn:", false);


        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setting welcome Text from Leanplum
        // I'm not putting this code inside a Callback since I'm closing the Splashscreen when Leanplum start callback is triggered



        Leanplum.addVariablesChangedHandler(new VariablesChangedCallback() {
            @Override
            public void variablesChanged() {
                TextView welcomeText = (TextView) findViewById(R.id.welcomeString);
                welcomeText.setText(GlobalVariables.welcomeString);
            }
        });

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

package com.android_playground;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumPushService;
import com.leanplum.callbacks.VariablesChangedCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.android_playground.GlobalVariables.welcomeString;


public class MainActivity extends AppCompatActivity {

    Map<String, Object> loggedoutAttribute = new HashMap<String, Object>();

//    Map<String, Object> orderAttribute = new HashMap<String, Object>();
//    Map<String, Object> attributes = new HashMap<String, Object>();
//    Map<String, Object> item= new HashMap<String, Object>();

    public static EditText mEdit;
    public static EditText getmEdit() {
        return mEdit;
    }


    public void login(View view) {
        mEdit = (EditText) findViewById(R.id.loginField);
        Leanplum.setUserId(mEdit.getText().toString());
        Log.i("####", "Logging in with Username: " + mEdit.getText().toString());

//        Leanplum.forceContentUpdate(new VariablesChangedCallback() {
//            @Override
//            public void variablesChanged() {
                Intent intent = new Intent(getApplicationContext(), LoggedInActivity.class);
                startActivity(intent);
//            }
//        });
    }

    public void doSomething(View view) {
//

        Leanplum.track("Test");

        // Purchase event example
//        String orderSummary = "35.99";
//        Map<String, Object> params = new HashMap<String, Object>();;
//        params.put("payment-type", "credit_Card");
//        Leanplum.track("purchase_test", Double.parseDouble(orderSummary), params);
//        Leanplum.track("logPurchase", 19.90);

        // UserAttribute test
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("Name", "Federico");
        attributes.put("ZIPcode", 94107);
        attributes.put("email", "federico@leanplum.com");
        Leanplum.setUserAttributes(attributes);

//        Log.i("### Leanplum", "Tracking 'Home' event");

//        Leanplum.track("Accept!");

//        orderAttribute.put("number", 12234);
//        orderAttribute.put("ingredient", "mushrooms");
//        Leanplum.track("Pizza", orderAttribute);
//
//
//        item.put("Articolo", "Frame");
//        Leanplum.track(Leanplum.PURCHASE_EVENT_NAME, 19.99, item);

//        Leanplum.track("Acquisto!", 20.01, item);


//        Leanplum.setUserAttributes(orderAttribute);

//        Location dummyLocation = new Location("");
//        dummyLocation.setLatitude(32.777930);
//        dummyLocation.setLongitude(-96.789344);
//        Leanplum.setDeviceLocation(dummyLocation);

//        Leanplum.forceContentUpdate(new VariablesChangedCallback() {
//            @Override
//            public void variablesChanged() {
//                Log.i("### ", "this is going to be executed when Force Content update is finished");
//            }
//        });

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        Log.i("### ", String.valueOf(Leanplum.variants()));

        loggedoutAttribute.put("isLoggedIn:", false);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Leanplum.addVariablesChangedHandler(new VariablesChangedCallback() {
            @Override
            public void variablesChanged() {
                Log.i("### ", welcomeString);
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

//
}

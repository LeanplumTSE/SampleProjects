package com.android_playground;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import com.leanplum.Leanplum;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.VariablesChangedCallback;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "MyPrefsFile";

    String token;
    Map<String, Object> loggedoutAttribute = new HashMap<String, Object>();
    Map<String, Object> orderAttribute = new HashMap<String, Object>();

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

        Log.i("### Leanplum", "Tracking 'Home' event");
        Leanplum.track("Home");

//        Leanplum.forceContentUpdate();
    }


    @Variable(group = "balanceShield.about", name = "subhead")
    public static String shieldAboutSubheadText = "Turn on Balance Shield, and we’ll automatically Cash Out up to $%1$s when your bank balance is feeling low. That means, if you’ve added earnings, there’s no more worrying about overdraft fees from small purchases.";

    public String getShieldAboutSubheadText() {
        return shieldAboutSubheadText;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Log.i("### ", String.valueOf(Leanplum.variants()));

        loggedoutAttribute.put("isLoggedIn:", false);

//        Leanplum.track("splashstart");


        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setting welcome Text from Leanplum
        // I'm not putting this code inside a Callback since I'm closing the Splashscreen when Leanplum start callback is triggered

        Leanplum.addVariablesChangedHandler(new VariablesChangedCallback() {
                                                @Override
                                                public void variablesChanged() {
                                                    Log.i("### ", getShieldAboutSubheadText());

                                                    String firstLineString = getShieldAboutSubheadText();

                                                    String secondLineString = String.format(getShieldAboutSubheadText(),
                                                            "100"
                                                    );
                                                }
                                            });

        Leanplum.track("MainActivity open");
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

    public void changeOrderUA(View view) {
        orderAttribute.put("Order", "newOrder+" + System.currentTimeMillis());
        Log.i("### Leanplum", "Setting " + orderAttribute.toString());
        Leanplum.setUserAttributes(orderAttribute);
    }

    public void trackRated(View view) {
        Log.i("### Leanplum", "Tracking 'Rated' event");
        Leanplum.track("Rated");
    }
}

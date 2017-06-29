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

//import static com.android_playground.GlobalVariables.boolTest;

public class MainActivity extends AppCompatActivity {

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

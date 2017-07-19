package com.android_playground;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.READ_CONTACTS;


public class MainActivity extends AppCompatActivity {

    Map<String, Object> loggedoutAttribute = new HashMap<String, Object>();
    public static EditText mEdit;
    public static EditText getmEdit() {
        return mEdit;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        Log.i("### ", "Permission requested");

        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    Log.i("###", "Permission denied");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
//                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    public void login(View view) {
        mEdit = (EditText) findViewById(R.id.loginField);
        Leanplum.setUserId(mEdit.getText().toString());
        Log.i("####", "Logging in with Username: " + mEdit.getText().toString());

        Intent intent = new Intent(this, LoggedInActivity.class);
        startActivity(intent);
    }

    public void doSomething(View view) {

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{READ_CONTACTS},
                1);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
}

package com.leanplum.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumPushService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    Map<String, Object> loggedoutAttribute = new HashMap<String, Object>();
    Map<String, Object> attributes = new HashMap<String, Object>();

    public static EditText mEdit;
    public static EditText getmEdit(){
        return mEdit;
    }

    public void setUA (View view) {
      attributes.put("OnBoardingDone", "Yes");
      Leanplum.setUserAttributes(attributes);
    };

    public void setEvents(View view){
        Leanplum.track("Event1_LoggedOut");
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.i("####", "User is logged out");
        Leanplum.setUserAttributes(loggedoutAttribute);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loggedoutAttribute.put("isLoggedIn", false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        attributes.put("OnBoardingDone", "No");
        Leanplum.setUserAttributes(attributes);

        try {
            Log.i("### ", getPackageManager().getPackageInfo("com.leanplum.myapplication", 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void login(View view) {
        mEdit = (EditText) findViewById(R.id.loginField);
        Leanplum.setUserId(mEdit.getText().toString());
        Log.i("####", "Logging in with Username: " + mEdit.getText().toString());

        Intent intent = new Intent(this, LoginWindow.class);
        startActivity(intent);
    }
}
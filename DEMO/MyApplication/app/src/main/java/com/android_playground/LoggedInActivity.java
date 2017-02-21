package com.android_playground;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.leanplum.Leanplum;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fede on 12/7/16.
 */

public class LoggedInActivity extends AppCompatActivity {

    Map<String, Object> loginAttribute = new HashMap<String, Object>();

    @Override
    public void onBackPressed(){
        Log.i("####" , "Back button disabled - click on Logout to return to Login");
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loggedin);

        loginAttribute.put("isLoggedIn:", true);
        Leanplum.setUserAttributes(loginAttribute);

        EditText medit = MainActivity.getmEdit();
        Log.i("####", "User is now logged in with User: " + medit.getText().toString());

    }

    public void logout(View view) {
        this.finish();
    }
}

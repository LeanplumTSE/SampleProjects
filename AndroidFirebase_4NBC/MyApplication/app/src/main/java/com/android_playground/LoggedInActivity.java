package com.android_playground;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.leanplum.Leanplum;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.VariablesChangedCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.android_playground.GlobalVariables.welcomeString;

/**
 * Created by fede on 12/7/16.
 */

public class LoggedInActivity extends AppCompatActivity {

    Map<String, Object> loginAttribute = new HashMap<String, Object>();
    Map<String, Object> UA = new HashMap<String, Object>();
    Map<String, Object> songParams = new HashMap<String, Object>();

    @Variable
    public static String test = "this is a test";

    @Override
    public void onBackPressed(){
        Log.i("####" , "Back button disabled - click on Logout to return to Login");
    };

    public void setUA(View view) {
        Log.i("### Leanplum", "Setting UA");
        UA.put("name", "federico");
        UA.put("item number", 12345);
        Leanplum.setUserAttributes(UA);
    }

    public void trackArtist(View view){
        songParams.put("Album", "Cosmic Egg");
        songParams.put("Song", "New Moon Rising");
        Leanplum.track("Wolfmother", songParams);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loggedin);

        Leanplum.track("loggedin");

        loginAttribute.put("isLoggedIn:", true);
        Leanplum.setUserAttributes(loginAttribute);

        EditText medit = MainActivity.getmEdit();
        Log.i("####", "User is now logged in with User: " + medit.getText().toString());

        Leanplum.addVariablesChangedHandler(new VariablesChangedCallback() {
            @Override
            public void variablesChanged() {
                // Some code executed here
                // In PRODUCTION this code would be executed, however the Variables data is being FETCHED
                // only ONCE - i.e. after Leanplum has started.
                Log.i("### ", "Login screen callback - " + test);
                TextView tv = (TextView)findViewById(R.id.textView);
                tv.setText(welcomeString);
            }
        });

//        Leanplum.forceContentUpdate(new VariablesChangedCallback() {
//            @Override
//            public void variablesChanged() {
//                TextView tv = (TextView)findViewById(R.id.textView);
//                tv.setText(welcomeString);
//            }
//        });

    }

    public void logout(View view) {
        this.finish();
    }
}

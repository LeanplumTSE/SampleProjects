package com.leanplum.myapplication;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.leanplum.Leanplum;
import com.leanplum.Var;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.VariablesChangedCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by fede on 3/13/16.
 */
public class LoginWindow extends Activity{

    Map<String, Object> loginAttribute = new HashMap<String, Object>();
    Map<String, Object> UserAttributes = new HashMap<String, Object>();

    public void Event_NO(View view){
        Leanplum.track("Event2_LoggedIN_NO");
    }

    public void Event_YES(View view){
        Leanplum.track("Event2_LoggedIN_YES");
    }


    public void SetUserAttributes(View view){
        UserAttributes.put("Name", "Federico");
        UserAttributes.put("Email", "f.casali@gmail.com");
        Leanplum.setUserAttributes(UserAttributes);
    }


    @Override
    public void onBackPressed(){
        Log.i("####" , "Back button disabled - click on Logout to return to Login");
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginwindow);

        loginAttribute.put("isLoggedIn", true);
        Leanplum.setUserAttributes(loginAttribute);

        EditText medit = MainActivity.getmEdit();
        Log.i("####", "User is now logged in with User: " + medit.getText().toString());

    }

    public void logout(View view) {
        this.finish();
    }

}

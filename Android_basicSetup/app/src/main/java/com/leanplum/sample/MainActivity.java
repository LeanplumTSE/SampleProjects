package com.leanplum.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.leanplum.Leanplum;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static Map<String, Object> attributes = new HashMap<String, Object>();

    public static void anotherUA(View view){
        attributes.put("anotherUA", "9999");
        Leanplum.setUserAttributes(attributes);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}

package com.leanplum.android_mparticle;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.leanplum.Leanplum;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.StartCallback;
import com.leanplum.callbacks.VariablesChangedCallback;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.kits.LeanplumKit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static EditText mEdit;
    public static String userID;

    // Ignore the following
//    public static Map<String, Object> LeanplumAttributes = new HashMap<String, Object>();
//    public static List<Object> products = new ArrayList<Object>();


    // creating a product for the Purchase event
    Product product = new Product.Builder("Foo name", "Foo sku", 100.00)
            .quantity(4)
            .build();

    // creating a transaction ID required for PURCHASE and REFUND events
    TransactionAttributes attributes = new TransactionAttributes("foo-transaction-id")
            .setRevenue(450.00)
            .setTax(30.00)
            .setShipping(20.00);


    public void setUserID(View view){
        mEdit = (EditText) findViewById(R.id.loginField);
        userID = mEdit.getText().toString();
        Leanplum.setUserId(userID);
        Log.i("### ", "Leanplum setUserID with user: " + userID);
        userID = null;
    }

    public void setUserAttributes(View view){

        // setting UserAttributes
        MParticle.getInstance().setUserAttribute("name", "federico");
        MParticle.getInstance().setUserAttribute("email", "fede@gmail.com");


        //associate a list of values with an attribute key
        List<String> attributeList = new ArrayList<>();
        attributeList.add("Apple");
        attributeList.add("Orange");
        attributeList.add("Peach");
        MParticle.getInstance().setUserAttributeList("Favorite Fruits", attributeList);


        // Ignore the following test
        // Adding Leanplum UserAttributes as an Array
//        products.add("paperino");
//        products.add("pluto");
//
//        LeanplumAttributes.put("otherNames", products);
//        Leanplum.setUserAttributes(LeanplumAttributes);
//        products.clear();

    }

    public void trackMPevent(View view){
        // Tracking a mParticle Custom Event passing parameters
        eventInfo.put("spice", "hot");
        eventInfo.put("menu", "weekdays");
        eventInfo.put("numberParam", "123456");
        MPEvent event = new MPEvent.Builder("Food Order", MParticle.EventType.Transaction)
                .duration(100)
                .info(eventInfo)
                .build();

        MParticle.getInstance().logEvent(event);


        // tracking purchase event with mParticle
        MParticle mp = MParticle.getInstance();
        CommerceEvent commercEvent = new CommerceEvent.Builder(Product.PURCHASE, product)
                .transactionAttributes(attributes)
                .build();
        mp.logEvent(commercEvent);
    }

    // Event parameters for mParticle
    Map<String, String> eventInfo = new HashMap<String, String>(2);

    // Event paramters for Leanplum
    Map<String, Object> params = new HashMap<String, Object>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Leanplum.addVariablesChangedHandler(new VariablesChangedCallback() {
            @Override
            public void variablesChanged() {
                Log.i("### ", "Leanplum variable: " + LPvariables.LPstring);
                Log.i("### ", "Leanplum variable: " + String.valueOf(LPvariables.LPint));
            }
        });



        Leanplum.addStartResponseHandler(new StartCallback() {
            @Override
            public void onResponse(boolean b) {

                Log.i("### ", "Leanplum started");

                // Loggin an event using Leanplum - you should see this event tracked in Leanplum
                params.put("post", 12345);
                Leanplum.track("Leanplum started", params);

                // Tracking events using MParticle - you should also see those events tracked in Leanplum
//                MParticle.getInstance().logEvent("Mparticle tracked event - other", MParticle.EventType.Other);
//                MParticle.getInstance().logEvent("Mparticle tracked event - location", MParticle.EventType.Location);
            }
        });
    }
}
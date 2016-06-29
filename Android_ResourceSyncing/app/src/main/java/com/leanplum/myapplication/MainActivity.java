package com.leanplum.myapplication;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumResources;
import com.leanplum.activities.LeanplumActivity;
import com.leanplum.annotations.Parser;
import com.leanplum.callbacks.VariablesChangedCallback;

public class MainActivity extends Activity {

    public void openPage2(View view) {
        Intent intent = new Intent(this, Page2Activity.class);
        startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Leanplum.setApplicationContext(this);
        Parser.parseVariables(this);
        LeanplumActivityHelper.enableLifecycleCallbacks(this.getApplication());

        setContentView(R.layout.activity_main);

        Leanplum.addVariablesChangedAndNoDownloadsPendingHandler(new VariablesChangedCallback() {
            @Override
            public void variablesChanged() {
                Log.i("#### ", "Variable changed");

//                Drawable resource = getResources().getDrawable(R.drawable.snoopy);
//                Drawable resource = getResources().getDrawable(R.drawable.pandaguns);
                ImageView imageView = (ImageView) findViewById(R.id.welcome_logo_large);
//                imageView.setImageResource(R.drawable.snoopy);

                Drawable drawable = getResources().getDrawable(R.drawable.snoopy);

                imageView.setImageDrawable(drawable);


//                int width = drawable.getIntrinsicWidth();
//                int height = drawable.getIntrinsicHeight();
//                Log.i("test", width + "-" + height);
            }
        });
    }

    @Override
    public Resources getResources() {
        return new LeanplumResources(super.getResources());
    }
}

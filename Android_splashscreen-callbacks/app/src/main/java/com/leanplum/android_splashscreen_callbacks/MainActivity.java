package com.leanplum.android_splashscreen_callbacks;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.leanplum.Leanplum;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.VariablesChangedCallback;

public class MainActivity extends AppCompatActivity {



    public void forceContentUpdate(View view){
        Log.i("### ", "Forcing content update");
        Leanplum.forceContentUpdate();
    }

    public void openNewAct(View view){
        Intent intent = new Intent(MainActivity.this, newActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ImageView imageView = (ImageView) findViewById(R.id.welcome_logo_large);

        Leanplum.addVariablesChangedAndNoDownloadsPendingHandler(new VariablesChangedCallback() {
            @Override
            public void variablesChanged() {
                Log.i("### ", "MainActivity - All variables changed and no download is pending -- " + GlobalVariables.String_Welcome1);

                if (imageView != null) {
                    imageView.setImageBitmap(BitmapFactory.decodeStream(GlobalVariables.mario1.stream()));
                    Leanplum.track("Image displayed!");
                }
            }
        });


        Leanplum.addOnceVariablesChangedAndNoDownloadsPendingHandler(new VariablesChangedCallback() {
            @Override
            public void variablesChanged() {
                Log.i("### ", "MainActivity ONCE - All variables changed and no download is pending -- " + GlobalVariables.String_Welcome1);


//                if (imageView != null) {
//                    imageView.setImageBitmap(BitmapFactory.decodeStream(GlobalVariables.mario1.stream()));
//                    Leanplum.track("Image displayed!");
//                }
            }
        });


    }
}

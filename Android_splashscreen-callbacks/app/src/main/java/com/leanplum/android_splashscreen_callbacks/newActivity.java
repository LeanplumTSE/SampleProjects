package com.leanplum.android_splashscreen_callbacks;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.leanplum.Leanplum;
import com.leanplum.callbacks.VariablesChangedCallback;

/**
 * Created by fede on 10/26/16.
 */

public class newActivity extends Activity {

    public void forceContentUpdate(View view){
        Log.i("### ", "Forcing content update");
        Leanplum.forceContentUpdate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newactivity);

        Leanplum.addVariablesChangedAndNoDownloadsPendingHandler(new VariablesChangedCallback() {
            @Override
            public void variablesChanged() {
                Log.i("### ", "newActivity - All variables changed and no download is pending");
                Log.i("### ", "newActivity  - All variables changed and no download is pending -- " + GlobalVariables.String_Welcome1);

//                if (imageView != null) {
//                    imageView.setImageBitmap(BitmapFactory.decodeStream(GlobalVariables.mario1.stream()));
//                    Leanplum.track("Image displayed!");
//                }
            }
        });

        Leanplum.addOnceVariablesChangedAndNoDownloadsPendingHandler(new VariablesChangedCallback() {
            @Override
            public void variablesChanged() {
                Log.i("### ", "newActivity ONCE - All variables changed and no download is pending");
                Log.i("### ", "newActivity ONCE - All variables changed and no download is pending -- " + GlobalVariables.String_Welcome1);

//                if (imageView != null) {
//                    imageView.setImageBitmap(BitmapFactory.decodeStream(GlobalVariables.mario1.stream()));
//                    Leanplum.track("Image displayed!");
//                }
            }
        });

    }
}

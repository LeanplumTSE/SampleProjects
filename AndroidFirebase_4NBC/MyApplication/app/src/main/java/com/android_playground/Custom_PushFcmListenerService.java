package com.android_playground;
import android.util.Log;
import com.leanplum.LeanplumPushFcmListenerService;

/**
 * Created by fede on 11/22/17.
 */

public class Custom_PushFcmListenerService extends LeanplumPushFcmListenerService {

    @Override
    public void onTokenRefresh() {

        super.onTokenRefresh();

        Log.i("### ", "this is also called");

    }
}

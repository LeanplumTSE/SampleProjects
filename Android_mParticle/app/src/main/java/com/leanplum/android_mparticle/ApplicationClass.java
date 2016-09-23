package com.leanplum.android_mparticle;

import android.app.Application;
import android.util.Log;

import com.leanplum.Leanplum;
import com.leanplum.callbacks.StartCallback;
import com.mparticle.MParticle;

/**
 * Created by fede on 9/19/16.
 */

public class ApplicationClass extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        MParticle.start(this, "04780a3595efa74fbe58dca6f7161f96", "lAYFYw6LEsj7ZJIizaxfvrLvHIOo9w-0NxopiUXIqc4Gh8J2b27ffzXDeRAAVi1T");
    }
}

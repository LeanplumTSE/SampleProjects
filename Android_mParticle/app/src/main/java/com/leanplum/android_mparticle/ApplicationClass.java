package com.leanplum.android_mparticle;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumPushNotificationCustomizer;
import com.leanplum.LeanplumPushService;
import com.leanplum.callbacks.StartCallback;
import com.mparticle.MParticle;

/**
 * Created by fede on 9/19/16.
 */

public class ApplicationClass extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        LeanplumPushService.setGcmSenderId(LeanplumPushService.LEANPLUM_SENDER_ID);

        LeanplumPushService.setCustomizer(new LeanplumPushNotificationCustomizer() {
            //
            @Override
            public void customize(NotificationCompat.Builder builder, Bundle notificationPayload) {

                // Setting a custom smallIcon included in the Drawable folder
                builder.setSmallIcon(R.drawable.atest);

                // Setting a custom largeIcon included in the Drawable folder
                Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.androidorange);
                builder.setLargeIcon(largeIcon);

                // Setting a custom Big Picture included in the Drawable folder, beneath the notification
                Bitmap androidBanner = BitmapFactory.decodeResource(getResources(), R.drawable.androidappsdev);
                builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(androidBanner));

            }
        });

        MParticle.start(this, "04780a3595efa74fbe58dca6f7161f96", "lAYFYw6LEsj7ZJIizaxfvrLvHIOo9w-0NxopiUXIqc4Gh8J2b27ffzXDeRAAVi1T");




    }
}

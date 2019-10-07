package com.testing.sim;

import android.app.Notification;
import android.app.NotificationManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessageService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMessage";
    private static String token;
    private NotificationManager notificationManager;
    private Notification notification;



    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Log.d(TAG, "From: " + remoteMessage.getFrom());


        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, remoteMessage.toString());
            showNotification(remoteMessage.getNotification().getTitle(),remoteMessage.getNotification().getBody());
        }



    }

    public void showNotification(String title,String body)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            notification = new Notification.Builder(this,"test")
                    .setSmallIcon(R.drawable.store)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.store))
                    .setContentTitle(title)
                    .setContentText(body)
                    .setColor(Color.rgb(233,32,32))
                    .build();
        }
        else
        {
            notification = new Notification.Builder(this).setSmallIcon(R.drawable.store)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.store))
                .setContentTitle(title)
                .setContentText(body)
                .setColor(Color.rgb(233,32,32))
                .build();
        }
        notificationManager.notify(2,notification);
    }

    @Override
    public void onNewToken(@NonNull String s) {
        Log.d(TAG,s);
    }
}

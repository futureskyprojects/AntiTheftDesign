package com.blogspot.tndev1403.antitheft.Modal;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.blogspot.tndev1403.antitheft.R;
import com.blogspot.tndev1403.antitheft.Stored.Config.Config;
import com.blogspot.tndev1403.antitheft.View.Home;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.Timer;
import java.util.TimerTask;

public class AntitheftService extends Service {
    public final static String TAG = "Service";
    NotificationManager notificationManager;
    Notification notification;
    Notification.Builder notificationBuilder;
    PendingIntent pendingIntent;
    Intent mIntent;
    ValueEventListener valueEventListener;
    int perviousType = -1;

    public AntitheftService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Object value = new Object();
                try {
                    value = dataSnapshot.getValue();
                    doUpdate(value);
                } catch (Exception e) {
                    Log.e(TAG, "onDataChange: " + e + " || " + value);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                warningNotification();
            }
        };
        this.mIntent = intent;
        showNotification();
        return Service.START_REDELIVER_INTENT;
    }

    /* Capture function */
    void doUpdate(Object value) {
        int i = -1;
        while (i < 10) {
            i++;
            try {
                int type = Integer.parseInt("" + value);
                if (perviousType == type)
                    return;
                if (type == Config.SAFE_TYPE)
                    safeNotification();
                else if (type == Config.DANGER_TYPE)
                    dangerNotification();
                else {
                    warningNotification();

                }
                return;
            } catch (Exception e) {
                Log.e(TAG, "doUpdate: " + e);
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
    private void capture() {
        Firebase.databaseReference.addValueEventListener(valueEventListener);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Firebase.databaseReference.removeEventListener(valueEventListener);
        notification.flags = STOP_FOREGROUND_REMOVE;
        notification.contentIntent = null;
        notificationManager.cancelAll();
        notificationManager.notify(Config.NOTIFICATION_REQUEST_CODE, notification);
        notificationManager.cancelAll();
        stopForeground(true);
        this.stopSelf();
    }

    /* Notification setting */
    void safeNotification() {
        perviousType = Config.SAFE_TYPE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setColor(ContextCompat.getColor(AntitheftService.this,
                    R.color.safeSignal));
        }
        notificationBuilder.setContentTitle("YÊN TĨNH");
        notificationBuilder.setContentText(Home.SAFE_DESCRIPTION);
        notificationBuilder.setProgress(0, 0, false);
        notificationManager.cancelAll();
        notificationManager.notify(Config.NOTIFICATION_REQUEST_CODE, notification);
    }

    void warningNotification() {
        perviousType = Config.WARNING_TYPE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setColor(ContextCompat.getColor(AntitheftService.this,
                    R.color.warningSignal));
        }
        notificationBuilder.setContentTitle("MẤT KẾT NỐI");
        notificationBuilder.setContentText(Home.WARNING_DESCRIPTION);
        notificationBuilder.setProgress(100, 30, true);
        notificationManager.cancelAll();
        notificationManager.notify(Config.NOTIFICATION_REQUEST_CODE, notification);
    }

    void dangerNotification() {
        perviousType = Config.DANGER_TYPE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setColor(ContextCompat.getColor(AntitheftService.this,
                    R.color.dangerSignal));
        }
        notificationBuilder.setContentTitle("BÁO ĐỘNG");
        notificationBuilder.setContentText(Home.DANGER_DESCRIPTION);
        notificationBuilder.setProgress(0, 0, false);
        notificationManager.cancelAll();
        notificationManager.notify(Config.NOTIFICATION_REQUEST_CODE, notification);

        try {
            if (!Home.acitivyState)
                pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("NewApi")
    void showNotification() {
        Intent notificationIntent = new Intent(AntitheftService.this, Home.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        pendingIntent = PendingIntent.getActivity(AntitheftService.this, Config.NOTIFICATION_REQUEST_CODE,
                notificationIntent, 0);
        /* Create notification builder */
        notificationBuilder = new Notification.Builder(AntitheftService.this);
        /* Apply config */
        notification = notificationBuilder.build();
        notification.icon = R.drawable.app_icon; // icon for services
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        notification.contentIntent = pendingIntent;
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        capture();
    }


    public boolean isInternetAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;
    }

    void TimerCheckInternet() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                // if not connect to the network, set warning
                if (!isInternetAvailable()) {
                    warningNotification();
                } else {
                    capture();
                }
            }
        }, 200, 300);
    }

    /* Now i won't use BoundServices */
    @Override
    public IBinder onBind(Intent intent) {
        // Don't use it
        return null;
    }
}

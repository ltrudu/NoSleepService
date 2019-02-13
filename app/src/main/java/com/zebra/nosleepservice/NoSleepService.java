package com.zebra.nosleepservice;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;

public class NoSleepService extends Service {
    private static final String TAG = "BacklightService";
    private static final int SERVICE_ID = 1;

    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;

    private PowerManager.WakeLock mWakeLock = null;

    public NoSleepService() {
    }

    public IBinder onBind(Intent paramIntent)
    {
        return null;
    }

    public void onCreate()
    {
        this.mPowerManager = ((PowerManager)getSystemService(Context.POWER_SERVICE));
        this.mKeyguardManager = ((KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE));
        this.mNotificationManager = ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        startService(flags, startId);
        return Service.START_STICKY;
    }

    public void onDestroy()
    {
        stopService();
    }

    @SuppressLint({"Wakelock"})
    private void startService(int flags, int startId)
    {
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                mainActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Create the Foreground Service
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        mNotification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setTicker(getString(R.string.notification_tickle))
                .setPriority(PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent)
                .build();

        TaskStackBuilder localTaskStackBuilder = TaskStackBuilder.create(this);
        localTaskStackBuilder.addParentStack(MainActivity.class);
        localTaskStackBuilder.addNextIntent(mainActivityIntent);
        notificationBuilder.setContentIntent(localTaskStackBuilder.getPendingIntent(0, FLAG_UPDATE_CURRENT));

        // Start foreground service
        startForeground(SERVICE_ID, mNotification);

        // Release current wakelock if any
        if ((this.mWakeLock != null) && (this.mWakeLock.isHeld())) {
            this.mWakeLock.release();
        }

        // Acquire wakelock for service
        this.mWakeLock = this.mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP, "zebra:NoSleepService");
        this.mWakeLock.setReferenceCounted(false);
        this.mWakeLock.acquire();

        // Disable keyguard
        this.mKeyguardManager.newKeyguardLock("zebra:NoSleepService").disableKeyguard();
    }

    private void stopService()
    {
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
        }
        stopForeground(true);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        NotificationChannel channel = new NotificationChannel(getString(R.string.nosleepservice_channel_id), getString(R.string.nosleepservice_channel_name), NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return getString(R.string.nosleepservice_channel_id);
    }

    private void logD(String message)
    {
        Log.d(TAG, message);
    }
}

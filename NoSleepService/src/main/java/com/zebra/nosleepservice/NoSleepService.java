package com.zebra.nosleepservice;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.backup.FullBackupDataOutput;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;

public class NoSleepService extends Service {
    private static final int SERVICE_ID = 122315;

    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;

    private PowerManager.WakeLock mWakeLock = null;

    private static View mView = null;
    private static WindowManager mWindowManager = null;


    public NoSleepService() {
    }

    public IBinder onBind(Intent paramIntent)
    {
        return null;
    }

    public void onCreate()
    {
        logD("onCreate");
        this.mPowerManager = ((PowerManager)getSystemService(Context.POWER_SERVICE));
        this.mKeyguardManager = ((KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE));
        this.mNotificationManager = ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logD("onStartCommand");
        super.onStartCommand(intent, flags, startId);
        startService();
        return Service.START_STICKY;
    }

    public void onDestroy()
    {
        logD("onDestroy");
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
        }
        cleanupWindow(this);
        stopService();
    }

    @SuppressLint({"Wakelock"})
    private void startService()
    {
        logD("startService");
        try
        {
            Intent mainActivityIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    0,
                    mainActivityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // Create the Foreground Service
            String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(mNotificationManager) : "";

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
            mNotification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.no_sleep_service_notification_title))
                    .setContentText(getString(R.string.no_sleep_service_notification_text))
                    .setTicker(getString(R.string.no_sleep_service_notification_tickle))
                    .setPriority(PRIORITY_MIN)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setContentIntent(pendingIntent)
                    .build();

            TaskStackBuilder localTaskStackBuilder = TaskStackBuilder.create(this);
            localTaskStackBuilder.addParentStack(MainActivity.class);
            localTaskStackBuilder.addNextIntent(mainActivityIntent);
            notificationBuilder.setContentIntent(localTaskStackBuilder.getPendingIntent(0, FLAG_UPDATE_CURRENT));

            try{
                startForeground(SERVICE_ID, mNotification);

            }
            catch(Exception e)
            {
                Log.d("toto", e.getMessage());
            }
            // Start foreground service

            // Release current wakelock if any
            if ((this.mWakeLock != null) && (this.mWakeLock.isHeld())) {
                this.mWakeLock.release();
            }

            // Acquire wakelock for service
            this.mWakeLock = this.mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP , "zebra:NoSleepService");
            this.mWakeLock.setReferenceCounted(false);
            this.mWakeLock.acquire();

            // Disable keyguard
            this.mKeyguardManager.newKeyguardLock("zebra:NoSleepService").disableKeyguard();

            createOverlayWindowToForceScreenOn(this);
            logD("startService:Service started without error.");

            //mNotificationManager.notify(SERVICE_ID, mNotification );
        }
        catch(Exception e)
        {
            logD("startService:Error while starting service.");
            e.printStackTrace();
        }


    }

    private void stopService()
    {
        try
        {
            logD("stopService.");
            //if (this.mWakeLock != null) {
            //    this.mWakeLock.release();
            //}
            //cleanupWindow(this);
            stopForeground(true);
            logD("stopService:Service stopped without error.");
        }
        catch(Exception e)
        {
            logD("Error while stopping service.");
            e.printStackTrace();

        }

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
        Log.d(Constants.TAG, message);
    }

    public static void startService(Context context)
    {
        Intent myIntent = new Intent(context, NoSleepService.class);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            // Use start foreground service to prevent the runtime error:
            // "not allowed to start service intent app is in background"
            // to happen when running on OS >= Oreo
            context.startForegroundService(myIntent);
        }
        else
        {
            context.startService(myIntent);
        }
    }

    public static void stopService(Context context)
    {
        Intent myIntent = new Intent(context, NoSleepService.class);
        context.stopService(myIntent);
    }

    public static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (NoSleepService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }



    private static boolean createOverlayWindowToForceScreenOn(Context context) {
        try
        {
            // We save the current state of mView
            // If a view is already existing we wants to remove it correctly.
            View saveView = mView;

            // Retrieve the window service
            if(mWindowManager == null)
                mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);


            // Create a new View for our layout
            mView = new View(context);

            // We create a new layout with the following parameters
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();

            // The smallest is the stealthiest
            layoutParams.width = 0;
            layoutParams.height = 0;

            // Transparency is a plus... for a zero sized layout
            layoutParams.format = PixelFormat.TRANSPARENT;
            layoutParams.alpha = 0f;

            // We force the window to be not focusable and not touchable to avoid
            // disruptions with the other apps and the launcher
            // In case of someone would manage to "touch" this zero sized sub pixel
            layoutParams.flags =
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

            // The type toast will be accepted by the system without specific permissions
            int windowType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
            layoutParams.type = windowType;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;



            mWindowManager.addView(mView, layoutParams);
            mView.setVisibility(View.VISIBLE);

            if(saveView != null)
            {
                saveView.setVisibility(View.GONE);
                mWindowManager.removeView(saveView);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void cleanupWindow(Context context) {
        // Revert to portray mode before cleaning up things
        createOverlayWindowToForceScreenOn(context);

        if(mView != null)
        {
            mWindowManager.removeView(mView);
            mView = null;
        }
        if(mWindowManager != null)
        {
            mWindowManager = null;
        }
    }

}

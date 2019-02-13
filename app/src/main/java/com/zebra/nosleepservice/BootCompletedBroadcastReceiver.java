package com.zebra.nosleepservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootCompletedBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(MainActivity.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean startService = sharedpreferences.getBoolean("StartService", false);
        if(startService)
        {
            Intent myIntent = new Intent(context, NoSleepService.class);
            context.startService(myIntent);
        }
    }
}

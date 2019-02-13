package com.zebra.nosleepservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class StopServiceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Stop service
        Intent myIntent = new Intent(context, NoSleepService.class);
        context.stopService(myIntent);
        // Setup shared preferences for next reboot
        SharedPreferences sharedpreferences = context.getSharedPreferences(MainActivity.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean("StartService", false);
        editor.commit();
        if(MainActivity.mMainActivity != null) // The application default activity has been opened
        {
            MainActivity.mMainActivity.setStartStopServiceSwitchValues(false, MainActivity.mMainActivity.getString(R.string.startService));
        }
    }
}

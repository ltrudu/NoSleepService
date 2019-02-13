package com.zebra.nosleepservice;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

// The service can be launched using the graphical user interface.
//
// If the device is rebooted while the service was started, it will
// be restarted automatically once the reboot is completed.
//
// The service respond to two intent actions (both uses the category: android.intent.category.DEFAULT)
// - com.zebra.nosleepservice.startservice :
//   Start the service.
//   If the device get rebooted the service will start automatically once the reboot is completed.
// - com.zebra.nosleepservice.stopservice
//   Stop the service.
//   If the device is rebooted, the service will not be started.
//
// The service can be started and stopped manually using the following adb commands:
//  - Start service:
//      adb shell am broadcast -a com.zebra.nosleepservice.startservice
//  - Stop service:
//      adb shell am broadcast -a com.zebra.nosleepservice.stopservice
public class MainActivity extends AppCompatActivity {

    public static final String SHARED_PREFERENCES_NAME = "NoSleepService";
    private Switch mStartStopServiceSwitch = null;
    public static MainActivity mMainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((Button)findViewById(R.id.btLicense)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ltrudu/NoSleepService/blob/master/README.md"));
                startActivity(browserIntent);
            }
        });

        mStartStopServiceSwitch = (Switch)findViewById(R.id.startStopServiceSwitch);
        mStartStopServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    mStartStopServiceSwitch.setText(getString(R.string.stopService));
                    startService();
                }
                else
                {
                    mStartStopServiceSwitch.setText(getString(R.string.startService));
                    stopService();
                }
                SharedPreferences sharedpreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean("StartService", isChecked);
                editor.commit();
            }
        });
    }

    @Override
    protected void onResume() {
        mMainActivity = this;
        super.onResume();
        SharedPreferences sharedpreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean startService = sharedpreferences.getBoolean("StartService", false);
        if(isMyServiceRunning(NoSleepService.class) || startService)
        {
            setStartStopServiceSwitchValues(true, getString(R.string.stopService));
            if(startService)
                startService();
        }
        else
        {
            setStartStopServiceSwitchValues(false, getString(R.string.startService));
        }
    }

    @Override
    protected void onPause() {
        mMainActivity = null;
        super.onPause();
    }

    public void setStartStopServiceSwitchValues(final boolean checked, final String text)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStartStopServiceSwitch.setChecked(checked);
                mStartStopServiceSwitch.setText(text);
            }
        });
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startService()
    {
        Intent myIntent = new Intent(this, NoSleepService.class);
        this.startService(myIntent);
    }

    private void stopService()
    {
        Intent myIntent = new Intent(this, NoSleepService.class);
        this.stopService(myIntent);
    }
}

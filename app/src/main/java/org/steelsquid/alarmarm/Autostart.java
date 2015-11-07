package org.steelsquid.alarmarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Execute when phone starts
 */
public class Autostart extends BroadcastReceiver {
    public void onReceive(Context arg0, Intent arg1) {
        Intent intent = new Intent(arg0, BackgroundService.class);
        arg0.startService(intent);
    }
}
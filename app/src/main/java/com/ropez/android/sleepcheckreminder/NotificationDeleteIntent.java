package com.ropez.android.sleepcheckreminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationDeleteIntent extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    	Log.i("NotificationDeleteIntent.onReceive");
    	SoundPlayerService.runAction(context, "stop");
    }
}

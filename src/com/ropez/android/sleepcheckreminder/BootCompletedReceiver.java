package com.ropez.android.sleepcheckreminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver 
{
	static final String LOGTAG = "SCR";
	
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		Log.i(LOGTAG, "BootCompletedReceiver.onReceive: Setting alarm");
		
		AlarmConfiguration alarmConfiguration = new AlarmConfiguration(context);
		String alarmMessage = alarmConfiguration.setAlarm();

		Log.i(LOGTAG, "BootCompletedReceiver.onReceive: " + alarmMessage);
	}
}

package com.ropez.android.sleepcheckreminder;

import com.ropez.android.sleepcheckreminder.AlarmConfiguration.NotificationMode;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver 
{
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		Log.i("AlarmReceiver.onReceive: Setting alarm");
		
		AlarmConfiguration alarmConfiguration = new AlarmConfiguration(context);
		String alarmMessage = alarmConfiguration.setAlarm();

		Log.i("AlarmReceiver.onReceive: " + alarmMessage);

		if (alarmConfiguration.isNotificationOn) {
			Log.i("AlarmReceiver.onReceive: Creating notification");
			
			NotificationManager manger = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
			Notification notification = new Notification(R.drawable.icon, "Do I sleep?", System.currentTimeMillis());
			
			Intent contentIntent = new Intent(context, MainScreen.class);
			contentIntent.putExtra("notification", true);
			PendingIntent contentPendingIntent = PendingIntent.getActivity(context, 0, contentIntent, 0);
			
			Intent deleteIntent = new Intent(context, NotificationDeleteIntent.class);
			deleteIntent.putExtra("notification", true);
			PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, 0);
			
			notification.setLatestEventInfo(context, "Sleep Check Reminder", "Check if you are sleeping now." , contentPendingIntent);
			notification.deleteIntent = deletePendingIntent;
			
			notification.flags = Notification.FLAG_AUTO_CANCEL;
            notification.defaults |= Notification.DEFAULT_LIGHTS;

            if (alarmConfiguration.isVibrationOn)
                notification.defaults |= Notification.DEFAULT_VIBRATE;

            if (alarmConfiguration.notificationMode == NotificationMode.SystemDefault)
                notification.defaults |= Notification.DEFAULT_SOUND;
            else
                notification.flags |= Notification.FLAG_INSISTENT;

            Log.i("AlarmReceiver.onReceive: notificationMode: " + alarmConfiguration.notificationMode);

            manger.notify(0, notification);
			Log.i("AlarmReceiver.onReceive: Notification created");

            Log.i("AlarmReceiver.onReceive: soundResourceName: " + alarmConfiguration.soundResourceName);

            if (alarmConfiguration.notificationMode != NotificationMode.SystemDefault &&
					alarmConfiguration.soundResourceName != null)
				SoundPlayerService.runAction(context, "start");
		}
	}
}

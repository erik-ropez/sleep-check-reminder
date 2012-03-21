package com.ropez.android.sleepcheckreminder;

import java.util.Date;

import com.hlidskialf.android.preference.SeekBarPreference;
import com.ropez.android.sleepcheckreminder.R;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.widget.ArrayAdapter;

public class AlarmConfiguration {
	
	static final String LOGTAG = "SCR";
	
	enum NotificationMode {
		DefaultNotification(0),
		BackgroundSound(1),
		AlertMode(2);
		
		private int mode;
		
		NotificationMode(int mode) {
			this.mode = mode;
		}
		
		int getMode() {
			return this.mode;
		}
		
		public static NotificationMode parse(int mode) {
			switch (mode) {
				case 0: return DefaultNotification;
				case 1: return BackgroundSound;
				case 2: return AlertMode;
				default: throw new IllegalArgumentException("Unsupported mode.");
			}
		}
	}
	
	enum RepeatMode {
		Random(0),
		Fixed(1);
		
		private int mode;
		
		RepeatMode(int mode) {
			this.mode = mode;
		}
		
		int getMode() {
			return this.mode;
		}
		
		public static RepeatMode parse(int mode) {
			switch (mode) {
				case 0: return Random;
				case 1: return Fixed;
				default: throw new IllegalArgumentException("Unsupported mode.");
			}
		}
	}
	
	public boolean isNotificationOn;
	public NotificationMode notificationMode;
	public String backgroundSoundResourceName;
	public int volume;
	public Uri alarmSoundUri;
	public Date timeFrom;
	public Date timeTo;
	public RepeatMode repeatMode;
	public int periodLength;
	public int timesPerDay;

	public long nextAlarm;
	public int helpTextId;
	
	private Context mContext;

	public AlarmConfiguration(Context context)
	{
		this.mContext = context;
	
		SharedPreferences settings = getSharedPreferences();
		
		this.isNotificationOn = settings.getBoolean("notification_on", false);
		
		this.notificationMode = NotificationMode.parse(Integer.parseInt(settings.getString("notification_mode", "0")));

		if (settings.contains("background_sound"))
			this.backgroundSoundResourceName = settings.getString("background_sound", "");
		else 
			this.backgroundSoundResourceName = null;
		
		this.volume = settings.getInt("volume_slider", 50);
		
		if (settings.contains("alarm"))
			this.alarmSoundUri = Uri.parse(settings.getString("alarm", ""));
		else 
			this.alarmSoundUri = null;

		this.timeFrom = getTimeFromPreference(settings, "start_at_timepicker", "0:00");
		this.timeTo = getTimeFromPreference(settings, "finish_at_timepicker", "0:00");
		this.repeatMode = RepeatMode.parse(Integer.parseInt(settings.getString("repeat_mode", "0")));
		this.timesPerDay = settings.getInt("times_per_day_slider", 5);
		this.periodLength = settings.getInt("period_length_slider", 45);
	
		this.nextAlarm = settings.getLong("nextAlarm", 0);
		this.helpTextId = settings.getInt("helpTextV4Id", 0);
	}

	private SharedPreferences getSharedPreferences() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.mContext);
		return settings;
	}

	private Date getTimeFromPreference(SharedPreferences settings, String key, String defaultValue) {
		String timeFromString = settings.getString(key, defaultValue);
		String[] timeFromParts = timeFromString.split(":");
		int timeFromHour = Integer.valueOf(timeFromParts[0]);
		int timeFromMinute = Integer.valueOf(timeFromParts[1]);
		return new Date(0, 0, 0, timeFromHour, timeFromMinute);
	}
	
	public void writePreferences() {
		SharedPreferences settings = getSharedPreferences();
		SharedPreferences.Editor editor = settings.edit();
		
		editor.putLong("nextAlarm", this.nextAlarm);
		editor.putInt("helpTextV4Id", this.helpTextId);
		
		editor.commit();
	}
	
	public String setAlarm() {
		AlarmManager alarmManager = (AlarmManager)this.mContext.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(this.mContext, AlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		if (!this.isNotificationOn) {
			alarmManager.cancel(pendingIntent);
			return "Alarm not set";
		}
		
		Date nextAlarm = getTimeForNextAlarm();
		
		alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarm.getTime(), pendingIntent);
		
		return "Alarm set for " + nextAlarm.toString();
	}
	
	private Date getTimeForNextAlarm() {
		Date nowDate = new Date();
		int from  = this.timeFrom.getMinutes() + this.timeFrom.getHours() * 60;
		int to  = this.timeTo.getMinutes() + this.timeTo.getHours() * 60;
		int now = nowDate.getHours() * 60 + nowDate.getMinutes();
		int oneDay = 24 * 60;
		
		if (from >= to) {
			if (now < to)
				from -= oneDay;
			else
				to += oneDay;
		}
		
		int fixedOffset = 0;
		int randomOffset = 0;
		
		if (this.repeatMode == RepeatMode.Random)
			randomOffset = (int)Math.round(((to - from) / this.timesPerDay) * Math.random() * 2) + 1;
		else
			if (this.repeatMode == RepeatMode.Fixed)
				fixedOffset = ((now - from) / this.periodLength + 1) * this.periodLength - now + from;
			else
				throw new IllegalArgumentException("Unsupported mode.");
		
		int nextAlarm = getNextAlarm(now, from, to, oneDay, fixedOffset, randomOffset);
		
		return new Date(nowDate.getYear(), nowDate.getMonth(), nowDate.getDate(), nextAlarm / 60, nextAlarm % 60);
	}

	private int getNextAlarm(int now, int from, int to, int oneDay, int fixedOffset, int randomOffset) {
		int nextAlarm;
		if (now < from)
			nextAlarm = from + randomOffset;
		else
			if (now < to) {
				nextAlarm = now + fixedOffset + randomOffset;
				if (nextAlarm > to)
					nextAlarm = from + oneDay + randomOffset;
			} else
				nextAlarm = from + oneDay + randomOffset;
		return nextAlarm;
	}
}

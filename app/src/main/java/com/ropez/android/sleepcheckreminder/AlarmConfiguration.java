package com.ropez.android.sleepcheckreminder;

import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

public class AlarmConfiguration {
	
	enum NotificationMode {
        SystemDefault(0),
        SystemAlarmSounds(1),
        BuiltInSounds(2),
        CustomSound(3);

		private int mode;
		
		NotificationMode(int mode) {
			this.mode = mode;
		}
		
		int getMode() {
			return this.mode;
		}
		
		public static NotificationMode parse(int mode) {
			switch (mode) {
				case 0: return SystemDefault;
				case 1: return SystemAlarmSounds;
				case 2: return BuiltInSounds;
                case 3: return CustomSound;
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
	public String soundResourceName;
	public int volume;
	public Date timeFrom;
	public Date timeTo;
	public RepeatMode repeatMode;
	public int periodLength;
	public int timesPerDay;
    public boolean isVibrationOn;
    public boolean isRepeatSoundOn;
    public boolean isFadeInOutOn;
    public int maximumPlayTime;

	public long nextAlarm;
	public int helpTextId;
	
	private Context mContext;

	public AlarmConfiguration(Context context)
	{
		this.mContext = context;
	
		SharedPreferences settings = getSharedPreferences();
		
		this.isNotificationOn = settings.getBoolean("notification_on", false);
		
		this.notificationMode = NotificationMode.parse(Integer.parseInt(settings.getString("sound_source", "0")));

        switch (this.notificationMode) {
            case SystemAlarmSounds:
                if (settings.contains("alarm_sound"))
                    this.soundResourceName = settings.getString("alarm_sound", "");
                break;
            case BuiltInSounds:
                if (settings.contains("in_built_sound"))
                    this.soundResourceName = settings.getString("in_built_sound", "");
                break;
            case CustomSound:
                if (settings.contains("custom_sound_picker"))
                    this.soundResourceName = settings.getString("custom_sound_picker", "");
                break;
        }

		this.volume = settings.getInt("volume_slider", 50);

		this.timeFrom = getTimeFromPreference(settings, "start_at_timepicker", "0:00");
		this.timeTo = getTimeFromPreference(settings, "finish_at_timepicker", "0:00");
		this.repeatMode = RepeatMode.parse(Integer.parseInt(settings.getString("repeat_mode", "0")));
		this.timesPerDay = settings.getInt("times_per_day_slider", 5);
		this.periodLength = settings.getInt("period_length_slider", 45);
	
		this.nextAlarm = settings.getLong("nextAlarm", 0);
		this.helpTextId = settings.getInt("helpTextV4Id", 0);

        this.isVibrationOn = settings.getBoolean("vibration_on", false);
        this.isRepeatSoundOn = settings.getBoolean("repeat_sound_on", false);
        this.isFadeInOutOn = settings.getBoolean("fade_in_out_on", false);
        this.maximumPlayTime = settings.getInt("maximum_play_time", 30);
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

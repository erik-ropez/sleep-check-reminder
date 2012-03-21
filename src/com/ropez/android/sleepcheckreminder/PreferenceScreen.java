package com.ropez.android.sleepcheckreminder;

import java.util.HashMap;
import com.hlidskialf.android.preference.SeekBarPreference;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.RingtonePreference;
import android.preference.TimePickerPreference;
import android.util.Log;

public class PreferenceScreen extends PreferenceActivity {
	
	static final String LOGTAG = "SCR";
	
	CheckBoxPreference mNotificationStatus;
	PreferenceCategory mNotificationCategory;
    ListPreference mNotificationMode;
    ListPreference mBackgroundSound;
    SeekBarPreference mVolume;
    RingtonePreference mAlarm;
    PreferenceCategory mTimingCategory;
    TimePickerPreference mStartAt;
    TimePickerPreference mFinishAt;
	ListPreference mRepeatMode;
	SeekBarPreference mTimesPerDay;
    SeekBarPreference mPeriodLength;    
    
    HashMap<String, String> mPreferenceValues;
	
    private void updateConfiguration(Preference preference, Object newValue) {
		this.mPreferenceValues.put(preference.getKey(), newValue.toString());
	}
    
    private OnPreferenceChangeListener genericOnPreferenceChangeListener = new OnPreferenceChangeListener() {
    	public boolean onPreferenceChange(Preference preference, Object newValue) {
    		if (newValue != null) {
    			updateConfiguration(preference, newValue);
    			updateDisplay();
    		}
            return true;
        }
    };

    private OnPreferenceChangeListener backgroundSoundOnPreferenceChangeListener = new OnPreferenceChangeListener() {
    	public boolean onPreferenceChange(Preference preference, Object newValue) {
    		if (newValue != null) {
    			updateConfiguration(preference, newValue);
    			updateDisplay();
	    		
    			int soundResourceId = getResources().getIdentifier(newValue.toString(), "raw", getPackageName());
	    		playSound(preference.getContext(), soundResourceId, mVolume.getProgress() / 100.0f);
    		}
            return true;
        }
    };
    
    private OnPreferenceChangeListener volumeOnPreferenceChangeListener = new OnPreferenceChangeListener() {
    	public boolean onPreferenceChange(Preference preference, Object newValue) {
    		if (newValue != null) {
    			updateConfiguration(preference, newValue);
    			updateDisplay();
	    		
    			String backgroundSound = mBackgroundSound.getValue();
    			if (backgroundSound != null) {
    				int soundResourceId = getResources().getIdentifier(backgroundSound, "raw", getPackageName());
    				int volume = Integer.parseInt(newValue.toString());
    				playSound(preference.getContext(), soundResourceId, volume / 100.0f);
    			}
    		}
            return true;
        }
    };
    
    protected void onSaveInstanceState(Bundle outState) {
    	setAlarm();
    }
    
    @Override
    protected void onPause() {
    	setAlarm();
    	super.onPause();
    }

	private void setAlarm() {
		AlarmConfiguration alarmConfiguration = new AlarmConfiguration(this);
		String alarmMessage = alarmConfiguration.setAlarm();
		alarmConfiguration.writePreferences();
		Log.i(LOGTAG, "PreferenceScreen.onSaveInstanceState: " + alarmMessage);
	}
    
	private void playSound(Context context, int soundResourceId, float volume) {
		MediaPlayer mp = MediaPlayer.create(context, soundResourceId);
		mp.setVolume(volume, volume);
	    mp.start();
	}
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        
        this.mPreferenceValues = new HashMap<String, String>();
        
        this.mNotificationStatus = (CheckBoxPreference)findPreference("notification_on"); 
        this.mNotificationCategory = (PreferenceCategory)getPreferenceScreen().findPreference("notification");
        this.mNotificationMode = (ListPreference)findPreference("notification_mode");
        this.mBackgroundSound = (ListPreference)findPreference("background_sound");
        this.mVolume = (SeekBarPreference)findPreference("volume_slider");
        this.mAlarm = (RingtonePreference)findPreference("alarm");
        this.mTimingCategory = (PreferenceCategory)getPreferenceScreen().findPreference("timing");
        this.mStartAt = (TimePickerPreference)findPreference("start_at_timepicker");
        this.mFinishAt = (TimePickerPreference)findPreference("finish_at_timepicker");
        this.mRepeatMode = (ListPreference)findPreference("repeat_mode");
        this.mTimesPerDay = (SeekBarPreference)findPreference("times_per_day_slider");
        this.mPeriodLength = (SeekBarPreference)findPreference("period_length_slider");
        
        this.mNotificationStatus.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mNotificationMode.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mBackgroundSound.setOnPreferenceChangeListener(backgroundSoundOnPreferenceChangeListener);
        this.mVolume.setOnPreferenceChangeListener(volumeOnPreferenceChangeListener);
        this.mAlarm.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mStartAt.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mFinishAt.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mRepeatMode.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mTimesPerDay.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mPeriodLength.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        
        this.mPreferenceValues.put(this.mNotificationStatus.getKey(), Boolean.toString(this.mNotificationStatus.isChecked()));
        this.mPreferenceValues.put(this.mNotificationMode.getKey(), this.mNotificationMode.getValue());
        this.mPreferenceValues.put(this.mBackgroundSound.getKey(), this.mBackgroundSound.getValue());
        this.mPreferenceValues.put(this.mVolume.getKey(), Integer.toString(this.mVolume.getProgress()));
        SharedPreferences sharedPreferences = this.mAlarm.getPreferenceManager().getSharedPreferences();
        this.mPreferenceValues.put(this.mAlarm.getKey(), sharedPreferences.getString(this.mAlarm.getKey(), null));
        this.mPreferenceValues.put(this.mStartAt.getKey(), this.mStartAt.getValue());
        this.mPreferenceValues.put(this.mFinishAt.getKey(), this.mFinishAt.getValue());
        this.mPreferenceValues.put(this.mRepeatMode.getKey(), this.mRepeatMode.getValue());
        this.mPreferenceValues.put(this.mTimesPerDay.getKey(), Integer.toString(this.mTimesPerDay.getProgress()));
        this.mPreferenceValues.put(this.mPeriodLength.getKey(), Integer.toString(this.mPeriodLength.getProgress()));
        
        updateDisplay();
    }
    
	private void updateDisplay() {
		boolean notificationStatus = Boolean.parseBoolean(this.mPreferenceValues.get(this.mNotificationStatus.getKey()));

		if (notificationStatus) {
			this.mStartAt.setEnabled(true);
			this.mFinishAt.setEnabled(true);
		} else {
			this.mStartAt.setEnabled(false);
			this.mFinishAt.setEnabled(false);
		}
		
		int notificationMode = Integer.parseInt(this.mPreferenceValues.get(this.mNotificationMode.getKey()));
		this.mNotificationMode.setSummary(getResources().getStringArray(R.array.notification_mode_entries)[notificationMode]);
		
		if (notificationMode == 0) { // Notification
			this.mBackgroundSound.setEnabled(false);
			this.mAlarm.setEnabled(false);
			this.mVolume.setEnabled(false);
			//this.mNotificationCategory.removePreference(this.mBackgroundSound);
			//this.mNotificationCategory.removePreference(this.mAlarm);
			//this.mNotificationCategory.removePreference(this.mVolume);
		}

		if (notificationMode == 1) { // Background
			this.mBackgroundSound.setEnabled(true);
			this.mAlarm.setEnabled(false);
			this.mVolume.setEnabled(true);
			//this.mNotificationCategory.removePreference(this.mAlarm);
			//this.mNotificationCategory.addPreference(this.mBackgroundSound);
			//this.mNotificationCategory.addPreference(this.mVolume);
		}

		if (notificationMode == 2) { // Alarm
			this.mBackgroundSound.setEnabled(false);
			this.mAlarm.setEnabled(true);
			this.mVolume.setEnabled(false);
			//this.mNotificationCategory.removePreference(this.mBackgroundSound);
			//this.mNotificationCategory.removePreference(this.mVolume);
			//this.mNotificationCategory.addPreference(this.mAlarm);
		}
		
		String backgroundSoundEntryValue = this.mPreferenceValues.get(this.mBackgroundSound.getKey());
		String backgroundSoundEntry = "Background sound not selected";
		if (backgroundSoundEntryValue != null) {
			String[] backgroundSoundEntryValues = getResources().getStringArray(R.array.background_sound_entryvalues);
			int backgroundSoundEntryIndex = -1;
			for (int i = 0; i < backgroundSoundEntryValues.length; i++)
				if (backgroundSoundEntryValues[i].compareTo(backgroundSoundEntryValue) == 0)
					backgroundSoundEntryIndex = i;
			if (backgroundSoundEntryIndex >= 0)
				backgroundSoundEntry = getResources().getStringArray(R.array.background_sound_entries)[backgroundSoundEntryIndex];
		}
		this.mBackgroundSound.setSummary(backgroundSoundEntry);
		
		this.mVolume.setSummary(this.mPreferenceValues.get(this.mVolume.getKey()) + "%");
		
		String alarmUriString = this.mPreferenceValues.get(this.mAlarm.getKey());
		String alarmTitle = "Alarm sound not selected";
		if (alarmUriString != null) {
			Uri alarmUri = Uri.parse(this.mPreferenceValues.get(this.mAlarm.getKey()));
			alarmTitle = RingtoneManager.getRingtone(this, alarmUri).getTitle(this);
		}
		this.mAlarm.setSummary(alarmTitle);
		
		int repeatMode = Integer.parseInt(this.mPreferenceValues.get(this.mRepeatMode.getKey()));
		
		this.mRepeatMode.setSummary(getResources().getStringArray(R.array.repeat_mode_entries)[repeatMode]);
		this.mTimesPerDay.setSummary(this.mPreferenceValues.get(this.mTimesPerDay.getKey()) + " times");
		this.mPeriodLength.setSummary(this.mPreferenceValues.get(this.mPeriodLength.getKey()) + " minutes");
		this.mStartAt.setSummary(this.mPreferenceValues.get(this.mStartAt.getKey()));
		this.mFinishAt.setSummary(this.mPreferenceValues.get(this.mFinishAt.getKey()));

		if (repeatMode == 0) {
			this.mTimesPerDay.setEnabled(true);
			this.mPeriodLength.setEnabled(false);
			//this.mTimingCategory.removePreference(this.mPeriodLength);
			//this.mTimingCategory.addPreference(this.mTimesPerDay);
		}
		if (repeatMode == 1) {
			this.mTimesPerDay.setEnabled(false);
			this.mPeriodLength.setEnabled(true);
			//this.mTimingCategory.removePreference(this.mTimesPerDay);
			//this.mTimingCategory.addPreference(this.mPeriodLength);
		}

	}
}

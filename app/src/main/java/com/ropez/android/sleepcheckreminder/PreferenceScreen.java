package com.ropez.android.sleepcheckreminder;

import java.util.HashMap;
import com.hlidskialf.android.preference.SeekBarPreference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.RingtonePreference;
import android.preference.TimePickerPreference;
import android.content.Intent;
import com.ipaulpro.afilechooser.utils.FileUtils;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class PreferenceScreen extends PreferenceActivity {
	
	CheckBoxPreference mGlobalStatus;
	PreferenceCategory mNotificationCategory;
    ListPreference mSoundSource;
    ListPreference mInBuiltSound;
    SeekBarPreference mVolume;
    SeekBarPreference mMaximumPlayTime;
    Preference mSystemDefaultSound;
    RingtonePreference mAlarmSound;
    PreferenceCategory mTimingCategory;
    TimePickerPreference mStartAt;
    TimePickerPreference mFinishAt;
	ListPreference mRepeatMode;
	SeekBarPreference mTimesPerDay;
    SeekBarPreference mPeriodLength;
    CheckBoxPreference mVibrationOn;
    CheckBoxPreference mRepeatSoundOn;
    CheckBoxPreference mFadeInOutOn;
    Preference mCustomSound;
    MediaPlayer mMediaPlayer;
    Timer mMediaPlayerTimer;

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

                int volume = Integer.parseInt(newValue.toString());

                AlarmConfiguration.NotificationMode notificationMode =
                    AlarmConfiguration.NotificationMode.parse(Integer.parseInt(mSoundSource.getValue()));

                if (notificationMode == AlarmConfiguration.NotificationMode.BuiltInSounds) {
                    String inBuiltSound = mInBuiltSound.getValue();
                    if (inBuiltSound != null) {
                        int soundResourceId = getResources().getIdentifier(inBuiltSound, "raw", getPackageName());
                        playSound(preference.getContext(), soundResourceId, volume / 100.0f);
                    }
                } else {
                    String soundUriKey = notificationMode == AlarmConfiguration.NotificationMode.SystemAlarmSounds ?
                            mAlarmSound.getKey() : mCustomSound.getKey();
                    String soundUriString = mPreferenceValues.get(soundUriKey);
                    if (soundUriString != null) {
                        Uri soundUri = Uri.parse(soundUriString);
                        playSound(preference.getContext(), soundUri, volume / 100.0f);
                    }
                }

                mMediaPlayerTimer.cancel();
                if (mMediaPlayerStopTimerTask != null)
                    mMediaPlayerStopTimerTask.cancel();
                mMediaPlayerStopTimerTask = new MediaPlayerStopTimerTask();
                mMediaPlayerTimer = new Timer();
                mMediaPlayerTimer.schedule(mMediaPlayerStopTimerTask, 3000);
    		}
            return true;
        }
    };

    MediaPlayerStopTimerTask mMediaPlayerStopTimerTask;

    class MediaPlayerStopTimerTask extends TimerTask {
        @Override
        public void run() {
            stopMediaPlayer();
        }
    }

    private static final int FILE_SELECT_CODE = 1234;

    private OnPreferenceClickListener customSoundPickerOnPreferenceClickListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent getContentIntent = FileUtils.createGetContentIntent();
            Intent intent = Intent.createChooser(getContentIntent, "Select a file");
            startActivityForResult(intent, FILE_SELECT_CODE);
            return true;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    final Uri uri = data.getData();
                    updateConfiguration(this.mCustomSound, uri.toString());

                    SharedPreferences preferences = this.mCustomSound.getPreferenceManager().getSharedPreferences();
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(this.mCustomSound.getKey(), uri.toString());
                    editor.commit();

                    updateDisplay();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

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
		Log.i("PreferenceScreen.onSaveInstanceState: " + alarmMessage);
	}
    
	private void playSound(Context context, int soundResourceId, float volume) {
        stopMediaPlayer();
        this.mMediaPlayer = MediaPlayer.create(context, soundResourceId);
        startMediaPlayer(volume);
	}

    private void playSound(Context context, Uri soundUri, float volume) {
        stopMediaPlayer();
        this.mMediaPlayer = MediaPlayer.create(context, soundUri);
        startMediaPlayer(volume);
    }

    private void startMediaPlayer(float volume) {
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.setVolume(volume, volume);
            this.mMediaPlayer.setLooping(false);
            this.mMediaPlayer.start();
        }
    }

    private void stopMediaPlayer() {
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.release();
            this.mMediaPlayer = null;
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        
        this.mPreferenceValues = new HashMap<String, String>();

        this.mGlobalStatus = (CheckBoxPreference)findPreference("notification_on");
        this.mVibrationOn = (CheckBoxPreference)findPreference("vibration_on");
        this.mRepeatSoundOn = (CheckBoxPreference)findPreference("repeat_sound_on");
        this.mFadeInOutOn = (CheckBoxPreference)findPreference("fade_in_out_on");
        this.mNotificationCategory = (PreferenceCategory)getPreferenceScreen().findPreference("notification");
        this.mSoundSource = (ListPreference)findPreference("sound_source");
        this.mInBuiltSound = (ListPreference)findPreference("in_built_sound");
        this.mVolume = (SeekBarPreference)findPreference("volume_slider");
        this.mMaximumPlayTime = (SeekBarPreference)findPreference("maximum_play_time");
        this.mAlarmSound = (RingtonePreference)findPreference("alarm_sound");
        this.mTimingCategory = (PreferenceCategory)getPreferenceScreen().findPreference("timing");
        this.mStartAt = (TimePickerPreference)findPreference("start_at_timepicker");
        this.mFinishAt = (TimePickerPreference)findPreference("finish_at_timepicker");
        this.mRepeatMode = (ListPreference)findPreference("repeat_mode");
        this.mTimesPerDay = (SeekBarPreference)findPreference("times_per_day_slider");
        this.mPeriodLength = (SeekBarPreference)findPreference("period_length_slider");
        this.mCustomSound = findPreference("custom_sound_picker");
        this.mSystemDefaultSound = findPreference("system_default_sound");
        this.mMediaPlayerTimer = new Timer();

        this.mGlobalStatus.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mVibrationOn.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mRepeatSoundOn.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mFadeInOutOn.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mSoundSource.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mInBuiltSound.setOnPreferenceChangeListener(backgroundSoundOnPreferenceChangeListener);
        this.mVolume.setOnPreferenceChangeListener(volumeOnPreferenceChangeListener);
        this.mMaximumPlayTime.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mAlarmSound.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mStartAt.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mFinishAt.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mRepeatMode.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mTimesPerDay.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mPeriodLength.setOnPreferenceChangeListener(genericOnPreferenceChangeListener);
        this.mCustomSound.setOnPreferenceClickListener(customSoundPickerOnPreferenceClickListener);

        this.mPreferenceValues.put(this.mGlobalStatus.getKey(), Boolean.toString(this.mGlobalStatus.isChecked()));
        this.mPreferenceValues.put(this.mVibrationOn.getKey(), Boolean.toString(this.mVibrationOn.isChecked()));
        this.mPreferenceValues.put(this.mRepeatSoundOn.getKey(), Boolean.toString(this.mRepeatSoundOn.isChecked()));
        this.mPreferenceValues.put(this.mFadeInOutOn.getKey(), Boolean.toString(this.mFadeInOutOn.isChecked()));
        this.mPreferenceValues.put(this.mSoundSource.getKey(), this.mSoundSource.getValue());
        this.mPreferenceValues.put(this.mInBuiltSound.getKey(), this.mInBuiltSound.getValue());
        SharedPreferences customSoundSharedPreferences = this.mCustomSound.getPreferenceManager().getSharedPreferences();
        this.mPreferenceValues.put(this.mCustomSound.getKey(), customSoundSharedPreferences.getString(this.mCustomSound.getKey(), null));
        this.mPreferenceValues.put(this.mVolume.getKey(), Integer.toString(this.mVolume.getProgress()));
        this.mPreferenceValues.put(this.mMaximumPlayTime.getKey(), Integer.toString(this.mMaximumPlayTime.getProgress()));
        SharedPreferences alarmSoundSharedPreferences = this.mAlarmSound.getPreferenceManager().getSharedPreferences();
        this.mPreferenceValues.put(this.mAlarmSound.getKey(), alarmSoundSharedPreferences.getString(this.mAlarmSound.getKey(), null));
        this.mPreferenceValues.put(this.mStartAt.getKey(), this.mStartAt.getValue());
        this.mPreferenceValues.put(this.mFinishAt.getKey(), this.mFinishAt.getValue());
        this.mPreferenceValues.put(this.mRepeatMode.getKey(), this.mRepeatMode.getValue());
        this.mPreferenceValues.put(this.mTimesPerDay.getKey(), Integer.toString(this.mTimesPerDay.getProgress()));
        this.mPreferenceValues.put(this.mPeriodLength.getKey(), Integer.toString(this.mPeriodLength.getProgress()));
        
        updateDisplay();
    }
    
	private void updateDisplay()
    {
        Resources resources = getResources();

		boolean notificationStatus = Boolean.parseBoolean(this.mPreferenceValues.get(this.mGlobalStatus.getKey()));

		if (notificationStatus) {
			this.mStartAt.setEnabled(true);
			this.mFinishAt.setEnabled(true);
		} else {
			this.mStartAt.setEnabled(false);
			this.mFinishAt.setEnabled(false);
		}
		
		int notificationMode = Integer.parseInt(this.mPreferenceValues.get(this.mSoundSource.getKey()));
		this.mSoundSource.setSummary(resources.getStringArray(R.array.sound_source_entries)[notificationMode]);

        Preference[] soundSelectors = {
            this.mSystemDefaultSound,
            this.mAlarmSound,
            this.mInBuiltSound,
            this.mCustomSound
        };
        for (int i = 0; i < soundSelectors.length; i++) {
            Preference preference = soundSelectors[i];
            if (i == notificationMode) {
                this.mNotificationCategory.addPreference(preference);
                preference.setEnabled(i > 0);
            } else {
                this.mNotificationCategory.removePreference(preference);
            }
        }

        if (notificationMode > 0) {
            this.mVolume.setEnabled(true);
            this.mRepeatSoundOn.setEnabled(true);
            boolean repeatSoundOn = Boolean.parseBoolean(this.mPreferenceValues.get(this.mRepeatSoundOn.getKey()));
            this.mFadeInOutOn.setEnabled(repeatSoundOn);
            this.mMaximumPlayTime.setEnabled(repeatSoundOn);
        } else {
            this.mVolume.setEnabled(false);
            this.mRepeatSoundOn.setEnabled(false);
            this.mFadeInOutOn.setEnabled(false);
            this.mMaximumPlayTime.setEnabled(false);
        }

		String backgroundSoundEntryValue = this.mPreferenceValues.get(this.mInBuiltSound.getKey());
		String backgroundSoundEntry = resources.getString(R.string.sound_not_selected_summary);
		if (backgroundSoundEntryValue != null) {
			String[] backgroundSoundEntryValues = resources.getStringArray(R.array.in_built_sound_entry_values);
			int backgroundSoundEntryIndex = -1;
			for (int i = 0; i < backgroundSoundEntryValues.length; i++)
				if (backgroundSoundEntryValues[i].compareTo(backgroundSoundEntryValue) == 0)
					backgroundSoundEntryIndex = i;
			if (backgroundSoundEntryIndex >= 0)
				backgroundSoundEntry = resources.getStringArray(R.array.in_built_sound_entries)[backgroundSoundEntryIndex];
		}
		this.mInBuiltSound.setSummary(backgroundSoundEntry);

        String customSoundEntryValue = this.mPreferenceValues.get(this.mCustomSound.getKey());
        String customSoundEntry = resources.getString(R.string.sound_not_selected_summary);
        if (customSoundEntryValue != null) {
            Uri customSoundUri = Uri.parse(customSoundEntryValue);
            String path = FileUtils.getPath(this, customSoundUri);
            if (path != null && FileUtils.isLocal(path)) {
                File customSoundFile = new File(path);
                customSoundEntry = customSoundFile.getName();
            }
        }
        this.mCustomSound.setSummary(customSoundEntry);

		this.mVolume.setSummary(this.mPreferenceValues.get(this.mVolume.getKey()) + "%");
        this.mMaximumPlayTime.setSummary(this.mPreferenceValues.get(this.mMaximumPlayTime.getKey()) + resources.getString(R.string._seconds));

		String alarmUriString = this.mPreferenceValues.get(this.mAlarmSound.getKey());
		String alarmTitle = resources.getString(R.string.sound_not_selected_summary);
		if (alarmUriString != null) {
			Uri alarmUri = Uri.parse(this.mPreferenceValues.get(this.mAlarmSound.getKey()));
			alarmTitle = RingtoneManager.getRingtone(this, alarmUri).getTitle(this);
		}
		this.mAlarmSound.setSummary(alarmTitle);
		
		int repeatMode = Integer.parseInt(this.mPreferenceValues.get(this.mRepeatMode.getKey()));
		
		this.mRepeatMode.setSummary(resources.getStringArray(R.array.repeat_mode_entries)[repeatMode]);
		this.mTimesPerDay.setSummary(this.mPreferenceValues.get(this.mTimesPerDay.getKey()) + resources.getString(R.string._times));
		this.mPeriodLength.setSummary(this.mPreferenceValues.get(this.mPeriodLength.getKey()) + resources.getString(R.string._minutes));
		this.mStartAt.setSummary(this.mPreferenceValues.get(this.mStartAt.getKey()));
		this.mFinishAt.setSummary(this.mPreferenceValues.get(this.mFinishAt.getKey()));

		if (repeatMode == 0) {
			this.mTimesPerDay.setEnabled(true);
			this.mPeriodLength.setEnabled(false);
		}
		if (repeatMode == 1) {
			this.mTimesPerDay.setEnabled(false);
			this.mPeriodLength.setEnabled(true);
		}
	}
}

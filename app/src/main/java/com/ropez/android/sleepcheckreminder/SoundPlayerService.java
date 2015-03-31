package com.ropez.android.sleepcheckreminder;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.net.Uri;

public class SoundPlayerService extends Service {

    private MediaPlayer mediaPlayer;
	private float maxVolume;
    private boolean isRepeatSoundOn;
    private boolean isFadeInOutOn;
    private int maximumPlayTime;
    private long start;
	
	private Timer timer = new Timer();
	
    @Override
    public void onCreate() {
        Log.i("SoundPlayerService.onCreate");
        this.timer = new Timer();
    }
    
    TimerTask repeatSoundTimerTaskOn = new TimerTask() {
		public void run() {
			float now = (new Date().getTime() - start) / 1000.0f;
            float fadeInTime = maximumPlayTime / 3.0f;
            float fadeOutTime = maximumPlayTime / 3.0f;
			if (now < maximumPlayTime) {
				if (isFadeInOutOn) {
                    float volume;
                    if (now < fadeInTime) {
                        volume = now / fadeInTime * maxVolume;
                    } else if (now < maximumPlayTime - fadeOutTime) {
                        volume = maxVolume;
                    } else {
                        volume = (maximumPlayTime - now) / fadeOutTime * maxVolume;
                    }
                    mediaPlayer.setVolume(volume, volume);
                }
			} else {
        		mediaPlayer.release();
                mediaPlayer = null;
				stopSelf();
			}
		}
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("SoundPlayerService.onStartCommand(" + startId + ")");
        
        String action = intent.getStringExtra("action");
        
        if (action.compareTo("start") == 0) {
        	Log.i("SoundPlayerService.onStartCommand.start");
        	
	        AlarmConfiguration alarmConfiguration = new AlarmConfiguration(this);

			this.maxVolume = alarmConfiguration.volume / 100.0f;
            this.isRepeatSoundOn = alarmConfiguration.isRepeatSoundOn;
            this.isFadeInOutOn = alarmConfiguration.isFadeInOutOn;
            this.maximumPlayTime = alarmConfiguration.maximumPlayTime;
			this.start = new Date().getTime();

            if (alarmConfiguration.notificationMode == AlarmConfiguration.NotificationMode.BuiltInSounds) {
                int soundResourceId = getResources().getIdentifier(alarmConfiguration.soundResourceName, "raw", this.getPackageName());
                this.mediaPlayer = MediaPlayer.create(this, soundResourceId);
            } else {
                Uri soundResourceUri = Uri.parse(alarmConfiguration.soundResourceName);
                this.mediaPlayer = MediaPlayer.create(this, soundResourceUri);
            }

            float startVolume = this.isRepeatSoundOn && this.isFadeInOutOn ? 0 : this.maxVolume;
			this.mediaPlayer.setVolume(startVolume, startVolume);
			this.mediaPlayer.setScreenOnWhilePlaying(false);
			this.mediaPlayer.setLooping(this.isRepeatSoundOn);
			this.mediaPlayer.start();

            if (this.isRepeatSoundOn)
			    this.timer.scheduleAtFixedRate(this.repeatSoundTimerTaskOn, 200, 200);

			return START_STICKY;
        }
		
        if (action.compareTo("stop") == 0) {
        	Log.i("SoundPlayerService.onStartCommand.stop");
        	if (this.timer != null)
        		this.timer.cancel();
        	if (this.mediaPlayer != null) {
        		this.mediaPlayer.release();
        	}
			stopSelf();
			return START_NOT_STICKY;
        }
        
        return START_NOT_STICKY;
    }
    
       @Override
    public void onDestroy() {
    	Log.i("SoundPlayerService.onDestroy");
    	this.timer.cancel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

	public static void runAction(Context context, String action) {
		Intent serviceIntent = new Intent(context, SoundPlayerService.class);
		serviceIntent.putExtra("action", action);
		Log.i("SoundPlayerService.runAction." + action);
		context.startService(serviceIntent);
	}
}

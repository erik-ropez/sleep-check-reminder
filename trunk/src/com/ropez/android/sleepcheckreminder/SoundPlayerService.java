package com.ropez.android.sleepcheckreminder;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.IBinder;

public class SoundPlayerService extends Service {

    private MediaPlayer mMediaPlayer;
	private float mMaxVolume;
	private long mStart;
	
	private Timer mTimer = new Timer(); 
	
    @Override
    public void onCreate() {
        Log.i("SoundPlayerService.onCreate");
        this.mTimer = new Timer();
    }
    
    TimerTask timerTask = new TimerTask() {
		public void run() {
			long now = new Date().getTime();
			if (now < mStart + 30 * 1000) {
				float volume; 
				if (now < mStart + 10 * 1000) {
					volume = ((now - mStart) / 10.0f / 1000.0f) * mMaxVolume; 
				} else
					if (now < mStart + 20 * 1000) {
						volume = mMaxVolume;
					} else {
						volume = (3 - ((now - mStart) / 10.0f / 1000.0f)) * mMaxVolume;
					}
				mMediaPlayer.setVolume(volume, volume);
			} else {
        		mMediaPlayer.release();
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
	        int soundResourceId = getResources().getIdentifier(alarmConfiguration.backgroundSoundResourceName, "raw", this.getPackageName());
	        
			this.mMaxVolume = alarmConfiguration.volume / 100.0f;
			this.mStart = new Date().getTime();

			this.mMediaPlayer = MediaPlayer.create(this, soundResourceId);
			this.mMediaPlayer.setVolume(0, 0);
			this.mMediaPlayer.setScreenOnWhilePlaying(false);
			this.mMediaPlayer.setLooping(true);
			this.mMediaPlayer.start();
			
			this.mTimer.scheduleAtFixedRate(this.timerTask, 200, 200);

			return START_STICKY;
        }
		
        if (action.compareTo("stop") == 0) {
        	Log.i("SoundPlayerService.onStartCommand.stop");
        	if (this.mTimer != null)
        		this.mTimer.cancel();
        	if (this.mMediaPlayer != null) {
        		this.mMediaPlayer.release();
        	}
			stopSelf();
			return START_NOT_STICKY;
        }
        
        return START_NOT_STICKY;
    }
    
       @Override
    public void onDestroy() {
    	Log.i("SoundPlayerService.onDestroy");
    	this.mTimer.cancel();
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

package com.ropez.android.sleepcheckreminder;

import java.util.Date;

import com.ropez.android.sleepcheckreminder.AlarmConfiguration.NotificationMode;
import com.ropez.android.sleepcheckreminder.AlarmConfiguration.RepeatMode;
import com.ropez.android.sleepcheckreminder.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

public class MainScreen extends Activity {
	
	static final String LOGTAG = "SCR";
	
	TextView mHelpText;
	Button mAboutButton;
	TextView mReminderSettings;
	
	ArrayAdapter<CharSequence> mTips;
	AlarmConfiguration mConfiguration;
	
	public void onAboutClick(View view) {
		this.mConfiguration.helpTextId = 0;
		this.mConfiguration.writePreferences();
		updateScreen();
	}
	
	public void onNextTipClick(View view) {
		nextTip();
		updateScreen();
	}

	private void nextTip() {
		this.mConfiguration.helpTextId = (int)Math.floor(Math.random() * this.mTips.getCount()) + 1;
		this.mConfiguration.writePreferences();
	}

	public void onPreferencesClick(View view) {
		Intent settings = new Intent();
		settings.setClassName("com.ropez.android.sleepcheckreminder", "com.ropez.android.sleepcheckreminder.PreferenceScreen");
		startActivityForResult(settings, 0);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		this.mConfiguration = new AlarmConfiguration(this);
        updateScreen();
	}
	
	public void onNewIntent(Intent intent) {
		nextTipIfNotification(intent);
	}
	
	void nextTipIfNotification(Intent intent) {
		if (intent.getBooleanExtra("notification", false)) {
			SoundPlayerService.runAction(this, "stop");
			nextTip();
			updateScreen();
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        this.mHelpText = (TextView)findViewById(R.id.helpText);
        this.mHelpText.setMovementMethod(new ScrollingMovementMethod());
        this.mAboutButton = (Button)findViewById(R.id.aboutButton);
        this.mReminderSettings = (TextView)findViewById(R.id.reminderSettings);
        
        this.mTips = ArrayAdapter.createFromResource(
    		this, R.array.tips, android.R.layout.simple_spinner_item);
        
        this.mConfiguration = new AlarmConfiguration(this);
        
        updateScreen();
        
        nextTipIfNotification(this.getIntent());
    }

	private void updateScreen() {
		if (this.mConfiguration.helpTextId > 0) {
    		this.mHelpText.setText(this.mTips.getItem(this.mConfiguration.helpTextId - 1).toString());
    		this.mHelpText.setGravity(Gravity.CENTER);
    		mAboutButton.setEnabled(true);
        } else {
        	this.mHelpText.setText(Html.fromHtml(getResources().getString(R.string.about_text)));
        	this.mHelpText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        	mAboutButton.setEnabled(false);
        }
		
		this.mReminderSettings.setText(getReminderSettings());
	}
	
	private String getReminderSettings() {
		StringBuilder builder = new StringBuilder();
		
		if (this.mConfiguration.isNotificationOn) {
			builder.append("Reminding ");
			
			if (this.mConfiguration.repeatMode == RepeatMode.Random)
				builder.append(Integer.toString(this.mConfiguration.timesPerDay)).append(" times per day ");
			if (this.mConfiguration.repeatMode == RepeatMode.Fixed)
				builder.append("every ").append(Integer.toString(this.mConfiguration.periodLength)).append(" minutes ");

			if (this.mConfiguration.timeFrom.getTime() == this.mConfiguration.timeTo.getTime()) {
				builder.append("all day");
			} else {
				builder.append("from ");
				appendTime(builder, this.mConfiguration.timeFrom);
				builder.append(" to ");
				appendTime(builder, this.mConfiguration.timeTo);
			}
			
			builder.append(" using ");
			
			if (this.mConfiguration.notificationMode == NotificationMode.DefaultNotification)
				builder.append("default notification");
			if (this.mConfiguration.notificationMode == NotificationMode.BackgroundSound)
				builder.append("background sound");
			if (this.mConfiguration.notificationMode == NotificationMode.AlertMode)
				builder.append("alarm");
		} else
			builder.append("Reminder is not enabled");
		
		return builder.toString();
	}

	private void appendTime(StringBuilder builder, Date time) {
		if (time.getHours() < 10)
			builder.append("0");
		builder.append(time.getHours());
		builder.append(":");
		if (time.getMinutes() < 10)
			builder.append("0");
		builder.append(time.getMinutes());
	}
}

// Please note this must be the package if you want to use XML-based preferences
package android.preference;
 
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;
 
/**
 * A preference type that allows a user to choose a time
 */
public class TimePickerPreference extends DialogPreference implements
		TimePicker.OnTimeChangedListener {
 
	private static final String androidns="http://schemas.android.com/apk/res/android";
	  
	/**
	 * The validation expression for this preference
	 */
	private static final String VALIDATION_EXPRESSION = "[0-2]*[0-9]:[0-5]*[0-9]";
 
	/**
	 * The default value for this preference
	 */
	private String defaultValue;
 
	/**
	 * @param context
	 * @param attrs
	 */
	public TimePickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(attrs);
	}
 
	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public TimePickerPreference(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		initialize(attrs);
	}
 
	/**
	 * Initialize this preference
	 */
	private void initialize(AttributeSet attrs) {
		this.defaultValue = attrs.getAttributeValue(androidns, "defaultValue");
		setPersistent(true);
	}
 
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.DialogPreference#onCreateDialogView()
	 */
	@Override
	protected View onCreateDialogView() {
 
		TimePicker tp = new TimePicker(getContext());
		tp.setOnTimeChangedListener(this);
 
		int h = getHour();
		int m = getMinute();
		if (h >= 0 && m >= 0) {
			tp.setCurrentHour(h);
			tp.setCurrentMinute(m);
		}
 
		return tp;
	}
 
	private static String pad(int c) {
        if (c >= 10)
            return String.valueOf(c);
        return "0" + String.valueOf(c);
    }
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.widget.TimePicker.OnTimeChangedListener#onTimeChanged(android
	 * .widget.TimePicker, int, int)
	 */
	public void onTimeChanged(TimePicker view, int hour, int minute) {
		String result = hour + ":" + pad(minute);
		persistString(result);
		callChangeListener(result);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.Preference#setDefaultValue(java.lang.Object)
	 */
	@Override
	public void setDefaultValue(Object defaultValue) {
		// BUG this method is never called if you use the 'android:defaultValue' attribute in your XML preference file, not sure why it isn't		
 
		super.setDefaultValue(defaultValue);
 
		if (!(defaultValue instanceof String)) {
			return;
		}
 
		if (!((String) defaultValue).matches(VALIDATION_EXPRESSION)) {
			return;
		}
 
		this.defaultValue = (String) defaultValue;
	}
 
	/**
	 * Get the hour value (in 24 hour time)
	 * 
	 * @return The hour value, will be 0 to 23 (inclusive)
	 */
	private int getHour() {
		String time = getPersistedString(this.defaultValue);
		if (time == null || !time.matches(VALIDATION_EXPRESSION)) {
			return -1;
		}
 
		return Integer.valueOf(time.split(":")[0]);
	}
 
	/**
	 * Get the minute value
	 * 
	 * @return the minute value, will be 0 to 59 (inclusive)
	 */
	private int getMinute() {
		String time = getPersistedString(this.defaultValue);
		if (time == null || !time.matches(VALIDATION_EXPRESSION)) {
			return -1;
		}
 
		return Integer.valueOf(time.split(":")[1]);
	}
	
	public String getValue() {
		return getPersistedString(this.defaultValue);
	}
}

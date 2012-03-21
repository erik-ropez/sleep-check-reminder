package com.ropez.android.sleepcheckreminder;

public class Log {
	
	private static final String LOGTAG = "SCR";
	
	public static void i(String msg) {
		android.util.Log.i(LOGTAG, msg);
	}
}

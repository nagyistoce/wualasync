package com.laksrecordings.wualasync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import java.util.Calendar;

public class InitExecutionService extends Service {
	private String LOG_TAG = getClass().getSimpleName();
	protected static boolean delayStartup = false;
	public static final int ADD_ACTION = 1;
	public static final int DELETE_ACTION = 0;

	private long getInterval() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int i = 60;
        try {
        	i = Integer.parseInt(preferences.getString("serviceSyncInterval", "60"));
        } catch (Exception e) {}
		long l = AlarmManager.INTERVAL_HOUR;
		switch (i) {
			case 15: l = AlarmManager.INTERVAL_FIFTEEN_MINUTES; break;
			case 30: l = AlarmManager.INTERVAL_HALF_HOUR; break;
			case 60: l = AlarmManager.INTERVAL_HOUR; break;
			case 720: l = AlarmManager.INTERVAL_HALF_DAY; break;
			case 1440: l = AlarmManager.INTERVAL_DAY; break;
		}
		return l;
	}
	
	private boolean isServiceEnabled() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    	return preferences.getBoolean("enableService", false);		
	}
	
	@Override public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent i, int flags, int startId) {
		Bundle b = i.getExtras();
    	Intent intent = new Intent(this, ExecuteBroadcastReciever.class);
    	PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);
    	AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
		am.cancel(pi);
		if (b.getInt("action") == ADD_ACTION) {
			// Add schedule
			if (isServiceEnabled()) {
				Log.d(LOG_TAG, "Scheduling sync task");
		    	try {
		    		Calendar cal = Calendar.getInstance();
		    		if (SyncFilesService.isExecutionRunning() || delayStartup) {
		    			cal.add(Calendar.MINUTE, 5);
		    		} else {
		    			cal.add(Calendar.MINUTE, 1);	    			
		    		}
			    	am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), getInterval(), pi);
					Log.d(LOG_TAG, "Scheduling sync task finished, next exec "+cal.getTime().toLocaleString());
		    	} catch (Exception e) {
		    		Log.e(LOG_TAG, "Error while enabling timer", e);
		    	}			
			}
			delayStartup = false;
		} else {
			// Delete schedule
			Log.d(LOG_TAG, "Sync task schedule deleted");
		}
		this.stopSelf(startId);
		return START_REDELIVER_INTENT;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}

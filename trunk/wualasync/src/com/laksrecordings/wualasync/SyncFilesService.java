package com.laksrecordings.wualasync;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class SyncFilesService extends Service {
	private static final int RETRY_COUNT = 5;
	private static final int SLEEP_ON_FAIL = 5000;
	private DataHelper db;
	private String LOG_TAG = getClass().getSimpleName();
	private File dstPath;
	private ArrayList<WualaFile> wf = new ArrayList<WualaFile>();
	protected boolean cancelRecieved = false;
	private static boolean executionRunning = false;
	private static boolean executionRequested = false;
	private static boolean serviceRunning = false;
	private static boolean executedOnStartup = false;
	//
	private String PREFS_WUALA_URL = "";
	private String PREFS_WUALA_KEY = "";
	private boolean PREFS_WUALA_DELETE = false;
	private int PREFS_WUALA_INTERVAL = 60;
	private boolean PREFS_ONLY_WIFI = true;
	private boolean PREFS_START_ON_BOOT = false;
	//
	private Timer timer = new Timer();
	private Timer reqTimer = new Timer();
	private TimerTask mainTask = new TimerTask() {
		public void run() {
			executionRequested = false;
			executeTask();
		}
	};
	private TimerTask reqTask = new TimerTask() {
		public void run() {
			if (!executionRunning && executionRequested)
				mainTask.run();
		}
	};
	//
	private static SyncFilesUIUpdaterListener UI_UPDATE_LISTENER;
	//private static SyncFiles MAIN_ACTIVITY;

    ///////////////////////////////////////////
	// Service control functions
    ///////////////////////////////////////////
	
	@Override public void onCreate() {
	  super.onCreate();
	  // init the service here
	  serviceRunning = true;
  	  dstPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
	  startService();
	}

	@Override public void onDestroy() {
	  super.onDestroy();
	  shutdownService();
	  serviceRunning = false;
  	  try {
  		  UI_UPDATE_LISTENER.setServiceStateChange();
		} catch (Exception e) {}
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	public static void requestExecution() {
		if (!executionRunning)
			executionRequested = true;
	}
	
	public static boolean isServiceRunning() {
		return serviceRunning;
	}
	
	public static void thisIsDeviceStartup() {
		executedOnStartup = true;
	}

	///////////////////////////////////////////

	private boolean isStorageAvailableAndWritable() {
    	boolean mExternalStorageAvailable = false;
    	boolean mExternalStorageWriteable = false;
    	String state = Environment.getExternalStorageState();

    	if (Environment.MEDIA_MOUNTED.equals(state)) {
    	    // We can read and write the media
    	    mExternalStorageAvailable = mExternalStorageWriteable = true;
    	} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
    	    // We can only read the media
    	    mExternalStorageAvailable = true;
    	    mExternalStorageWriteable = false;
    	} else {
    	    // Something else is wrong. It may be one of many other states, but all we need
    	    //  to know is we can neither read nor write
    	    mExternalStorageAvailable = mExternalStorageWriteable = false;
    	}
    	return mExternalStorageAvailable && mExternalStorageWriteable;
	}
	
	private boolean checkInternet() {
		ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
		return (cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isAvailable() && !PREFS_ONLY_WIFI) || 
		  cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isAvailable();
	}
	
    ///////////////////////////////////////////
	
	private ArrayList<WualaFile> readWualaFiles() {
		wf.clear();
		int retry = RETRY_COUNT;
		while (retry > 0) {
			try {
		    	WualaDirectoryReader dr = new WualaDirectoryReader(PREFS_WUALA_URL, PREFS_WUALA_KEY);
		    	dr.setDB(db);
		    	dr.setDstPath(dstPath);
		    	dr.setMainService(this);
		    	wf = dr.read();
		    	retry = 0;
			} catch (Exception e) {
				retry--;
				if (retry == 0 || cancelRecieved) {
					throw new RuntimeException(e);
				} else {
					try {
						Thread.sleep(SLEEP_ON_FAIL);
					} catch (Exception ex) {}
				}
			}
		}
    	return wf;
	}
	
	private void checkLocalFiles() {
		ArrayList<String> removedFiles = new ArrayList<String>();
		{
			ArrayList<String> localFiles = db.selectAll();
			for (int i = 0; i < localFiles.size(); i++) {
				File f = new File(localFiles.get(i));
				//Log.d(LOG_TAG, "Local file from db: "+localFiles.get(i));				
				if (!f.exists()) {
					removedFiles.add(localFiles.get(i));
				} else if (!db.existsInWuala(localFiles.get(i)) && PREFS_WUALA_DELETE) {
					f.delete();
					Log.d(LOG_TAG, "Deleted file: "+localFiles.get(i));
					removedFiles.add(localFiles.get(i));
				}
			}
		}
		for (int i = 0; i < removedFiles.size(); i++) {
			db.delete(removedFiles.get(i));
			Log.d(LOG_TAG, "Removed file from db: "+removedFiles.get(i));
		}
	}
	
    private void readPrefs() {
    	boolean changed = false;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        String url = preferences.getString("wualaURL", "");
        String key = preferences.getString("wualaKey", "");
        String interval = preferences.getString("serviceSyncInterval", "60");

    	PREFS_WUALA_DELETE = preferences.getBoolean("allowDelete", false);
    	PREFS_ONLY_WIFI = preferences.getBoolean("onlyWifi", true);
    	PREFS_START_ON_BOOT = preferences.getBoolean("startupOnBoot", false);
    	try {
    		PREFS_WUALA_INTERVAL = Integer.parseInt(interval);
    	} catch (Exception e) {
    		PREFS_WUALA_INTERVAL = 60;
    	}
        
        if (url.contains("?key=")) {
        	int i = url.lastIndexOf("?key=");
        	key = url.substring(i+5);
        	url = url.substring(0, i);
        	changed = true;
        }
        
        if (url.startsWith("http://") && !key.equals("")) {
        	url = url.replace("http://", "https://");
        	changed = true;        	
        }
        
        if (url.contains(" ")) {
        	url = url.replace(" ", "");
        	changed = true;        	
        }
        
        if (changed) {        	
        	Editor e = preferences.edit();
        	e.putString("wualaURL", url);
        	e.putString("wualaKey", key);
        	e.commit();
        }

        PREFS_WUALA_URL = url;    	
        PREFS_WUALA_KEY = key;   	
    }
	
	private void executeTask() {
		readPrefs();
		if (PREFS_WUALA_URL.equals("")) {
			try {
				UI_UPDATE_LISTENER.setNotConfigured();
			} catch (Exception e) {}
			Log.e(LOG_TAG, "Cannot execute task, not configured");			
		} else
		if (isStorageAvailableAndWritable() && checkInternet() && !executionRunning) {
			executionRunning = true;
			try {
				Log.i(LOG_TAG, "Task execution started");
				db = new DataHelper(this);
	    		dstPath.mkdirs();
	    		//
	    		try {
	    			UI_UPDATE_LISTENER.setPreparing();
				} catch (Exception e) {}
				readWualaFiles();
				try {
					UI_UPDATE_LISTENER.setMaxFiles(wf.size());
				} catch (Exception e) {}
				checkLocalFiles();
				for (int i = 0; i < wf.size(); i++) {
					if (cancelRecieved || !isStorageAvailableAndWritable() || !checkInternet())
						break;
					try {
						UI_UPDATE_LISTENER.setCurrentFile(i+1);
						UI_UPDATE_LISTENER.setFilename(wf.get(i).toString());
					} catch (Exception e) {}
					wf.get(i).setMainService(this);
					int retry = RETRY_COUNT;
					while (retry > 0) {
						if (cancelRecieved || !isStorageAvailableAndWritable() || !checkInternet())
							break;
						try {
			        		String createdFileName = wf.get(i).syncFile(dstPath.getPath(), PREFS_WUALA_KEY);
			        		if (!createdFileName.equals(""))
			        			db.insert(createdFileName);
							retry = 0;
							Log.i(LOG_TAG, "Downloaded ("+String.valueOf(retry)+"): "+wf.get(i).toString());
						} catch (Exception e) {
							retry--;
							Log.e(LOG_TAG, "Download failed ("+String.valueOf(retry)+"): "+e.getMessage());
							if (retry > 0 && !cancelRecieved) {
								try {
									Thread.sleep(SLEEP_ON_FAIL);
								} catch (Exception ex) {}
							}
						}
					}
				}
				db.close();
				Log.i(LOG_TAG, "Task execution finished");
			} catch (Exception e) {
				Log.e(LOG_TAG, "Task execution failed: "+e.getMessage());
				db.close();
			}
			executionRunning = false;
			try {
				UI_UPDATE_LISTENER.setNotActive();
			} catch (Exception e) {}
		} else {
			if (!executionRunning) {
				try {
					UI_UPDATE_LISTENER.setCannotExecute();
				} catch (Exception e) {}
				Log.e(LOG_TAG, "Cannot execute task, no SD or internet");
			} else {
				Log.i(LOG_TAG, "Cannot execute task, already running");				
			}
		}
		wf.clear();
	}
	
	///////////////////////////////////////////
	private void startService() {
		if (!executionRunning) {
			cancelRecieved = false;
			readPrefs();
			if (!executedOnStartup || (executedOnStartup && PREFS_START_ON_BOOT)) {
				timer.schedule(mainTask, 0, PREFS_WUALA_INTERVAL*60*1000);
				reqTimer.schedule(reqTask, 10000, 10000);
				Log.i(LOG_TAG, "Service started");
			} else {
				Log.i(LOG_TAG, "Service not allowed to start on boot");				
				this.stopSelf();
			}
			executedOnStartup = false;
		} else {
			Log.e(LOG_TAG, "Service not started, execution was still running");
			this.stopSelf();
		}
	}

	private void shutdownService() {
		cancelRecieved = true;
		if (timer != null)
			timer.cancel();
		if (reqTimer != null)
			reqTimer.cancel();
		try {
			UI_UPDATE_LISTENER.setStopping();
		} catch (Exception e) {}
		Log.i(LOG_TAG, "Service stopped");
	}
	
	public static void setUIUpdaterListener(SyncFilesUIUpdaterListener l) {
		UI_UPDATE_LISTENER = l;
	}
	
}

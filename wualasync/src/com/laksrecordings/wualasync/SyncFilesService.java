package com.laksrecordings.wualasync;

import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.os.IBinder;
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
	private boolean cancelRecieved = false;
	private static boolean executionRunning = false;
	private static boolean executionRequested = false;

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
	private static SyncFiles MAIN_ACTIVITY;


	@Override public void onCreate() {
	  super.onCreate();
	  // init the service here
  	  dstPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
	  startService();
	}

	@Override public void onDestroy() {
	  super.onDestroy();
	  shutdownService();
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void requestExecution() {
		if (!executionRunning)
			executionRequested = true;
	}

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
		return cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isAvailable() || 
		  cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isAvailable();
	}
	
    ///////////////////////////////////////////
	
	private ArrayList<WualaFile> readWualaFiles() {
		wf.clear();
		int retry = RETRY_COUNT;
		while (retry > 0) {
			try {
		    	WualaDirectoryReader dr = new WualaDirectoryReader(MAIN_ACTIVITY.PREFS_WUALA_URL, MAIN_ACTIVITY.PREFS_WUALA_KEY);
		    	dr.setDB(db);
		    	dr.setDstPath(dstPath);
		    	wf = dr.read();
		    	retry = 0;
			} catch (Exception e) {
				retry--;
				if (retry == 0) {
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
				} else if (!db.existsInWuala(localFiles.get(i)) && MAIN_ACTIVITY.PREFS_WUALA_DELETE) {
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
	
	private void executeTask() {
		if (MAIN_ACTIVITY.PREFS_WUALA_URL.equals("")) {
			UI_UPDATE_LISTENER.setNotConfigured();
			Log.e(LOG_TAG, "Cannot execute task, not configured");			
		} else
		if (isStorageAvailableAndWritable() && checkInternet() && !executionRunning) {
			executionRunning = true;
			try {
				Log.i(LOG_TAG, "Task execution started");
				db = new DataHelper(this);
	    		dstPath.mkdirs();
	    		if (UI_UPDATE_LISTENER != null)
	    			UI_UPDATE_LISTENER.setPreparing();
				readWualaFiles();
				if (UI_UPDATE_LISTENER != null)
					UI_UPDATE_LISTENER.setMaxFiles(wf.size());
				checkLocalFiles();
				for (int i = 0; i < wf.size(); i++) {
					if (cancelRecieved || !isStorageAvailableAndWritable() || !checkInternet())
						break;
					if (UI_UPDATE_LISTENER != null) {
						UI_UPDATE_LISTENER.setCurrentFile(i+1);
						UI_UPDATE_LISTENER.setFilename(wf.get(i).toString());
					}
					int retry = RETRY_COUNT;
					while (retry > 0) {
						if (cancelRecieved || !isStorageAvailableAndWritable() || !checkInternet())
							break;
						try {
			        		String createdFileName = wf.get(i).syncFile(dstPath.getPath(), MAIN_ACTIVITY.PREFS_WUALA_KEY);
			        		if (!createdFileName.equals(""))
			        			db.insert(createdFileName);
							retry = 0;
							Log.i(LOG_TAG, "Downloaded ("+String.valueOf(retry)+"): "+wf.get(i).toString());
						} catch (Exception e) {
							retry--;
							Log.e(LOG_TAG, "Download failed ("+String.valueOf(retry)+"): "+e.getMessage());
							if (retry > 0) {
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
			if (UI_UPDATE_LISTENER != null)
				UI_UPDATE_LISTENER.setNotActive();
		} else if (UI_UPDATE_LISTENER != null) {
			if (!executionRunning) {
				UI_UPDATE_LISTENER.setCannotExecute();
				Log.e(LOG_TAG, "Cannot execute task, no SD or internet");
			} else {
				Log.i(LOG_TAG, "Cannot execute task, already running");				
			}
		}
	}
	
	///////////////////////////////////////////
	private void startService() {
		if (!executionRunning) {
			cancelRecieved = false;
			timer.schedule(mainTask, 0, MAIN_ACTIVITY.PREFS_WUALA_INTERVAL*60*1000);
			reqTimer.schedule(reqTask, 10000, 10000);
			Log.i(LOG_TAG, "Service started");
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
		if (UI_UPDATE_LISTENER != null)
			UI_UPDATE_LISTENER.setStopping();
		Log.i(LOG_TAG, "Service stopped");
	}
	
	public static void setMainActivity(SyncFiles s) {
		MAIN_ACTIVITY = s;
	}
	
	public static void setUIUpdaterListener(SyncFilesUIUpdaterListener l) {
		UI_UPDATE_LISTENER = l;
	}
	
}

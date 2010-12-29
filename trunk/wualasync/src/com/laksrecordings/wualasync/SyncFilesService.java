package com.laksrecordings.wualasync;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;

public class SyncFilesService extends Service {
	private static final int RETRY_COUNT = 5;
	private static final int SLEEP_ON_FAIL = 5000;
	private DataHelper db;
	private String LOG_TAG = getClass().getSimpleName();
	private File dstPath;
	private ArrayList<WualaFile> wf = new ArrayList<WualaFile>();
	protected static boolean cancelRecieved = false;
	private static boolean executionRunning = false;
	//
	private String PREFS_WUALA_URL = "";
	private String PREFS_WUALA_KEY = "";
	private boolean PREFS_WUALA_DELETE = false;
	private boolean PREFS_ONLY_WIFI = true;
	//
	private static SyncFilesUIUpdaterListener UI_UPDATE_LISTENER;
	protected static int progressMax = 0;
	protected static int progressCurrent = 0;
	protected static String progressMessage = "Not active";
	protected static String progressTitle = "Syncing files";

    ///////////////////////////////////////////
	// Service control functions
    ///////////////////////////////////////////
	
	@Override public void onCreate() {
		super.onCreate();
	}
	
	public void onLowMemory () {
		Log.i(LOG_TAG, "Low memory signalled");
		super.onLowMemory();
	}
	
	public int onStartCommand(Intent i, int flags, int startId) {
	  	dstPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
		if (!executionRunning) {
			cancelRecieved = false;
			readPrefs();
			new RunnableTask().execute();
		} else {
			Log.e(LOG_TAG, "Service not started, execution was still running");
		}
		return START_NOT_STICKY;
	}

	@Override public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	///////////////////////////////////////////
	private class RunnableTask extends AsyncTask<Void, Void, Void> {
		@Override protected Void doInBackground(Void... params) {
			executeTask();
			return null;
		}
		
		@Override protected void onPostExecute(Void result) {
			stopSelf();
		}
 
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
	
    private void writeLastExecutionTime(boolean isStart) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    	Editor e = preferences.edit();
    	Date date = new Date();
    	if (isStart)
    		e.putString("lastExecStart", date.toLocaleString());
    	else
    		e.putString("lastExec", date.toLocaleString());
    	e.commit();    	
    }
    
	private void readPrefs() {
    	boolean changed = false;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        String url = preferences.getString("wualaURL", "");
        String key = preferences.getString("wualaKey", "");

    	PREFS_WUALA_DELETE = preferences.getBoolean("allowDelete", false);
    	PREFS_ONLY_WIFI = preferences.getBoolean("onlyWifi", true);
        
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
		writeLastExecutionTime(true);
		if (PREFS_WUALA_URL.equals("")) {
			setNotConfigured();
			Log.e(LOG_TAG, "Cannot execute task, not configured");			
		} else
		if (isStorageAvailableAndWritable() && checkInternet() && !executionRunning) {
			executionRunning = true;
			try {
				Log.i(LOG_TAG, "Task execution started");
				db = new DataHelper(this);
	    		dstPath.mkdirs();
	    		//
	    		setPreparing();
				readWualaFiles();
				setMaxFiles(wf.size());
				checkLocalFiles();
				for (int i = 0; i < wf.size(); i++) {
					if (cancelRecieved || !isStorageAvailableAndWritable() || !checkInternet())
						break;
					setCurrentFile(i+1);
					setFilename(wf.get(i).toString());
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
				writeLastExecutionTime(false);
				Log.i(LOG_TAG, "Task execution finished");
			} catch (Exception e) {
				Log.e(LOG_TAG, "Task execution failed: "+e.getMessage());
				db.close();
			}
			executionRunning = false;
			setNotActive();
		} else {
			if (!executionRunning) {
				setCannotExecute();
				Log.e(LOG_TAG, "Cannot execute task, no SD or internet");
			} else {
				Log.i(LOG_TAG, "Cannot execute task, already running");				
			}
		}
		wf.clear();
	}
	
	///////////////////////////////////////////
	private void updateProgress() {
		try {
			if (UI_UPDATE_LISTENER != null)
				UI_UPDATE_LISTENER.updateProgress();
		} catch (Exception e) {}
	}
	public void setFilename(String filename) {
		progressMessage = filename;
		updateProgress();
	}
	public void setMaxFiles(int maxFiles) {
		progressMax = maxFiles;
		updateProgress();
	}
	public void setCurrentFile(int currentFile) {
		progressCurrent = currentFile;
		updateProgress();
	}
	public void setPreparing() {
		progressCurrent = 0;
		progressMax = 0;
		progressMessage = "Preparing";
		updateProgress();        		
	}
	public void setNotActive() {
		progressCurrent = 0;
		progressMax = 0;
		progressMessage = "Not active";
		updateProgress();
	}
	public void setStopping() {
		progressCurrent = 0;
		progressMax = 0;
		progressMessage = "Stopping";
		updateProgress();
	}
	public void setCannotExecute() {
		progressCurrent = 0;
		progressMax = 0;
		progressMessage = "Cannot execute, no SD card or internet";
		updateProgress();        		
	}
	public void setNotConfigured() {
		progressCurrent = 0;
		progressMax = 0;
		progressMessage = "Application is not configured";
		updateProgress();        		
	}
	public void setServiceStateChange() {
		updateProgress();        		        		
	}
	
	///////////////////////////////////////////
	public static void setUIUpdaterListener(SyncFilesUIUpdaterListener l) {
		UI_UPDATE_LISTENER = l;
	}
	
	public static void cancelExecution() {
		cancelRecieved = true;
	}
	
	public static boolean isExecutionRunning() {
		return executionRunning;
	}
	
}

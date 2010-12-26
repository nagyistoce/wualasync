package com.laksrecordings.wualasync;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.util.Log;
import android.view.View;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;


public class SyncFiles extends Activity {
	private static final int PROGRESS_DIALOG = 0;
    private ProgressDialog progressDialog;
	private String LOG_TAG = getClass().getSimpleName();
	//
	private int progressMax = 0;
	private int progressCurrent = 0;
	private String progressMessage = "Not active";
	private String progressTitle = "Syncing files";
	private SyncFiles CURRENT_ACTIVITY;
	//
	protected String PREFS_WUALA_URL = "";
	protected String PREFS_WUALA_KEY = "";
	protected boolean PREFS_WUALA_DELETE = false;
	protected int PREFS_WUALA_INTERVAL = 60;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        CURRENT_ACTIVITY = this;
        SyncFilesService.setUIUpdaterListener(new SyncFilesUIUpdaterListener() {
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
        	private void updateProgress() {
        		CURRENT_ACTIVITY.runOnUiThread(new Runnable() {
        			public void run() {
        				if (progressDialog != null) {
	        				progressDialog.setMessage(progressMessage);
	        				progressDialog.setTitle(progressTitle);
	        				progressDialog.setProgress(progressCurrent);
	        				progressDialog.setMax(progressMax);
        				}
        			}
        		});        		
        	}
        });
    }
    
    protected void onResume() {
    	super.onRestart();
        readPrefs();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	stopService();
    }
    
    private void startService() {
    	try {
			SyncFilesService.setMainActivity(this);
			Intent svc = new Intent(this, SyncFilesService.class);
			startService(svc);
    	} catch (Exception e) {
   		    Log.e(LOG_TAG, "Service creation problem", e);
    	}
    }

    private void stopService() {
		Intent svc = new Intent(this, SyncFilesService.class);
		stopService(svc);
    }

    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case PROGRESS_DIALOG:
            progressDialog = new ProgressDialog(SyncFiles.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMessage(progressMessage);
            progressDialog.setTitle(progressTitle);
            progressDialog.setCancelable(true);
            return progressDialog;
        default: 
            return null;
        }
    }

    protected void onPrepareDialog(int id, Dialog d) {
        switch(id) {
	        case PROGRESS_DIALOG:
	        	ProgressDialog pd = (ProgressDialog)d;
			    pd.setMessage(progressMessage);
			    pd.setTitle(progressTitle);
			    pd.setProgress(progressCurrent);
			    pd.setMax(progressMax);
        default: 
        }
    }
    
    private void readPrefs() {
    	boolean changed = false;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        String url = preferences.getString("wualaURL", "");
        String key = preferences.getString("wualaKey", "");
        String interval = preferences.getString("serviceSyncInterval", "60");

    	PREFS_WUALA_DELETE = preferences.getBoolean("allowDelete", false);
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
    		
    public void startServiceButtonClickHandler(View view) {
    	startService();
    	showSyncButtonClickHandler(view);
    }

    public void stopServiceButtonClickHandler(View view) {
    	stopService();
    }
    
	public void showSyncButtonClickHandler(View view) {
		showDialog(PROGRESS_DIALOG);
	}

	public void execSyncButtonClickHandler(View view) {
		SyncFilesService.requestExecution();
    	showSyncButtonClickHandler(view);		
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu, menu);
    	return true;
    }

	// This method is called once the menu is selected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.preferences:
			Intent i = new Intent(SyncFiles.this, Preferences.class);
			startActivity(i);
			break;
		case R.id.exit:
			finish();
			break;
		}
		return true;
	}
	
}
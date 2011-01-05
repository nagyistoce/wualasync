package com.laksrecordings.wualasync;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
//import android.view.MenuItem;
//import android.view.MenuInflater;
import android.util.Log;
import android.view.View;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.content.DialogInterface;

public class SyncFiles extends Activity {
	private static final int PROGRESS_DIALOG = 0;
    private ProgressDialog progressDialog;
	private String LOG_TAG = getClass().getSimpleName();
	//
	private SyncFiles CURRENT_ACTIVITY;
	private boolean isServiceEnabled = false;
	private String serviceSyncInterval = "";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        isServiceEnabled = preferences.getBoolean("enableService", false);
        serviceSyncInterval = preferences.getString("serviceSyncInterval", "60");
        //
        CURRENT_ACTIVITY = this;
        SyncFilesService.setUIUpdaterListener(new SyncFilesUIUpdaterListener() {
        	public void updateProgress() {
        		CURRENT_ACTIVITY.runOnUiThread(new Runnable() {
        			public void run() {
        				if (progressDialog != null) {
	        				progressDialog.setMessage(SyncFilesService.progressMessage);
	        				progressDialog.setTitle(SyncFilesService.progressTitle);
	        				progressDialog.setProgress(SyncFilesService.progressCurrent);
	        				progressDialog.setMax(SyncFilesService.progressMax);
	        				progressDialog.setSecondaryProgress(SyncFilesService.progressSecondary);
        				}
        			}
        		});        		
        	}
        });
    }
    
    protected void onStart() {
    	super.onStart();
    	//
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean newServiceEnabled = preferences.getBoolean("enableService", false);
        String newServiceSyncInterval = preferences.getString("serviceSyncInterval", "60");
    	//
    	if (isServiceEnabled != newServiceEnabled) {
    		if (newServiceEnabled)
    			startService();
    		else
    			stopService();
    	} else if (serviceSyncInterval != newServiceSyncInterval) {
			startService();
    	}
    	isServiceEnabled = newServiceEnabled;
    	serviceSyncInterval = newServiceSyncInterval;
    	// Show last execution time
		TextView t = (TextView)findViewById(R.id.TextView03);
		t.setText("Last start: "+preferences.getString("lastExecStart", "Not executed"));
		TextView t2 = (TextView)findViewById(R.id.TextView04);
		t2.setText("Last finish: "+preferences.getString("lastExec", "Not finished"));
    }
    
    private void startService() {
    	try {
			Intent svc = new Intent(this, InitExecutionService.class);
			svc.putExtra("action", InitExecutionService.ADD_ACTION);
			startService(svc);
    	} catch (Exception e) {
   		    Log.e(LOG_TAG, "Service creation problem", e);
    	}
    }

    private void stopService() {
    	try {
			Intent svc = new Intent(this, InitExecutionService.class);
			svc.putExtra("action", InitExecutionService.DELETE_ACTION);
			startService(svc);
    	} catch (Exception e) {
    		Log.e(LOG_TAG, "Error while cancelling service", e);
    	}
    }

    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case PROGRESS_DIALOG:
            progressDialog = new ProgressDialog(SyncFiles.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMessage(SyncFilesService.progressMessage);
            progressDialog.setTitle(SyncFilesService.progressTitle);
            progressDialog.setCancelable(true);
            progressDialog.setButton("Cancel execution", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    	if (SyncFilesService.isExecutionRunning())
                    		SyncFilesService.cancelExecution();
                    }
            });
            return progressDialog;
        default: 
            return null;
        }
    }

    protected void onPrepareDialog(int id, Dialog d) {
        switch(id) {
	        case PROGRESS_DIALOG:
	        	ProgressDialog pd = (ProgressDialog)d;
			    pd.setMessage(SyncFilesService.progressMessage);
			    pd.setTitle(SyncFilesService.progressTitle);
			    pd.setProgress(SyncFilesService.progressCurrent);
			    pd.setMax(SyncFilesService.progressMax);
        default: 
        }
    }
        		
	public void showSyncButtonClickHandler(View view) {
		showDialog(PROGRESS_DIALOG);
	}

	public void execSyncButtonClickHandler(View view) {
		//startService();
		Intent svc = new Intent(this, SyncFilesService.class);
		this.startService(svc);
    	showSyncButtonClickHandler(view);		
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		Intent i = new Intent(SyncFiles.this, Preferences.class);
		startActivity(i);
/*    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu, menu);*/
    	return true;
    }

	// This method is called once the menu is selected
/*	@Override
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
	}*/
	
}
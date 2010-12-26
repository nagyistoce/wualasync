package com.laksrecordings.wualasync;

import java.util.ArrayList;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.app.Activity;
import java.io.File;

public class CheckExistingFiles {
	static final String PREFS_NAME = "SyncedFiles";
	SharedPreferences settings;
	//private ArrayList<WualaFile> wf;
	private ArrayList<String> localFiles = new ArrayList<String>();
	private ArrayList<String> wualaFiles = new ArrayList<String>();
	public final static String LOG_TAG = "com.laksrecordings.wualasync.CheckExistingFiles";

	CheckExistingFiles(Activity a) {
		this.settings = a.getSharedPreferences(PREFS_NAME, 0);
		//readPrefs();
	}
	
	public void addWualaFiles(ArrayList<String> wf) {
    	File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
    	String basePath = path.getPath();
    	wualaFiles.clear();
    	for (int i = 0; i < wf.size(); i++) {
    		wualaFiles.add(basePath+wf.get(i));
    		Log.d(LOG_TAG, "addWualaFile: "+basePath+wf.get(i));
    	}
	}
	
	public void readPrefs() {
		localFiles.clear();
		int countFiles = settings.getInt("count", 0);
		for (int i = 0; i < countFiles; i++) {
			String s = settings.getString("filename"+String.valueOf(i), "");
			if (!s.equals("")) {
				File f = new File(s);
				if (f.exists()) {
					localFiles.add(s);
					Log.d(LOG_TAG, "From prefs: "+s);
				}
			}
		}
	}
	
	private boolean existsWualaFile(String filename) {
		return wualaFiles.contains(filename);
	}
	
	public void deleteFiles() {
		ArrayList<Integer> removedIndexes = new ArrayList<Integer>();
		// Counting backwards so removed indexes would be descending
		for (int i = localFiles.size()-1; i >= 0; i--) {
			if (!existsWualaFile(localFiles.get(i))) {
				File f = new File(localFiles.get(i));
				f.delete();
				removedIndexes.add(i);
				Log.d(LOG_TAG, "Deleted: "+localFiles.get(i));
			}
		}
		// Removing from array
		for (int i = 0; i < removedIndexes.size(); i++) {
			String s = localFiles.remove(removedIndexes.get(i).intValue());
			Log.d(LOG_TAG, "Removed from local list: "+s);
		}
	}
	
	public void addFiles(ArrayList<String> filenames) {
		localFiles.addAll(filenames);
	}
	
	public void save() {
		SharedPreferences.Editor editor = settings.edit();
		editor.clear();
		editor.putInt("count", localFiles.size());
		for (int i = 0; i < localFiles.size(); i++) {
			editor.putString("filename"+String.valueOf(i), localFiles.get(i));
			Log.d(LOG_TAG, "To prefs: "+localFiles.get(i));
		}
		editor.commit();
	}
	
}

package com.laksrecordings.wualasync;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;

import android.media.MediaScannerConnection;
import android.os.StatFs;

import android.util.Log;

public class WualaFile {
	private String directory;
	private String filename;
	private String url;
	private long filesize;
	private static String LOG_TAG = "WualaFile";
	private SyncFilesService MAIN_SERVICE;
	
	WualaFile(String directory, String filename, String url, long filesize) {
		this.directory = directory;
		this.filename = filename;
		this.url = encodeUrl(url);
		this.filesize = filesize;
	}
	
	public void setMainService(SyncFilesService s) {
		this.MAIN_SERVICE = s;
	}
	
	public static String encodeUrl(String url) {
		// Need to only encode multibyte UTF-8 characters
		String s = "";
		try {
			if (url.getBytes("UTF-8").length > url.length()) {
				for(int i = 0; i < url.length(); i++) {
					char c = url.charAt(i);
					if (String.valueOf(c).getBytes("UTF-8").length > 1) {
						s = s + URLEncoder.encode(String.valueOf(c));
					} else {
						s = s + c;
					}
				}
			} else {
				s = url;
			}
		} catch (Exception e) {
			s = url;
		}
		//Log.d(LOG_TAG, s);
		return s;
	}
	
	public String toString() {
		return filename;
	}
	
	public String getFullFilePath() {
		return directory+"/"+filename;
	}
	
	public boolean exists(String destDir) {
		File file = new File(destDir+directory, filename);
		return (file.exists() && file.length() == filesize);
	}
	
	public String syncFile(String destDir, String key) {
		String createFileName = "";
		File path = new File(destDir+directory);
		path.mkdirs();
		
        StatFs stat = new StatFs(path.getPath());
        long bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
        if (bytesAvailable <= filesize) {
        	Log.e(LOG_TAG, "Not enough space to download: "+filename);
        	throw new RuntimeException("Not enough space available on SD card");
        }
		
		File file = new File(destDir+directory, filename);
		if (!exists(destDir)) {
			try {
		    	URL url = new URL(this.url.replace("www.wuala.com", "content.wuala.com/contents")+"?key="+key);
		        HttpURLConnection c = (HttpURLConnection)url.openConnection();
		        c.setRequestMethod("GET");
		        c.setDoOutput(true);
		        c.connect();
		        
		        FileOutputStream fos = new FileOutputStream(file);
		        InputStream is = c.getInputStream();

		        byte[] buffer = new byte[1024];
		        int len1 = 0;
		        while ((len1 = is.read(buffer)) != -1) {
		            fos.write(buffer, 0, len1);
		            // Check for cancel signal
		            if (MAIN_SERVICE != null && MAIN_SERVICE.cancelRecieved)
				        throw new RuntimeException("Cancel recieved from main service");
		        }
		        fos.close();
		        is.close();
		        createFileName = file.getAbsolutePath();
		        // Scan file with Android Mediascanner
		        MediaScannerConnection.scanFile(MAIN_SERVICE, new String[] {createFileName}, null, null);
		        
			} catch (Exception e) {
		        file.delete();
		        throw new RuntimeException(e);
			}
		}
		return createFileName;
	}
	
}

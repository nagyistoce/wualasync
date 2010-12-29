package com.laksrecordings.wualasync;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.io.File;
import org.xml.sax.Attributes;
import android.sax.Element;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;

public class WualaDirectoryReader {
	private String url;
	private String key;
	private ArrayList<WualaFile> wf;
	final String LOG_TAG = getClass().getSimpleName();
	private File dstPath;
	private DataHelper db;
	
	WualaDirectoryReader(String url, String key) {
		this.url = url;
		this.key = key;
	}
	
	public void setDB(DataHelper db) {
		this.db = db;
	}
	
	private void readWualaDir(String wualaurl, String relDirName) {
		final String reldir = relDirName;
		final ArrayList<WualaDirRecord> subdirs = new ArrayList<WualaDirRecord>();
		
		// Directories
		try {
	    	URL url = new URL(wualaurl.replace("www.wuala.com", "api.wuala.com/publicFolders")+"?key="+key+"&il=1&ff=0");
	        URLConnection conn = url.openConnection();
	        
	        RootElement root = new RootElement("result");
	        Element publicFolders = root.getChild("publicFolders");
	        Element item = publicFolders.getChild("item");
	        item.setStartElementListener(new StartElementListener(){
	            public void start(Attributes attributes) {
	            	if (attributes != null && attributes.getValue("", "isEmpty").equals("false")) {
	            		subdirs.add(new WualaDirRecord(
	            				WualaFile.encodeUrl(attributes.getValue("", "url")), 
	            				reldir+"/"+attributes.getValue("", "name")));
	            	}
	            }
	        });
            Xml.parse(conn.getInputStream(), Xml.Encoding.UTF_8, root.getContentHandler());
        } catch (Exception e) {
        	Log.d(LOG_TAG, "Dir: "+wualaurl+"; "+e.getMessage());
        	throw new RuntimeException(e);
        }
        
        if (SyncFilesService.cancelRecieved)
        	throw new RuntimeException("Cancel recieved from main service");

        // Files
        try {
	    	URL url = new URL(wualaurl.replace("www.wuala.com", "api.wuala.com/publicFiles")+"?key="+key+"&il=1&ff=0");
	        URLConnection conn = url.openConnection();
	        
	        RootElement root = new RootElement("result");
	        Element publicFiles = root.getChild("publicFiles");
	        Element items = publicFiles.getChild("items");
	        Element item = items.getChild("item");
	        item.setStartElementListener(new StartElementListener(){
	            public void start(Attributes attributes) {
	            	if (attributes != null) {
	            		WualaFile w = new WualaFile(reldir, attributes.getValue("", "name"), attributes.getValue("", "url"), 
		            			Integer.parseInt(attributes.getValue("", "size")));
	            		if (db != null && dstPath != null)
	            			db.insertWuala(dstPath.getPath()+w.getFullFilePath());
	            		if (dstPath == null || (dstPath != null && !w.exists(dstPath.getPath()))) {
	            			wf.add(w);
	            			Log.d(LOG_TAG, "Added to list: "+w.toString());
	            		} else {
	            			Log.d(LOG_TAG, "Rejected from list: "+w.toString());	            			
	            		}
	            	}
	            }
	        });
            Xml.parse(conn.getInputStream(), Xml.Encoding.UTF_8, root.getContentHandler());
        } catch (Exception e) {
        	Log.d(LOG_TAG, "File: "+wualaurl+"; "+e.getMessage());
        	throw new RuntimeException(e);
        } 
                
        if (SyncFilesService.cancelRecieved)
        	throw new RuntimeException("Cancel recieved from main service");

        // Read subdirectories
        for (int i = 0; i < subdirs.size(); i++) {
        	readWualaDir(subdirs.get(i).url, subdirs.get(i).relDirName);
        }
        
	}
	
	public void setDstPath(File dstPath) {
		this.dstPath = dstPath;		
	}
	
	public ArrayList<WualaFile> read() {
		if (db != null)
			db.deleteWualaAll();
		wf = new ArrayList<WualaFile>();
		try {
			readWualaDir(this.url, "");
		} catch (Exception e) {
			wf.clear();
            Log.e(LOG_TAG, "Read: "+e.getMessage());
        	throw new RuntimeException(e);
		}
		return wf;
	}
	
}

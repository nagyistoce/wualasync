package com.laksrecordings.wualasync;

public interface SyncFilesUIUpdaterListener {

	public void setFilename(String filename);
	public void setMaxFiles(int maxFiles);
	public void setCurrentFile(int currentFile);
	public void setPreparing();
	public void setNotActive();
	public void setStopping();
	public void setCannotExecute();
	public void setNotConfigured();
	public void setServiceStateChange();

}

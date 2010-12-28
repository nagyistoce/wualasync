package com.laksrecordings.wualasync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SyncFilesStartupIntentReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		// Startup service
		SyncFilesService.thisIsDeviceStartup();
		Intent svc = new Intent(arg0, SyncFilesService.class);
		arg0.startService(svc);
	}

}

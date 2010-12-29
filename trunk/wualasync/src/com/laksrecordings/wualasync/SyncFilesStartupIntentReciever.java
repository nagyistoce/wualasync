package com.laksrecordings.wualasync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SyncFilesStartupIntentReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		// Startup service
		InitExecutionService.delayStartup = true;
		Intent svc = new Intent(arg0, InitExecutionService.class);
		svc.putExtra("action", InitExecutionService.ADD_ACTION);
		arg0.startService(svc);
	}

}

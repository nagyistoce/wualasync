package com.laksrecordings.wualasync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ExecuteBroadcastReciever extends BroadcastReceiver {
	private String LOG_TAG = getClass().getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(LOG_TAG, "Execution message recieved");
		try {
			Intent svc = new Intent(context, SyncFilesService.class);
			context.startService(svc);
		} catch (Exception e) {
			Log.e(LOG_TAG, "Error executing service");
		}
	}

}

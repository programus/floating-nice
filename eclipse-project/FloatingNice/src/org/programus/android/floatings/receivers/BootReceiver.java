package org.programus.android.floatings.receivers;

import org.programus.android.floatings.Constants;
import org.programus.android.floatings.services.NetSpeedService;
import org.programus.android.floatings.services.SleepService;
import org.programus.android.floatings.utils.Utilities;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

	@SuppressWarnings("unchecked")
	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences sp = context.getSharedPreferences(Constants.GLOBAL_KEY, Context.MODE_PRIVATE);
		@SuppressWarnings("rawtypes")
		Class[] services = new Class[] {
				NetSpeedService.class,
				SleepService.class
		};
		for (Class<? extends Service> sClass : services) {
			if (sp.getBoolean(Utilities.getKey(sClass, Constants.ENABLED), false)) {
				Intent sIntent = new Intent(context, sClass);
				context.startService(sIntent);
			}
		}
	}
}

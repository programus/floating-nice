package org.programus.android.floatings.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OrientationChangedReceiver extends BroadcastReceiver {
	public static interface IOrientationChangedListener {
		void onOrientationChanged(int orientation);
	}
	
	public final static String BCAST_CONFIGCHANGED = "android.intent.action.CONFIGURATION_CHANGED";
	
	private IOrientationChangedListener listener;
	public OrientationChangedReceiver(IOrientationChangedListener listener) {
		this.listener = listener;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(BCAST_CONFIGCHANGED)) {
			int or = context.getResources().getConfiguration().orientation;
			this.listener.onOrientationChanged(or);
		}
	}
}

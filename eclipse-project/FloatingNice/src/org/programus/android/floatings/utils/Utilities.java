package org.programus.android.floatings.utils;

import android.app.Service;
import android.net.TrafficStats;

public class Utilities {
	public static String getKey(Class<? extends Service> sClass, String suffix) {
		return sClass.getCanonicalName() + suffix;
	}
	
	public static boolean isTrafficStatsSupported() {
		long mtx = TrafficStats.getMobileTxBytes();
		long wtx = TrafficStats.getTotalTxBytes();
		return mtx != TrafficStats.UNSUPPORTED && wtx != TrafficStats.UNSUPPORTED;
	}
}

package org.programus.android.floatings.utils;

import android.app.Service;
import android.net.TrafficStats;
 
/**
 * Utilities class
 * @author programus
 *
 */
public class Utilities {
    /**
     * generate keys for service status saving
     * @param sClass service class
     * @param suffix suffix for the status need to be saved.
     * @return the key
     */
	public static String getKey(Class<? extends Service> sClass, String suffix) {
		return sClass.getCanonicalName() + suffix;
	}
	
	/**
	 * return the support of traffic stats
	 * @return <code>true</code> if supported
	 */
	public static boolean isTrafficStatsSupported() {
		long mtx = TrafficStats.getMobileTxBytes();
		long wtx = TrafficStats.getTotalTxBytes();
		return mtx != TrafficStats.UNSUPPORTED && wtx != TrafficStats.UNSUPPORTED;
	}
}

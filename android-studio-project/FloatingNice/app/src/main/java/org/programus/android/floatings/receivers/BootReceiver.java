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

/**
 * An broadcast receiver to receive the boot broadcast so that the services
 * could be started on boot.
 * 
 * @author programus
 * 
 */
public class BootReceiver extends BroadcastReceiver {

    /*
     * (non-Javadoc)
     * 
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sp = context.getSharedPreferences(Constants.GLOBAL_KEY, Context.MODE_PRIVATE);

        // Services need to be started.
        @SuppressWarnings("rawtypes")
        Class[] services = new Class[] { NetSpeedService.class, SleepService.class };
        for (Class<? extends Service> sClass : services) {
            // Start the service only when the service is started in previous
            // saved record.
            if (sp.getBoolean(Utilities.getKey(sClass, Constants.ENABLED), false)) {
                Intent sIntent = new Intent(context, sClass);
                context.startService(sIntent);
            }
        }
    }
}

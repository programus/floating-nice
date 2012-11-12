package org.programus.android.floatings.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;

/**
 * This receiver is used to inform others who put a listener in that the screen
 * orientation had been changed.
 * <p>
 * <div><strong>Example</strong></div> <code>
 * <pre>
 *  public class SomeService extends Service implements IOrientationChangedListener {
 *  
 *  	private BroadcastReceiver receiver;
 *  	
 *  	&#064Override
 *  	public void onCreate() {
 *  		super.onCreate();
 *  		
 *  		IntentFilter filter = new IntentFilter();
 *  		filter.addAction(OrientationChangedReceiver.BCAST_CONFIGCHANGED);
 *  		if (this.receiver == null) {
 *  			receiver = new OrientationChangedReceiver(this);
 *  		}
 *  		this.registerReceiver(receiver, filter);
 *  	}
 *  	
 *  	&#064Override
 *  	public void onDestroy() {
 *  		super.onDestroy();
 *  		if (this.receiver != null) {
 *  			this.unregisterReceiver(receiver);
 *  		}
 *  	}
 *  	
 *  	&#064Override
 *  	public void onOrientationChanged(int orientation) {
 *  	    // your logic for orientation changed.
 *  	}
 *  }
 * </pre>
 * </code>
 * 
 * @author programus
 * 
 */
public class OrientationChangedReceiver extends BroadcastReceiver {
    /**
     * A listener interface to offer a callback function which will be called
     * when the screen orientation is changed.
     * 
     * @author programus
     * 
     */
    public static interface IOrientationChangedListener {
        /**
         * This method will be called after the screen orientation is changed.
         * 
         * @param orientation
         *            will be a value of
         *            {@link Configuration#ORIENTATION_LANDSCAPE},
         *            {@link Configuration#ORIENTATION_PORTRAIT}
         * @see Configuration
         */
        void onOrientationChanged(int orientation);
    }

    /**
     * The action name for intent filter
     */
    public final static String          BCAST_CONFIGCHANGED = "android.intent.action.CONFIGURATION_CHANGED";

    private IOrientationChangedListener listener;

    /**
     * Constructor of this class.
     * 
     * @param listener
     *            the listener will be called after screen orientation changed.
     */
    public OrientationChangedReceiver(IOrientationChangedListener listener) {
        this.listener = listener;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(BCAST_CONFIGCHANGED)) {
            int or = context.getResources().getConfiguration().orientation;
            if (this.listener != null) {
                this.listener.onOrientationChanged(or);
            }
        }
    }
}

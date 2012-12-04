package org.programus.android.floatings.services.core;

import org.programus.android.floatings.receivers.OrientationChangedReceiver;
import org.programus.android.floatings.viewhelper.MovableFloatingView;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;

/**
 * The service to display the floating views. All floating views must extends this class.
 * @author programus
 *
 */
public abstract class FloatingService extends Service implements OnTouchListener {
	
    /** an instance of the view helper */
	private MovableFloatingView view;
	private static int notificationId = 2;
	/** a notification to keep the service alive when task manager try to kill it */
	private static Notification notification;
	
	/** the window manager to add or remove views */
	private WindowManager wm;
	/** a receiver to receive orientation change broadcast */
	private BroadcastReceiver receiver;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("DEBUG", "Service is created");
		// initialize member variables
		this.wm = (WindowManager) this.getSystemService(WINDOW_SERVICE);
		View v = this.getFloatingView();
		this.view = new MovableFloatingView(v, this.getClass().getCanonicalName());
		this.view.setOnTouchListener(this);
		Point p = this.getInitPosition();
		this.view.init(this.wm, p.x, p.y);
		if (notification == null) {
		    // build a notification to show nothing just to keep this service alive
			NotificationCompat.Builder builder = new NotificationCompat.Builder(this.getApplicationContext());
			notification = builder.build();
		}
		
		// set up the orientation change receiver.
		IntentFilter filter = new IntentFilter();
		filter.addAction(OrientationChangedReceiver.BCAST_CONFIGCHANGED);
		if (this.receiver == null) {
			receiver = new OrientationChangedReceiver(this.view);
		}
		this.registerReceiver(receiver, filter);
	}
	
	/**
	 * set up the view
	 * @return the real view to show
	 */
	protected abstract View getFloatingView();
	
	/**
	 * set up the initial point to display the view
	 * @return the initial point
	 */
	protected Point getInitPosition() {
		Point p = new Point(0, 0);
		return p;
	}
	
	/**
	 * detect whether the view is being moved
	 * @return <code>true</code> if moving
	 */
	public boolean isMoving() {
		return this.view.isMoving();
	}
	
	/**
	 * detect whether the view is moved
	 * @return <code>true</code> if moved
	 */
	public boolean isMoved() {
		return this.view.isMoved();
	}
	
	/**
	 * detect whether the view is being touched
	 * @return <code>true</code> if being touched
	 */
	public boolean isTouching() {
		return this.view.isTouching();
	}
	
	/**
	 * force stable means this view is forced stable
	 * @return <code>true</code> if forced stable
	 */
	public boolean isForceStable() {
		return this.view.isForceStable();
	}
	
	/**
	 * set the force stable status.
	 * @param stable <code>true</code> if force stable
	 */
	public void setForceStable(boolean stable) {
		this.view.setForceStable(stable);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.d("DEBUG", "Service is started");
		this.view.updateViewPosition();
		this.startForeground(notificationId, notification);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.receiver != null) {
			this.unregisterReceiver(receiver);
		}
		Log.d("DEBUG", "Service is destroyed");
		this.view.removeView();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (v != null) {
				v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
			}
		}
		return false;
	}

	protected WindowManager getWm() {
		return wm;
	}
	
	protected Point getAdjustedViewPosition(Point p) {
	    return this.view.getAdjustedViewPosition(p);
	}
}

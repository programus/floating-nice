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
import android.view.WindowManager.LayoutParams;

public abstract class FloatingService extends Service implements OnTouchListener {
	
	private MovableFloatingView view;
	private static int notificationId = 2;
	private static Notification notification;
	
	private WindowManager wm;
	private BroadcastReceiver receiver;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("DEBUG", "Service is created");
		this.wm = (WindowManager) this.getSystemService(WINDOW_SERVICE);
		View v = this.getFloatingView();
		this.view = new MovableFloatingView(v, this.getClass().getCanonicalName());
		this.view.setOnTouchListener(this);
		Point p = this.getInitPosition();
		this.view.init(this.wm, p.x, p.y);
		if (notification == null) {
			NotificationCompat.Builder builder = new NotificationCompat.Builder(this.getApplicationContext());
//			builder.setSmallIcon(android.R.drawable.ic_dialog_info);
//			builder.setContentTitle("Floating");
//			builder.setContentText("Running...");
			notification = builder.build();
		}
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(OrientationChangedReceiver.BCAST_CONFIGCHANGED);
		if (this.receiver == null) {
			receiver = new OrientationChangedReceiver(this.view);
		}
		this.registerReceiver(receiver, filter);
	}
	
	protected abstract View getFloatingView();
	
	protected Point getInitPosition() {
		Point p = new Point(0, 0);
		return p;
	}
	
	public boolean isMoving() {
		return this.view.isMoving();
	}
	
	public boolean isMoved() {
		return this.view.isMoved();
	}
	
	public boolean isTouching() {
		return this.view.isTouching();
	}
	
	public boolean isForceStable() {
		return this.view.isForceStable();
	}
	
	public void setForceStable(boolean stable) {
		this.view.setForceStable(stable);
	}
	
	protected LayoutParams getLp() {
		return this.view.getLp();
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

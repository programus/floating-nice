package org.programus.android.floatings.viewhelper;

import org.programus.android.floatings.Constants;
import org.programus.android.floatings.receivers.OrientationChangedReceiver.IOrientationChangedListener;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

/**
 * The view helper class to locate, move, show, orientation change the view.
 * @author programus
 *
 */
public class MovableFloatingView implements IOrientationChangedListener {
	private View view;
	private SharedPreferences sp;
	private final static int THRESHOLD = 20;
	
	private OnTouchListener onTouchListener;
	
	private WindowManager wm;
	private LayoutParams lp;
	private boolean added;
	
	private Rect r = new Rect();
	
	private int orientation;
	
	private boolean moving;
	private boolean moved;
	private boolean touching;
	private boolean forceStable;
	
	private OnTouchListener touchMove = new OnTouchListener() {
		private float x;
		private float y;
	    private Point p = new Point();
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch(action) {
			case MotionEvent.ACTION_DOWN:
			    p = getAdjustedViewPosition(p);
				this.x = event.getRawX() - p.x;
				this.y = event.getRawY() - p.y;
				setTouching(true);
				setMoved(false);
				break;
			case MotionEvent.ACTION_MOVE:
//				Log.d("[x,y]", MessageFormat.format("({0},{1}), ({2},{3})", event.getRawX(), event.getRawY(), x, y));
			    p = getAdjustedViewPosition(p);
				int nx = (int)(event.getRawX() - x);
				int ny = (int)(event.getRawY() - y);
				int dx = Math.abs(nx - p.x);
				int dy = Math.abs(ny - p.y);
//				Log.d("[x,y]", String.format("(%f, %f) - (%f, %f) -> (dx, dy) = (%d, %d) / (%d, %d)", event.getRawX(), event.getRawY(), x, y, dx, dy, lp.x, lp.y));
				if (!isForceStable() && (isMoving() || dx > THRESHOLD || dy > THRESHOLD)) {
					MovableFloatingView.this.calcViewPostion((int)(nx), (int)(ny));
					MovableFloatingView.this.updateViewPosition();
					setMoving(true);
					setMoved(true);
				}
				break;
			case MotionEvent.ACTION_UP:
				setMoving(false);
				setTouching(false);
				save();
				break;
			}
			
			if (onTouchListener != null) {
				onTouchListener.onTouch(v, event);
			}
			return false;
		}
	};
	
	private void save() {
		SharedPreferences.Editor editor = sp.edit();
		editor.putInt(Constants.X, lp.x);
		editor.putInt(Constants.Y, lp.y);
		editor.putInt(Constants.OR, this.orientation);
		editor.commit();
	}

	/**
	 * constructor
	 * @param view the view to show, move, ... etc.
	 * @param key the key to save/restore the data of the view
	 */
	public MovableFloatingView(View view, String key) {
		this.view = view;
		this.sp = view.getContext().getSharedPreferences(key, Context.MODE_PRIVATE);
	}
	
	/**
	 * return the position in the screen. 
	 * 
	 * Normally, the position might be out of the screen, but the view is still be shown in the screen. 
	 * This method returns the position which is be shown.
	 * @param p the raw position
	 * @return the adjusted position
	 */
	public Point getAdjustedViewPosition(Point p) {
	    int x = lp.x;
	    int y = lp.y;
	    this.view.getRootView().getWindowVisibleDisplayFrame(r);
	    if (x < 0) {
	        x = 0;
	    } else if (x + this.view.getWidth() > r.width()) {
	        x = r.width() - this.view.getWidth();
	    }
	    if (y < 0) {
	        y = 0;
	    } else if (y + this.view.getHeight() > r.height()) {
	        y = r.height() - this.view.getHeight();
	    }
	    if (p == null) {
	        p = new Point(x, y);
	    } else {
	        p.x = x;
	        p.y = y;
	    }
	    return p;
	}

	/**
	 * update view position
	 */
	public void updateViewPosition() {
		if (this.added) {
			this.wm.updateViewLayout(this.view, this.lp);
		} else {
			this.wm.addView(this.view, this.lp);
			this.added = true;
		}
	}
	
	/**
	 * hide view
	 */
	public void removeView() {
		if (this.added) {
			this.wm.removeView(this.view);
			this.added = false;
		}
	}
	
	protected void calcViewPostion(int x, int y) {
		this.lp.x = x;
		this.lp.y = y;
//		Log.d("[x,y]", MessageFormat.format("({0}, {1})", lp.x, lp.y));
	}

	/**
	 * init the view
	 * @param wm
	 * @param x
	 * @param y
	 */
	public void init(WindowManager wm, int x, int y) {
		this.wm = wm;
		this.lp = new LayoutParams(
				LayoutParams.WRAP_CONTENT, 
				LayoutParams.WRAP_CONTENT, 
				LayoutParams.TYPE_SYSTEM_ERROR,
				LayoutParams.FLAG_NOT_FOCUSABLE, 
				PixelFormat.TRANSPARENT);
		this.lp.gravity = Gravity.TOP | Gravity.LEFT;
		this.lp.x = this.sp.getInt(Constants.X, x);
		this.lp.y = this.sp.getInt(Constants.Y, y);
		this.orientation = this.sp.getInt(Constants.OR, Configuration.ORIENTATION_PORTRAIT);
		
		this.onOrientationChanged( this.view.getResources().getConfiguration().orientation);
		
		this.view.setOnTouchListener(this.touchMove);
	}
	
	/**
	 * init the view by the (0, 0) position as default
	 * @param wm
	 */
	public void init(WindowManager wm) {
		this.init(wm, 0, 0);
	}

	/**
	 * 
	 * @return true if moving
	 */
	public boolean isMoving() {
		return moving;
	}

	/**
	 * set the moving status
	 * @param moving
	 */
	public void setMoving(boolean moving) {
		this.moving = moving;
	}

	/**
	 * 
	 * @return true if touching
	 */
	public boolean isTouching() {
		return touching;
	}

	/**
	 * set the touching status
	 * @param touching
	 */
	public void setTouching(boolean touching) {
		this.touching = touching;
	}

	/**
	 * 
	 * @return true if force stable 
	 */
	public boolean isForceStable() {
		return forceStable;
	}

	/**
	 * set the force stable status
	 * @param forceStable
	 */
	public void setForceStable(boolean forceStable) {
		this.forceStable = forceStable;
		if (forceStable) {
			this.setMoving(false);
		}
	}

	/**
	 * set the touch listener need to process the touch event
	 * @param onTouchListener
	 */
	public void setOnTouchListener(OnTouchListener onTouchListener) {
		this.onTouchListener = onTouchListener;
	}

	/**
	 * 
	 * @return the moved status
	 */
	public boolean isMoved() {
		return moved;
	}

	/**
	 * set the moved status
	 * @param moved
	 */
	public void setMoved(boolean moved) {
		this.moved = moved;
	}

	@Override
	public void onOrientationChanged(int orientation) {
		Log.d("ORC", String.format("Orientation: %d -> %d", this.orientation, orientation));
		if (this.orientation != orientation) {
			View root = this.view.getRootView();
			root.getWindowVisibleDisplayFrame(r);
			int x = lp.x;
			int y = lp.y;
			int vw = this.view.getWidth();
			int vh = this.view.getHeight();
			int w = r.width();
			int h = r.height();
			int ow = h + r.top;
			int oh = w - r.top;
			Log.d("ORC", String.format("(%d, %d), (%d, %d), (%d, %d)", x, y, vw, vh, ow, oh));
			lp.x = (x < (ow - vw) >> 1) ? x * w / ow : (x * w + (w - ow) * vw) / ow;
			lp.y = (y < (oh - vh) >> 1) ? y * h / oh : (y * h + (h - oh) * vh) / oh;
			Log.d("ORC", String.format("(%d, %d), (%d, %d), (%d, %d)", lp.x, lp.y, vw, vh, w, h));
			
			this.orientation = orientation;
			this.updateViewPosition();
			this.onTouchListener.onTouch(this.view, MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));
		}
	}
}

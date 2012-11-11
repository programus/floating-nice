package org.programus.android.floatings.viewhelper;

import org.programus.android.floatings.Constants;
import org.programus.android.floatings.receivers.OrientationChangedReceiver.IOrientationChangedListener;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

public class MovableFloatingView implements IOrientationChangedListener {
	private View view;
	private SharedPreferences sp;
	private final static int THRESHOLD = 20;
	
	private OnTouchListener onTouchListener;
	
	private WindowManager wm;
	private LayoutParams lp;
	private boolean added;
	
	private int orientation;
	
	private boolean moving;
	private boolean moved;
	private boolean touching;
	private boolean forceStable;
	
	private OnTouchListener touchMove = new OnTouchListener() {
		private float x;
		private float y;
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch(action) {
			case MotionEvent.ACTION_DOWN:
				this.x = event.getRawX() - lp.x;
				this.y = event.getRawY() - lp.y;
				setTouching(true);
				setMoved(false);
				break;
			case MotionEvent.ACTION_MOVE:
//				Log.d("[x,y]", MessageFormat.format("({0},{1}), ({2},{3})", event.getRawX(), event.getRawY(), x, y));
				int nx = (int)(event.getRawX() - x);
				int ny = (int)(event.getRawY() - y);
				int dx = Math.abs(nx - lp.x);
				int dy = Math.abs(ny - lp.y);
//				Log.d("[x,y]", String.format("(dx, dy) = (%f, %f)", dx, dy));
				if (!isForceStable() && (isMoving() || dx > THRESHOLD || dy > THRESHOLD)) {
					MovableFloatingView.this.calcViewPostion((int)(nx), (int)(ny));
					MovableFloatingView.this.updateViewPostion();
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

	public MovableFloatingView(View view, String key) {
		this.view = view;
		this.sp = view.getContext().getSharedPreferences(key, Context.MODE_PRIVATE);
	}

	public void updateViewPostion() {
		if (this.added) {
			this.wm.updateViewLayout(this.view, this.lp);
		} else {
			this.wm.addView(this.view, this.lp);
			this.added = true;
		}
	}
	
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
	
	public void init(WindowManager wm) {
		this.init(wm, 0, 0);
	}

	public boolean isMoving() {
		return moving;
	}

	public void setMoving(boolean moving) {
		this.moving = moving;
	}

	public boolean isTouching() {
		return touching;
	}

	public void setTouching(boolean touching) {
		this.touching = touching;
	}

	public boolean isForceStable() {
		return forceStable;
	}

	public void setForceStable(boolean forceStable) {
		this.forceStable = forceStable;
		if (forceStable) {
			this.setMoving(false);
		}
	}

	public LayoutParams getLp() {
		return lp;
	}

	public OnTouchListener getOnTouchListener() {
		return onTouchListener;
	}

	public void setOnTouchListener(OnTouchListener onTouchListener) {
		this.onTouchListener = onTouchListener;
	}

	public boolean isMoved() {
		return moved;
	}

	public void setMoved(boolean moved) {
		this.moved = moved;
	}

	@Override
	public void onOrientationChanged(int orientation) {
		Log.d("ORC", String.format("Orientation: %d -> %d", this.orientation, orientation));
		if (this.orientation != orientation) {
			View root = this.view.getRootView();
			Rect r = new Rect();
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
			this.updateViewPostion();
			this.onTouchListener.onTouch(this.view, MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));
		}
	}
}

package org.programus.android.floatings.services;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.programus.android.floatings.Constants;
import org.programus.android.floatings.R;
import org.programus.android.floatings.SettingsActivity;
import org.programus.android.floatings.activities.DialogActivity;
import org.programus.android.floatings.services.core.FloatingService;

import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The service for multi-function floating view.
 * @author programus
 *
 */
public class SleepService extends FloatingService {
	
	private DevicePolicyManager mDPM;
	private ImageView image;
	private Handler handler;
	private boolean extended;
	/** the directions: → ↓ ← ↑ */
	private int crossDir;
	/** the directions: ↘ ↙ ↖ ↗ */
	private int xDir;
	
	/** The view to expand the status bar */
	private ImageView down;
	private LayoutParams downLp;
	private boolean downAdded;
	
	private ImageView sound;
	private LayoutParams soundLp;
	private boolean soundAdded;
	
	/** The view to show clipboard content */
	private View clip;
	private LayoutParams clipLp;
	private TextView clipContent;
	private boolean clipAdded;
	private boolean clipHolded;
	
	private Point pos = new Point();
	
	private Bitmap[] menubmps = new Bitmap[4];
	
	private final static String STATUSBAR = "statusbar";
	private final static String EXPAND = "expand";
	private final static String SBM_CLASSNAME = "android.app.StatusBarManager";
	
	private final static int TIMEOUT = 1000;
	private final static int MARGIN = 5;
	
	private static class RestoreIconHandler extends Handler {
		private SleepService ss;
		public RestoreIconHandler(SleepService ss) {
			this.ss = ss;
		}
		@Override
		public void handleMessage(Message msg) {
			if (ss.image != null) {
				if (ss.isTouching()) {
					ss.lock(ss.image);
				}
				ss.restoreMenu(ss.image);
			}
		}
	};

	@Override
	protected View getFloatingView() {
		View v = LayoutInflater.from(this).inflate(R.layout.sleep_view, null);
		this.functionView(v);
		this.prepareImages();
		return v;
	}
	
	private void prepareImages() {
		Matrix matrix = new Matrix();
		Resources res = getResources();
		BitmapDrawable menu = (BitmapDrawable) res.getDrawable(R.drawable.menu);
		Bitmap orig = menu.getBitmap();
		this.menubmps[0] = orig;
		for (int i = 1; i < Constants.angles.length; i++) {
			int angle = Constants.angles[i];
			matrix.reset();
			matrix.postRotate(angle);
			Bitmap bm = Bitmap.createBitmap(orig, 0, 0, orig.getWidth(), orig.getHeight(), matrix, true);
			this.menubmps[i] = bm;
		}
	}
	
	private void rotateMenuImage(int dir) {
		if (this.image != null) {
			this.image.setImageBitmap(this.menubmps[dir]);
		}
	}
	
	private DevicePolicyManager getDPM() {
		if (this.mDPM == null) {
			this.mDPM = (DevicePolicyManager) this.getSystemService(Context.DEVICE_POLICY_SERVICE);
		}
		return this.mDPM;
	}

	private void functionView(View v) {
		image = (ImageView) v.findViewById(R.id.sleepImageView);
		this.initDownView();
		this.initSoundView();
		this.initClipView();
		if (this.handler == null) {
			this.handler = new RestoreIconHandler(this);
		}
		v.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Log.d("DEBUG", String.format("moving: %s", isMoving()));
				return extendMenu(image);
			}
		});
		
		v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				lock(image);
				restoreMenu(image);
				if (!isMoved()) {
					extendMenu(image);
				}
			}
		});
	}
	
	private void initClipView() {
		this.clip = LayoutInflater.from(this).inflate(R.layout.clipboard_view, null);
		this.clipContent = (TextView) this.clip.findViewById(R.id.clipTextView);
		this.clipLp = new LayoutParams(
				LayoutParams.WRAP_CONTENT, 
				LayoutParams.WRAP_CONTENT, 
				LayoutParams.TYPE_SYSTEM_ERROR,
				LayoutParams.FLAG_NOT_FOCUSABLE, 
				PixelFormat.TRANSPARENT);
		this.clipLp.gravity = Gravity.TOP | Gravity.LEFT;
		
		this.clip.setOnTouchListener(new OnTouchListener() {
			private long lastHoldTime;
			private long lastMoveTime;
			private PointF lastHoldPoint = new PointF();
			private final static int THRESHOLD = 5;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					lastHoldTime = event.getDownTime();
					lastHoldPoint.x = event.getX();
					lastHoldPoint.y = event.getY();
					clipHolded = true;
					v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
					break;
				case MotionEvent.ACTION_MOVE:
					lastMoveTime = event.getEventTime();
					if (PointF.length(event.getX() - lastHoldPoint.x, event.getY() - lastHoldPoint.y) > THRESHOLD) {
						lastHoldPoint.x = event.getX();
						lastHoldPoint.y = event.getY();
						lastHoldTime = event.getEventTime();
					}
					break;
				case MotionEvent.ACTION_UP:
					if (event.getEventTime() - lastMoveTime < 100 && event.getEventTime() - lastHoldTime < 500) {
						Intent intent = new Intent(SleepService.this, DialogActivity.class); 
						intent.putExtra(DialogActivity.CLASS_KEY, SleepService.class.getCanonicalName());
						intent.putExtra(DialogActivity.TITLE_KEY, R.string.clear_clip_confirm_title);
						intent.putExtra(DialogActivity.MESSAGE_KEY, R.string.clear_clip_confirm_text);
						intent.putExtra(DialogActivity.POS_BUTTON_KEY, R.string.clear_confirm_ok);
						intent.putExtra(DialogActivity.NEG_BUTTON_KEY, R.string.clear_confirm_cancel);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
					} else {
						hideClipViewConditionallyWhenTouchUp();
					}
					break;
				}
				return false;
			}
		});
	}
	
	private int getDialogResult(Intent intent) {
		int result = -1;
		Bundle extras = intent.getExtras();
		if (extras != null) {
			result = extras.getInt(DialogActivity.RESULT_KEY, -1);
		}
		return result;
	}
	
	private void clearClipboardConditionally(int result) {
		if (result >= 0) {
			if (result == DialogActivity.RESULT_OK) {
				this.clearClipboard();
			}
			this.hideClipViewConditionallyWhenTouchUp();
		}
	}
	
	private void hideClipViewConditionallyWhenTouchUp() {
		clipHolded = false;
		if (!extended) {
			hideClipView();
		}
	}
	
	private void toggleRingMode() {
	    AudioManager am = (AudioManager) this.getSystemService(AUDIO_SERVICE);
	    am.setRingerMode(am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL ? 
	        AudioManager.RINGER_MODE_VIBRATE : AudioManager.RINGER_MODE_NORMAL);
	}
	
	private void initSoundView() {
	    View v = LayoutInflater.from(this).inflate(R.layout.sound_view, null);
	    this.sound = (ImageView) v.findViewById(R.id.soundImageView);
	    this.soundLp = new LayoutParams(
	        LayoutParams.WRAP_CONTENT,
	        LayoutParams.WRAP_CONTENT,
	        LayoutParams.TYPE_SYSTEM_ERROR,
	        LayoutParams.FLAG_NOT_FOCUSABLE,
	        PixelFormat.TRANSPARENT);
	    this.soundLp.gravity = Gravity.TOP | Gravity.LEFT;
	    
	    this.sound.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            toggleRingMode();
	            setSoundImage();
	            restoreMenu(image);
	            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
	        }
	    });
	}
	
	private void initDownView() {
		View v = LayoutInflater.from(this).inflate(R.layout.down_view, null);
		this.down = (ImageView) v.findViewById(R.id.downImageView);
		this.downLp = new LayoutParams(
				LayoutParams.WRAP_CONTENT, 
				LayoutParams.WRAP_CONTENT, 
				LayoutParams.TYPE_SYSTEM_ERROR,
				LayoutParams.FLAG_NOT_FOCUSABLE, 
				PixelFormat.TRANSPARENT);
		this.downLp.gravity = Gravity.TOP | Gravity.LEFT;
		
		this.down.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				expandStatusBar();
				restoreMenu(image);
			}
		});
		
		this.down.setOnTouchListener(new OnTouchListener() {
			private float x;
			private float y;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					x = event.getRawX();
					y = event.getRawY();
					break;
				case MotionEvent.ACTION_UP:
					float dx = event.getRawX() - x;
					float dy = event.getRawY() - y;
					float length = PointF.length(dx, dy);
					if (length > v.getWidth()) {
						SettingsActivity.startActivity(getApplicationContext());
						restoreMenu(image);
					}
					break;
				}
				return false;
			}
		});
	}
	
	private void expandStatusBar() {
		Object service = this.getSystemService(STATUSBAR);
		try {
			Class<?> statusBarManagerClass = Class.forName(SBM_CLASSNAME);
			Method expand = statusBarManagerClass.getMethod(EXPAND);
			expand.invoke(service);
		} catch (IllegalArgumentException e) {
			Log.d("ERROR", "expand status bar", e);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			Log.d("ERROR", "expand status bar", e);
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			Log.d("ERROR", "expand status bar", e);
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			Log.d("ERROR", "expand status bar", e);
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			Log.d("ERROR", "expand status bar", e);
			e.printStackTrace();
		}
	}
	
	private Rect getVisibleRect(View v) {
		View root = v.getRootView();
		Rect r = new Rect();
		root.getWindowVisibleDisplayFrame(r);
		return r;
	}
	
	private void showDownView(ImageView image, int dir) {
		Rect r = this.getVisibleRect(image);
		if (!this.downAdded && r.top > 0) {
			int w = image.getWidth() + MARGIN;
			int h = image.getHeight() + MARGIN;
	        Point p = this.getAdjustedViewPosition(pos);
			int[] offset = Constants.offsets[dir];
			this.downLp.x = p.x + w * 2 * offset[0];           // double width because there is the sound icon
			this.downLp.y = p.y + h * 2 * offset[1];   // double height because there is the sound icon
			this.getWm().addView(this.down, this.downLp);
			this.downAdded = true;
		}
	}
	
	private void setSoundImage() {
	    AudioManager am = (AudioManager) this.getSystemService(AUDIO_SERVICE);
	    int imageResource = am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL ? R.drawable.sound : R.drawable.mute;
	    this.sound.setImageResource(imageResource);
	}
	
	private void showSoundView(ImageView image, int dir) {
	    if (!this.soundAdded) {
	        this.setSoundImage();
	        int w = image.getWidth() + MARGIN;
	        int h = image.getHeight() + MARGIN;
	        Point p = this.getAdjustedViewPosition(pos);
	        int[] offset = Constants.offsets[dir];
	        this.soundLp.x = p.x + w * offset[0];
	        this.soundLp.y = p.y + h * offset[1];
	        this.getWm().addView(this.sound, this.soundLp);
	        this.soundAdded = true;
	    }
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private CharSequence getClipboardContent() {
		CharSequence ret = null;
		// comment out because of compiling error. seeking solutions to solve this. 
		int apiLevel = Build.VERSION.SDK_INT;
		if (apiLevel < Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager cm = (android.text.ClipboardManager) this.getSystemService(CLIPBOARD_SERVICE);
			if (cm != null && cm.hasText()) {
				ret = cm.getText();
			}
		} else {
			android.content.ClipboardManager cm = (android.content.ClipboardManager) this.getSystemService(CLIPBOARD_SERVICE);
			if (cm != null && cm.hasPrimaryClip()) {
				ClipData cd = cm.getPrimaryClip();
				if (cd.getItemCount() > 0) {
					ret = cd.getItemAt(0).coerceToText(this);
				}
			}
		}
		return ret;
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private void clearClipboard() {
		int apiLevel = Build.VERSION.SDK_INT;
		if (apiLevel < Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager cm = (android.text.ClipboardManager) this.getSystemService(CLIPBOARD_SERVICE);
			if (cm != null && cm.hasText()) {
				cm.setText("");
			}
		} else {
			android.content.ClipboardManager cm = (android.content.ClipboardManager) this.getSystemService(CLIPBOARD_SERVICE);
			if (cm != null && cm.hasPrimaryClip()) {
				ClipData cd = ClipData.newPlainText("", "");
				cm.setPrimaryClip(cd);
			}
		}
	}
	
	private void showClipView(View base, int dir, int xdir) {
		if (this.clip != null && !this.clipAdded) {
			CharSequence content = this.getClipboardContent();
			if (content != null && content.length() > 0) {
				this.clipContent.setText(content);
				
				this.clip.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
				int w = this.clip.getMeasuredWidth();
				int h = this.clip.getMeasuredHeight();
				int vw = base.getWidth();
				int vh = base.getHeight();
				
				int[][] offset = Constants.xoffsets[xdir][dir];
				int[] offsetX = offset[0];
				int[] offsetY = offset[1];
				
    	        Point p = this.getAdjustedViewPosition(pos);
    			
				this.clipLp.x = p.x + vw * offsetX[0] + MARGIN * offsetX[1] + w * offsetX[2];
				this.clipLp.y = p.y + vh * offsetY[0] + MARGIN * offsetY[1] + h * offsetY[2];
				this.getWm().addView(this.clip, this.clipLp);
				this.clipAdded = true;
			}
		}
	}
	
	private void hideSoundView() {
	    if (this.soundAdded) {
	        this.getWm().removeView(this.sound);
	        this.soundAdded = false;
	    }
	}
	
	private void hideDownView() {
		if (this.downAdded) {
			this.getWm().removeView(this.down);
			this.downAdded = false;
		}
	}
	
	private void hideClipView() {
		if (this.clipAdded) {
			this.getWm().removeView(this.clip);
			this.clipAdded = false;
		}
	}
	
	private boolean extendMenu(ImageView image) {
		boolean ret = !extended && !isMoving();
		if (ret) {
			image.setImageResource(R.drawable.lock);
			this.showDownView(image, this.crossDir);
			this.showSoundView(image, this.crossDir);
			this.showClipView(image, this.crossDir, this.xDir);
			extended = true;
			Log.d("DEBUG", "changed to big icon");
			handler.sendEmptyMessageDelayed(0, TIMEOUT);
			this.setForceStable(true);
		}
		
		return ret;
	}
	
	private void restoreMenu(ImageView image) {
		if (extended) {
			this.rotateMenuImage(this.crossDir);
			this.hideDownView();
			this.hideSoundView();
			if (!this.clipHolded) {
				this.hideClipView();
			}
			extended = false;
			this.setForceStable(false);
			Log.d("DEBUG", "changed to small icon");
		}
	}
	
	private void lock(ImageView image) {
		if (extended) {
			this.getDPM().lockNow();
			this.vibrate();
		}
	}
	
	private void vibrate() {
		this.image.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		super.onTouch(v, event);
		if (this.isMoving() || event.getAction() == MotionEvent.ACTION_CANCEL) {
			this.updateDir(v);
		}
		
		return false;
	}
	
	private void updateDir(View v) {
		Rect bdRect = this.getVisibleRect(v);
		Point p = this.getAdjustedViewPosition(pos);
		int x = p.x + (this.menubmps[0].getWidth() >> 1);
		int y = p.y + (this.menubmps[0].getHeight() >> 1);
		y -= bdRect.top;
		Log.d("DIR", String.format("(%d, %d)", x, y));
		int bx = bdRect.width();
		int by = bdRect.height();
		double a = (double) by / bx;
		double b = -a;
		
		double my = x * a;
		double ny = x * b + by;
		
		if (y < my) {
			if (y < ny) {
				this.crossDir = 3;
			} else {
				this.crossDir = 0;
			}
		} else {
			if (y < ny) {
				this.crossDir = 2;
			} else {
				this.crossDir = 1;
			}
		}
		
		int ox = (bx >> 1);
		int oy = (by >> 1);
		
		if (y > oy) {
			if (x > ox) {
				this.xDir = 0;
			} else {
				this.xDir = 1;
			}
		} else {
			if (x < ox) {
				this.xDir = 2;
			} else {
				this.xDir = 3;
			}
		}
		
		Log.d("DIR", String.format("+dir: %d / xdir: %d", crossDir, xDir));
		
		this.rotateMenuImage(this.crossDir);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int ret = super.onStartCommand(intent, flags, startId);
		int dialogResult = this.getDialogResult(intent);
		if (dialogResult >= 0) {
			this.clearClipboardConditionally(dialogResult);
		} else {
			this.updateDir(this.image);
		}
		return ret;
	}

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    @Override
    protected Point getInitPosition() {
        Point p = new Point();
        Display display = this.getWm().getDefaultDisplay();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
            p.x = display.getWidth();
            p.y = display.getHeight();
        } else {
            display.getSize(p);
        }
        
        p.x >>= 1;
        
        return p;
    }
}

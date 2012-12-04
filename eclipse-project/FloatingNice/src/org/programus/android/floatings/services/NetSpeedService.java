package org.programus.android.floatings.services;

import java.text.DecimalFormat;

import org.programus.android.floatings.R;
import org.programus.android.floatings.SettingsActivity;
import org.programus.android.floatings.services.core.FloatingService;

import android.content.Intent;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.TextView;

/**
 * The floating view to show the net speed
 * @author programus
 *
 */
public class NetSpeedService extends FloatingService {
	private View view;
	private TextView upValue;
	private TextView dnValue;
	
	private final static String[] UNITS = {" ", " K", " M", " G", " T", " P"};
	private final static int U = 1024;
	private final static String UP = "UP";
	private final static String DN = "DOWN";
	
	private boolean stopped;
	private long interval;
	
	/**
	 * the flag to stop loop
	 * @return <code>true</code> if loop is stopped flag
	 */
	public boolean isStopped() {
		return stopped;
	}
	
	/**
	 * set the stop loop flag
	 * @param stopped set this as <code>true</code> when the loop need to be stopped.
	 */
	public void setStopped(boolean stopped) {
		this.stopped = stopped;
	}
	
	/**
	 * return the interval for refreshing
	 * @return the interval
	 */
	public long getInterval() {
		return this.interval;
	}
	
	/**
	 * set the interval for refreshing
	 * @param interval the interval
	 */
	public void setInterval(long interval) {
		this.interval = interval;
	}
		
	private Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			Bundle data = msg.getData();
			updateText(data.getDouble(UP), data.getDouble(DN));
			return false;
		}
	});
	
	private Runnable netSpeedGetter = new Runnable() {
		private long prevTime = -1;
		private long prevTx = -1;
		private long prevRx = -1;
		
		@Override
		public void run() {
			while (!isStopped()) {
				long time = System.currentTimeMillis();
				long tx = TrafficStats.getMobileTxBytes() + TrafficStats.getTotalTxBytes();
				long rx = TrafficStats.getMobileRxBytes() + TrafficStats.getTotalRxBytes();
				if (prevTime >= 0) {
					long dt = time - this.prevTime;
					double dtx = (double)(tx - this.prevTx) * 1000 / dt;
					double drx = (double)(rx - this.prevRx) * 1000 / dt;
					Bundle data = new Bundle();
//					Log.d("DEBUG", String.format("prx=%d, rx=%d, drx=%f, dt=%d", this.prevRx, rx, drx, dt));
					data.putDouble(UP, dtx);
					data.putDouble(DN, drx);
					Message msg = new Message();
					msg.setData(data);
					handler.sendMessage(msg);
				}
				prevTime = time;
				prevTx = tx;
				prevRx = rx;
				try {
					Thread.sleep(prevTime >= 0 ? getInterval() : 10);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	};
	
	private Thread updatingThread;

	@Override
	protected View getFloatingView() {
		this.view = LayoutInflater.from(this).inflate(R.layout.netspeed_view, null);
		this.init();
		return this.view;
	}
	
	private void init() {
		this.upValue = (TextView) this.view.findViewById(R.id.upValue);
		this.dnValue = (TextView) this.view.findViewById(R.id.dnValue);
		this.view.setOnLongClickListener(new OnLongClickListener(){
            @Override
            public boolean onLongClick(View v) {
                if (!isMoving()) {
                    SettingsActivity.startActivity(NetSpeedService.this);
                }
                return false;
            }
		});
		this.setInterval(500);
		if (this.updatingThread == null || !this.updatingThread.isAlive()) {
			this.updatingThread = new Thread(this.netSpeedGetter);
			this.updatingThread.start();
		}
	}

	private void updateText(double up, double down) {
//		Log.d("DEBUG", String.format("UP: %f / DN: %f", up, down));
		if (upValue != null) {
			this.upValue.setText(this.getDisplayNumber(up));
		}
		if (dnValue != null) {
			this.dnValue.setText(this.getDisplayNumber(down));
		}
	}
	
	private String getDisplayNumber(double value) {
		String[] numFormats = {"0.##", "#0.#", "#0"};
		int i = 0;
		for (i = 0; i < UNITS.length; i++) {
			if (value >= 1000) {
				value /= U;
			} else {
				break;
			}
		}
		DecimalFormat format = new DecimalFormat(value >= 10 ? value >= 100 ? numFormats[2] : numFormats[1] : numFormats[0]);
		String num = format.format(value);
		
		return String.format("%6sB/s", num + UNITS[i]);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.setStopped(true);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		init();
		return super.onStartCommand(intent, flags, startId);
	}
}

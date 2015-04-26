package org.programus.android.floatings;

import org.programus.android.floatings.receivers.LockScreenReceiver;
import org.programus.android.floatings.services.NetSpeedService;
import org.programus.android.floatings.services.SleepService;
import org.programus.android.floatings.utils.Utilities;

import android.app.Activity;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * The main activity of this application which is also the settings UI
 * @author programus
 *
 */
public class SettingsActivity extends Activity {
	private SharedPreferences sp;
	private DevicePolicyManager dpm;
	private ComponentName cName;
	
	private class CheckedChangeListener implements OnCheckedChangeListener {
		private Class<? extends Service> serviceClass;
		
		public CheckedChangeListener(Class<? extends Service> serviceClass) {
			this.serviceClass = serviceClass;
		}
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			SharedPreferences.Editor editor = sp.edit();
			Intent intent = new Intent(SettingsActivity.this.getApplicationContext(), this.serviceClass);
			if (isChecked) {
				SettingsActivity.this.startService(intent);
				Log.d("DEBUG", "Service should be started");
			} else {
				SettingsActivity.this.stopService(intent);
				Log.d("DEBUG", "Service should be stopped");
			}
			editor.putBoolean(Utilities.getKey(this.serviceClass, Constants.ENABLED), isChecked);
			editor.commit();
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
		if (this.dpm == null) {
			this.dpm = (DevicePolicyManager) SettingsActivity.this.getSystemService(Context.DEVICE_POLICY_SERVICE);
		}
		if (this.sp == null) {
			sp = this.getApplicationContext().getSharedPreferences(Constants.GLOBAL_KEY, MODE_PRIVATE);
		}
		if (this.cName == null) {
			cName = new ComponentName(this, LockScreenReceiver.class);
		}
		
        CheckBox netCheckbox = (CheckBox) this.findViewById(R.id.netSwitchCheckbox);
        netCheckbox.setOnCheckedChangeListener(new CheckedChangeListener(NetSpeedService.class));
        
        final CheckBox sleepCheckbox = (CheckBox) this.findViewById(R.id.sleepSwitchCheckbox);
        final OnCheckedChangeListener sleepListener = new CheckedChangeListener(SleepService.class);
        sleepCheckbox.setOnCheckedChangeListener(sleepListener);
		
        CheckBox enableAdminCheckbox = (CheckBox) this.findViewById(R.id.enabledAdmin);
        enableAdminCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					if (!dpm.isAdminActive(cName)) {
						Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
						intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cName);
						intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, SettingsActivity.this.getString(R.string.add_admin_prompt));
						startActivityForResult(intent, 1);
					}
				} else {
					dpm.removeActiveAdmin(cName);
					sleepCheckbox.setEnabled(false);
					sleepCheckbox.setChecked(false);
//					sleepListener.onCheckedChanged(null, false);
				}
			}
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_settings, menu);
        return true;
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d("DEBUG", "Resume");
        CheckBox netCheckbox = (CheckBox) this.findViewById(R.id.netSwitchCheckbox);
        netCheckbox.setChecked(Utilities.isTrafficStatsSupported() ? sp.getBoolean(Utilities.getKey(NetSpeedService.class, Constants.ENABLED), false) : false);
        netCheckbox.setEnabled(Utilities.isTrafficStatsSupported());
        
        final CheckBox sleepCheckbox = (CheckBox) this.findViewById(R.id.sleepSwitchCheckbox);
		sleepCheckbox.setChecked(dpm.isAdminActive(cName) ? sp.getBoolean(Utilities.getKey(SleepService.class, Constants.ENABLED), false) : false);
		sleepCheckbox.setEnabled(dpm.isAdminActive(cName));
		
        CheckBox enableAdminCheckbox = (CheckBox) this.findViewById(R.id.enabledAdmin);
        enableAdminCheckbox.setChecked(dpm.isAdminActive(cName));
	}
	
	/**
	 * Start this activity. This method is used by other components to start this activity. 
	 * @param context
	 */
	public static void startActivity(Context context) {
		Intent intent = new Intent(context, SettingsActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra(Constants.SELF_START, true);
		context.startActivity(intent);
	}
}

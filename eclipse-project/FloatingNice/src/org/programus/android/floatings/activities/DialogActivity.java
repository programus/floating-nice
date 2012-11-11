package org.programus.android.floatings.activities;

import org.programus.android.floatings.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class DialogActivity extends Activity {
	private AlertDialog dialog;
	
	public final static String CLASS_KEY = "org.programus.android.floatings.SourceClass";
	public final static String RESULT_KEY = "org.programus.android.floatings.DialogActivity.Result";
	public final static int RESULT_OK = 1;
	public final static int RESULT_CANCEL = 0;

    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		AlertDialog.Builder builder;
		int apiLevel = Build.VERSION.SDK_INT;
        if (apiLevel < Build.VERSION_CODES.HONEYCOMB) {
        	 builder = new AlertDialog.Builder(this);
        } else if (apiLevel < Build.VERSION_CODES.ICE_CREAM_SANDWICH){
        	builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
        } else {
        	builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
        }
		builder.setTitle(R.string.clear_clip_confirm_title).setMessage(R.string.clear_clip_confirm_text);
		builder.setPositiveButton(R.string.clear_confirm_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				returnOk(true);
			}
		});
		builder.setNegativeButton(R.string.clear_confirm_cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				returnOk(false);
			}
		});
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				returnOk(false);
			}
		});
		this.dialog = builder.create();
		this.dialog.show();
    }
    
    private void returnOk(boolean ok) {
    	Intent srcIntent = this.getIntent();
    	Intent tgtIntent = new Intent();
    	String className = srcIntent.getExtras().getString(CLASS_KEY);
    	Log.d("DialogActivity", "Class Name: " + className);
    	ComponentName cn = new ComponentName(this.getApplicationContext(), className);
    	tgtIntent.setComponent(cn);
    	tgtIntent.putExtra(RESULT_KEY, ok ? RESULT_OK : RESULT_CANCEL);
    	this.startService(tgtIntent);
		this.finish();
    }
}

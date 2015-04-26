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

/**
 * This is an Activity just to show the AlertDialog. <br />
 * This is used to display the confirm dialog from services.
 * 
 * <p>
 * <div><strong>An example</strong></div> <div>Start this activity in service to
 * show the confirm dialog:</div> <code>
 * <pre>
 *      Intent intent = new Intent(SleepService.this, DialogActivity.class); 
 *      intent.putExtra(DialogActivity.CLASS_KEY, this.getClass().getName());
 *      intent.putExtra(DialogActivity.TITLE_KEY, title);
 *      intent.putExtra(DialogActivity.MESSAGE_KEY, message);
 *      intent.putExtra(DialogActivity.POS_BUTTON_KEY, okButton);
 *      intent.putExtra(DialogActivity.NEG_BUTTON_KEY, cancelButton);
 *      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
 *      startActivity(intent);
 * </pre>
 * </code> <div>Get the result from this activity:</div> <code>
 * <pre>
 *      &#064Override
 *      public int onStartCommand(Intent intent, int flags, int startId) {
 *          int ret = super.onStartCommand(intent, flags, startId);
 *          Bundle extras = intent.getExtras();
 *          if (extras != null) {
 *              int result = extras.getInt(DialogActivity.RESULT_KEY, -1);
 *              if (result >= 0) {
 *                  if (result == DialogActivity.RESULT_OK) {
 *                      // Your logic here...
 *                  }
 *              } else {
 *                  // Your other start logic here...
 *              }
 *          }
 *          return ret;
 *      }
 * </pre>
 * </code>
 * </p>
 * 
 * @author programus
 * 
 */
public class DialogActivity extends Activity {
    private AlertDialog        dialog;

    /**
     * Intent extras key to pass the original class which started this activity.
     * The value will used to start the service when finish.
     */
    public final static String CLASS_KEY      = "org.programus.android.floatings.SourceClass";
    /**
     * Intent extras key to pass the result back.
     */
    public final static String RESULT_KEY     = "org.programus.android.floatings.DialogActivity.Result";
    /** Intent extras key to pass title */
    public final static String TITLE_KEY      = "org.programus.android.floatings.Title";
    /** Intent extras key to pass message */
    public final static String MESSAGE_KEY    = "org.programus.android.floatings.Message";
    /** Intent extras key to pass label for positive button */
    public final static String POS_BUTTON_KEY = "org.programus.android.floatings.PositiveButton";
    /** Intent extras key to pass label for negative button */
    public final static String NEG_BUTTON_KEY = "org.programus.android.floatings.NegativeButton";

    /** Result OK */
    public final static int    RESULT_OK      = 1;
    /** Result Cancel */
    public final static int    RESULT_CANCEL  = 0;

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve data necessary for an AlertDialog from intent.
        Intent intent = this.getIntent();
        Bundle extras = intent.getExtras();
        int title = extras.getInt(TITLE_KEY);
        int message = extras.getInt(MESSAGE_KEY);
        int posButton = extras.getInt(POS_BUTTON_KEY);
        int negButton = extras.getInt(NEG_BUTTON_KEY);

        // Construct a builder to build dialog
        AlertDialog.Builder builder;
        // prepare different themes for different version
        int apiLevel = Build.VERSION.SDK_INT;
        if (apiLevel < Build.VERSION_CODES.HONEYCOMB) {
            builder = new AlertDialog.Builder(this);
        } else if (apiLevel < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
        } else {
            builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
        }

        if (title != 0) {
            builder.setTitle(title);
        }
        if (message != 0) {
            builder.setMessage(message);
        }
        if (posButton != 0) {
            builder.setPositiveButton(R.string.clear_confirm_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    returnResult(true);
                }
            });
        }
        if (negButton != 0) {
            builder.setNegativeButton(R.string.clear_confirm_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    returnResult(false);
                }
            });
        }
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                returnResult(false);
            }
        });

        // create and show dialog
        this.dialog = builder.create();
        this.dialog.show();
    }

    /**
     * Return result
     * 
     * @param ok
     *            <code>true</code> if the result is OK, <code>false</code>
     *            else.
     */
    private void returnResult(boolean ok) {
        // get caller which started this activity
        Intent srcIntent = this.getIntent();
        String className = srcIntent.getExtras().getString(CLASS_KEY);
        Log.d("DialogActivity", "Class Name: " + className);
        ComponentName cn = new ComponentName(this.getApplicationContext(), className);

        // start the caller again to pass the result back
        Intent tgtIntent = new Intent();
        tgtIntent.setComponent(cn);
        tgtIntent.putExtra(RESULT_KEY, ok ? RESULT_OK : RESULT_CANCEL);
        this.startService(tgtIntent);
        this.finish();
    }
}

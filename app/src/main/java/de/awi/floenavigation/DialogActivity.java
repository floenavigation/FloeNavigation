package de.awi.floenavigation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class DialogActivity extends Activity {

    private String dialogTitle;
    private String dialogMsg;
    private int dialogIcon;
    public static final String DIALOG_BUNDLE = "dialogBundle";
    public static final String DIALOG_TITLE = "title";
    public static final String DIALOG_MSG = "message";
    public static final String DIALOG_ICON = "icon";
    private static final String TAG = "DialogActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent callingIntent = getIntent();
        if(callingIntent.getExtras().containsKey(DIALOG_TITLE)){
            dialogTitle = callingIntent.getExtras().getString(DIALOG_TITLE);
        }
        if(callingIntent.getExtras().containsKey(DIALOG_MSG)){
            dialogMsg = callingIntent.getExtras().getString(DIALOG_MSG);
        }
        if(callingIntent.getExtras().containsKey(DIALOG_ICON)){
            dialogIcon = callingIntent.getExtras().getInt(DIALOG_ICON);
        }
        Log.d(TAG, dialogTitle);
        Log.d(TAG, dialogMsg);
        //setContentView(R.layout.activity_dialog);
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        alertDialog.setIcon(dialogIcon);
        alertDialog.setTitle(dialogTitle);
        alertDialog.setMessage(dialogMsg);
        alertDialog.show();

    }
}

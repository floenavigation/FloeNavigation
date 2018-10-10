package de.awi.floenavigation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import de.awi.floenavigation.initialsetup.GridSetupActivity;
import de.awi.floenavigation.initialsetup.SetupActivity;

public class DialogActivity extends Activity {

    private String dialogTitle;
    private String dialogMsg;
    private int dialogIcon;
    private boolean showDialogOptions = false;
    public static final String DIALOG_BUNDLE = "dialogBundle";
    public static final String DIALOG_TITLE = "title";
    public static final String DIALOG_MSG = "message";
    public static final String DIALOG_ICON = "icon";
    public static final String DIALOG_OPTIONS = "options";
    private static final String TAG = "DialogActivity";
    private AlertDialog alertDialog;

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
        if(callingIntent.getExtras().containsKey(DIALOG_OPTIONS)){
            showDialogOptions = true;
        }
        Log.d(TAG, dialogTitle);
        Log.d(TAG, dialogMsg);
        //setContentView(R.layout.activity_dialog);
        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);

        alertBuilder.setIcon(dialogIcon);
        alertBuilder.setTitle(dialogTitle);
        alertBuilder.setMessage(dialogMsg);


        if (showDialogOptions){
            alertBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //clearDatabase();
                    Intent intent = new Intent(getApplicationContext(), SetupActivity.class);
                    startActivity(intent);
                }
            });
            alertBuilder.setNegativeButton("Finish", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    alertDialog.cancel();
                    SetupActivity.runServices(getApplicationContext());
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                }
            });
        }

        alertDialog = alertBuilder.create();
        alertDialog.show();
    }

    private void clearDatabase(){
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());;
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            db.execSQL("delete from AIS_STATION_LIST");
            db.execSQL("delete from AIS_FIXED_STATION_POSITION");
            db.execSQL("delete from BASE_STATIONS");
        } catch (SQLiteException e){
            Toast.makeText(this, "Database Unavailable", Toast.LENGTH_LONG).show();
        }
    }
}
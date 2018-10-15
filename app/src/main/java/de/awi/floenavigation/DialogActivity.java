package de.awi.floenavigation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
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
    public static final String DIALOG_BETA = "beta";
    private static final String TAG = "DialogActivity";
    private AlertDialog alertDialog;
    private double receivedBeta = 0.0;

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
            showDialogOptions = callingIntent.getExtras().getBoolean(DIALOG_OPTIONS);
        }
        if(callingIntent.getExtras().containsKey(DIALOG_BETA)){
            receivedBeta = callingIntent.getExtras().getDouble(DIALOG_BETA);
        }

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
                    new CreateTablesOnStartup().execute();
                    showNavigationBar();
                    SetupActivity.runServices(getApplicationContext());
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                }
            });
        }

        Log.d(TAG, dialogTitle);
        Log.d(TAG, String.valueOf(showDialogOptions));
        alertDialog = alertBuilder.create();
        if(showDialogOptions){
            alertDialog.setCancelable(false);
            alertDialog.setCanceledOnTouchOutside(false);
        }

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

    private class CreateTablesOnStartup extends AsyncTask<Void,Void,Boolean> {

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            updateBetaTable(receivedBeta, db);
            return DatabaseHelper.createTables(db);

        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result){
                Log.d(TAG, "CreateTablesOnStartup Async Task: Database Error");
            }
        }
    }

    private void updateBetaTable(double recdBeta, SQLiteDatabase db){

        try {
            ContentValues beta = new ContentValues();
            beta.put(DatabaseHelper.beta, recdBeta);
            beta.put(DatabaseHelper.updateTime, SystemClock.elapsedRealtime());
            db.insert(DatabaseHelper.betaTable, null, beta);
            long test = DatabaseUtils.queryNumEntries(db, DatabaseHelper.betaTable);
            Log.d(TAG, String.valueOf(test));


        } catch(SQLException e){
            Log.d(TAG, "Error Updating Beta Table");
            e.printStackTrace();
        }

    }

    private void showNavigationBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
        decorView.setSystemUiVisibility(uiOptions);
    }
}

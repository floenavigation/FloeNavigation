package de.awi.floenavigation;

import android.app.Activity;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.view.View;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.widget.Toast;

import de.awi.floenavigation.initialsetup.GridSetupActivity;

public class AdminPageActivity extends ActionBarActivity {
    CardView gridConfigOption, SyncOption, adminPrivilegesOption, configParamsOption;
    Handler handler = new Handler();
    Runnable gridConfigRunnable = new Runnable() {
        @Override
        public void run() {
            gridConfigOption.setVisibility(View.VISIBLE);
        }
    };
    Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            SyncOption.setVisibility(View.VISIBLE);
        }
    };
    Runnable adminPrivilegesRunnable = new Runnable() {
        @Override
        public void run() {
            adminPrivilegesOption.setVisibility(View.VISIBLE);
        }
    };

    Runnable configParamsRunnable = new Runnable() {
        @Override
        public void run() {
            configParamsOption.setVisibility(View.VISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_page);
        dialogBoxDisplay();
        gridConfigOption = (CardView) findViewById(R.id.gridconfigcardView);
        handler.postDelayed(gridConfigRunnable, 100);
        SyncOption = (CardView) findViewById(R.id.synccardView);
        handler.postDelayed(syncRunnable, 500);
        adminPrivilegesOption = (CardView) findViewById(R.id.admincardView);
        handler.postDelayed(adminPrivilegesRunnable, 1000);
        configParamsOption = (CardView) findViewById(R.id.configparamcardView);
        handler.postDelayed(configParamsRunnable, 1500);

    }

    public void onClickListener(View view) {
    }

    public void onClickGridConfiguration(View view){
       if(!isSetupComplete()){
           Intent intent = new Intent(this, GridSetupActivity.class);
           startActivity(intent);
       } else{
           Toast.makeText(this, "Grid is already Setup", Toast.LENGTH_LONG).show();
           //For testing purposes has to be removed later.
           clearDatabase();
       }
    }

    private boolean isSetupComplete(){
        long count = 0;
        boolean success = true;
        try{

            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());;
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //db = databaseHelper.getReadableDatabase();
            count = DatabaseUtils.queryNumEntries(db, DatabaseHelper.stationListTable);
            //db.close();
        } catch (SQLiteException e){
            Toast.makeText(this, "Database Unavailable", Toast.LENGTH_LONG).show();
        }

        if (count < 2){
            success =  false;
        }
        return success;
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

    public void onClickConfigurationParams(View view) {
        Intent configActivityIntent = new Intent(this, ConfigurationActivity.class);
        startActivity(configActivityIntent);
    }

    public void onClickAdminPrivilegesListener(View view) {
        Intent adminUserPwdActIntent = new Intent(this, AdminUserPwdActivity.class);
        startActivity(adminUserPwdActIntent);
    }

    private void dialogBoxDisplay() {

        String popupMsg = "Please Give a Unique ID to this Tablet: ";
        String title = "Setup Tablet ID";
        Intent dialogIntent = new Intent(this, DialogActivity.class);
        dialogIntent.putExtra(DialogActivity.DIALOG_TITLE, title);
        dialogIntent.putExtra(DialogActivity.DIALOG_MSG, popupMsg);
        dialogIntent.putExtra(DialogActivity.DIALOG_ICON, R.drawable.ic_done_all_black_24dp);
        dialogIntent.putExtra(DialogActivity.DIALOG_OPTIONS, false);
        dialogIntent.putExtra(DialogActivity.DIALOG_TABLETID, true);
        startActivity(dialogIntent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);
    }
}

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

public class AdminPageActivity extends Activity {
    CardView options_1, options_2, options_3;
    Handler handler = new Handler();
    Runnable runnable_1 = new Runnable() {
        @Override
        public void run() {
            options_1.setVisibility(View.VISIBLE);
        }
    };
    Runnable runnable_2 = new Runnable() {
        @Override
        public void run() {
            options_2.setVisibility(View.VISIBLE);
        }
    };
    Runnable runnable_3 = new Runnable() {
        @Override
        public void run() {
            options_3.setVisibility(View.VISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_page);
        options_1 = (CardView) findViewById(R.id.cardView_1);
        handler.postDelayed(runnable_1, 100);
        options_2 = (CardView) findViewById(R.id.cardView_2);
        handler.postDelayed(runnable_2, 500);
        options_3 = (CardView) findViewById(R.id.cardView_3);
        handler.postDelayed(runnable_3, 1000);

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
        } catch (SQLiteException e){
            Toast.makeText(this, "Database Unavailable", Toast.LENGTH_LONG).show();
        }
    }
}

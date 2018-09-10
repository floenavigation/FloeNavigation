package de.awi.floenavigation;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class AngleCalculationService extends IntentService {

    private static final String TAG = "AngleCalcJobService";
    private static final int INITIALIZATION_SIZE = 2;
    private double[] stationLatitude;
    private double[] stationLongitude;
    final Handler mHandler;
    private int[] mmsi;


    public AngleCalculationService() {

        super("AngleCalculationService");

        this.mHandler = new Handler();
        stationLatitude = new double[INITIALIZATION_SIZE];
        stationLongitude = new double[INITIALIZATION_SIZE];
        mmsi = new int[INITIALIZATION_SIZE];
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

            Runnable betaRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        SQLiteOpenHelper databaseHelper = new DatabaseHelper(getApplicationContext());
                        SQLiteDatabase db = databaseHelper.getReadableDatabase();
                        Cursor cursor;

                        cursor = db.query(DatabaseHelper.baseStationTable,
                                new String[] {DatabaseHelper.mmsi}, null,
                                null, null, null, null);

                        if (cursor.moveToFirst()){
                            int index = 0;
                            do{
                                mmsi[index] = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.mmsi));
                                index++;
                            } while(cursor.moveToNext());
                            cursor.close();


                            cursor = db.query(DatabaseHelper.fixedStationTable,
                                    new String[] {DatabaseHelper.latitude, DatabaseHelper.longitude}, DatabaseHelper.mmsi + " = ?",
                                    new String[] {Integer.toString(mmsi[0]), Integer.toString(mmsi[1])},
                                    null, null, null);
                            if (cursor.moveToFirst()){
                                index = 0;
                                do{
                                    stationLatitude[index] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.latitude));
                                    stationLongitude[index] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.longitude));
                                    index++;
                                } while(cursor.moveToNext());
                                cursor.close();

                                double beta = NavigationFunctions.calculateAngleBeta(stationLatitude[0], stationLongitude[0], stationLatitude[1], stationLongitude[1]);
                                cursor = db.query(DatabaseHelper.betaTable, null, null, null,
                                        null, null, null);
                                ContentValues mContentValues = new ContentValues();
                                mContentValues.put(DatabaseHelper.beta, beta);
                                mContentValues.put(DatabaseHelper.updateTime, SystemClock.elapsedRealtime());
                                db.update(DatabaseHelper.betaTable, mContentValues, null, null);
                                cursor.close();
                                db.close();
                            }

                        }

                        mHandler.postDelayed(this, 10000);
                    }catch (SQLException e){
                        String text = "Database unavailable";
                        Log.d(TAG, text);
                    }
                }
            };

            /*Runnable alphaRunnable = new Runnable() {
                @Override
                public void run() {

                }
            };*/

            mHandler.postDelayed(betaRunnable, 10000);
            //mHandler.postDelayed(alphaRunnable, 10000);
        }
    }


}

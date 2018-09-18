package de.awi.floenavigation;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;

import android.database.Cursor;
import android.database.DatabaseUtils;
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
    private final Handler mHandler;
    private int[] mmsi;
    private static final int CALCULATION_TIME = 1000;
    private Cursor mBaseStnCursor, mFixedStnCursor, mBetaCursor;


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
                        SQLiteOpenHelper databaseHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                        //SQLiteOpenHelper databaseHelper = new DatabaseHelper(getApplicationContext());
                        SQLiteDatabase db = databaseHelper.getReadableDatabase();


                        mBaseStnCursor = db.query(DatabaseHelper.baseStationTable, new String[] {DatabaseHelper.mmsi}, null,
                                null, null, null, null);
                        if (mBaseStnCursor.getCount() != DatabaseHelper.NUM_OF_BASE_STATIONS){
                            Log.d(TAG, "Error Reading from Base Station Table ");
                        } else {
                            if (mBaseStnCursor.moveToFirst()) {
                                int index = 0;
                                do {
                                    mmsi[index] = mBaseStnCursor.getInt(mBaseStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                                    index++;
                                } while (mBaseStnCursor.moveToNext());
                                mBaseStnCursor.close();

                                betaAngleCalculation(db);
                                alphaAngleCalculation(db);

                            }
                        }
                        //db.close();
                        mHandler.postDelayed(this, CALCULATION_TIME);
                    }catch (SQLException e){
                        String text = "Database unavailable";
                        Log.d(TAG, text);
                    }
                }
            };


            mHandler.postDelayed(betaRunnable, CALCULATION_TIME);
        }
    }

    private void betaAngleCalculation(SQLiteDatabase db){
        //Beta Angle Calculation
        mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable,
                new String[]{DatabaseHelper.latitude, DatabaseHelper.longitude}, DatabaseHelper.mmsi + " = ?",
                new String[]{Integer.toString(mmsi[0]), Integer.toString(mmsi[1])},
                null, null, null);
        if (mFixedStnCursor.moveToFirst()) {
            int index = 0;
            do {
                stationLatitude[index] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
                stationLongitude[index] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
                index++;
            } while (mFixedStnCursor.moveToNext());
            mFixedStnCursor.close();

            double beta = NavigationFunctions.calculateAngleBeta(stationLatitude[0], stationLongitude[0], stationLatitude[1], stationLongitude[1]);
            Log.d(TAG, "Lat1: " + stationLatitude[0] + " Lon1: " + stationLongitude[0]);
            Log.d(TAG, "Lat2: " + stationLatitude[1] + " Lon2: " + stationLongitude[1]);
            Log.d(TAG, "Beta: " + String.valueOf(beta));
            ContentValues mContentValues = new ContentValues();
            mContentValues.put(DatabaseHelper.beta, beta);
            mContentValues.put(DatabaseHelper.updateTime, SystemClock.elapsedRealtime());
            db.update(DatabaseHelper.betaTable, mContentValues, null, null);
            mFixedStnCursor.close();
        }
    }

    private void alphaAngleCalculation(SQLiteDatabase db){
        //Alpha Angle Calculation
        long numOfEntries = DatabaseUtils.queryNumEntries(db, DatabaseHelper.fixedStationTable);
        if (numOfEntries > DatabaseHelper.NUM_OF_BASE_STATIONS) {
            //Alpha angle calculation
            double fixedStationLatitude;
            double fixedStationLongitude;
            double fixedStationBeta;
            int fixedStationMMSI;
            ContentValues mContentValues = new ContentValues(); //for updating alpha value
            mBetaCursor = db.query(DatabaseHelper.betaTable, new String[]{DatabaseHelper.beta, DatabaseHelper.updateTime},
                    null, null, null, null, null);
            if (mBetaCursor.getCount() != 1){
                Log.d(TAG, "Error reading from Beta Table");
            } else {
                if (mBetaCursor.moveToFirst()) {
                    fixedStationBeta = mBetaCursor.getDouble(mBetaCursor.getColumnIndex(DatabaseHelper.beta));

                    mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable,
                            new String[]{DatabaseHelper.latitude, DatabaseHelper.longitude, DatabaseHelper.mmsi}, DatabaseHelper.mmsi + " != ?",
                            new String[]{Integer.toString(mmsi[0]), Integer.toString(mmsi[1])},
                            null, null, null);
                    if (mFixedStnCursor.moveToFirst()) {
                        do {
                            fixedStationLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
                            fixedStationLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
                            fixedStationMMSI = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                            double theta = NavigationFunctions.calculateAngleBeta(stationLatitude[0], stationLongitude[0], fixedStationLatitude, fixedStationLongitude);
                            double alpha = Math.abs(theta - fixedStationBeta);
                            double distance = NavigationFunctions.calculateDifference(stationLatitude[0], stationLongitude[0], fixedStationLatitude, fixedStationLongitude);
                            double stationX = distance * Math.cos(Math.toRadians(alpha));
                            double stationY = distance * Math.sin(Math.toRadians(alpha));
                            Log.d(TAG, "Alpha: " + String.valueOf(alpha));
                            Log.d(TAG, "stationX: " + stationX + "stationY: " + stationY);
                            mContentValues.put(DatabaseHelper.alpha, alpha);
                            mContentValues.put(DatabaseHelper.xPosition, stationX);
                            mContentValues.put(DatabaseHelper.yPosition, stationY);
                            db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(fixedStationMMSI)});
                        } while (mFixedStnCursor.moveToNext());
                        mFixedStnCursor.close();
                    }
                    mBetaCursor.close();
                }
            }
        } else {
            Log.d(TAG, "No New Stations Deployed");
        }
    }


}

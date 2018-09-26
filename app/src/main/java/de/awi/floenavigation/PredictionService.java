package de.awi.floenavigation;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class PredictionService extends IntentService {

    private final static String TAG = "PREDICTION_SERVICE: ";
    private final Handler mPredictionHandler;
    private final int PREDICTION_TIME = 10000;

    public PredictionService() {
        super("PredictionService");
        mPredictionHandler = new Handler();
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

                Runnable predictionRunnable = new Runnable() {
                    @Override
                    public void run() {
                        try{
                            DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                            SQLiteDatabase db = dbHelper.getReadableDatabase();
                            Cursor mFixedStnCursor;
                            double stationLatitude, stationLongitude, stationSOG, stationCOG;
                            double[] predictedCoordinate;
                            int mmsi;

                            mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.recvdLatitude, DatabaseHelper.recvdLongitude,
                                    DatabaseHelper.sog, DatabaseHelper.cog},null, null, null, null, null);
                            if (mFixedStnCursor.moveToFirst()) {
                                do {
                                    mmsi = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                                    stationLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.recvdLatitude));
                                    stationLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.recvdLongitude));
                                    stationSOG = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.sog));
                                    stationCOG = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.cog));
                                    predictedCoordinate = NavigationFunctions.calculateNewPosition(stationLatitude, stationLongitude, stationSOG, stationCOG);
                                    ContentValues mContentValues = new ContentValues();
                                    mContentValues.put(DatabaseHelper.latitude, predictedCoordinate[0]);
                                    mContentValues.put(DatabaseHelper.longitude, predictedCoordinate[1]);
                                    mContentValues.put(DatabaseHelper.isPredicted, 1);
                                    db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[] {String.valueOf(mmsi)});
                                    Log.d(TAG, "Lat: " + stationLatitude + " Lon: " + stationLongitude);
                                    Log.d(TAG, "PredLat: " + predictedCoordinate[0] + "PredLon: " + predictedCoordinate[1]);
                                } while (mFixedStnCursor.moveToNext());
                                mFixedStnCursor.close();
                            }
                            else {
                                Log.d(TAG, "FixedStationTable Cursor Error");
                            }

                            mPredictionHandler.postDelayed(this, PREDICTION_TIME);
                        }catch (SQLException e){
                            String text = "Database unavailable";
                            Log.d(TAG, text);
                        }
                    }

                };
            mPredictionHandler.postDelayed(predictionRunnable, PREDICTION_TIME);
            }
        }
}

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
    private final int PREDICTION_TIME = 10 * 1000;
    public static int ERROR_THRESHOLD_VALUE;
    public static int PREDICTION_ACCURACY_THRESHOLD_VALUE;

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
                            int predictionAccuracy;
                            retrieveConfigurationParametersDatafromDB(db);

                            mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.latitude, DatabaseHelper.longitude,
                                    DatabaseHelper.recvdLatitude, DatabaseHelper.recvdLongitude, DatabaseHelper.sog, DatabaseHelper.cog, DatabaseHelper.predictionAccuracy},null, null, null, null, null);
                            if (mFixedStnCursor.moveToFirst()) {
                                do {
                                    mmsi = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                                    predictionAccuracy = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.predictionAccuracy));
                                    if (predictionAccuracy > PREDICTION_ACCURACY_THRESHOLD_VALUE) {
                                        stationLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
                                        stationLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
                                    }else{
                                        stationLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.recvdLatitude));
                                        stationLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.recvdLongitude));
                                    }
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

    private void retrieveConfigurationParametersDatafromDB(SQLiteDatabase db){
        try{
            Cursor configParamCursor = db.query(DatabaseHelper.configParametersTable, null, null,
                    null, null, null, null);
            String parameterName = null;
            int parameterValue = 0;

            if (configParamCursor.moveToFirst()){
                do{
                    parameterName = configParamCursor.getString(configParamCursor.getColumnIndex(DatabaseHelper.parameterName));
                    parameterValue = configParamCursor.getInt(configParamCursor.getColumnIndex(DatabaseHelper.parameterValue));

                    switch (parameterName) {
                        case DatabaseHelper.error_threshold:
                            ERROR_THRESHOLD_VALUE = parameterValue;
                            break;
                        case DatabaseHelper.prediction_accuracy_threshold:
                            PREDICTION_ACCURACY_THRESHOLD_VALUE = parameterValue;
                            break;
                    }
                }while (configParamCursor.moveToNext());
            }else {
                Log.d(TAG, "Config Parameter table cursor error");
            }
            configParamCursor.close();
        }catch (SQLException e){

            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        }
    }
}

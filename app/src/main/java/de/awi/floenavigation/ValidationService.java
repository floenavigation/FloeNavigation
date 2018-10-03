package de.awi.floenavigation;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
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
public class ValidationService extends IntentService {

    private final Handler mValidationHandler;
    private static final String TAG = "Validation Service: ";
    private static final int VALIDATION_TIME = 3 * 60 * 1000;
    private int[] baseStnMMSI = new int[DatabaseHelper.INITIALIZATION_SIZE];
    public static int ERROR_THRESHOLD_VALUE;
    public static int PREDICTION_ACCURACY_THRESHOLD_VALUE;


    public ValidationService() {
        super("ValidationService");
        this.mValidationHandler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

            Runnable validationRunnable = new Runnable() {
                @Override
                public void run() {

                    try{
                        SQLiteOpenHelper databaseHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                        SQLiteDatabase db = databaseHelper.getReadableDatabase();
                        baseStationsRetrievalfromDB(db);
                        retrieveConfigurationParametersDatafromDB(db);
                        Cursor mFixedStnCursor;
                        double fixedStnrecvdLatitude;
                        double fixedStnrecvdLongitude;
                        double fixedStnLatitude;
                        double fixedStnLongitude;
                        double evaluationDifference;
                        int predictionAccuracy;
                        int mmsi;

                        mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.recvdLatitude, DatabaseHelper.recvdLongitude,
                                DatabaseHelper.latitude, DatabaseHelper.longitude, DatabaseHelper.predictionAccuracy},null, null, null, null, null);
                        if (mFixedStnCursor.moveToFirst()){
                            do{
                                mmsi = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                                fixedStnrecvdLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.recvdLatitude));
                                fixedStnrecvdLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.recvdLongitude));
                                fixedStnLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
                                fixedStnLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
                                predictionAccuracy = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.predictionAccuracy));

                                if (predictionAccuracy > PREDICTION_ACCURACY_THRESHOLD_VALUE){
                                    //To be decided
                                    if (mmsi == baseStnMMSI[DatabaseHelper.firstStationIndex] || mmsi == baseStnMMSI[DatabaseHelper.secondStationIndex]){
                                        deleteEntryfromStationListTableinDB(mmsi, db);
                                    }else{
                                        deleteEntryfromStationListTableinDB(mmsi, db);
                                        deleteEntryfromFixedStationTableinDB(mmsi, db);
                                    }

                                }else {

                                    evaluationDifference = NavigationFunctions.calculateDifference(fixedStnLatitude, fixedStnLongitude, fixedStnrecvdLatitude, fixedStnrecvdLongitude);
                                    Log.d(TAG, "EvalDiff: " + String.valueOf(evaluationDifference) + " predictionAccInDb: " + predictionAccuracy);
                                    if (evaluationDifference > ERROR_THRESHOLD_VALUE) {
                                        ContentValues mContentValues = new ContentValues();
                                        mContentValues.put(DatabaseHelper.predictionAccuracy, ++predictionAccuracy);
                                        Log.d(TAG, "EvaluationDifference > Threshold: predictionAccuracy: " + String.valueOf(predictionAccuracy));
                                        db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
                                    }
                                }
                            } while(mFixedStnCursor.moveToNext());
                            mFixedStnCursor.close();
                        }else {
                            Log.d(TAG, "FixedStationTable Cursor Error");
                        }

                        mValidationHandler.postDelayed(this, VALIDATION_TIME);
                    }catch (SQLException e){
                        Log.d(TAG, String.valueOf(e));
                    }

                }
            };

            mValidationHandler.postDelayed(validationRunnable, VALIDATION_TIME);
        }
    }

    private void deleteEntryfromStationListTableinDB(int mmsiToBeRemoved, SQLiteDatabase db){
        db.delete(DatabaseHelper.stationListTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsiToBeRemoved)});
    }

    private void deleteEntryfromFixedStationTableinDB(int mmsiToBeRemoved, SQLiteDatabase db){
        db.delete(DatabaseHelper.fixedStationTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsiToBeRemoved)});
    }

    private void baseStationsRetrievalfromDB(SQLiteDatabase db){

        try {
            //SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            //SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor mBaseStnCursor = db.query(DatabaseHelper.baseStationTable, new String[]{DatabaseHelper.mmsi},
                    null, null, null, null, null);

            if (mBaseStnCursor.getCount() == DatabaseHelper.NUM_OF_BASE_STATIONS) {
                int index = 0;

                if (mBaseStnCursor.moveToFirst()) {
                    do {
                        baseStnMMSI[index] = mBaseStnCursor.getInt(mBaseStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                        index++;
                    } while (mBaseStnCursor.moveToNext());
                }else {
                    Log.d(TAG, "Base stn cursor error");
                }

            } else {
                Log.d(TAG, "Error reading from base stn table");
            }
            mBaseStnCursor.close();
        }catch (SQLException e){

            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
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

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
    private static final int ERROR_THRESHOLD_VALUE = 10;
    private static final int PREDICTION_ACCURACY_THRESHOLD_VALUE = 5;
    private static final int VALIDATION_TIME = 3 * 60 * 1000;

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
                                }

                                evaluationDifference = NavigationFunctions.calculateDifference(fixedStnLatitude, fixedStnLongitude, fixedStnrecvdLatitude, fixedStnrecvdLongitude);
                                Log.d(TAG, "EvalDiff: " + String.valueOf(evaluationDifference) + " predictionAccInDb: " + predictionAccuracy);
                                if (evaluationDifference > ERROR_THRESHOLD_VALUE){
                                    ContentValues mContentValues = new ContentValues();
                                    mContentValues.put(DatabaseHelper.predictionAccuracy, ++predictionAccuracy);
                                    Log.d(TAG, "EvaluationDifference > Threshold: predictionAccuracy: " + String.valueOf(predictionAccuracy));
                                    db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[] {String.valueOf(mmsi)});
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

}

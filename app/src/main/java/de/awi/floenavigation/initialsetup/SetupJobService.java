package de.awi.floenavigation.initialsetup;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import de.awi.floenavigation.DatabaseHelper;
import de.awi.floenavigation.NavigationFunctions;

/*
    Class for creating the Initial Setup task as a Background Job Service
    This Class is not currently being used and can be used in case performance from Timer is not according to requirements.
 */
public class SetupJobService extends JobService {
    private static final String TAG = "InitialSetupJobService";
    private static final int INITIALIZATION_SIZE = 2;
    private static final String LOCATION_PREDICTION = "PredictedLocations";


    private boolean jobCancelled = false;
    private double[] stationLatitude = new double[INITIALIZATION_SIZE];
    private double[] stationLongitude = new double[INITIALIZATION_SIZE];
    private double[] stationSOG = new double[INITIALIZATION_SIZE];
    private double[] stationCOG = new double[INITIALIZATION_SIZE];
    private double[] predictedLatitude = new double[INITIALIZATION_SIZE];
    private double[] predictedLongitude = new double[INITIALIZATION_SIZE];


    @Override
    public boolean onStartJob(JobParameters params){
        Log.d(TAG, "Job Started");
        new ReadParamsFromDB().execute();
        for(int i = 0; i < 2; i++){
            double[] predictedCoordinates = NavigationFunctions.calculateNewPosition(stationLatitude[i], stationLongitude[i], stationSOG[i], stationCOG[i]);
            predictedLatitude[i] = predictedCoordinates[0];
            predictedLongitude[i] = predictedCoordinates[1];
        }
        sendDataToActivity();
        Toast.makeText(this, "3 Min Job", Toast.LENGTH_LONG).show();
        return true;
    }

    private void sendDataToActivity(){
        Intent intent = new Intent(LOCATION_PREDICTION);
        if(jobCancelled){
            intent.putExtra("Cancelled", true);
        } else{
            intent.putExtra("Cancelled", false);
            intent.putExtra("Station_1_Latitude", predictedLatitude[0]);
            intent.putExtra("Station_1_Longitude", predictedLongitude[0]);
            intent.putExtra("Station_2_Latitude", predictedLatitude[1]);
            intent.putExtra("Station_2_Longitude", predictedLongitude[1]);
        }
        sendBroadcast(intent);

    }


    @Override
    public boolean onStopJob(JobParameters params){
        Log.d(TAG, "Job Cancelled");
        jobCancelled = true;
        return true;
    }

    private class ReadParamsFromDB extends AsyncTask<Void,Void,Boolean>{

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            DatabaseHelper helper = new DatabaseHelper(getApplicationContext());
            try{
                SQLiteDatabase db = helper.getReadableDatabase();
                Cursor cursor = db.query(DatabaseHelper.fixedStationTable,
                        new String[] {DatabaseHelper.latitude, DatabaseHelper.longitude, DatabaseHelper.sog, DatabaseHelper.cog, DatabaseHelper.mmsi},
                        null, null, null, null, null);
                if (cursor.moveToFirst()){
                    int i = 0;
                    do{
                        stationLatitude[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.latitude));
                        stationLongitude[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.longitude));
                        stationSOG[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.sog));
                        stationCOG[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.cog));
                        i++;
                    } while(cursor.moveToNext());
                    cursor.close();
                    //db.close();

                }
                return true;
            } catch (SQLiteException e){
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result){
                Log.d(TAG, "Database Unavailable");
            }
        }
    }
}

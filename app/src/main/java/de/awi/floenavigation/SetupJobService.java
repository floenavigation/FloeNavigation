package de.awi.floenavigation;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

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
            double[] predictedCoordinates = calculateNewPosition(stationLatitude[i], stationLongitude[i], stationSOG[i], stationCOG[i]);
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


    public double[] calculateNewPosition(double lat, double lon, double speed, double bearing){

        final double r = 6371 * 1000; // Earth Radius in m
        double distance = speed * 10;

        double lat2 = Math.asin(Math.sin(Math.toRadians(lat)) * Math.cos(distance / r)
                + Math.cos(Math.toRadians(lat)) * Math.sin(distance / r) * Math.cos(Math.toRadians(bearing)));
        double lon2 = Math.toRadians(lon)
                + Math.atan2(Math.sin(Math.toRadians(bearing)) * Math.sin(distance / r) * Math.cos(Math.toRadians(lat)), Math.cos(distance / r)
                - Math.sin(Math.toRadians(lat)) * Math.sin(lat2));
        lat2 = Math.toDegrees( lat2);
        lon2 = Math.toDegrees(lon2);
        return new double[]{lat2, lon2};
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
                Cursor cursor = db.query("AIS_FIXED_STATION_POSITION",
                        new String[] {"LATITUDE", "LONGITUDE", "SPEED_OVER_GROUND", "COURSE_OVER_GROUND", "MMSI"},
                        null, null, null, null, null);
                if (cursor.moveToFirst()){
                    int i = 0;
                    do{
                        stationLatitude[i] = cursor.getDouble(cursor.getColumnIndex("LATITUDE"));
                        stationLongitude[i] = cursor.getDouble(cursor.getColumnIndex("LONGITUDE"));
                        stationSOG[i] = cursor.getDouble(cursor.getColumnIndex("SPEED_OVER_GROUND"));
                        stationCOG[i] = cursor.getDouble(cursor.getColumnIndex("COURSE_OVER_GROUND"));
                        i++;
                    } while(cursor.moveToNext());
                    cursor.close();
                    db.close();

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

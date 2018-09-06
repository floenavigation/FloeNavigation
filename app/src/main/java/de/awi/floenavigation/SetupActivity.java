package de.awi.floenavigation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.internal.NavigationMenuItemView;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;


public class SetupActivity extends Activity {

    private static final String TAG = "SetupActivity";
    private static final int INITIALIZATION_SIZE = 2;
    private static final int JOB_ID = 100;
    private static final int PREDICTION_TIME = 65 * 1000;
    private static final int PREDICATION_TIME_PERIOD = 10 * 1000;

    Timer parentTimer = new Timer();
    Timer timer = new Timer();
    private int timerCounter = 0;

    private double[] stationLatitude = new double[INITIALIZATION_SIZE];
    private double[] stationLongitude = new double[INITIALIZATION_SIZE];
    private double[] stationSOG = new double[INITIALIZATION_SIZE];
    private double[] stationCOG = new double[INITIALIZATION_SIZE];
    private double[] predictedLatitude = new double[INITIALIZATION_SIZE];
    private double[] predictedLongitude = new double[INITIALIZATION_SIZE];
    private double[] distanceDiff = new double[INITIALIZATION_SIZE];




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        setContentView(R.layout.activity_setup);
        findViewById(R.id.first_station_predicted_Latitude).setEnabled(false);
        findViewById(R.id.first_station_predicted_Longitude).setEnabled(false);
        findViewById(R.id.first_station_received_Latitude).setEnabled(false);
        findViewById(R.id.first_station_received_Longitude).setEnabled(false);
        findViewById(R.id.first_station_diff_distance).setEnabled(false);
        findViewById(R.id.second_station_predicted_latitude).setEnabled(false);
        findViewById(R.id.second_station_predicted_longitude).setEnabled(false);
        findViewById(R.id.second_station_received_latitude).setEnabled(false);
        findViewById(R.id.second_station_received_longitude).setEnabled(false);
        findViewById(R.id.second_station_diff_distance).setEnabled(false);


       /*
       //Code for using the JobService class; currently unused

       ComponentName componentName = new ComponentName(this, SetupJobService.class);
        @SuppressLint("MissingPermission") JobInfo info = new JobInfo.Builder(JOB_ID, componentName)
                .setPeriodic(10  * 1000)
                .setPersisted(false)
                .build();
        final JobScheduler scheduler = (JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);
        int resultcode = scheduler.schedule(info);
        if(resultcode == JobScheduler.RESULT_SUCCESS){
            Log.d(TAG, "JobScheduled");
        } else {
            Log.d(TAG, "Job scheduling failed");
        }*/
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "3 minutes Timer");
                timerCounter++;
                new ReadParamsFromDB().execute();
                for(int i = 0; i < 2; i++){
                    double[] predictedCoordinates = NavigationFunctions.calculateNewPosition(stationLatitude[i], stationLongitude[i], stationSOG[i], stationCOG[i]);
                    predictedLatitude[i] = predictedCoordinates[0];
                    predictedLongitude[i] = predictedCoordinates[1];
                    distanceDiff[i] = NavigationFunctions.calculateDifference(stationLatitude[i], stationLongitude[i], predictedLatitude[i], predictedLongitude[i]);
                }
                //calculateDifference();
                refreshScreen();
            }
        }, 0, PREDICATION_TIME_PERIOD);

        parentTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //scheduler.cancel(JOB_ID);
                Log.d(TAG, "In Parent Timer" + Integer.toString(timerCounter));
                if (timerCounter >= 6)
                {
                    timer.cancel();
                    Log.d(TAG, "Completed");
                    timer.cancel();
                    parentTimer.cancel();
                    new CreateTablesOnStartup().execute();
                    //create rest of the tables
                    //start the other services
                }
                            }
        }, PREDICTION_TIME, 500);
    }

    private void refreshScreen(){
        final EditText ais1RcvLatitude = findViewById(R.id.first_station_received_Latitude);
        final EditText ais1RcvLongitude = findViewById(R.id.first_station_received_Longitude);
        final EditText ais1PrdLatitude = findViewById(R.id.first_station_predicted_Latitude);
        final EditText ais1PrdLongitude = findViewById(R.id.first_station_predicted_Longitude);
        final EditText ais1Difference = findViewById(R.id.first_station_diff_distance);
        final EditText ais2RcvLatitude = findViewById(R.id.second_station_received_latitude);
        final EditText ais2RcvLongitude = findViewById(R.id.second_station_received_longitude);
        final EditText ais2PrdLatitude = findViewById(R.id.second_station_predicted_latitude);
        final EditText ais2PrdLongitude = findViewById(R.id.second_station_predicted_longitude);
        final EditText ais2Difference = findViewById(R.id.second_station_diff_distance);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ais1Difference.setEnabled(true);
                ais1PrdLatitude.setEnabled(true);
                ais1PrdLongitude.setEnabled(true);
                ais1RcvLatitude.setEnabled(true);
                ais1RcvLongitude.setEnabled(true);
                ais2Difference.setEnabled(true);
                ais2PrdLatitude.setEnabled(true);
                ais2PrdLongitude.setEnabled(true);
                ais2RcvLatitude.setEnabled(true);
                ais2RcvLongitude.setEnabled(true);
                ais1Difference.setText(String.valueOf(distanceDiff[0]));
                ais1PrdLatitude.setText(String.valueOf(predictedLatitude[0]));
                ais1PrdLongitude.setText(String.valueOf(predictedLongitude[0]));
                ais1RcvLatitude.setText(String.valueOf(stationLatitude[0]));
                ais1RcvLongitude.setText(String.valueOf(stationLongitude[0]));
                ais2Difference.setText(String.valueOf(distanceDiff[1]));
                ais2PrdLatitude.setText(String.valueOf(predictedLatitude[1]));
                ais2PrdLongitude.setText(String.valueOf(predictedLongitude[1]));
                ais2RcvLatitude.setText(String.valueOf(stationLatitude[1]));
                ais2RcvLongitude.setText(String.valueOf(stationLongitude[1]));
                ais1Difference.setEnabled(false);
                ais1PrdLatitude.setEnabled(false);
                ais1PrdLongitude.setEnabled(false);
                ais1RcvLatitude.setEnabled(false);
                ais1RcvLongitude.setEnabled(false);
                ais2Difference.setEnabled(false);
                ais2PrdLatitude.setEnabled(false);
                ais2PrdLongitude.setEnabled(false);
                ais2RcvLatitude.setEnabled(false);
                ais2RcvLongitude.setEnabled(false);
            }
        });
    }


    private class ReadParamsFromDB extends AsyncTask<Void,Void,Boolean> {

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

    private class CreateTablesOnStartup extends AsyncTask<Void,Void,Boolean> {

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            DatabaseHelper helper = new DatabaseHelper(getApplicationContext());
            return helper.createTables();

        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result){
                Log.d(TAG, "Database Unavailable");
            }
        }
    }
}

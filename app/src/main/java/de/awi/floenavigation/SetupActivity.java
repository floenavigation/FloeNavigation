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
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.internal.NavigationMenuItemView;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;


public class SetupActivity extends Activity {

    private static final String TAG = "SetupActivity";
    private static final int JOB_ID = 100;
    private static final int PREDICTION_TIME = 65 * 1000;
    private static final int PREDICATION_TIME_PERIOD = 10 * 1000;


    Timer parentTimer = new Timer();
    Timer timer = new Timer();
    private int timerCounter = 0;

    private double[] stationLatitude = new double[DatabaseHelper.INITIALIZATION_SIZE];
    private double[] stationLongitude = new double[DatabaseHelper.INITIALIZATION_SIZE];
    private double[] stationSOG = new double[DatabaseHelper.INITIALIZATION_SIZE];
    private double[] stationCOG = new double[DatabaseHelper.INITIALIZATION_SIZE];
    private double[] predictedLatitude = new double[DatabaseHelper.INITIALIZATION_SIZE];
    private double[] predictedLongitude = new double[DatabaseHelper.INITIALIZATION_SIZE];
    private double[] distanceDiff = new double[DatabaseHelper.INITIALIZATION_SIZE];
    private double predictedBeta = 0.0;
    private double receivedBeta = 0.0;
    private double betaDifference = 0.0;




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
        findViewById(R.id.receivedBeta).setEnabled(false);
        findViewById(R.id.calculatedBeta).setEnabled(false);
        findViewById(R.id.betaDifference).setEnabled(false);


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
                Log.d(TAG, "Predicting New Values");
                timerCounter++;
                new ReadParamsFromDB().execute();
                for(int i = 0; i < DatabaseHelper.INITIALIZATION_SIZE; i++){
                    double[] predictedCoordinates = NavigationFunctions.calculateNewPosition(stationLatitude[i], stationLongitude[i], stationSOG[i], stationCOG[i]);
                    predictedLatitude[i] = predictedCoordinates[0];
                    predictedLongitude[i] = predictedCoordinates[1];
                    distanceDiff[i] = NavigationFunctions.calculateDifference(stationLatitude[i], stationLongitude[i], predictedLatitude[i], predictedLongitude[i]);


                }

                predictedBeta = NavigationFunctions.calculateAngleBeta(predictedLatitude[DatabaseHelper.firstStationIndex], predictedLongitude[DatabaseHelper.firstStationIndex], predictedLatitude[DatabaseHelper.secondStationIndex], predictedLongitude[DatabaseHelper.secondStationIndex]);
                receivedBeta = NavigationFunctions.calculateAngleBeta(stationLatitude[DatabaseHelper.firstStationIndex], stationLongitude[DatabaseHelper.firstStationIndex], stationLatitude[DatabaseHelper.secondStationIndex], stationLongitude[DatabaseHelper.secondStationIndex]);
                betaDifference = Math.abs(predictedBeta - receivedBeta);
                //calculateDifference();
                refreshScreen();
            }
        }, 0, PREDICATION_TIME_PERIOD);

        parentTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //scheduler.cancel(JOB_ID);
                Log.d(TAG, "StartupComplete");
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
        final EditText calculatedBeta = findViewById(R.id.calculatedBeta);
        final EditText rcvBeta = findViewById(R.id.receivedBeta);
        final EditText betaDiff = findViewById(R.id.betaDifference);
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
                calculatedBeta.setEnabled(true);
                rcvBeta.setEnabled(true);
                betaDiff.setEnabled(true);
                ais1Difference.setText(String.valueOf(distanceDiff[DatabaseHelper.firstStationIndex]));
                ais1PrdLatitude.setText(String.valueOf(predictedLatitude[DatabaseHelper.firstStationIndex]));
                ais1PrdLongitude.setText(String.valueOf(predictedLongitude[DatabaseHelper.firstStationIndex]));
                ais1RcvLatitude.setText(String.valueOf(stationLatitude[DatabaseHelper.firstStationIndex]));
                ais1RcvLongitude.setText(String.valueOf(stationLongitude[DatabaseHelper.firstStationIndex]));
                ais2Difference.setText(String.valueOf(distanceDiff[DatabaseHelper.secondStationIndex]));
                ais2PrdLatitude.setText(String.valueOf(predictedLatitude[DatabaseHelper.secondStationIndex]));
                ais2PrdLongitude.setText(String.valueOf(predictedLongitude[DatabaseHelper.secondStationIndex]));
                ais2RcvLatitude.setText(String.valueOf(stationLatitude[DatabaseHelper.secondStationIndex]));
                ais2RcvLongitude.setText(String.valueOf(stationLongitude[DatabaseHelper.secondStationIndex]));
                calculatedBeta.setText(String.valueOf(predictedBeta));
                rcvBeta.setText(String.valueOf(receivedBeta));
                betaDiff.setText(String.valueOf(betaDifference));
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
                calculatedBeta.setEnabled(false);
                rcvBeta.setEnabled(false);
                betaDiff.setEnabled(false);
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
                long stationCount = DatabaseUtils.queryNumEntries(db, DatabaseHelper.stationListTable);
                if (stationCount == DatabaseHelper.INITIALIZATION_SIZE) {
                    if (cursor.moveToFirst()) {
                        int i = 0;
                        do {
                            stationLatitude[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.latitude));
                            stationLongitude[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.longitude));
                            stationSOG[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.sog));
                            stationCOG[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.cog));
                            i++;
                        } while (cursor.moveToNext());
                        cursor.close();
                        db.close();

                    }

                    return true;
                } else{
                    Log.d(TAG, "Invalid Number of Entries in Station List Table");
                    return false;
                }
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
                Log.d(TAG, "Database Error");
            }
        }
    }
}

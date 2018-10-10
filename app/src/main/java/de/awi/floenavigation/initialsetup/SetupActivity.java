package de.awi.floenavigation.initialsetup;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import de.awi.floenavigation.AlphaCalculationService;
import de.awi.floenavigation.AngleCalculationService;
import de.awi.floenavigation.DatabaseHelper;
import de.awi.floenavigation.DialogActivity;
import de.awi.floenavigation.MainActivity;
import de.awi.floenavigation.NavigationFunctions;
import de.awi.floenavigation.PredictionService;
import de.awi.floenavigation.R;
import de.awi.floenavigation.ValidationService;


public class SetupActivity extends Activity {

    private static final String TAG = "SetupActivity";
    private static final int JOB_ID = 100;
    private static final int PREDICTION_TIME = 20 * 1000; //30 * 60 * 1000;
    private static final int PREDICATION_TIME_PERIOD = 10 * 1000;
    private static final int MAX_TIMER_COUNT = 3;

    private static final String toastMsg = "Please wait while Coordinate System is being Setup";


    Timer parentTimer = new Timer();
    Timer timer = new Timer();
    private int timerCounter = 0;

    private int[] stationMMSI = new int[DatabaseHelper.INITIALIZATION_SIZE];
    private double[] stationLatitude = new double[DatabaseHelper.INITIALIZATION_SIZE];
    private double[] stationLongitude = new double[DatabaseHelper.INITIALIZATION_SIZE];
    private double[] stationSOG = new double[DatabaseHelper.INITIALIZATION_SIZE];
    private double[] stationCOG = new double[DatabaseHelper.INITIALIZATION_SIZE];
    private double[] predictedLatitude = new double[DatabaseHelper.INITIALIZATION_SIZE];
    private double[] predictedLongitude = new double[DatabaseHelper.INITIALIZATION_SIZE];
    private double[] distanceDiff = new double[DatabaseHelper.INITIALIZATION_SIZE];
    private double[] stationUpdateTime = new double[DatabaseHelper.INITIALIZATION_SIZE];
    private double predictedBeta = 0.0;
    private double receivedBeta = 0.0;
    private double betaDifference = 0.0;
    private double xAxisDistance = 0.0;
    private boolean changeFormat = false;
    private int timerIndex = 0;
    private ProgressBar timerProgress;
    private int timerPercentage;
    private TextView progressBarValue;
    private static int firstStationMessageCount = 0;
    private static int secondStationMessageCount = 0;
    private boolean backButtonEnabled = false;
    private static double firstStationpreviousUpdateTime = 0;
    private static double secondStationpreviousUpdateTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        setContentView(R.layout.activity_setup);

        progressBarValue = findViewById(R.id.progressBarText);
        progressBarValue.setEnabled(true);

        timerProgress = findViewById(R.id.progressBar);
        timerProgress.setEnabled(true);
        timerPercentage = 0;


        //Populate Screen with Initial Values from DB
        new ReadParamsFromDB().execute();
        receivedBeta = NavigationFunctions.calculateAngleBeta(stationLatitude[DatabaseHelper.firstStationIndex],
                stationLongitude[DatabaseHelper.firstStationIndex], stationLatitude[DatabaseHelper.secondStationIndex], stationLongitude[DatabaseHelper.secondStationIndex]);
        refreshScreen();

        /*findViewById(R.id.first_station_predicted_Latitude).setEnabled(false);
        findViewById(R.id.first_station_predicted_Longitude).setEnabled(false);
        findViewById(R.id.first_station_received_Latitude).setEnabled(false);
        findViewById(R.id.first_station_received_Longitude).setEnabled(false);
        findViewById(R.id.first_station_diff_distance).setEnabled(false);
        findViewById(R.id.second_station_predicted_latitude).setEnabled(false);
        findViewById(R.id.second_station_predicted_longitude).setEnabled(false);
        findViewById(R.id.second_station_received_latitude).setEnabled(false);
        findViewById(R.id.second_station_received_longitude).setEnabled(false);
        findViewById(R.id.second_station_diff_distance).setEnabled(false);
        findViewById(R.id.first_station_MMSI).setEnabled(false);
        findViewById(R.id.second_station_MMSI).setEnabled(false);
        findViewById(R.id.first_station_updateTime).setEnabled(false);
        findViewById(R.id.second_station_updateTime).setEnabled(false);
        findViewById(R.id.receivedBeta).setEnabled(false);
        findViewById(R.id.calculatedBeta).setEnabled(false);
        findViewById(R.id.betaDifference).setEnabled(false);*/

        xAxisDistance = NavigationFunctions.calculateDifference(stationLatitude[DatabaseHelper.firstStationIndex], stationLongitude[DatabaseHelper.firstStationIndex],
                stationLatitude[DatabaseHelper.secondStationIndex], stationLongitude[DatabaseHelper.secondStationIndex]);
        new InsertXAxisDistance().execute();



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
                    //Log.d(TAG, "StationLatitude: " + String.valueOf(i) + " " + String.valueOf(stationLatitude[i]));
                    //Log.d(TAG, "StationLongitude: " + String.valueOf(i) + " " + String.valueOf(stationLongitude[i]));
                    predictedLatitude[i] = predictedCoordinates[DatabaseHelper.LATITUDE_INDEX];
                    predictedLongitude[i] = predictedCoordinates[DatabaseHelper.LONGITUDE_INDEX];
                    distanceDiff[i] = NavigationFunctions.calculateDifference(stationLatitude[i], stationLongitude[i], predictedLatitude[i], predictedLongitude[i]);

                }

                predictedBeta = NavigationFunctions.calculateAngleBeta(predictedLatitude[DatabaseHelper.firstStationIndex], predictedLongitude[DatabaseHelper.firstStationIndex], predictedLatitude[DatabaseHelper.secondStationIndex], predictedLongitude[DatabaseHelper.secondStationIndex]);
                receivedBeta = NavigationFunctions.calculateAngleBeta(stationLatitude[DatabaseHelper.firstStationIndex], stationLongitude[DatabaseHelper.firstStationIndex], stationLatitude[DatabaseHelper.secondStationIndex], stationLongitude[DatabaseHelper.secondStationIndex]);
                //Log.d(TAG, "AIS 1: " + String.valueOf(stationLatitude[DatabaseHelper.firstStationIndex]) + " " + String.valueOf(stationLongitude[DatabaseHelper.firstStationIndex]));
                //Log.d(TAG, "AIS 2: " + String.valueOf(stationLatitude[DatabaseHelper.secondStationIndex]) + " " + String.valueOf(stationLongitude[DatabaseHelper.secondStationIndex]));
                //Log.d(TAG, "Beta: " + String.valueOf(receivedBeta));
                betaDifference = Math.abs(predictedBeta - receivedBeta);
                //calculateDifference();

                refreshScreen();
                progressBarValueUpdates();



            }
        }, 0, PREDICATION_TIME_PERIOD);


        parentTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //scheduler.cancel(JOB_ID);
                Log.d(TAG, "StartupComplete");
                if (timerCounter >= MAX_TIMER_COUNT)
                {
                    try {
                        DatabaseHelper databaseHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                        SQLiteDatabase db = databaseHelper.getReadableDatabase();

                        ContentValues beta = new ContentValues();
                        beta.put(DatabaseHelper.beta, receivedBeta);
                        beta.put(DatabaseHelper.updateTime, SystemClock.elapsedRealtime());
                        db.insert(DatabaseHelper.betaTable, null, beta);
                        long test = DatabaseUtils.queryNumEntries(db, DatabaseHelper.betaTable);
                        Log.d(TAG, String.valueOf(test));

                    } catch(SQLException e){
                        Log.d(TAG, "Error Updating Beta Table");
                        e.printStackTrace();
                    }
                    timer.cancel();
                    Log.d(TAG, "Completed");
                    //For Test purposes only
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Setup Complete", Toast.LENGTH_LONG).show();
                        }
                    });
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.setup_finish).setVisibility(View.VISIBLE);
                            timerProgress.setVisibility(View.GONE);
                            findViewById(R.id.progressBarText).setVisibility(View.GONE);
                        }
                    });


                    //------//
                    backButtonEnabled = true;
                    timer.cancel();
                    parentTimer.cancel();
                    new CreateTablesOnStartup().execute();



                }
                            }
        }, PREDICTION_TIME, 500);
    }

    private void progressBarValueUpdates(){
        //Progress Bar Value update

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBarValue.setText(String.format("%s%%", String.valueOf(timerPercentage)));
                timerIndex++;
                timerPercentage = (int)timerIndex * 100 / (PREDICTION_TIME / PREDICATION_TIME_PERIOD);
            }
        });
    }

    private void dialogBoxDisplay() {

        String popupMsg = "Do you wish to rerun the initial setup?, then press Confirm!";
        String title = "Initial Setup Completed";
        Intent dialogIntent = new Intent(this, DialogActivity.class);
        dialogIntent.putExtra(DialogActivity.DIALOG_TITLE, title);
        dialogIntent.putExtra(DialogActivity.DIALOG_MSG, popupMsg);
        dialogIntent.putExtra(DialogActivity.DIALOG_ICON, R.drawable.ic_done_all_black_24dp);
        dialogIntent.putExtra(DialogActivity.DIALOG_OPTIONS, "true");
        startActivity(dialogIntent);
    }

    public static void runServices(Context mContext){
        Intent angleCalcServiceIntent = new Intent(mContext, AngleCalculationService.class);
        Intent alphaCalcServiceIntent = new Intent (mContext, AlphaCalculationService.class);
        Intent predictionServiceIntent = new Intent(mContext, PredictionService.class);
        Intent validationServiceIntent = new Intent(mContext, ValidationService.class);
        mContext.startService(angleCalcServiceIntent);
        mContext.startService(alphaCalcServiceIntent);
        mContext.startService(predictionServiceIntent);
        mContext.startService(validationServiceIntent);
    }

    private void refreshScreen(){
        final EditText ais1MMSI = findViewById(R.id.first_station_MMSI);
        final EditText ais1UpdateTime = findViewById(R.id.first_station_updateTime);
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
        final EditText ais2MMSI = findViewById(R.id.second_station_MMSI);
        final EditText ais2UpdateTime = findViewById(R.id.second_station_updateTime);
        final EditText calculatedBeta = findViewById(R.id.calculatedBeta);
        final EditText rcvBeta = findViewById(R.id.receivedBeta);
        final EditText betaDiff = findViewById(R.id.betaDifference);
        final EditText ais1StationMsgCount = findViewById(R.id.first_station_msgCount);
        final EditText ais2StationMsgCount = findViewById(R.id.second_station_msgCount);


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ais1MMSI.setEnabled(true);
                ais1UpdateTime.setEnabled(true);
                ais1Difference.setEnabled(true);
                ais1PrdLatitude.setEnabled(true);
                ais1PrdLongitude.setEnabled(true);
                ais1RcvLatitude.setEnabled(true);
                ais1RcvLongitude.setEnabled(true);
                ais1StationMsgCount.setEnabled(true);
                ais2Difference.setEnabled(true);
                ais2PrdLatitude.setEnabled(true);
                ais2PrdLongitude.setEnabled(true);
                ais2RcvLatitude.setEnabled(true);
                ais2RcvLongitude.setEnabled(true);
                ais2MMSI.setEnabled(true);
                ais2UpdateTime.setEnabled(true);
                ais2StationMsgCount.setEnabled(true);
                calculatedBeta.setEnabled(true);
                rcvBeta.setEnabled(true);
                betaDiff.setEnabled(true);
                ais1MMSI.setText(String.valueOf(stationMMSI[DatabaseHelper.firstStationIndex]));
                ais1UpdateTime.setText(String.valueOf(stationUpdateTime[DatabaseHelper.firstStationIndex]));
                ais1Difference.setText(String.valueOf(distanceDiff[DatabaseHelper.firstStationIndex]));


                if(changeFormat){
                    String[] ais1FormattedPredictedCoordinates = NavigationFunctions.locationInDegrees(predictedLatitude[DatabaseHelper.firstStationIndex],
                                                                predictedLongitude[DatabaseHelper.firstStationIndex]);
                    ais1PrdLatitude.setText(ais1FormattedPredictedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
                    ais1PrdLongitude.setText(ais1FormattedPredictedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);

                    String[] ais1FormattedReceivedCoordinates = NavigationFunctions.locationInDegrees(stationLatitude[DatabaseHelper.firstStationIndex],
                                                                stationLongitude[DatabaseHelper.firstStationIndex]);
                    ais1RcvLatitude.setText(ais1FormattedReceivedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
                    ais1RcvLongitude.setText(ais1FormattedReceivedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);
                } else {
                    ais1PrdLatitude.setText(String.valueOf(predictedLatitude[DatabaseHelper.firstStationIndex]));
                    ais1PrdLongitude.setText(String.valueOf(predictedLongitude[DatabaseHelper.firstStationIndex]));
                    ais1RcvLatitude.setText(String.valueOf(stationLatitude[DatabaseHelper.firstStationIndex]));
                    ais1RcvLongitude.setText(String.valueOf(stationLongitude[DatabaseHelper.firstStationIndex]));
                }

                ais2MMSI.setText(String.valueOf(stationMMSI[DatabaseHelper.secondStationIndex]));
                ais2UpdateTime.setText(String.valueOf(stationUpdateTime[DatabaseHelper.secondStationIndex]));
                ais2Difference.setText(String.valueOf(distanceDiff[DatabaseHelper.secondStationIndex]));

                if(changeFormat){
                    String[] ais2FormattedPredictedCoordinates = NavigationFunctions.locationInDegrees(predictedLatitude[DatabaseHelper.secondStationIndex],
                                                    predictedLongitude[DatabaseHelper.secondStationIndex]);
                    ais2PrdLatitude.setText(ais2FormattedPredictedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
                    ais2PrdLongitude.setText(ais2FormattedPredictedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);

                    String[] ais2FormattedReceivedCoordinates = NavigationFunctions.locationInDegrees(stationLatitude[DatabaseHelper.secondStationIndex],
                                                            stationLongitude[DatabaseHelper.secondStationIndex]);
                    ais2RcvLatitude.setText(ais2FormattedReceivedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
                    ais2RcvLongitude.setText(ais2FormattedReceivedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);
                } else {
                    ais2PrdLatitude.setText(String.valueOf(predictedLatitude[DatabaseHelper.secondStationIndex]));
                    ais2PrdLongitude.setText(String.valueOf(predictedLongitude[DatabaseHelper.secondStationIndex]));
                    ais2RcvLatitude.setText(String.valueOf(stationLatitude[DatabaseHelper.secondStationIndex]));
                    ais2RcvLongitude.setText(String.valueOf(stationLongitude[DatabaseHelper.secondStationIndex]));
                }

                ais1StationMsgCount.setText(String.valueOf(firstStationMessageCount));
                ais2StationMsgCount.setText(String.valueOf(secondStationMessageCount));
                calculatedBeta.setText(String.valueOf(predictedBeta));
                rcvBeta.setText(String.valueOf(receivedBeta));
                betaDiff.setText(String.valueOf(betaDifference));
                ais1MMSI.setEnabled(false);
                ais1UpdateTime.setEnabled(false);
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
                ais2MMSI.setEnabled(false);
                ais2UpdateTime.setEnabled(false);
                calculatedBeta.setEnabled(false);
                rcvBeta.setEnabled(false);
                betaDiff.setEnabled(false);
                ais1StationMsgCount.setEnabled(false);
                ais2StationMsgCount.setEnabled(false);
            }
        });
    }



    public void onClickNext(View view) {
        //Intent mainIntent = new Intent(this, MainActivity.class);
        //startActivity(mainIntent);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialogBoxDisplay();
            }
        });
    }

    @Override
    public void onBackPressed(){

        if (backButtonEnabled){
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }else {
            Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        switch (menuItem.getItemId()){
            case R.id.changeLatLonFormat:
                changeFormat = !changeFormat;
                refreshScreen();
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);

        }
    }


    private class ReadParamsFromDB extends AsyncTask<Void,Void,Boolean> {

        int[] mmsi;
        double[] latitude;
        double[] longitude;
        double[] sog;
        double[] cog;
        double[] updateTime;


        @Override
        protected void onPreExecute(){


        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            mmsi = new int[DatabaseHelper.INITIALIZATION_SIZE];
            latitude = new double[DatabaseHelper.INITIALIZATION_SIZE];
            longitude = new double[DatabaseHelper.INITIALIZATION_SIZE];
            sog = new double[DatabaseHelper.INITIALIZATION_SIZE];
            cog = new double[DatabaseHelper.INITIALIZATION_SIZE];
            updateTime = new double[DatabaseHelper.INITIALIZATION_SIZE];
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());

            try{
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.query(DatabaseHelper.fixedStationTable,
                        new String[] {DatabaseHelper.mmsi, DatabaseHelper.recvdLatitude, DatabaseHelper.recvdLongitude, DatabaseHelper.sog, DatabaseHelper.cog, DatabaseHelper.updateTime},
                        null, null, null, null, null);
                long stationCount = DatabaseUtils.queryNumEntries(db, DatabaseHelper.stationListTable);
                if (stationCount == DatabaseHelper.INITIALIZATION_SIZE) {
                    if (cursor.moveToFirst()) {
                        Log.d(TAG, "Row Count: " + String.valueOf(cursor.getCount()));
                        int i = 0;
                        do {
                            mmsi[i] = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.mmsi));
                            latitude[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.recvdLatitude));
                            Log.d(TAG, String.valueOf(i) + " " + String.valueOf(latitude[i]));
                            Log.d(TAG, "MMSIs: " + String.valueOf(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.mmsi))));
                            longitude[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.recvdLongitude));
                            sog[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.sog));
                            cog[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.cog));
                            if (i == 0 && (cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.updateTime)) > firstStationpreviousUpdateTime)){
                                firstStationpreviousUpdateTime = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.updateTime));
                                firstStationMessageCount++;
                            }else if (i == 1 && (cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.updateTime)) > secondStationpreviousUpdateTime)){
                                secondStationpreviousUpdateTime = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.updateTime));
                                secondStationMessageCount++;
                            }
                            updateTime[i] = SystemClock.elapsedRealtime() - cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.updateTime));
                            i++;
                        } while (cursor.moveToNext());
                        cursor.close();
                        //db.close();

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
                Log.d(TAG, "ReadParamsFromDB Async Task: Database Unavailable");
            } else{
                stationMMSI = mmsi;
                stationLatitude = latitude;
                stationLongitude = longitude;
                stationSOG = sog;
                stationCOG = cog;
                stationUpdateTime = updateTime;
            }
        }
    }

    private class CreateTablesOnStartup extends AsyncTask<Void,Void,Boolean> {

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            return DatabaseHelper.createTables(db);

        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result){
                Log.d(TAG, "CreateTablesOnStartup Async Task: Database Error");
            }
        }
    }

    private class InsertXAxisDistance extends AsyncTask<Void,Void,Boolean> {

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                ContentValues stationData = new ContentValues();
                stationData.put(DatabaseHelper.distance, xAxisDistance);
                stationData.put(DatabaseHelper.xPosition, xAxisDistance);
                db.update(DatabaseHelper.fixedStationTable, stationData,
                        DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(stationMMSI[DatabaseHelper.secondStationIndex])});
                return true;
            } catch (SQLiteException e){
                e.printStackTrace();
                Log.d(TAG, "Error Updating Distance in Fixed Station Table");
                return false;
            }

        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result){
                Log.d(TAG, "InsertXAxisDistance Async Task: Database Error");
            }
        }
    }
}

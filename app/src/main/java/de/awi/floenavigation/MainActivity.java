package de.awi.floenavigation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import de.awi.floenavigation.deployment.DeploymentActivity;
import de.awi.floenavigation.grid.GridActivity;
import de.awi.floenavigation.network.NetworkService;
import de.awi.floenavigation.sample_measurement.SampleMeasurementActivity;
import de.awi.floenavigation.waypoint.WaypointActivity;

public class MainActivity extends ActionBarActivity {

    private static boolean networkSetup = false;
    //private static boolean gpssetup = false;
    //public static boolean servicesStarted = true;
    public static final int GPS_REQUEST_CODE = 10;
    public static long numOfBaseStations;
    private static long numOfDeviceList;
    private static final String TAG = "MainActivity";

    public static Intent angleCalculationServiceIntent;
    public static Intent alphaCalculationServiceIntent;
    public static Intent predictionServiceIntent;
    public static Intent validationServiceIntent;
    public static Intent networkServiceIntent;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Start Network Monitor Service

        if(!networkSetup){
            Log.d(TAG, "NetworkServicie not Running. Starting NetworkService");
            networkSetup = true;
            networkServiceIntent = new Intent(this, NetworkService.class);
            startService(networkServiceIntent);

        } else{
            Log.d(TAG, "NetworkService Already Running");
        }

        //Start GPS Service
        if (!GPS_Service.isInstanceCreated()) {
            Log.d(TAG, "GPS_SERVICE not Running. Starting GPS_SERVICE");
            checkPermission();
        }else{
            Log.d(TAG, "GPSService Already Running");
        }


        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            numOfBaseStations = DatabaseUtils.queryNumEntries(db, DatabaseHelper.baseStationTable);
            numOfDeviceList = DatabaseUtils.queryNumEntries(db, DatabaseHelper.deviceListTable);
        }catch (SQLiteException e){
            Log.d(TAG, "Error reading database");
            e.printStackTrace();
        }
        if(numOfBaseStations >= DatabaseHelper.INITIALIZATION_SIZE){

            if(!AngleCalculationService.isInstanceCreated()){
                Log.d(TAG, "AngleCalculationService not Running. Starting AngleCalulationService");
                startService(angleCalculationServiceIntent);
            } else{
                Log.d(TAG, "AngleCalculationService already Running");
            }
            if(!AlphaCalculationService.isInstanceCreated()){
                Log.d(TAG, "AlphaCalculationService not Running. Starting AlphaCalulationService");
                startService(alphaCalculationServiceIntent);
            } else{
                Log.d(TAG, "AlphaCalculationService already Running");
            }
            if(!PredictionService.isInstanceCreated()){
                Log.d(TAG, "PredictionService not Running. Starting PredictionService");
                startService(predictionServiceIntent);
            } else{
                Log.d(TAG, "PredictionService already Running");
            }
            if(!ValidationService.isInstanceCreated()){
                Log.d(TAG, "ValidationService not Running. Starting ValidationService");
                startService(validationServiceIntent);
            } else{
                Log.d(TAG, "ValidationService already Running");
            }
        } else{
            angleCalculationServiceIntent = new Intent(getApplicationContext(), AngleCalculationService.class);
            alphaCalculationServiceIntent = new Intent(getApplicationContext(), AlphaCalculationService.class);
            predictionServiceIntent = new Intent(getApplicationContext(), PredictionService.class);
            validationServiceIntent = new Intent(getApplicationContext(), ValidationService.class);
        }


    }

    private void checkPermission(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                ActivityCompat.requestPermissions(this,
                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.INTERNET},
                                GPS_REQUEST_CODE);
            }
            return;
        }
        Intent intent = new Intent(this, GPS_Service.class);
        startService(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        switch (requestCode){
            case GPS_REQUEST_CODE:
                checkPermission();
                break;
            default:
                break;
        }
    }


    public void onClickListener(View view){

    }

    public void onClickDeploymentBtn(View view){
        if (numOfBaseStations >= DatabaseHelper.NUM_OF_BASE_STATIONS) {
            Intent deploymentIntent = new Intent(this, DeploymentActivity.class);
            deploymentIntent.putExtra("DeploymentSelection", false);
            startActivity(deploymentIntent);
        }else {
            Toast.makeText(getApplicationContext(), "Initial configuration is not completed", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickAdminBtn(View view){
        Intent intent = new Intent(this, LoginPage.class);
        startActivity(intent);
    }

    public void onClickSampleMeasureBtn(View view) {
        if (numOfBaseStations >= DatabaseHelper.NUM_OF_BASE_STATIONS && numOfDeviceList != 0) {
            Intent sampleMeasureIntent = new Intent(this, SampleMeasurementActivity.class);
            startActivity(sampleMeasureIntent);
        }else {
            Toast.makeText(getApplicationContext(), "Initial configuration is not completed", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickGridButton(View view) {
        if (numOfBaseStations >= DatabaseHelper.NUM_OF_BASE_STATIONS) {
            Intent gridActivityIntent = new Intent(this, GridActivity.class);
            startActivity(gridActivityIntent);
        }else {
            Toast.makeText(getApplicationContext(), "Initial configuration is not completed", Toast.LENGTH_SHORT).show();
        }
    }


    public void onClickWaypointBtn(View view) {
        if (numOfBaseStations >= DatabaseHelper.NUM_OF_BASE_STATIONS) {
            Intent waypointIntent = new Intent(this, WaypointActivity.class);
            startActivity(waypointIntent);
        }else {
            Toast.makeText(getApplicationContext(), "Initial configuration is not completed", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }
}

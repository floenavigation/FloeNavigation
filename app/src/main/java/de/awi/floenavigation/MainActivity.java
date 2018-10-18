package de.awi.floenavigation;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.UUID;

import de.awi.floenavigation.deployment.DeploymentActivity;
import de.awi.floenavigation.initialsetup.CoordinateFragment;
import de.awi.floenavigation.network.NetworkService;
import de.awi.floenavigation.sample_measurement.SampleMeasurementActivity;
import de.awi.floenavigation.waypoint.WaypointActivity;

public class MainActivity extends ActionBarActivity {

    private static boolean networkSetup = false;
    private static boolean gpssetup = false;
    public static final int GPS_REQUEST_CODE = 10;
    private static long numOfBaseStations;
    private static final String TAG = "MainActivity";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Start Network Monitor Service

        if (!networkSetup) {
            Intent networkServiceIntent = new Intent(this, NetworkService.class);
            startService(networkServiceIntent);
            networkSetup = true;
        }

        //Start GPS Service
        if (!gpssetup) {
            checkPermission();
            gpssetup = true;
        }

        SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        numOfBaseStations = DatabaseUtils.queryNumEntries(db, DatabaseHelper.baseStationTable);


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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("NetworkState", networkSetup);
    }

    public void onClickListener(View view){

    }

    public void onClickDeploymentBtn(View view){
        if (numOfBaseStations >= DatabaseHelper.NUM_OF_BASE_STATIONS) {
            Intent deploymentIntent = new Intent(this, DeploymentActivity.class);
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
        if (numOfBaseStations >= DatabaseHelper.NUM_OF_BASE_STATIONS) {
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

    public void onClickRecoveryListener(View view) {
        if (numOfBaseStations >= DatabaseHelper.NUM_OF_BASE_STATIONS) {
            Intent recoveryActivityIntent = new Intent(this, RecoveryActivity.class);
            startActivity(recoveryActivityIntent);
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

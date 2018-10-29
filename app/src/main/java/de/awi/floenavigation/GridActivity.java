package de.awi.floenavigation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ScaleGestureDetectorCompat;
import android.view.ScaleGestureDetector;


public class GridActivity extends Activity {

    private static final int ASYNC_TASK_TIMER_PERIOD = 10 * 1000;
    private static final int ASYNC_TASK_TIMER_DELAY = 0;
    private static final int SCREEN_REFRESH_TIMER_PERIOD = 10 * 1000;
    private static final int SCREEN_REFRESH_TIMER_DELAY = 0;
    private static final String TAG = "GridActivity";


    private BroadcastReceiver gpsBroadcastReceiver;
    private double tabletLat;
    private double tabletLon;
    private double originLatitude;
    private double originLongitude;
    private int originMMSI;
    private double beta;
    private double tabletX;
    private double tabletY;
    private double tabletDistance;
    private double tabletTheta;
    private double tabletAlpha;
    //private double[] mFixedStationXs;
    private ArrayList<Double> mFixedStationXs = new ArrayList<>();
    //private double[] mFixedStationYs;
    private ArrayList<Double> mFixedStationYs = new ArrayList<>();
    //private int[] mFixedStationMMSIs;
    private ArrayList<Integer> mFixedStationMMSIs = new ArrayList<>();
    //private double[] mMobileStationXs;
    private ArrayList<Double> mMobileStationXs = new ArrayList<>();
    //private double[] mMobileStationYs;
    private ArrayList<Double> mMobileStationYs = new ArrayList<>();
    //private int[] mMobileStationMMSIs;
    private ArrayList<Integer> mMobileStationMMSIs = new ArrayList<>();
    //private double[] mStaticStationXs;
    private ArrayList<Double> mStaticStationXs = new ArrayList<>();
    //private double[] mStaticStationYs;
    private ArrayList<Double> mStaticStationYs = new ArrayList<>();
    //private String[] mStaticStationNames;
    private ArrayList<String> mStaticStationNames = new ArrayList<>();
    //private double[] mWaypointsXs;
    private ArrayList<Double> mWaypointsXs = new ArrayList<>();
    //private double[] mWaypointsYs;
    private ArrayList<Double> mWaypointsYs = new ArrayList<>();
    //private String[] mWaypointsLabels;
    private ArrayList<String> mWaypointsLabels = new ArrayList<>();

    private static final double scale = 500;
    private LocationManager locationManager;
    private Timer asyncTaskTimer = new Timer();
    private Timer refreshScreenTimer = new Timer();

    //Action Bar Updates
    private BroadcastReceiver aisPacketBroadcastReceiver;
    private boolean locationStatus = false;
    private boolean packetStatus = false;
    private final Handler statusHandler = new Handler();
    private MenuItem gpsIconItem, aisIconItem;
    private View myGridView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myGridView = new MapView(this);
        //setContentView(R.layout.activity_grid);
        //myGridView = findViewById(R.id.GridView);
        //myGridView.invalidate();
        //setContentView(myGridView);
        setContentView(R.layout.activity_grid);

    }



    @Override
    protected void onStart() {
        super.onStart();
        //Broadcast receiver for tablet location
        actionBarUpdatesFunction();
        new ReadStaticStationsFromDB().execute();
        new ReadWaypointsFromDB().execute();
        new ReadOriginFromDB().execute();
        new ReadFixedStationsFromDB().execute();
        new ReadMobileStationsFromDB().execute();

        asyncTaskTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                new ReadOriginFromDB().execute();
                new ReadFixedStationsFromDB().execute();
                new ReadMobileStationsFromDB().execute();

            }
        }, ASYNC_TASK_TIMER_DELAY, ASYNC_TASK_TIMER_PERIOD);

        /*refreshScreenTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        myGridView.postInvalidateOnAnimation();
                    }
                });

            }
        }, SCREEN_REFRESH_TIMER_DELAY, SCREEN_REFRESH_TIMER_PERIOD);*/
    }


    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(gpsBroadcastReceiver);
        gpsBroadcastReceiver = null;
        unregisterReceiver(aisPacketBroadcastReceiver);
        aisPacketBroadcastReceiver = null;
        asyncTaskTimer.cancel();
        MapView.refreshScreenTimer.cancel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main_menu, menu);

        int[] iconItems = {R.id.currentLocationAvail, R.id.aisPacketAvail};
        gpsIconItem = menu.findItem(iconItems[0]);
        gpsIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        aisIconItem = menu.findItem(iconItems[1]);
        aisIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public void onBackPressed(){
        Log.d(TAG, "BackPressed");
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        asyncTaskTimer.cancel();
        MapView.refreshScreenTimer.cancel();
    }

    private void calculateTabletGridCoordinates(){
        if (tabletLat == 0.0){
            try {
                if (getLastKnownLocation() != null) {
                    tabletLat = getLastKnownLocation().getLatitude();
                }
            } catch (SecurityException e){
                Toast.makeText(this,"Location Service Problem", Toast.LENGTH_LONG).show();
            }
        }
        if (tabletLon == 0.0){
            try {
                if(getLastKnownLocation() != null) {
                    tabletLon = getLastKnownLocation().getLongitude();
                }
            } catch (SecurityException e){
                Toast.makeText(this,"Location Service Problem", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Location Service Not Available");
                e.printStackTrace();
            }
        }
        tabletDistance = NavigationFunctions.calculateDifference(tabletLat, tabletLon, originLatitude, originLongitude);
        //Log.d(TAG + "TabletParam", "TabletLat: " + String.valueOf(tabletLat)+ " TabletLon: "+ String.valueOf(tabletLon));
        //Log.d(TAG + "TabletParam", "OriginLat: " + String.valueOf(originLatitude)+ " OriginLon: " + String.valueOf(originLongitude));
        tabletTheta = NavigationFunctions.calculateAngleBeta(tabletLat, tabletLon, originLatitude, originLongitude);
        //Log.d(TAG + "TabletParam", "TabletDistance: " + String.valueOf(tabletDistance));
        tabletAlpha = Math.abs(tabletTheta - beta);
        tabletX = tabletDistance * Math.cos(Math.toRadians(tabletAlpha));
        tabletY = tabletDistance * Math.sin(Math.toRadians(tabletAlpha));
    }

    public double getFixedXposition(int index){
        return  mFixedStationXs.get(index);
    }

    public double getFixedYposition(int index){
        return  mFixedStationYs.get(index);
    }

    public double getMobileXposition(int index){
        return  mMobileStationXs.get(index);
    }

    public double getMobileYposition(int index){
        return  mMobileStationYs.get(index);
    }

    public double getStaticXposition(int index){
        return  mStaticStationXs.get(index);
    }

    public double getStaticYposition(int index){
        return  mStaticStationYs.get(index);
    }


    public double getWaypointXposition(int index){
        return  mWaypointsXs.get(index);
    }

    public double getWaypointYposition(int index){
        return  mWaypointsYs.get(index);
    }

    public double getTabletX(){
        return tabletX;
    }

    public double getTabletY(){
        return tabletY;
    }


    private float translateCoord(double coordinate){
        float result = (float) (coordinate / scale);
        return result;
    }

    private void actionBarUpdatesFunction() {

        ///*****************ACTION BAR UPDATES*************************/
        if(gpsBroadcastReceiver == null){
            gpsBroadcastReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent){
                    //Log.d(TAG, "BroadCast Received");
                    /*String coordinateString = intent.getExtras().get("coordinates").toString();
                    String[] coordinates = coordinateString.split(",");*/
                    tabletLat = intent.getExtras().getDouble(GPS_Service.latitude);
                    tabletLon = intent.getExtras().getDouble(GPS_Service.longitude);
                    locationStatus = intent.getExtras().getBoolean(GPS_Service.locationStatus);
                    //tabletTime = Long.parseLong(intent.getExtras().get(GPS_Service.GPSTime).toString());

                    //Log.d(TAG, "Tablet Lat: " + String.valueOf(tabletLat));
                    //Log.d(TAG, "Tablet Lon: " + String.valueOf(tabletLon));
                    //Toast.makeText(getActivity(),"Received Broadcast", Toast.LENGTH_LONG).show();
                    //populateTabLocation();
                    calculateTabletGridCoordinates();
                }
            };
        }


        if (aisPacketBroadcastReceiver == null){
            aisPacketBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    packetStatus = intent.getExtras().getBoolean(GPS_Service.AISPacketStatus);
                }
            };
        }

        registerReceiver(aisPacketBroadcastReceiver, new IntentFilter(GPS_Service.AISPacketBroadcast));
        registerReceiver(gpsBroadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));

        Runnable gpsLocationRunnable = new Runnable() {
            @Override
            public void run() {
                if (locationStatus){
                    if (gpsIconItem != null)
                        gpsIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorGreen), PorterDuff.Mode.SRC_IN);
                }
                else {
                    if (gpsIconItem != null)
                        gpsIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorRed), PorterDuff.Mode.SRC_IN);
                }
                if (packetStatus){
                    if (aisIconItem != null)
                        aisIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorGreen), PorterDuff.Mode.SRC_IN);
                }else {
                    if (aisIconItem != null)
                        aisIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorRed), PorterDuff.Mode.SRC_IN);
                }
                statusHandler.postDelayed(this, ActionBarActivity.UPDATE_TIME);
            }
        };

        statusHandler.postDelayed(gpsLocationRunnable, ActionBarActivity.UPDATE_TIME);
        ///******************************************/
    }

    private class ReadOriginFromDB extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                int i = 0;
                DatabaseHelper databaseHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                Cursor baseStationCursor = db.query(DatabaseHelper.baseStationTable,
                        new String[] {DatabaseHelper.mmsi},
                        null,
                        null,
                        null, null, null);
                if (baseStationCursor.getCount() != DatabaseHelper.INITIALIZATION_SIZE){
                    Log.d(TAG, "Error Reading from BaseStation Table");
                    return false;
                } else{
                    if(baseStationCursor.moveToFirst()){
                        originMMSI = baseStationCursor.getInt(baseStationCursor.getColumnIndex(DatabaseHelper.mmsi));
                    }
                }

                /*if(baseStationCursor.moveToFirst()) {
                    do {
                        int mmsi = baseStationCursor.getInt(baseStationCursor.getColumnIndex(DatabaseHelper.mmsi));

                        Log.d(TA    G, "MMSIs: " + String.valueOf(i) + " " + String.valueOf(mmsi));
                        i++;
                    } while (baseStationCursor.moveToNext());
                }*/

                Cursor fixedStationCursor = db.query(DatabaseHelper.fixedStationTable,
                        new String[] {DatabaseHelper.latitude, DatabaseHelper.longitude},
                        DatabaseHelper.mmsi +" = ?",
                        new String[] {String.valueOf(originMMSI)},
                        null, null, null);
                if (fixedStationCursor.getCount() != 1){
                    Log.d(TAG, "Error Reading Origin Latitude Longitude");
                    return false;
                } else{
                    if(fixedStationCursor.moveToFirst()){
                        originLatitude = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.latitude));
                        originLongitude = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.longitude));
                    }
                }

                Cursor betaCursor = db.query(DatabaseHelper.betaTable,
                        new String[]{DatabaseHelper.beta, DatabaseHelper.updateTime},
                        null, null,
                        null, null, null);
                if (betaCursor.getCount() == 1) {
                    if (betaCursor.moveToFirst()) {
                        beta = betaCursor.getDouble(betaCursor.getColumnIndex(DatabaseHelper.beta));
                    }

                } else {
                    Log.d(TAG, "Error in Beta Table");
                    return false;
                }
                betaCursor.close();
                baseStationCursor.close();
                fixedStationCursor.close();
                return true;
            } catch(SQLiteException e){
                Log.d(TAG, "Database Error");
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result){
            if(!result){
                Log.d(TAG, "ReadOriginFromDB AsyncTask Error");
            }
        }

    }

    private class ReadFixedStationsFromDB extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor mFixedStnCursor;
                //double xPosition, yPosition;
                //int mmsi;

                mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.xPosition, DatabaseHelper.yPosition},
                        DatabaseHelper.mmsi + " != ?",
                        new String[] {String.valueOf(originMMSI)},
                        null, null, null, null);
                //mFixedStationMMSIs = new int[mFixedStnCursor.getCount()];
                //mFixedStationXs = new double[mFixedStnCursor.getCount()];
                //mFixedStationYs = new double[mFixedStnCursor.getCount()];

                if (mFixedStnCursor.moveToFirst()) {
                    for(int i = 0; i < mFixedStnCursor.getCount(); i++){
                        mFixedStationMMSIs.add(i, mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi)));
                        mFixedStationXs.add(i, mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.xPosition)));
                        mFixedStationYs.add(i, mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.yPosition)));
                        mFixedStnCursor.moveToNext();
                    }
                    mFixedStnCursor.close();
                    return true;
                }
                else {
                    mFixedStnCursor.close();
                    Log.d(TAG, "FixedStationTable Cursor Error");
                    return false;
                }
            } catch (SQLiteException e) {
                Log.d(TAG, "Error reading database");
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result){
            if(!result){
                Log.d(TAG, "ReadFixedStationParams AsyncTask Error");
            }
        }
    }

    private class ReadMobileStationsFromDB extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor mMobileStnCursor;
                //double xPosition, yPosition;
                //int mmsi;

                mMobileStnCursor = db.query(DatabaseHelper.mobileStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.xPosition, DatabaseHelper.yPosition},
                        null,
                        null,
                        null, null, null, null);
                //mMobileStationXs = new double[mMobileStnCursor.getCount()];
                //mMobileStationYs = new double[mMobileStnCursor.getCount()];
                //mMobileStationMMSIs = new int[mMobileStnCursor.getCount()];
                if (mMobileStnCursor.moveToFirst()) {
                    for(int i = 0; i < mMobileStnCursor.getCount(); i++){
                        mMobileStationMMSIs.add(i, mMobileStnCursor.getInt(mMobileStnCursor.getColumnIndex(DatabaseHelper.mmsi)));
                        mMobileStationXs.add(i, mMobileStnCursor.getDouble(mMobileStnCursor.getColumnIndex(DatabaseHelper.xPosition)));
                        mMobileStationYs.add(i, mMobileStnCursor.getDouble(mMobileStnCursor.getColumnIndex(DatabaseHelper.yPosition)));
                        mMobileStnCursor.moveToNext();
                    }
                    mMobileStnCursor.close();
                    return true;
                }
                else {
                    mMobileStnCursor.close();
                    Log.d(TAG, "MobileStation Cursor Error");
                    return false;
                }
            } catch (SQLiteException e) {
                Log.d(TAG, "Error reading database");
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result){
            if(!result){
                Log.d(TAG, "ReadMobileStationFromDB AsyncTask Error");
            }
        }
    }

    private class ReadStaticStationsFromDB extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor mStaticStationCursor;
                //double xPosition, yPosition;
                //int mmsi;

                mStaticStationCursor = db.query(DatabaseHelper.staticStationListTable, new String[]{DatabaseHelper.staticStationName, DatabaseHelper.xPosition, DatabaseHelper.yPosition},
                        null,
                        null,
                        null, null, null, null);
                //mStaticStationXs = new double[mStaticStationCursor.getCount()];
                //mStaticStationYs = new double[mStaticStationCursor.getCount()];
                //mStaticStationNames = new String[mStaticStationCursor.getCount()];
                if (mStaticStationCursor.moveToFirst()) {
                    for(int i = 0; i < mStaticStationCursor.getCount(); i++){
                        mStaticStationNames.add(i, mStaticStationCursor.getString(mStaticStationCursor.getColumnIndex(DatabaseHelper.staticStationName)));
                        mStaticStationXs.add(i, mStaticStationCursor.getDouble(mStaticStationCursor.getColumnIndex(DatabaseHelper.xPosition)));
                        mStaticStationYs.add(i, mStaticStationCursor.getDouble(mStaticStationCursor.getColumnIndex(DatabaseHelper.yPosition)));
                        mStaticStationCursor.moveToNext();
                    }
                    mStaticStationCursor.close();
                    return true;
                }
                else {
                    mStaticStationCursor.close();
                    Log.d(TAG, "StaticStation Cursor Error");
                    return false;
                }
            } catch (SQLiteException e) {
                Log.d(TAG, "Error reading database");
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result){
            if(!result){
                Log.d(TAG, "ReadStaticStationFromDB AsyncTask Error");
            }
        }
    }

    private class ReadWaypointsFromDB extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor mWaypointsCursor;
                //double xPosition, yPosition;
                //int mmsi;

                mWaypointsCursor = db.query(DatabaseHelper.waypointsTable, new String[]{DatabaseHelper.labelID, DatabaseHelper.xPosition, DatabaseHelper.yPosition},
                        null,
                        null,
                        null, null, null, null);
                //mWaypointsXs = new double[mWaypointsCursor.getCount()];
                //mWaypointsYs = new double[mWaypointsCursor.getCount()];
                //mWaypointsLabels = new String[mWaypointsCursor.getCount()];
                if (mWaypointsCursor.moveToFirst()) {
                    for(int i = 0; i < mWaypointsCursor.getCount(); i++){
                        mWaypointsLabels.add(i, mWaypointsCursor.getString(mWaypointsCursor.getColumnIndex(DatabaseHelper.labelID)));
                        mWaypointsXs.add(i, mWaypointsCursor.getDouble(mWaypointsCursor.getColumnIndex(DatabaseHelper.xPosition)));
                        mWaypointsYs.add(i, mWaypointsCursor.getDouble(mWaypointsCursor.getColumnIndex(DatabaseHelper.yPosition)));
                        mWaypointsCursor.moveToNext();
                    }
                    mWaypointsCursor.close();
                    return true;
                }
                else {
                    mWaypointsCursor.close();
                    Log.d(TAG, "Waypoints Cursor Error");
                    return false;
                }
            } catch (SQLiteException e) {
                Log.d(TAG, "Error reading database");
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result){
            if(!result){
                Log.d(TAG, "ReadWaypointsFromDB AsyncTask Error");
            }
        }
    }


    @SuppressLint("MissingPermission")
    private Location getLastKnownLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }



}

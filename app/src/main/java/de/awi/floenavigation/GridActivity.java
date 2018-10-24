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
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.awi.floenavigation.deployment.StationInstallFragment;

public class GridActivity extends ActionBarActivity {

    private static final int ASYNC_TASK_TIMER_PERIOD = 10 * 1000;
    private static final int ASYNC_TASK_TIMER_DELAY = 0;
    private static final int SCREEN_REFRESH_TIMER_PERIOD = 10 * 1000;
    private static final int SCREEN_REFRESH_TIMER_DELAY = 0;
    private static final String TAG = "GridActivity";

    private static final int ORIGIN_X_POSITION = 0;
    private static final int ORIGIN_Y_POSITION = 0;
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
    private double[] mFixedStationXs;
    private double[] mFixedStationYs;
    private int[] mFixedStationMMSIs;
    private double[] mMobileStationXs;
    private double[] mMobileStationYs;
    private int[] mMobileStationMMSIs;
    private double[] mStaticStationXs;
    private double[] mStaticStationYs;
    private String[] mStaticStationNames;
    private double[] mWaypointsXs;
    private double[] mWaypointsYs;
    private String[] mWaypointsLabels;
    private static final double scale = 500;
    private LocationManager locationManager;
    private Timer asyncTaskTimer = new Timer();
    private Timer refreshScreenTimer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_grid);

        if(gpsBroadcastReceiver == null){
            gpsBroadcastReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent){
                    //Log.d(TAG, "BroadCast Received");
                    /*String coordinateString = intent.getExtras().get("coordinates").toString();
                    String[] coordinates = coordinateString.split(",");*/
                    tabletLat = intent.getExtras().getDouble(GPS_Service.latitude);
                    tabletLon = intent.getExtras().getDouble(GPS_Service.longitude);
                    //tabletTime = Long.parseLong(intent.getExtras().get(GPS_Service.GPSTime).toString());

                    //Log.d(TAG, "Tablet Lat: " + String.valueOf(tabletLat));
                    //Log.d(TAG, "Tablet Lon: " + String.valueOf(tabletLon));
                    //Toast.makeText(getActivity(),"Received Broadcast", Toast.LENGTH_LONG).show();
                    //populateTabLocation();
                    calculateTabletGridCoordinates();
                }
            };
        }
        registerReceiver(gpsBroadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));

        asyncTaskTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                new ReadOriginFromDB().execute();
                new ReadFixedStationsFromDB().execute();
                new ReadMobileStationsFromDB().execute();
                new ReadStaticStationsFromDB().execute();
                new ReadWaypointsFromDB().execute();
            }
        }, ASYNC_TASK_TIMER_DELAY, ASYNC_TASK_TIMER_PERIOD);

        setContentView(new myView(this));
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



    @Override
    public void onBackPressed(){
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        unregisterReceiver(gpsBroadcastReceiver);
        gpsBroadcastReceiver = null;
    }

    public class myView extends View{
        Paint paint = null;
        private static final int DEFAULT_PAINT_COLOR = Color.BLACK;
        private static final int DEFAULT_NUMBER_OF_ROWS = 20;
        private static final int DEFAULT_NUMBER_OF_COLUMNS = 40;
        private static final int CircleSize = 5;

        private int numRows = DEFAULT_NUMBER_OF_ROWS, numColumns = DEFAULT_NUMBER_OF_COLUMNS;

        public myView(Context context)
        {
            super(context);
            paint = new Paint();
            init();
            refreshScreenTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            invalidate();
                        }
                    });
                }
            }, SCREEN_REFRESH_TIMER_DELAY, SCREEN_REFRESH_TIMER_PERIOD);
        }

        private void init() {
            paint.setColor(DEFAULT_PAINT_COLOR);
            //mRectSquare = new Rect();
        }

        public void setLineColor(int color) {
            paint.setColor(color);
        }

        @Override
        protected void onDraw(Canvas canvas) {

            int width = getMeasuredWidth();
            int height = getMeasuredHeight();


            // Vertical lines
            for (int i = 1; i < numColumns; i++) {
                canvas.drawLine(width * i / numColumns, 0, width * i / numColumns, height, paint);
            }

            // Horizontal lines
            for (int i = 1; i < numRows; i++) {
                canvas.drawLine(0, height * i / numRows, width, height * i / numRows, paint);
            }

            setLineColor(Color.GREEN);
            canvas.translate(getWidth()/2f,getHeight()/2f);

            Log.d(TAG, "Translated X: " + String.valueOf(translateCoord(mFixedStationXs[0])));
            Log.d(TAG, "Unit Columns: " + String.valueOf(getWidth()/numColumns));
            Log.d(TAG, "X" + String.valueOf(getWidth() * (translateCoord(mFixedStationXs[0]) / numColumns)));
            Log.d(TAG, "TabletX: " + String.valueOf(tabletX) + " TabletY: " + String.valueOf(tabletY));
            canvas.drawCircle(translateCoord(mFixedStationXs[0]) * getWidth()/numColumns, 0, CircleSize, paint);

            //canvas.drawCircle(width * 20 / numColumns, height * 10 / numRows, 15, paint);
            //Draw Origin
            canvas.drawCircle(ORIGIN_X_POSITION,ORIGIN_Y_POSITION, CircleSize, paint);

            //Draw Tablet Position
            setLineColor(Color.RED);
            drawTriangle(translateCoord(tabletX) * getWidth()/numColumns, translateCoord(tabletY) * getHeight() / numRows, 10, 10, false, paint, canvas);
            //For Loop Fixed Station
            setLineColor(Color.GREEN);
            for(int i = 0; i < mFixedStationMMSIs.length; i++){
                canvas.drawCircle(translateCoord(mFixedStationXs[i]) * getWidth()/numColumns, translateCoord(mFixedStationYs[i]) * getHeight()/numRows, CircleSize, paint);
                Log.d(TAG, "FixedStationX: " + String.valueOf(mFixedStationXs[i]));
                Log.d(TAG, "FixedStationY: " + String.valueOf(mFixedStationYs[i]));
                Log.d(TAG, "Loop Counter: " + String.valueOf(i));
                Log.d(TAG, "Length: " + String.valueOf(mFixedStationMMSIs.length));
                Log.d(TAG, "MMSIs: " + String.valueOf(mFixedStationMMSIs[i]));
                Log.d(TAG, "FixedStation TranslatedX: " + String.valueOf(translateCoord(mFixedStationXs[i]) * getWidth()/numColumns));
                Log.d(TAG, "FixedStation TranslatedY: " + String.valueOf(translateCoord(mFixedStationYs[i]) * getHeight()/numRows));
            }
            //For Loop Mobile Station
            setLineColor(Color.BLUE);
            for(int i = 0; i < mMobileStationMMSIs.length; i++){
                canvas.drawCircle(translateCoord(mMobileStationXs[i]) * getWidth()/numColumns, translateCoord(mMobileStationYs[i]) * getHeight()/numRows, CircleSize, paint);
            }
            //For Loop Static Station
            setLineColor(Color.YELLOW);
            for(int i = 0; i < mStaticStationNames.length; i++){
                canvas.drawCircle(translateCoord(mStaticStationXs[i]) * getWidth()/numColumns, translateCoord(mStaticStationYs[i]) * getHeight()/numRows, CircleSize, paint);
                Log.d(TAG, "StaticStation TranslatedX: " + String.valueOf(translateCoord(mStaticStationXs[i]) * getWidth()/numColumns));
                Log.d(TAG, "StaticStation TranslatedY: " + String.valueOf(translateCoord(mStaticStationYs[i]) * getHeight()/numRows));
            }
            //For Loop Waypoint
            setLineColor(Color.BLACK);
            for(int i = 0; i < mWaypointsLabels.length; i++){
                drawTriangle(translateCoord(mWaypointsXs[i]) * getWidth()/numColumns, translateCoord(mWaypointsYs[i]) * getHeight()/numRows, 10, 10, true, paint, canvas);
            }
            setLineColor(Color.BLACK);
        }

        private void drawTriangle(float x, float y, int width, int height, boolean inverted, Paint paint, Canvas canvas){

            PointF p1 = new PointF(x,y);
            float pointX = x + width/2f;
            float pointY = inverted?  y + height : y - height;

            PointF p2 = new PointF(pointX,pointY);
            PointF p3 = new PointF(x+width,y);


            Path path = new Path();
            path.setFillType(Path.FillType.EVEN_ODD);
            path.moveTo(p1.x,p1.y);
            path.lineTo(p2.x,p2.y);
            path.lineTo(p3.x,p3.y);
            path.close();

            canvas.drawPath(path, paint);
        }

        private void drawStar(int width, int height, Paint paint, Canvas canvas)
        {
            float mid = width / 2;
            float min = Math.min(width, height);
            float fat = min / 17;
            float half = min / 2;
            mid = mid - half;

            paint.setStrokeWidth(fat);
            paint.setStyle(Paint.Style.STROKE);
            Path path = new Path();


            path.reset();

            paint.setStyle(Paint.Style.FILL);


            // top left
            path.moveTo(mid + half * 0.5f, half * 0.84f);
            // top right
            path.lineTo(mid + half * 1.5f, half * 0.84f);
            // bottom left
            path.lineTo(mid + half * 0.68f, half * 1.45f);
            // top tip
            path.lineTo(mid + half * 1.0f, half * 0.5f);
            // bottom right
            path.lineTo(mid + half * 1.32f, half * 1.45f);
            // top left
            path.lineTo(mid + half * 0.5f, half * 0.84f);

            path.close();
            canvas.drawPath(path, paint);

        }
    }

    private float translateCoord(double coordinate){
        float result = (float) (coordinate / scale);
        return result;
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

                        Log.d(TAG, "MMSIs: " + String.valueOf(i) + " " + String.valueOf(mmsi));
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
                mFixedStationMMSIs = new int[mFixedStnCursor.getCount()];
                mFixedStationXs = new double[mFixedStnCursor.getCount()];
                mFixedStationYs = new double[mFixedStnCursor.getCount()];

                if (mFixedStnCursor.moveToFirst()) {
                    for(int i = 0; i < mFixedStnCursor.getCount(); i++){
                        mFixedStationMMSIs[i] = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                        mFixedStationXs[i] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.xPosition));
                        mFixedStationYs[i] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.yPosition));
                        mFixedStnCursor.moveToNext();
                    }
                    mFixedStnCursor.close();
                    return true;
                }
                else {
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
                mMobileStationXs = new double[mMobileStnCursor.getCount()];
                mMobileStationYs = new double[mMobileStnCursor.getCount()];
                mMobileStationMMSIs = new int[mMobileStnCursor.getCount()];
                if (mMobileStnCursor.moveToFirst()) {
                    for(int i = 0; i < mMobileStnCursor.getCount(); i++){
                        mMobileStationMMSIs[i] = mMobileStnCursor.getInt(mMobileStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                        mMobileStationXs[i] = mMobileStnCursor.getDouble(mMobileStnCursor.getColumnIndex(DatabaseHelper.xPosition));
                        mMobileStationYs[i] = mMobileStnCursor.getDouble(mMobileStnCursor.getColumnIndex(DatabaseHelper.yPosition));
                        mMobileStnCursor.moveToNext();
                    }
                    mMobileStnCursor.close();
                    return true;
                }
                else {
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
                mStaticStationXs = new double[mStaticStationCursor.getCount()];
                mStaticStationYs = new double[mStaticStationCursor.getCount()];
                mStaticStationNames = new String[mStaticStationCursor.getCount()];
                if (mStaticStationCursor.moveToFirst()) {
                    for(int i = 0; i < mStaticStationCursor.getCount(); i++){
                        mStaticStationNames[i] = mStaticStationCursor.getString(mStaticStationCursor.getColumnIndex(DatabaseHelper.staticStationName));
                        mStaticStationXs[i] = mStaticStationCursor.getDouble(mStaticStationCursor.getColumnIndex(DatabaseHelper.xPosition));
                        mStaticStationYs[i] = mStaticStationCursor.getDouble(mStaticStationCursor.getColumnIndex(DatabaseHelper.yPosition));
                        mStaticStationCursor.moveToNext();
                    }
                    mStaticStationCursor.close();
                    return true;
                }
                else {
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
                mWaypointsXs = new double[mWaypointsCursor.getCount()];
                mWaypointsYs = new double[mWaypointsCursor.getCount()];
                mWaypointsLabels = new String[mWaypointsCursor.getCount()];
                if (mWaypointsCursor.moveToFirst()) {
                    for(int i = 0; i < mWaypointsCursor.getCount(); i++){
                        mWaypointsLabels[i] = mWaypointsCursor.getString(mWaypointsCursor.getColumnIndex(DatabaseHelper.labelID));
                        mWaypointsXs[i] = mWaypointsCursor.getDouble(mWaypointsCursor.getColumnIndex(DatabaseHelper.xPosition));
                        mWaypointsYs[i] = mWaypointsCursor.getDouble(mWaypointsCursor.getColumnIndex(DatabaseHelper.yPosition));
                        mWaypointsCursor.moveToNext();
                    }
                    mWaypointsCursor.close();
                    return true;
                }
                else {
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

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
        //setContentView(R.layout.activity_grid);
        myGridView = new myView(this);
        /*new ReadStaticStationsFromDB().execute();
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

        refreshScreenTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        myGridView.invalidate();
                    }
                });

            }
        }, SCREEN_REFRESH_TIMER_DELAY, SCREEN_REFRESH_TIMER_PERIOD);
*/
        setContentView(myGridView);

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
        refreshScreenTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        myGridView.invalidate();
                    }
                });

            }
        }, SCREEN_REFRESH_TIMER_DELAY, SCREEN_REFRESH_TIMER_PERIOD);

    }


    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(gpsBroadcastReceiver);
        gpsBroadcastReceiver = null;
        unregisterReceiver(aisPacketBroadcastReceiver);
        aisPacketBroadcastReceiver = null;
        asyncTaskTimer.cancel();
        refreshScreenTimer.cancel();
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
        refreshScreenTimer.cancel();
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

    public class myView extends View{
        Paint paint = null;
        private static final int DEFAULT_PAINT_COLOR = Color.BLACK;
        private static final int DEFAULT_NUMBER_OF_ROWS = 20;
        private static final int DEFAULT_NUMBER_OF_COLUMNS = 40;
        private static final int CircleSize = 5;

        private int numRows = DEFAULT_NUMBER_OF_ROWS, numColumns = DEFAULT_NUMBER_OF_COLUMNS;

        // The current viewport. This rectangle represents the currently visible
        // chart domain and range.
        private static final float AXIS_X_MIN = -1f;
        private static final float AXIS_X_MAX = 1f;
        private static final float AXIS_Y_MIN = -1f;
        private static final float AXIS_Y_MAX = 1f;
        private RectF mCurrentViewport =
                new RectF(AXIS_X_MIN, AXIS_Y_MIN, AXIS_X_MAX, AXIS_Y_MAX);
        private Rect mContentRect = new Rect();
        private ScaleGestureDetector mScaleGestureDetector;
        private GestureDetectorCompat mGestureDetector;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            boolean retVal = mScaleGestureDetector.onTouchEvent(event);
            retVal = mGestureDetector.onTouchEvent(event) || retVal;
            return retVal || super.onTouchEvent(event);
        }

        /**
         * The scale listener, used for handling multi-finger scale gestures.
         */
        private final ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener
                = new ScaleGestureDetector.SimpleOnScaleGestureListener(){

            /**
             * This is the active focal point in terms of the viewport. Could be a local
             * variable but kept here to minimize per-frame allocations.
             */
            private PointF viewportFocus = new PointF();
            private float lastSpanX;
            private float lastSpanY;

            // Detects that new pointers are going down.
            @Override
            public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
                lastSpanX = mScaleGestureDetector.
                        getCurrentSpanX();
                lastSpanY = mScaleGestureDetector.
                        getCurrentSpanY();
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {

                float spanX = scaleGestureDetector.
                        getCurrentSpanX();
                float spanY = scaleGestureDetector.
                        getCurrentSpanY();

                float newWidth = lastSpanX / spanX * mCurrentViewport.width();
                float newHeight = lastSpanY / spanY * mCurrentViewport.height();

                float focusX = scaleGestureDetector.getFocusX();
                float focusY = scaleGestureDetector.getFocusY();
                // Makes sure that the chart point is within the chart region.
                // See the sample for the implementation of hitTest().
                hitTest(scaleGestureDetector.getFocusX(),
                        scaleGestureDetector.getFocusY(),
                        viewportFocus);

                mCurrentViewport.set(
                        viewportFocus.x
                                - newWidth * (focusX - mContentRect.left)
                                / mContentRect.width(),
                        viewportFocus.y
                                - newHeight * (mContentRect.bottom - focusY)
                                / mContentRect.height(),
                        0,
                        0);
                mCurrentViewport.right = mCurrentViewport.left + newWidth;
                mCurrentViewport.bottom = mCurrentViewport.top + newHeight;

                // Invalidates the View to update the display.
                ViewCompat.postInvalidateOnAnimation(myView.this);

                lastSpanX = spanX;
                lastSpanY = spanY;
                return true;
            }

            private boolean hitTest(float x, float y, PointF dest) {
                if (!mContentRect.contains((int) x, (int) y)) {
                    return false;
                }

                dest.set(
                        mCurrentViewport.left
                                + mCurrentViewport.width()
                                * (x - mContentRect.left) / mContentRect.width(),
                        mCurrentViewport.top
                                + mCurrentViewport.height()
                                * (y - mContentRect.bottom) / -mContentRect.height());
                return true;
            }


        };

        public myView(Context context)
        {
            super(context);
            paint = new Paint();
            mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);
            mGestureDetector = new GestureDetectorCompat(context, mGestureListener);
            init();
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

            //Log.d(TAG, "Translated X: " + String.valueOf(translateCoord(mFixedStationXs[0])));
            //Log.d(TAG, "Unit Columns: " + String.valueOf(getWidth()/numColumns));
            //Log.d(TAG, "X" + String.valueOf(getWidth() * (translateCoord(mFixedStationXs[0]) / numColumns)));
            //Log.d(TAG, "TabletX: " + String.valueOf(tabletX) + " TabletY: " + String.valueOf(tabletY));
            //canvas.drawCircle(translateCoord(mFixedStationXs[0]) * getWidth()/numColumns, 0, CircleSize, paint);

            //canvas.drawCircle(width * 20 / numColumns, height * 10 / numRows, 15, paint);
            //Draw Origin
            canvas.drawCircle(ORIGIN_X_POSITION,ORIGIN_Y_POSITION, CircleSize, paint);

            //Draw Tablet Position
            setLineColor(Color.RED);
            drawTriangle(translateCoord(tabletX) * getWidth()/numColumns, translateCoord(tabletY) * getHeight() / numRows, 10, 10, false, paint, canvas);
            //For Loop Fixed Station
            setLineColor(Color.GREEN);
            for(int i = 0; i < mFixedStationMMSIs.size(); i++){
                canvas.drawCircle(translateCoord(mFixedStationXs.get(i)) * getWidth()/numColumns, translateCoord(mFixedStationYs.get(i)) * getHeight()/numRows, CircleSize, paint);
                Log.d(TAG, "FixedStationX: " + String.valueOf(mFixedStationXs.get(i)));
                Log.d(TAG, "FixedStationY: " + String.valueOf(mFixedStationYs.get(i)));
                Log.d(TAG, "Loop Counter: " + String.valueOf(i));
                Log.d(TAG, "Length: " + String.valueOf(mFixedStationMMSIs.size()));
                Log.d(TAG, "MMSIs: " + String.valueOf(mFixedStationMMSIs.get(i)));
                Log.d(TAG, "FixedStation TranslatedX: " + String.valueOf(translateCoord(mFixedStationXs.get(i)) * getWidth()/numColumns));
                Log.d(TAG, "FixedStation TranslatedY: " + String.valueOf(translateCoord(mFixedStationYs.get(i)) * getHeight()/numRows));
            }
            //For Loop Mobile Station
            setLineColor(Color.BLUE);
            for(int i = 0; i < mMobileStationMMSIs.size(); i++){  //
                canvas.drawCircle(translateCoord(mMobileStationXs.get(i)) * getWidth()/numColumns, translateCoord(mMobileStationYs.get(i)) * getHeight()/numRows, CircleSize, paint);
            }
            //For Loop Static Station
            setLineColor(Color.YELLOW);
            for(int i = 0; i < mStaticStationNames.size(); i++){
                canvas.drawCircle(translateCoord(mStaticStationXs.get(i)) * getWidth()/numColumns, translateCoord(mStaticStationYs.get(i)) * getHeight()/numRows, CircleSize, paint);
                Log.d(TAG, "StaticStation TranslatedX: " + String.valueOf(translateCoord(mStaticStationXs.get(i)) * getWidth()/numColumns));
                Log.d(TAG, "StaticStation TranslatedY: " + String.valueOf(translateCoord(mStaticStationYs.get(i)) * getHeight()/numRows));
            }
            //For Loop Waypoint
            setLineColor(Color.BLACK);
            for(int i = 0; i < mWaypointsLabels.size(); i++){
                drawTriangle(translateCoord(mWaypointsXs.get(i)) * getWidth()/numColumns, translateCoord(mWaypointsYs.get(i)) * getHeight()/numRows, 10, 10, true, paint, canvas);
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

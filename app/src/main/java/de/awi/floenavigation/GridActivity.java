package de.awi.floenavigation;

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
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import de.awi.floenavigation.deployment.StationInstallFragment;

public class GridActivity extends ActionBarActivity {

    private static final String TAG = "GridActivity";
    private BroadcastReceiver gpsBroadcastReceiver;
    private Double tabletLat;
    private Double tabletLon;
    private Double originLatitude;
    private Double originLongitude;
    private int originMMSI;
    private Double beta;
    private Double tabletX;
    private Double tabletY;
    private Double tabletDistance;
    private Double tabletTheta;
    private Double tabletAlpha;
    private Double[] mFixedStationXs;
    private Double[] mFixedStationYs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_grid);
        setContentView(new myView(this));
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

                    //Log.d(TAG, "Tablet Loc: " + tabletLat);
                    //Toast.makeText(getActivity(),"Received Broadcast", Toast.LENGTH_LONG).show();
                    //populateTabLocation();
                }
            };
        }
        registerReceiver(gpsBroadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));

        new ReadOriginFromDB().execute();
        try{
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        calculateTabletGridCoordinates();
        readDatabase();


    }

    private void calculateTabletGridCoordinates(){
        tabletDistance = NavigationFunctions.calculateDifference(tabletLat, tabletLon, originLatitude, originLongitude);
        tabletTheta = NavigationFunctions.calculateAngleBeta(tabletLat, tabletLon, originLatitude, originLongitude);
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

        private int numRows = DEFAULT_NUMBER_OF_ROWS, numColumns = DEFAULT_NUMBER_OF_COLUMNS;

        public myView(Context context)
        {
            super(context);
            paint = new Paint();
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
            canvas.drawCircle(0,0, 15, paint);
            //canvas.drawCircle(width * 20 / numColumns, height * 10 / numRows, 15, paint);

            setLineColor(Color.BLACK);
        }
    }

    private class ReadOriginFromDB extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
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

    private void readDatabase() {
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor mFixedStnCursor;
            //double xPosition, yPosition;
            //int mmsi;

            mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.xPosition, DatabaseHelper.yPosition},
                    DatabaseHelper.mmsi + " != ?",
                    new String[] {String.valueOf(originMMSI)},
                    null, null, null, null);
            mFixedStationXs = new Double[mFixedStnCursor.getCount()];
            mFixedStationYs = new Double[mFixedStnCursor.getCount()];
            if (mFixedStnCursor.moveToFirst()) {
                for(int i = 0; i < mFixedStnCursor.getCount(); i++){
                    //mmsi = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                    mFixedStationXs[i] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.xPosition));
                    mFixedStationYs[i] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.yPosition));
                }
                mFixedStnCursor.close();
            }
            else {
                Log.d(TAG, "FixedStationTable Cursor Error");
            }
        } catch (SQLiteException e) {
            Log.d(TAG, "Error reading database");
            e.printStackTrace();
        }
    }


}

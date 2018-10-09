package de.awi.floenavigation.waypoint;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.awi.floenavigation.DatabaseHelper;
import de.awi.floenavigation.GPS_Service;
import de.awi.floenavigation.MainActivity;
import de.awi.floenavigation.NavigationFunctions;
import de.awi.floenavigation.R;

public class WaypointActivity extends Activity implements View.OnClickListener{

    private static final String TAG = "WaypointActivity";

    private static final String changeText = "Waypoint Installed";

    private BroadcastReceiver broadcastReceiver;
    private Double tabletLat;
    private Double tabletLon;
    private double originLatitude;
    private double originLongitude;
    private int originMMSI;
    private double beta;
    private double distance;
    private double alpha;
    private double xPosition;
    private double yPosition;
    private double theta;
    private String waypointLabel;
    private String time;
    private boolean changeFormat = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waypoint);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.waypoint_confirm).setOnClickListener(this);
        findViewById(R.id.waypoint_finish).setOnClickListener(this);

        if (broadcastReceiver ==  null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    if(intent.getExtras()!= null) {
                        tabletLat = intent.getExtras().getDouble(GPS_Service.latitude);
                        tabletLon = intent.getExtras().getDouble(GPS_Service.longitude);
                        if((findViewById(R.id.waypointCoordinateView).getVisibility()) == View.VISIBLE) {
                            populateTabLocation();
                        }
                    }
                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));
    }

    private void populateTabLocation(){

        TextView latView = findViewById(R.id.waypointTabletLat);
        TextView lonView = findViewById(R.id.waypointTabletLon);
        if(changeFormat){
            String[] formattedCoordinates = NavigationFunctions.locationInDegrees(tabletLat, tabletLon);
            latView.setText(formattedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
            lonView.setText(formattedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);
        } else {
            latView.setText(String.valueOf(tabletLat));
            lonView.setText(String.valueOf(tabletLon));
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
                populateTabLocation();
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
    }

    public void onClick(View v){

        switch (v.getId()){
            case R.id.waypoint_confirm:
                onClickConfirm();
                break;

            case R.id.waypoint_finish:
                onClickFinish();
                break;
        }

    }

    private void onClickConfirm(){

        if(tabletLat != null && tabletLon != null) {
            DatabaseHelper databaseHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            findViewById(R.id.waypointCoordinateView).setVisibility(View.GONE);
            findViewById(R.id.waypointWaitingView).setVisibility(View.VISIBLE);
            if (getOriginCoordinates(db)){
                calculateWaypointParameters();
                createLabel();
                if(insertInDatabase(db)){
                    Log.d(TAG, "Waypoint Inserted");
                    ProgressBar progress = findViewById(R.id.waypointProgress);
                    progress.stopNestedScroll();
                    progress.setVisibility(View.GONE);
                    findViewById(R.id.waypoint_finish).setClickable(true);
                    findViewById(R.id.waypoint_finish).setEnabled(true);
                    TextView waitingMsg = findViewById(R.id.waypointWaitingMsg);
                    waitingMsg.setText(changeText);
                }else {
                    Log.d(TAG, "Error inserting new Waypoint");
                }
            } else{
                Log.d(TAG, "Error reading Origin Coordinates");
            }
        } else{
            Log.d(TAG, "Error with GPS Service");
            Toast.makeText(this, "Error reading Device Lat and Long", Toast.LENGTH_LONG).show();
        }
    }

    private boolean insertInDatabase(SQLiteDatabase db){
        ContentValues waypoint = new ContentValues();
        waypoint.put(DatabaseHelper.latitude, tabletLat);
        waypoint.put(DatabaseHelper.longitude, tabletLon);
        waypoint.put(DatabaseHelper.xPosition, xPosition);
        waypoint.put(DatabaseHelper.yPosition, yPosition);
        waypoint.put(DatabaseHelper.updateTime, time);
        waypoint.put(DatabaseHelper.label, waypointLabel);
        Log.d(TAG, waypointLabel);
        long result = db.insert(DatabaseHelper.waypointsTable, null, waypoint);
        if(result != -1){
            Log.d(TAG, "Waypoint Inserted Successfully");
            return true;
        } else{
            Log.d(TAG, "Error Inserting Waypoint");
            return false;
        }
    }

    private void onClickFinish(){
        Log.d(TAG, "Activity Finished");
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);
    }

    private void calculateWaypointParameters(){
        distance = NavigationFunctions.calculateDifference(tabletLat, tabletLon, originLatitude, originLongitude);
        theta = NavigationFunctions.calculateAngleBeta(tabletLat, tabletLon, originLatitude, originLongitude);
        alpha = Math.abs(theta - beta);
        xPosition = distance * Math.cos(Math.toRadians(alpha));
        yPosition = distance * Math.sin(Math.toRadians(alpha));
    }

    private void createLabel(){
        time = String.valueOf(SystemClock.elapsedRealtime());
        List<String> labelElements = new ArrayList<String>();
        labelElements.add(time);
        labelElements.add(String.valueOf(tabletLat));
        labelElements.add(String.valueOf(tabletLon));
        labelElements.add(String.valueOf(xPosition));
        labelElements.add(String.valueOf(yPosition));
        waypointLabel = TextUtils.join(",", labelElements);
    }

    private boolean getOriginCoordinates(SQLiteDatabase db){

        try {

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

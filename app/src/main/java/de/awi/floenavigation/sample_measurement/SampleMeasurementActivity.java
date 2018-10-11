package de.awi.floenavigation.sample_measurement;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import de.awi.floenavigation.ActionBarActivity;
import de.awi.floenavigation.DatabaseHelper;
import de.awi.floenavigation.GPS_Service;
import de.awi.floenavigation.MainActivity;
import de.awi.floenavigation.NavigationFunctions;
import de.awi.floenavigation.R;

public class SampleMeasurementActivity extends Activity {

    private static final String TAG = "SampleMeasureActivity";
    private BroadcastReceiver broadcastReceiver;
    private Double tabletLat;
    private Double tabletLon;
    private ArrayList<String> selectedDeviceAttributes;
    private String deviceSelectedName;
    private final int deviceIDIndex = 0;
    private final int deviceFullNameIndex = 1;
    private final int deviceTypeIndex = 2;
    private double originLatitude;
    private double originLongitude;
    private int originMMSI;
    private double beta;
    private double distance;
    private double alpha;
    private double xPosition;
    private double yPosition;
    private double theta;
    private Spinner operation;
    private boolean changeFormat = false;

    //Action Bar Updates
    private BroadcastReceiver aisPacketBroadcastReceiver;
    private boolean locationStatus = false;
    private boolean packetStatus = false;
    private final Handler statusHandler = new Handler();
    private MenuItem gpsIconItem, aisIconItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_measurement);


        //Advanced Search Feature
        DatabaseHelper.loadDeviceList(getApplicationContext()); //only for debugging purpose
        setSpinnerValues();
        AutoCompleteTextView deviceNameTextView = findViewById(R.id.deviceshortname);
        ArrayAdapter<String> adapter = DatabaseHelper.advancedSearchTextView(getApplicationContext());
        deviceNameTextView.setDropDownBackgroundResource(R.color.backgroundGradStart);
        deviceNameTextView.setThreshold(1);
        deviceNameTextView.setAdapter(adapter);

        //on Click listener for device name
        deviceNameTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                deviceSelectedName = (String)parent.getItemAtPosition(position);
                selectedDeviceAttributes = DatabaseHelper.getDeviceAttributes((String)parent.getItemAtPosition(position));
                populateDeviceAttributes();
            }
        });

        //on Click listener for confirmbutton
        Button confirmButton = findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Data Sample Confirmed", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Data Sample Confirmed");
                populateDatabaseTable();

                Intent mainActivityIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainActivityIntent);
            }
        });
    }

    private void actionBarUpdatesFunction() {

        /*****************ACTION BAR UPDATES*************************/
        if (broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    tabletLat = intent.getExtras().getDouble(GPS_Service.latitude);
                    tabletLon = intent.getExtras().getDouble(GPS_Service.longitude);
                    locationStatus = intent.getExtras().getBoolean(GPS_Service.locationStatus);
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
        registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));

        Runnable gpsLocationRunnable = new Runnable() {
            @Override
            public void run() {
                if (locationStatus){
                    if (gpsIconItem != null)
                        gpsIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorGreen), PorterDuff.Mode.SRC_IN);
                }
                if (packetStatus){
                    if (aisIconItem != null)
                        aisIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorGreen), PorterDuff.Mode.SRC_IN);
                }

                statusHandler.postDelayed(this, ActionBarActivity.UPDATE_TIME);
            }
        };

        statusHandler.postDelayed(gpsLocationRunnable, ActionBarActivity.UPDATE_TIME);
        /******************************************/
    }

    private void setSpinnerValues(){
        operation = findViewById(R.id.operationspinner);
        String[] contents = new String[]{"Sample", "Measurement"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, contents);
        operation.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Broadcast receiver for tablet location
        actionBarUpdatesFunction();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        unregisterReceiver(aisPacketBroadcastReceiver);
        aisPacketBroadcastReceiver = null;
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

    private void populateTabLocation(){

        TextView latView = findViewById(R.id.tabLat);
        TextView lonView = findViewById(R.id.tabLon);

        if (changeFormat){
            String[] formattedCoordinates = NavigationFunctions.locationInDegrees(tabletLat, tabletLon);
            latView.setText(formattedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
            lonView.setText(formattedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);
        } else{
            latView.setText(String.valueOf(tabletLat));
            lonView.setText(String.valueOf(tabletLon));
        }

    }

    private void populateDeviceAttributes(){

        TextView deviceFullNameView = findViewById(R.id.devicefullname);
        TextView deviceIDView = findViewById(R.id.deviceid);
        TextView deviceTypeView = findViewById(R.id.devicetype);

        deviceIDView.setText(selectedDeviceAttributes.get(deviceIDIndex));
        deviceFullNameView.setText(selectedDeviceAttributes.get(deviceFullNameIndex));
        deviceTypeView.setText(selectedDeviceAttributes.get(deviceTypeIndex));
    }

    private void populateDatabaseTable(){

        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            if (getOriginCoordinates()) {
                calculateSampledLocationParameters();
                ContentValues mContentValues = new ContentValues();
                mContentValues.put(DatabaseHelper.deviceID, selectedDeviceAttributes.get(deviceIDIndex));
                mContentValues.put(DatabaseHelper.deviceName, selectedDeviceAttributes.get(deviceFullNameIndex));
                mContentValues.put(DatabaseHelper.deviceShortName, deviceSelectedName);
                mContentValues.put(DatabaseHelper.operation, operation.getSelectedItem().toString());
                mContentValues.put(DatabaseHelper.deviceType, selectedDeviceAttributes.get(deviceTypeIndex));
                mContentValues.put(DatabaseHelper.latitude, tabletLat);
                mContentValues.put(DatabaseHelper.longitude, tabletLon);
                mContentValues.put(DatabaseHelper.xPosition, xPosition);
                mContentValues.put(DatabaseHelper.yPosition, yPosition);
                mContentValues.put(DatabaseHelper.updateTime, SystemClock.elapsedRealtime());
                db.insert(DatabaseHelper.sampleMeasurementTable, null, mContentValues);
            } else {
                Log.d(TAG, "Error Inserting new data");
            }
        }catch(SQLiteException e) {
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }
    }

    private void calculateSampledLocationParameters(){
        distance = NavigationFunctions.calculateDifference(tabletLat, tabletLon, originLatitude, originLongitude);
        theta = NavigationFunctions.calculateAngleBeta(tabletLat, tabletLon, originLatitude, originLongitude);
        alpha = Math.abs(theta - beta);
        xPosition = distance * Math.cos(Math.toRadians(alpha));
        yPosition = distance * Math.sin(Math.toRadians(alpha));
    }

    private boolean getOriginCoordinates(){

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

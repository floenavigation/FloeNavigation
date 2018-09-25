package de.awi.floenavigation.sample_measurement;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.app.Activity;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import de.awi.floenavigation.DatabaseHelper;
import de.awi.floenavigation.GPS_Service;
import de.awi.floenavigation.R;

public class SampleMeasurementActivity extends Activity {

    private static final String TAG = "SampleMeasureActivity";
    private BroadcastReceiver broadcastReceiver;
    private String tabletLat;
    private String tabletLon;
    private ArrayList<String> selectedDeviceAttributes;
    private String deviceSelectedName;
    private final int deviceIDIndex = 0;
    private final int deviceFullNameIndex = 1;
    private final int deviceTypeIndex = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_measurement);


        //Advanced Search Feature
        AutoCompleteTextView deviceNameTextView = findViewById(R.id.deviceshortname);
        ArrayAdapter<String> adapter = DatabaseHelper.advancedSearchTextView(getApplicationContext());
        deviceNameTextView.setAdapter(adapter);

        //Broadcast receiver for tablet location
        if (broadcastReceiver ==  null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    if(intent.getExtras()!= null) {
                        tabletLat = intent.getExtras().get(GPS_Service.latitude).toString();
                        tabletLon = intent.getExtras().get(GPS_Service.longitude).toString();
                        populateTabLocation();
                    }
                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));

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
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    private void populateTabLocation(){

        TextView latView = findViewById(R.id.tabLat);
        TextView lonView = findViewById(R.id.tabLon);

        latView.setText(tabletLat);
        lonView.setText(tabletLon);
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

        SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        ContentValues mContentValues = new ContentValues();
        mContentValues.put(DatabaseHelper.deviceID, selectedDeviceAttributes.get(deviceIDIndex));
        mContentValues.put(DatabaseHelper.deviceName, selectedDeviceAttributes.get(deviceFullNameIndex));
        mContentValues.put(DatabaseHelper.deviceShortName, deviceSelectedName);
        mContentValues.put(DatabaseHelper.deviceType, selectedDeviceAttributes.get(deviceTypeIndex));
        mContentValues.put(DatabaseHelper.latitude, tabletLat);
        mContentValues.put(DatabaseHelper.longitude, tabletLon);
        mContentValues.put(DatabaseHelper.updateTime, SystemClock.elapsedRealtime());
        db.insert(DatabaseHelper.sampleMeasurementTable, null, mContentValues);
    }
}

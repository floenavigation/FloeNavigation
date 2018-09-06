package de.awi.floenavigation;


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


/**
 * A simple {@link Fragment} subclass.
 */
public class CoordinateFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "CoordinateFragment";


    public CoordinateFragment() {
        // Required empty public constructor
    }

    public static final String MMSI_NUMBER = "mmsi";
    public static final int GPS_REQUEST_CODE = 10;
    private int MMSINumber;
    private double latitude;
    private double longitude;
    private String stationName;
    private int locationReceived;
    private LocationManager locationManager;
    private LocationListener listener;
    private BroadcastReceiver broadcastReceiver;
    private final Handler handler = new Handler();
    private String tabletLat;
    private String tabletLon;
    private boolean isConfigDone;
    private long countAIS;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_coordinate, container, false);
        Button confirmButton = layout.findViewById(R.id.confirm_Coordinates);
        confirmButton.setOnClickListener(this);
        //configureTabLocation();
        if (savedInstanceState != null){
            isConfigDone = savedInstanceState.getBoolean("isConfigDone");
        }
        MMSINumber = getArguments().getInt(MMSI_NUMBER);
       return layout;
    }

    @Override
    public void onStart(){
        super.onStart();
        if (isConfigDone) {
            changeLayout();
            populateTabLocation();
        } else{
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (checkForCoordinates()){

                        isConfigDone = true;
                        //show the packet received
                        changeLayout();
                        populateTabLocation();
                    } else {
                        Toast.makeText(getActivity(), "In Coordinate Fragment", Toast.LENGTH_LONG).show();
                        handler.postDelayed(this, 1000);
                    }
                }
            });
        }
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent){
                    /*String coordinateString = intent.getExtras().get("coordinates").toString();
                    String[] coordinates = coordinateString.split(",");*/
                    tabletLat = intent.getExtras().get("Latitude").toString();
                    tabletLon = intent.getExtras().get("Longitude").toString();
                    //Toast.makeText(getActivity(),"Received Broadcast", Toast.LENGTH_LONG).show();
                    populateTabLocation();
                }
            };
        }
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter("location_updates"));
    }

    @Override
    public void onPause(){
        super.onPause();
        if(broadcastReceiver != null){
            getActivity().unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        savedInstanceState.putBoolean("isConfigDone", isConfigDone);
    }

    private boolean checkForCoordinates(){
        int index;
        boolean success = false;
        try{
            SQLiteDatabase db;
            DatabaseHelper databaseHelper = new DatabaseHelper(getActivity());
            db = databaseHelper.getReadableDatabase();
            countAIS = DatabaseUtils.queryNumEntries(db, DatabaseHelper.stationListTable);
            Cursor cursor = db.query(DatabaseHelper.fixedStationTable,
                    new String[]{DatabaseHelper.stationName, DatabaseHelper.latitude, DatabaseHelper.longitude, DatabaseHelper.isLocationReceived},
                    "MMSI = ?",
                    new String[] {Integer.toString(MMSINumber)},
                    null, null, null);
            if(cursor.moveToFirst()){
                stationName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.stationName));
                //index = cursor.getColumnIndexOrThrow(DatabaseHelper.latitude);
                latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.latitude));

                longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.longitude));
                locationReceived = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.isLocationReceived));
                Toast.makeText(getActivity(), "LocationReceived: " + String.valueOf(locationReceived), Toast.LENGTH_LONG).show();
                if(locationReceived == 1) {
                    success = true;
                    Toast.makeText(getActivity(), "Success True", Toast.LENGTH_LONG).show();
                }
            }
            cursor.close();
            db.close();
        } catch (SQLiteException e){
            Toast.makeText(getActivity(), "Database Unavailable", Toast.LENGTH_LONG).show();
        }
        return success;
    }

    private void changeLayout(){
        View v = getView();
        LinearLayout waitingLayout = v.findViewById(R.id.waitingView);
        waitingLayout.setVisibility(View.GONE);
        LinearLayout coordinateLayout = v.findViewById(R.id.coordinateView);
        coordinateLayout.setVisibility(View.VISIBLE);
        v.findViewById(R.id.ais_station).setEnabled(false);
        v.findViewById(R.id.ais_station_latitude).setEnabled(false);
        v.findViewById(R.id.ais_station_longitude).setEnabled(false);
        v.findViewById(R.id.tablet_latitude).setEnabled(false);
        v.findViewById(R.id.tablet_longitude).setEnabled(false);
        if(countAIS == 2){
            Button confirmButton = v.findViewById(R.id.confirm_Coordinates);
            confirmButton.setText(R.string.startSetup);
        }
    }

    private void populateTabLocation(){
        View v = getView();
        final TextView tabLat = v.findViewById(R.id.tablet_latitude);
        final TextView tabLon = v.findViewById(R.id.tablet_longitude);
        final TextView aisLat = v.findViewById(R.id.ais_station_latitude);
        final TextView aisLon = v.findViewById(R.id.ais_station_longitude);
        final TextView aisName = v.findViewById(R.id.ais_station);

        if (tabletLat == null || tabletLat.isEmpty()){
            try {
                locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);


                tabletLat = String.valueOf(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude());

            } catch (SecurityException e){
                Toast.makeText(getActivity(),"Location Service Problem", Toast.LENGTH_LONG).show();
            }
        }
        if (tabletLon == null || tabletLon.isEmpty()){
            try {
                tabletLon = String.valueOf(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude());

            } catch (SecurityException e){
                Toast.makeText(getActivity(),"Location Service Problem", Toast.LENGTH_LONG).show();
            }
        }
        tabLat.setEnabled(true);
        tabLat.setText(tabletLat);
        tabLat.setEnabled(false);
        tabLon.setEnabled(true);
        tabLon.setText(tabletLon);
        tabLon.setEnabled(false);
        aisName.setEnabled(true);
        aisName.setText(stationName);
        aisName.setEnabled(false);
        aisLat.setEnabled(true);
        aisLat.setText(String.valueOf(latitude));
        aisLat.setEnabled(false);
        aisLon.setEnabled(true);
        aisLon.setText(String.valueOf(longitude));
        aisLon.setEnabled(false);


    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.confirm_Coordinates:
                onClickBtn();
                break;
        }
    }

    private void onClickBtn(){
        if(countAIS < 2) {
            MMSIFragment mmsiFragment = new MMSIFragment();
            FragmentChangeListener fc = (FragmentChangeListener) getActivity();
            fc.replaceFragment(mmsiFragment);
        } else{
            Intent intent = new Intent(getActivity(), SetupActivity.class);
            startActivity(intent);
        }
    }
}

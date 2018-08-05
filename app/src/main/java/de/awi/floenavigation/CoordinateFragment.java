package de.awi.floenavigation;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


/**
 * A simple {@link Fragment} subclass.
 */
public class CoordinateFragment extends Fragment implements View.OnClickListener {


    public CoordinateFragment() {
        // Required empty public constructor
    }

    public static final String MMSI_NUMBER = "mmsi";
    private int MMSINumber;
    private double latitude;
    private double longitude;
    private LocationManager locationManager;
    private LocationListener listener;
    private final Handler handler = new Handler();
    private double tabletLat;
    private double tabletLon;
    private boolean isConfigDone;
    private long countAIS;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_coordinate, container, false);
        Button confirmButton = layout.findViewById(R.id.confirm_Coordinates);
        confirmButton.setOnClickListener(this);
        configureTabLocation();
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
                        handler.postDelayed(this, 1000);
                    }
                }
            });
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
            countAIS = DatabaseUtils.queryNumEntries(db, "AIS_STATION_LIST");
            success = true;
           /* Cursor cursor = db.query("AIS_FIXED_STATION_POSITION",
                    new String[]{"LATITUDE", "LONGITUDE"},
                    "MMSI = ?",
                    new String[] {Integer.toString(MMSINumber)},
                    null, null, null);
            if(cursor.moveToFirst()){
                index = cursor.getColumnIndexOrThrow("LATITUDE");
                latitude = cursor.getDouble(index);

                index = cursor.getColumnIndexOrThrow("LONGITUDE");
                longitude = cursor.getDouble(index);
                success = true;
            }
            cursor.close();*/
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
            confirmButton.setText("Start Setup");
        }
    }

    private void populateTabLocation(){
        View v = getView();
        final TextView tabLat = v.findViewById(R.id.tablet_latitude);
        final TextView tabLon = v.findViewById(R.id.tablet_longitude);
        tabLat.setEnabled(true);
        if (tabletLat == 0){
            try {
                tabletLat = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude();
            } catch (SecurityException e){
                checkPermission();
            }
        }
        if (tabletLon == 0){
            try {
                tabletLon = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude();
            } catch (SecurityException e){
                checkPermission();
            }
        }
        tabLat.setText(String.format("%f", tabletLat));
        tabLat.setEnabled(false);
        tabLon.setEnabled(true);
        tabLon.setText(String.format("%f", tabletLon));
        tabLon.setEnabled(false);
    }

    private void configureTabLocation(){
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                tabletLat = location.getLatitude();
                tabletLon = location.getLongitude();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };
        checkPermission();
    }

    private void checkPermission(){
        if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                ActivityCompat.requestPermissions(getActivity(),
                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.INTERNET},
                        10);
            }
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, listener);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        switch (requestCode){
            case 10:
                checkPermission();
                break;
            default:
                break;
        }
    }

}

package de.awi.floenavigation.deployment;


import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import de.awi.floenavigation.DatabaseHelper;
import de.awi.floenavigation.GPS_Service;
import de.awi.floenavigation.NavigationFunctions;
import de.awi.floenavigation.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class StaticStationFragment extends Fragment implements View.OnClickListener{

    private static final String TAG = "StaticStationDeployFrag";
    private static final String changeText = "Station Installed";

    private String stationName;
    private String stationType;
    private BroadcastReceiver broadcastReceiver;
    private double tabletLat;
    private double tabletLon;
    private double originLatitude;
    private double originLongitude;
    private int originMMSI;
    private double beta;
    private double distance;
    private double alpha;
    private double xPosition;
    private double yPosition;
    private double theta;


    public StaticStationFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout =  inflater.inflate(R.layout.fragment_static_station, container, false);
        layout.findViewById(R.id.static_station_finish).setOnClickListener(this);
        layout.findViewById(R.id.static_station_finish).setEnabled(false);
        layout.findViewById(R.id.static_station_finish).setClickable(false);
        stationName = getArguments().getString(DatabaseHelper.staticStationName);
        stationType = getArguments().getString(DatabaseHelper.stationType);
        if (broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    tabletLat = Double.parseDouble(intent.getExtras().get(GPS_Service.latitude).toString());
                    tabletLon = Double.parseDouble(intent.getExtras().get(GPS_Service.longitude).toString());
                }
            };
        }
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));
        if(getOriginCoordinates()) {
            calculateStaticStationParameters();
            insertStaticStation();
            ProgressBar progress = layout.findViewById(R.id.staticStationProgress);
            progress.stopNestedScroll();
            progress.setVisibility(View.GONE);
            TextView msg = layout.findViewById(R.id.staticStationFragMsg);
            msg.setText(changeText);
            layout.findViewById(R.id.static_station_finish).setEnabled(true);
            layout.findViewById(R.id.static_station_finish).setClickable(true);
            Log.d(TAG, "Station Installed");
            Toast.makeText(getContext(), "Station Installed", Toast.LENGTH_LONG).show();
        } else{
            Log.d(TAG, "Error Inserting new Station");
        }
        return layout;
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.static_station_finish:

        }
    }

    private void calculateStaticStationParameters(){
        distance = NavigationFunctions.calculateDifference(tabletLat, tabletLon, originLatitude, originLongitude);
        theta = NavigationFunctions.calculateAngleBeta(tabletLat, tabletLon, originLatitude, originLongitude);
        alpha = Math.abs(theta - beta);
        xPosition = distance * Math.cos(Math.toRadians(alpha));
        yPosition = distance * Math.sin(Math.toRadians(alpha));
    }

    private void insertStaticStation(){
        DatabaseHelper databaseHelper = DatabaseHelper.getDbInstance(getActivity());
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        ContentValues staticStation = new ContentValues();
        staticStation.put(DatabaseHelper.staticStationName, stationName);
        staticStation.put(DatabaseHelper.stationType, stationType);
        staticStation.put(DatabaseHelper.xPosition, xPosition);
        staticStation.put(DatabaseHelper.yPosition, yPosition);
        staticStation.put(DatabaseHelper.distance, distance);
        staticStation.put(DatabaseHelper.alpha, alpha);
        db.insert(DatabaseHelper.staticStationListTable, null, staticStation);
    }

    private boolean getOriginCoordinates(){

        try {
            DatabaseHelper databaseHelper = DatabaseHelper.getDbInstance(getActivity());
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
                Log.d(TAG, "Error Reading Origin Latitude Longtidue");
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

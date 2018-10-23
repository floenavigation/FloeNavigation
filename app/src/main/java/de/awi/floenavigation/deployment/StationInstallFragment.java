package de.awi.floenavigation.deployment;


import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.awi.floenavigation.DatabaseHelper;
import de.awi.floenavigation.FragmentChangeListener;
import de.awi.floenavigation.GPS_Service;
import de.awi.floenavigation.NavigationFunctions;
import de.awi.floenavigation.R;
import de.awi.floenavigation.aismessages.AISDecodingService;


/**
 * A simple {@link Fragment} subclass.
 */
public class StationInstallFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "StationInstallFragment";
    private static final int VALID_MMSI_LENGTH = 9;

    public StationInstallFragment() {
        // Required empty public constructor
    }

    View activityView;
    private boolean stationTypeAIS;
    private BroadcastReceiver broadcastReceiver;
    private Double tabletLat;
    private Double tabletLon;
    private boolean changeFormat;
    private int numOfSignificantFigures;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_station_install, container, false);
        stationTypeAIS = getArguments().getBoolean("stationTypeAIS");
        layout.findViewById(R.id.station_confirm).setOnClickListener(this);

        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver(){
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
                    if(!stationTypeAIS) {
                        populateTabLocation();
                    }
                }
            };
        }
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));

        if (stationTypeAIS) {
            layout.findViewById(R.id.stationMMSI).setVisibility(View.VISIBLE);
            layout.findViewById(R.id.station_mmsi).setEnabled(true);
            layout.findViewById(R.id.staticStationTabletLat).setVisibility(View.GONE);
            layout.findViewById(R.id.staticStationTabletLon).setVisibility(View.GONE);
            layout.findViewById(R.id.station_confirm).setClickable(true);
            layout.findViewById(R.id.station_confirm).setEnabled(true);
            setHasOptionsMenu(false);
        }else {
            layout.findViewById(R.id.staticStationTabletLat).setVisibility(View.VISIBLE);
            layout.findViewById(R.id.staticStationTabletLon).setVisibility(View.VISIBLE);
            layout.findViewById(R.id.stationMMSI).setVisibility(View.GONE);
            layout.findViewById(R.id.station_confirm).setClickable(true);
            layout.findViewById(R.id.station_confirm).setEnabled(true);
            changeFormat = DatabaseHelper.readCoordinateDisplaySetting(getActivity());
            numOfSignificantFigures = DatabaseHelper.readSiginificantDigitsSetting(getActivity());
            setHasOptionsMenu(true);
        }

        populateStationType(layout);
        return layout;
    }

    @Override
    public void onClick(View v){
        activityView = getView();
        switch (v.getId()){
            case R.id.station_confirm:
                if(stationTypeAIS) {
                    insertAISStation();
                } else{
                    insertStaticStation();
                }
                break;
        }
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
    public boolean onOptionsItemSelected(MenuItem menuItem){
        switch (menuItem.getItemId()){
            case R.id.changeLatLonFormat:
                changeFormat = !changeFormat;
                populateTabLocation();
                DatabaseHelper.updateCoordinateDisplaySetting(getActivity(), changeFormat);
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void populateTabLocation(){
        Log.d(TAG, "Tab Location" + String.valueOf(changeFormat));
        View v = getView();
        TextView latView = v.findViewById(R.id.staticStationCurrentLat);
        TextView lonView = v.findViewById(R.id.staticStationCurrentLon);
        String formatString = "%." + String.valueOf(numOfSignificantFigures) + "f";
        if(changeFormat){
            String[] formattedCoordinates = NavigationFunctions.locationInDegrees(tabletLat, tabletLon);
            latView.setText(formattedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
            lonView.setText(formattedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);
        } else {
            latView.setText(String.format(formatString, tabletLat));
            lonView.setText(String.format(formatString, tabletLon));
        }
    }

    private boolean validateMMSINumber(EditText mmsi) {
        return mmsi.getText().length() == VALID_MMSI_LENGTH && !TextUtils.isEmpty(mmsi.getText().toString()) && TextUtils.isDigitsOnly(mmsi.getText().toString());
    }

    private void insertAISStation(){

        EditText mmsi_TV = activityView.findViewById(R.id.station_mmsi);
        EditText stationName_TV = activityView.findViewById(R.id.station_name);

        Spinner stationTypeOption = activityView.findViewById(R.id.stationType);
        String stationType = stationTypeOption.getSelectedItem().toString();
        String stationName = stationName_TV.getText().toString();

        if (TextUtils.isEmpty(stationName_TV.getText().toString())){
            Toast.makeText(getActivity(), "Invalid Station Name", Toast.LENGTH_LONG).show();
            return;
        }

        if (!validateMMSINumber(mmsi_TV)) {
            Toast.makeText(getActivity(), "MMSI Number does not match the requirements", Toast.LENGTH_LONG).show();
            return;
        }

        int mmsi = Integer.parseInt(mmsi_TV.getText().toString());


        try {
            DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            if (checkStationInDBTables(db, mmsi)){
                Toast.makeText(getActivity(), "Duplicate MMSI, AIS Station already exists", Toast.LENGTH_LONG).show();
                return;
            }
            ContentValues station = new ContentValues();
            ContentValues fixedStation = new ContentValues();
            station.put(DatabaseHelper.mmsi, mmsi);
            station.put(DatabaseHelper.stationName, stationName);
            fixedStation.put(DatabaseHelper.mmsi, mmsi);
            fixedStation.put(DatabaseHelper.stationName, stationName);
            fixedStation.put(DatabaseHelper.stationType, stationType);
            fixedStation.put(DatabaseHelper.isLocationReceived, DatabaseHelper.IS_LOCATION_RECEIVED_INITIAL_VALUE);

            db.insert(DatabaseHelper.stationListTable, null, station);
            db.insert(DatabaseHelper.fixedStationTable, null, fixedStation);

            AISStationCoordinateFragment aisFragment = new AISStationCoordinateFragment();
            Bundle argument = new Bundle();
            argument.putInt(DatabaseHelper.mmsi, mmsi);
            aisFragment.setArguments(argument);
            FragmentChangeListener fc = (FragmentChangeListener) getActivity();
            if (fc != null) {
                fc.replaceFragment(aisFragment);
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
            Log.d(TAG, "Database Unavailable");
        }


    }

    private void insertStaticStation(){

        EditText stationName_TV = activityView.findViewById(R.id.station_name);
        String stationName = stationName_TV.getText().toString();
        Spinner stationTypeOption = activityView.findViewById(R.id.stationType);
        String stationType = stationTypeOption.getSelectedItem().toString();
        tabletLat = (tabletLat == null) ? 0.0 : tabletLat;
        tabletLon = (tabletLon == null) ? 0.0 : tabletLon;
        if(tabletLat != 0.0 && tabletLon != 0.0) {
            if (!TextUtils.isEmpty(stationName_TV.getText().toString())) {

                DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                if (checkStaticStationInDBTables(db, stationName)) {
                    Toast.makeText(getActivity(), "Duplicate Static Station, Station already exists", Toast.LENGTH_LONG).show();
                    return;
                }
                StaticStationFragment stationFragment = new StaticStationFragment();
                Bundle arguments = new Bundle();
                arguments.putString(DatabaseHelper.staticStationName, stationName);
                arguments.putString(DatabaseHelper.stationType, stationType);
                stationFragment.setArguments(arguments);
                FragmentChangeListener fc = (FragmentChangeListener) getActivity();
                fc.replaceFragment(stationFragment);

            } else {
                Toast.makeText(getActivity(), "Invalid Station Name", Toast.LENGTH_LONG).show();
            }
        }else{
            Log.d(TAG, "Error with GPS Service");
            Toast.makeText(getActivity(), "Error reading Device Lat and Long", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onResume(){
        super.onResume();
        DeploymentActivity activity = (DeploymentActivity)getActivity();
        if(activity != null){
            activity.showUpButton();
        }
    }

    private void populateStationType(View v){
        List<String> stationList = new ArrayList<String>();
        /*for(int i = 0; i < DatabaseHelper.stationTypes.length; i++){
            stationList.add(DatabaseHelper.stationTypes[i]);
        }*/
        stationList.addAll(Arrays.asList(DatabaseHelper.stationTypes));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.simple_spinner_item, stationList
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner stationType = v.findViewById(R.id.stationType);
        stationType.setAdapter(adapter);
    }

    private boolean checkStationInDBTables(SQLiteDatabase db, int MMSI){

        try{

            Cursor mStationListCursor, mFixedStnCursor;
            mStationListCursor = db.query(DatabaseHelper.stationListTable, new String[]{DatabaseHelper.mmsi}, DatabaseHelper.mmsi + " = ?",
                    new String[]{String.valueOf(MMSI)}, null, null, null);
            mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable, new String[]{DatabaseHelper.mmsi}, DatabaseHelper.mmsi + " = ?",
                    new String[]{String.valueOf(MMSI)}, null, null, null);

            return mStationListCursor.moveToFirst() && mFixedStnCursor.moveToFirst();
        }catch (SQLException e){
            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
            return false;
        }
    }


    private boolean checkStaticStationInDBTables(SQLiteDatabase db, String stationName){

        try{
            Cursor mStaticStationListCursor;
            mStaticStationListCursor = db.query(DatabaseHelper.staticStationListTable, new String[]{DatabaseHelper.staticStationName}, DatabaseHelper.staticStationName + " = ?",
                    new String[]{stationName}, null, null, null);

            return mStaticStationListCursor.moveToFirst();
        }catch (SQLException e){
            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
            return false;
        }
    }


}

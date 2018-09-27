package de.awi.floenavigation.initialsetup;


import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import de.awi.floenavigation.DatabaseHelper;
import de.awi.floenavigation.FragmentChangeListener;
import de.awi.floenavigation.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class MMSIFragment extends Fragment implements View.OnClickListener{


    private SQLiteDatabase db;
    private long stationCount;
    private static final String TAG = "MMSI Fragment";
    private SQLiteOpenHelper dbHelper;


    public MMSIFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout =  inflater.inflate(R.layout.fragment_mmsi, container, false);

        Button confirmButton = layout.findViewById(R.id.confirm_Button);
        confirmButton.setOnClickListener(this);
        dbHelper = DatabaseHelper.getDbInstance(getActivity());
        db = dbHelper.getReadableDatabase();
        stationCount = DatabaseUtils.queryNumEntries(db, DatabaseHelper.stationListTable);
        return layout;
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.confirm_Button:
                onClickConfirm();
                break;
        }
    }


    public void onClickConfirm(){
        View view = getView();
        EditText aisStationName = view.findViewById(R.id.ais_station_name);
        EditText mmsi = view.findViewById(R.id.mmsi_field);

        String stationName = aisStationName.getText().toString();
        int mmsiNumber = Integer.parseInt(mmsi.getText().toString());

        insertStation(stationName, mmsiNumber);
        //if(stationCount == 0) {
            /*AISMessageReceiver aisMessage = new AISMessageReceiver(GridSetupActivity.dstAddress, GridSetupActivity.dstPort, getActivity().getApplicationContext());
            Thread aisMessageReceiver = new Thread(aisMessage);
            aisMessageReceiver.start();*/
        //}
        CoordinateFragment coordinateFragment = new CoordinateFragment();
        Bundle argument = new Bundle();
        argument.putInt(DatabaseHelper.mmsi, mmsiNumber);
        argument.putString(DatabaseHelper.stationName, stationName);
        coordinateFragment.setArguments(argument);
        FragmentChangeListener fc = (FragmentChangeListener)getActivity();
        fc.replaceFragment(coordinateFragment);
    }

    private void insertStation(String AISStationName, int MMSI){
        //DatabaseHelper databaseHelper = new DatabaseHelper(getActivity());
        try{
            //db = databaseHelper.getWritableDatabase();
            dbHelper = DatabaseHelper.getDbInstance(getActivity());
            db = dbHelper.getReadableDatabase();
            ContentValues station = new ContentValues();
            station.put(DatabaseHelper.mmsi, MMSI);
            station.put(DatabaseHelper.stationName, AISStationName);
            ContentValues stationData = new ContentValues();
            stationData.put(DatabaseHelper.mmsi, MMSI);
            stationData.put(DatabaseHelper.stationName, AISStationName);
            stationData.put(DatabaseHelper.isLocationReceived, DatabaseHelper.IS_LOCATION_RECEIVED_INITIAL_VALUE);
            if(stationCount == 0){
                stationData.put(DatabaseHelper.xPosition, DatabaseHelper.station1InitialX);
                stationData.put(DatabaseHelper.yPosition, DatabaseHelper.station1InitialY);
                stationData.put(DatabaseHelper.alpha, DatabaseHelper.station1Alpha);
                stationData.put(DatabaseHelper.distance, DatabaseHelper.ORIGIN_DISTANCE);
            } else if(stationCount == 1){
                //stationData.put(DatabaseHelper.xPosition, DatabaseHelper.station2InitialX);
                stationData.put(DatabaseHelper.yPosition, DatabaseHelper.station2InitialY);
                stationData.put(DatabaseHelper.alpha, DatabaseHelper.station2Alpha);

            } else{
                Toast.makeText(getActivity(), "Wrong Data", Toast.LENGTH_LONG).show();
                Log.d(TAG, "StationCount Greater than 2");
            }
            db.insert(DatabaseHelper.stationListTable, null, station);
            db.insert(DatabaseHelper.baseStationTable, null, station);
            db.insert(DatabaseHelper.fixedStationTable, null, stationData);
            //db.close();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Unavailable");
            e.printStackTrace();
        }

    }

    @Override
    public String toString(){
        return "mmsiFragment";
    }




}

package de.awi.floenavigation;


import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


/**
 * A simple {@link Fragment} subclass.
 */
public class MMSIFragment extends Fragment implements View.OnClickListener{


    public MMSIFragment() {
        // Required empty public constructor
    }


    private SQLiteDatabase db;
    private long stationCount;
    private static final String TAG = "MMSI Fragment";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout =  inflater.inflate(R.layout.fragment_mmsi, container, false);

        Button confirmButton = layout.findViewById(R.id.confirm_Button);
        confirmButton.setOnClickListener(this);
        SQLiteOpenHelper databaseHelper = new DatabaseHelper(getActivity());
        db = databaseHelper.getReadableDatabase();
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
        argument.putInt(CoordinateFragment.MMSI_NUMBER, mmsiNumber);
        coordinateFragment.setArguments(argument);
        FragmentChangeListener fc = (FragmentChangeListener)getActivity();
        fc.replaceFragment(coordinateFragment);
    }

    private void insertStation(String AISStationName, int MMSI){
        //DatabaseHelper databaseHelper = new DatabaseHelper(getActivity());
        try{
            //db = databaseHelper.getWritableDatabase();
            ContentValues station = new ContentValues();
            station.put(DatabaseHelper.mmsi, MMSI);
            station.put(DatabaseHelper.stationName, AISStationName);
            ContentValues stationData = new ContentValues();
            stationData.put(DatabaseHelper.mmsi, MMSI);
            stationData.put(DatabaseHelper.stationName, AISStationName);
            stationData.put(DatabaseHelper.isLocationReceived, 0);
            if(stationCount == 0){
                stationData.put(DatabaseHelper.xPosition, DatabaseHelper.station1InitialX);
                stationData.put(DatabaseHelper.yPosition, DatabaseHelper.station1InitialY);
            } else if(stationCount == 1){
                stationData.put(DatabaseHelper.xPosition, DatabaseHelper.station2InitialX);
                stationData.put(DatabaseHelper.yPosition, DatabaseHelper.station2InitialY);
            } else{
                Toast.makeText(getActivity(), "Wrong Data", Toast.LENGTH_LONG).show();
                Log.d(TAG, "StationCount Greater than 2");
            }
                db.insert(DatabaseHelper.stationListTable, null, station);
                db.insert(DatabaseHelper.baseStationTable, null, station);
                db.insert(DatabaseHelper.fixedStationTable, null, stationData);
                db.close();
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

package de.awi.floenavigation;


import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;


/**
 * A simple {@link Fragment} subclass.
 */
public class MMSIFragment extends Fragment implements View.OnClickListener{


    public MMSIFragment() {
        // Required empty public constructor
    }


    private SQLiteDatabase db;
    private long stationCount;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout =  inflater.inflate(R.layout.fragment_mmsi, container, false);

        Button confirmButton = layout.findViewById(R.id.confirm_Button);
        confirmButton.setOnClickListener(this);
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
        CoordinateFragment coordinateFragment = new CoordinateFragment();
        Bundle argument = new Bundle();
        argument.putInt(CoordinateFragment.MMSI_NUMBER, mmsiNumber);
        coordinateFragment.setArguments(argument);
        FragmentChangeListener fc = (FragmentChangeListener)getActivity();
        fc.replaceFragment(coordinateFragment);
    }

    private void insertStation(String AISStationName, int MMSI){
        DatabaseHelper databaseHelper = new DatabaseHelper(getActivity());
        db = databaseHelper.getWritableDatabase();
        ContentValues station = new ContentValues();
        station.put(DatabaseHelper.mmsi, MMSI);
        station.put(DatabaseHelper.stationName, AISStationName);
       // ContentValues stationData =
        db.insert(DatabaseHelper.stationListTable, null, station);
        db.insert(DatabaseHelper.fixedStationTable, null, station);
        db.close();
    }

    @Override
    public String toString(){
        return "mmsiFragment";
    }



}

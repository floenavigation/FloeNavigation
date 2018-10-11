package de.awi.floenavigation.deployment;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.awi.floenavigation.DatabaseHelper;
import de.awi.floenavigation.FragmentChangeListener;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_station_install, container, false);
        layout.findViewById(R.id.station_confirm).setOnClickListener(this);
        layout.findViewById(R.id.withAIS).setOnClickListener(this);
        layout.findViewById(R.id.withoutAIS).setOnClickListener(this);
        layout.findViewById(R.id.station_mmsi).setEnabled(false);
        layout.findViewById(R.id.station_confirm).setClickable(false);
        layout.findViewById(R.id.station_confirm).setEnabled(false);

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

            case R.id.withAIS:
                activityView.findViewById(R.id.stationMMSI).setVisibility(View.VISIBLE);
                activityView.findViewById(R.id.station_mmsi).setEnabled(true);
                stationTypeAIS = true;
                activityView.findViewById(R.id.station_confirm).setClickable(true);
                activityView.findViewById(R.id.station_confirm).setEnabled(true);
                break;

            case R.id.withoutAIS:
                activityView.findViewById(R.id.stationMMSI).setVisibility(View.GONE);
                stationTypeAIS = false;
                activityView.findViewById(R.id.station_confirm).setClickable(true);
                activityView.findViewById(R.id.station_confirm).setEnabled(true);
                break;
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

        if (validateMMSINumber(mmsi_TV)) {
            int mmsi = Integer.parseInt(mmsi_TV.getText().toString());

            try {
                DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                ContentValues station = new ContentValues();
                ContentValues fixedStation = new ContentValues();
                station.put(DatabaseHelper.mmsi, mmsi);
                station.put(DatabaseHelper.stationName, stationName);
                fixedStation.put(DatabaseHelper.mmsi, mmsi);
                fixedStation.put(DatabaseHelper.stationName, stationName);
                fixedStation.put(DatabaseHelper.stationType, stationType);
                fixedStation.put(DatabaseHelper.isLocationReceived, DatabaseHelper.IS_LOCATION_RECEIVED_INITIAL_VALUE);
                try {
                    db.insert(DatabaseHelper.stationListTable, null, station);
                    db.insert(DatabaseHelper.fixedStationTable, null, fixedStation);

                    AISStationCoordinateFragment aisFragment = new AISStationCoordinateFragment();
                    Bundle argument = new Bundle();
                    argument.putInt(DatabaseHelper.mmsi, mmsi);
                    aisFragment.setArguments(argument);
                    FragmentChangeListener fc = (FragmentChangeListener) getActivity();
                    fc.replaceFragment(aisFragment);
                }catch (SQLiteConstraintException e){
                    Toast.makeText(getActivity(), "MMSI already present", Toast.LENGTH_LONG).show();
                }
            } catch (SQLiteException e) {
                e.printStackTrace();
                Log.d(TAG, "Database Unavailable");
            }
        }else {
            Toast.makeText(getActivity(), "MMSI Number does not match the requirements", Toast.LENGTH_LONG).show();
        }

    }

    private void insertStaticStation(){

        EditText stationName_TV = activityView.findViewById(R.id.station_name);
        String stationName = stationName_TV.getText().toString();
        Spinner stationTypeOption = activityView.findViewById(R.id.stationType);
        String stationType = stationTypeOption.getSelectedItem().toString();


        StaticStationFragment stationFragment = new StaticStationFragment();
        Bundle arguments = new Bundle();
        arguments.putString(DatabaseHelper.staticStationName, stationName);
        arguments.putString(DatabaseHelper.stationType, stationType);
        stationFragment.setArguments(arguments);
        FragmentChangeListener fc = (FragmentChangeListener) getActivity();
        fc.replaceFragment(stationFragment);

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


}

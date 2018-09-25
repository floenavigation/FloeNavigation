package de.awi.floenavigation.deployment;


import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import de.awi.floenavigation.DatabaseHelper;
import de.awi.floenavigation.MainActivity;
import de.awi.floenavigation.R;
import de.awi.floenavigation.aismessages.AISDecodingService;

/**
 * A simple {@link Fragment} subclass.
 */
public class AISStationCoordinateFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "AISStationDeployFrag";

    private static final String changeText = "AIS Packet Received from the new Station";
    private int MMSINumber;
    private final Handler handler = new Handler();
    private static final int checkInterval = 1000;


    public AISStationCoordinateFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_station_coordinate, container, false);
        layout.findViewById(R.id.station_finish).setOnClickListener(this);
        layout.findViewById(R.id.station_finish).setClickable(false);

        MMSINumber = getArguments().getInt(DatabaseHelper.mmsi);

        handler.post(new Runnable() {
            @Override
            public void run() {

                if(checkForAISPacket()){
                    Log.d(TAG, "AIS Packet Received");
                    packetReceived();
                } else{
                    Log.d(TAG, "Waiting for AIS Packet");
                    handler.postDelayed(this, checkInterval);
                }
            }
        });

        return layout;
    }


    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.station_finish:
                onClickFinish();
        }
    }

    /**
     *
     */

    private void packetReceived(){
        View v = getView();
        ProgressBar progress = v.findViewById(R.id.aisStationProgress);
        progress.stopNestedScroll();
        progress.setVisibility(View.GONE);
        TextView msg = v.findViewById(R.id.aisStationFragMsg);
        msg.setText(changeText);
        v.findViewById(R.id.station_finish).setClickable(true);
        v.findViewById(R.id.station_finish).setEnabled(true);
    }

    /**
     * Method for checking whether a Position Report has been received from an AIS Station as it is being deployed.
     * @return True if packet is received.
     */

    private boolean checkForAISPacket(){
        boolean success = false;
        int locationReceived;
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getActivity());;
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(DatabaseHelper.fixedStationTable,
                    new String[]{DatabaseHelper.isLocationReceived},
                    DatabaseHelper.mmsi + " = ? AND (" + DatabaseHelper.packetType + " = ? OR " + DatabaseHelper.packetType + " = ? )",
                    new String[] {Integer.toString(MMSINumber), Integer.toString(AISDecodingService.POSITION_REPORT_CLASSA_TYPE_1), Integer.toString(AISDecodingService.POSITION_REPORT_CLASSB)},
                    null, null, null);
            if(cursor.moveToFirst()){
                locationReceived = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.isLocationReceived));
                if(locationReceived == 1) {
                    success = true;
                    //Toast.makeText(getActivity(), "Success True", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Packet Recieved from AIS Station");
                }
            }
            cursor.close();
            //db.close();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Unavailable");
            Toast.makeText(getActivity(), "Database Unavailable", Toast.LENGTH_LONG).show();
        }
        return success;
    }

    private void onClickFinish(){
        Toast.makeText(getContext(), "Deployment Complete", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(getActivity(), MainActivity.class);
        getActivity().startActivity(intent);
    }

}

package de.awi.floenavigation;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class RecoveryActivity extends ActionBarActivity {

    private static final String TAG = "RecoveryActivity";
    private boolean aisDeviceCheck = true;
    private int[] baseStnMMSI = new int[DatabaseHelper.INITIALIZATION_SIZE];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recovery);

        final RadioButton withAISButton = findViewById(R.id.withAIS);
        final RadioButton withoutAISButton = findViewById(R.id.withoutAIS);
        final TextView devicesOptionSelection = findViewById(R.id.devicesOptionSelection);
        final TextView AISdeviceSelectedView = findViewById(R.id.AISdeviceSelected);
        final TextView StaticStationSelectedView = findViewById(R.id.StaticStationSelected);
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.RadioSelect);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                if (withAISButton.equals(findViewById(checkedId))){
                    AISdeviceSelectedView.setVisibility(View.VISIBLE);
                    StaticStationSelectedView.setVisibility(View.GONE);
                    devicesOptionSelection.setText(R.string.mmsi);
                    aisDeviceCheck = true;
                }else{
                    AISdeviceSelectedView.setVisibility(View.GONE);
                    StaticStationSelectedView.setVisibility(View.VISIBLE);
                    devicesOptionSelection.setText(R.string.staticstn);
                    aisDeviceCheck = false;
                }

            }
        });
    }


    public void onClickRecoveryListenerconfirm(View view) {
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            baseStationsRetrievalfromDB(db);
            TextView AISdeviceSelectedView = findViewById(R.id.AISdeviceSelected);
            TextView StaticStationSelectedView = findViewById(R.id.StaticStationSelected);
            if (aisDeviceCheck) {
                int mmsiValue = Integer.parseInt(AISdeviceSelectedView.getText().toString());
                if (mmsiValue == baseStnMMSI[DatabaseHelper.firstStationIndex] || mmsiValue == baseStnMMSI[DatabaseHelper.secondStationIndex]){
                    Toast.makeText(getApplicationContext(), "cannot be recovered since its a base station", Toast.LENGTH_LONG).show();
                    return;
                }else{
                    db.delete(DatabaseHelper.stationListTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsiValue)});
                    db.delete(DatabaseHelper.fixedStationTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsiValue)});
                }
            } else {
                String staticStnName = StaticStationSelectedView.getText().toString();
                db.delete(DatabaseHelper.staticStationListTable, DatabaseHelper.staticStationName + " = ?", new String[]{staticStnName});
            }
            Toast.makeText(getApplicationContext(), "Device Recovered", Toast.LENGTH_LONG).show();

        }catch(SQLiteException e) {
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }
    }

    private void baseStationsRetrievalfromDB(SQLiteDatabase db){

        try {
            //SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            //SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor mBaseStnCursor = db.query(DatabaseHelper.baseStationTable, new String[]{DatabaseHelper.mmsi},
                    null, null, null, null, null);

            if (mBaseStnCursor.getCount() == DatabaseHelper.NUM_OF_BASE_STATIONS) {
                int index = 0;

                if (mBaseStnCursor.moveToFirst()) {
                    do {
                        baseStnMMSI[index] = mBaseStnCursor.getInt(mBaseStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                        index++;
                    } while (mBaseStnCursor.moveToNext());
                }else {
                    Log.d(TAG, "Base stn cursor error");
                }

            } else {
                Log.d(TAG, "Error reading from base stn table");
            }
            mBaseStnCursor.close();
        }catch (SQLException e){

            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        }

    }

    public void onClickViewDeployedStations(View view) {
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            long numOfStaticStations = DatabaseUtils.queryNumEntries(db, DatabaseHelper.staticStationListTable);
            Intent listViewIntent = new Intent(this, ListViewActivity.class);
            if (aisDeviceCheck)
                listViewIntent.putExtra("GenerateDataOption", "AISRecoverActivity");
            else {
                if (numOfStaticStations > 0) {
                    listViewIntent.putExtra("GenerateDataOption", "StaticStationRecoverActivity");
                }else {
                    Toast.makeText(this, "No static station are deployed in the grid", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            startActivity(listViewIntent);

        }catch (SQLiteException e){
            Log.d(TAG, "Error in reading database");
            e.printStackTrace();
        }

    }

}

package de.awi.floenavigation;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ConfigurationActivity extends ActionBarActivity {

    private static final String TAG = "ConfigurationActivity";
    private EditText valueField;
    private boolean coordinateTypeDegMinSec = false;
    private boolean isNormalParam = true;
    private int spinnerItem = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        populateParameters();
        Spinner paramType = findViewById(R.id.parameterSelect);
        paramType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if(position != 2){
                    findViewById(R.id.normalParam).setVisibility(View.VISIBLE);
                    findViewById(R.id.latLonViewParam).setVisibility(View.GONE);
                    spinnerItem = position;
                    isNormalParam = true;
                } else {
                    findViewById(R.id.normalParam).setVisibility(View.GONE);
                    findViewById(R.id.latLonViewParam).setVisibility(View.VISIBLE);
                    isNormalParam = false;
                    spinnerItem = 2;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                return;
            }
        } );
    }



    public void onClickConfigurationParamsconfirm(View view) {
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            valueField = findViewById(R.id.parameterValue);
            if(isNormalParam){
                if(validateValueField(valueField)){
                    String paramName = DatabaseHelper.configurationParameters[spinnerItem];
                    String paramValue = valueField.getText().toString();
                    updateDatabaseTable(db, paramName, paramValue);
                }else {
                    Toast.makeText(this, "Please Enter a Correct Parameter Value", Toast.LENGTH_LONG).show();
                }
            } else{

                String paramName = DatabaseHelper.configurationParameters[spinnerItem];
                String paramValue;
                if(coordinateTypeDegMinSec){
                    paramValue = "0";

                } else {
                    paramValue = "1";
                }
                Log.d(TAG, paramName);
                Log.d(TAG, paramValue);
                updateDatabaseTable(db, paramName, paramValue);
            }

            Toast.makeText(getApplicationContext(), "Configuration Saved", Toast.LENGTH_LONG).show();


        } catch(SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }
    }

    private void populateParameters(){
        List<String> parameterList = new ArrayList<String>();

        parameterList.addAll(Arrays.asList(DatabaseHelper.configurationParameters));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, parameterList
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner parameterType = findViewById(R.id.parameterSelect);
        parameterType.setAdapter(adapter);
    }

    private void updateDatabaseTable(SQLiteDatabase db, String parameterName, String inputValue){
        ContentValues configParamsContents = new ContentValues();
        configParamsContents.put(DatabaseHelper.parameterName, parameterName);
        configParamsContents.put(DatabaseHelper.parameterValue, inputValue);
        db.update(DatabaseHelper.configParametersTable, configParamsContents, DatabaseHelper.parameterName + " = ?",
                new String[] {parameterName});

    }

    public void onClickViewConfigurationParams(View view) {
        Intent parameterActivityIntent = new Intent(this, ParameterViewActivity.class);
        startActivity(parameterActivityIntent);
    }

    private boolean validateValueField(EditText valueField) {
        return !TextUtils.isEmpty(valueField.getText().toString()) && TextUtils.isDigitsOnly(valueField.getText().toString());
    }

    public void onClickDegreeFraction(View view) {
        coordinateTypeDegMinSec = false;
    }

    public void onClickDegMinSec(View view) {
        coordinateTypeDegMinSec = true;
    }
}

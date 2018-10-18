package de.awi.floenavigation;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ConfigurationActivity extends ActionBarActivity {

    private static final String TAG = "ConfigurationActivity";
    private EditText valueField;
    private boolean coordinateTypeDegMinSec = false;
    private boolean isNormalParam = true;
    private boolean isNormalInitialRangeParam = false;
    private int spinnerItem = 0;
    private int MIN_VALUE = 10;
    private int progressValue = MIN_VALUE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        populateParameters();
        final Spinner paramType = findViewById(R.id.parameterSelect);

        SeekBar initialTimeRange = findViewById(R.id.seekBarInitialTimeRange);
        final TextView progressBarValue = findViewById(R.id.progressBarInitValue);
        initialTimeRange.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressValue = MIN_VALUE + progress;
                progressBarValue.setText(String.valueOf(progressValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        paramType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                EditText paramVal = findViewById(R.id.parameterValue);
                int defaultInputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_NUMBER_VARIATION_PASSWORD;
                if (position != 2) {
                    findViewById(R.id.normalParam).setVisibility(View.VISIBLE);
                    findViewById(R.id.latLonViewParam).setVisibility(View.GONE);
                    findViewById(R.id.normalInitialRangeParam).setVisibility(View.GONE);
                    spinnerItem = position;
                    isNormalInitialRangeParam = false;
                    isNormalParam = true;
                } else {
                    findViewById(R.id.normalParam).setVisibility(View.GONE);
                    findViewById(R.id.latLonViewParam).setVisibility(View.VISIBLE);
                    findViewById(R.id.normalInitialRangeParam).setVisibility(View.GONE);
                    isNormalParam = false;
                    isNormalInitialRangeParam = false;
                    spinnerItem = 2;
                }
                if (position == 4){
                    findViewById(R.id.normalParam).setVisibility(View.GONE);
                    findViewById(R.id.normalInitialRangeParam).setVisibility(View.VISIBLE);
                    isNormalInitialRangeParam = true;
                    isNormalParam = false;
                    progressBarValue.setText(String.valueOf(MIN_VALUE));
                }
                if(position == 5){
                    //defaultInputType = paramVal.getInputType();
                    paramVal.setInputType(InputType.TYPE_CLASS_TEXT);
                } else{
                    paramVal.setInputType(defaultInputType);

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                return;
            }
        });
    }



    public void onClickConfigurationParamsconfirm(View view) {
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            if(isNormalParam){
                valueField = findViewById(R.id.parameterValue);
                if(validateValueField(valueField)){
                    String paramName = DatabaseHelper.configurationParameters[spinnerItem];
                    String paramValue = valueField.getText().toString();
                    updateDatabaseTable(db, paramName, paramValue);

                }else {
                    Toast.makeText(this, "Please Enter a Correct Parameter Value", Toast.LENGTH_LONG).show();
                }
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            } else if (isNormalInitialRangeParam) {
                String paramName = DatabaseHelper.configurationParameters[spinnerItem];
                Log.d(TAG, String.valueOf(progressValue * 60 * 1000));
                updateDatabaseTable(db, paramName, String.valueOf(progressValue * 60 * 1000));
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

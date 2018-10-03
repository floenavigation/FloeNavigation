package de.awi.floenavigation;

import android.app.Activity;
import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class ConfigurationActivity extends Activity {

    private static final String TAG = "ConfigurationActivity";
    private EditText errorThresholdView, predAccThresholdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        errorThresholdView = findViewById(R.id.error_threshold_id);
        predAccThresholdView = findViewById(R.id.prediction_accuracy_threshold_id);
    }


    public void onClickConfigurationParamsconfirm(View view) {
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            int errorThresholdValue = Integer.parseInt(errorThresholdView.getText().toString());
            int predAccThresholdValue = Integer.parseInt(predAccThresholdView.getText().toString());

            updateDatabaseTable(db, DatabaseHelper.error_threshold, errorThresholdValue);
            updateDatabaseTable(db, DatabaseHelper.prediction_accuracy_threshold, predAccThresholdValue);

        } catch(SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }
    }

    private void updateDatabaseTable(SQLiteDatabase db, String parameterName, int inputValue){
        ContentValues configParamsContents = new ContentValues();
        configParamsContents.put(DatabaseHelper.parameterName, parameterName);
        configParamsContents.put(DatabaseHelper.parameterValue, inputValue);
        db.update(DatabaseHelper.configParametersTable, configParamsContents, null, null);
    }
}

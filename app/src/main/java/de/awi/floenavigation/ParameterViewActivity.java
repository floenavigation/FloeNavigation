package de.awi.floenavigation;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.SwipeDismissBehavior;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class ParameterViewActivity extends ListActivity {

    private static final String TAG = "ParameterViewActivity";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private ArrayAdapter arrayAdapter;
    private ArrayList<ParameterObject> parameterObjects = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_parameter_view);



        //ListView parametersLV = findViewById(R.id.parametersListView);
        arrayAdapter = new ParameterListAdapter(this, generateData());
        setListAdapter(arrayAdapter);

        //displayData();

    }

    private ArrayList<ParameterObject> generateData(){
        try{
            dbHelper = DatabaseHelper.getDbInstance(this);
            db = dbHelper.getReadableDatabase();
            Cursor paramsCursor = db.query(DatabaseHelper.configParametersTable,
                    new String[] {DatabaseHelper.parameterName, DatabaseHelper.parameterValue},
                    null,
                    null,
                    null, null, null);
            while(paramsCursor.moveToNext()){
               String paramName = paramsCursor.getString(paramsCursor.getColumnIndexOrThrow(DatabaseHelper.parameterName));
               String paramValue = String.valueOf(paramsCursor.getString(paramsCursor.getColumnIndexOrThrow(DatabaseHelper.parameterValue)));
                Log.d(TAG, paramName);
               Log.d(TAG, paramValue);
               if(paramName.equals(DatabaseHelper.lat_long_view_format)){
                   if(paramValue.equals("0")){
                       paramValue = getResources().getString(R.string.latLonDegMinSec);
                   } else if(paramValue.equals("1")){
                       paramValue = getResources().getString(R.string.latLonFraction);
                   }
               }
               if (paramName.equals(DatabaseHelper.initial_setup_time)) {
                   paramValue = String.valueOf(Integer.parseInt(paramValue) / 60000);
                   paramValue = paramValue + " mins";
               }


               parameterObjects.add(new ParameterObject(paramName, paramValue));

            }
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
        }
        return parameterObjects;
        //arrayAdapter.notifyDataSetChanged();

    }
}

class ParameterObject{
    private String parameterName;
    private String parameterValue;

    ParameterObject(String name, String value){
        this.parameterName = name;
        this.parameterValue = value;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getParameterValue() {
        return parameterValue;
    }
}

class ParameterListAdapter extends ArrayAdapter<ParameterObject>{

    private ArrayList<ParameterObject> parameters;
    private Context context;

    public ParameterListAdapter(Context con, ArrayList<ParameterObject> params){
        super(con, R.layout.parameter_list_item, params);
        this.context = con;
        this.parameters = params;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.parameter_list_item, parent, false);

        TextView paramName = (TextView)rowView.findViewById(R.id.column1);
        TextView paramValue = rowView.findViewById(R.id.column2);
        RelativeLayout item = rowView.findViewById(R.id.item);

        paramName.setText(parameters.get(position).getParameterName());
        paramValue.setText(parameters.get(position).getParameterValue());

        return rowView;
    }
}

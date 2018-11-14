package de.awi.floenavigation.Synchronization;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.awi.floenavigation.DatabaseHelper;

public class BaseStationSync {
    private static final String TAG = "BaseStationSync";
    private Context mContext;

    private static final String pushURL = "http://192.168.137.1:80/BaseStation/pullStations.php";
    private static final String pullURL = "http://192.168.137.1:80/BaseStation/pushStations.php";

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private StringRequest request;

    private HashMap<Integer, String> baseStationName = new HashMap<>();
    private HashMap<Integer, Integer> mmsiData = new HashMap<>();
    private HashMap<Integer, Integer> isOriginData = new HashMap<>();

    private Cursor baseStationCursor;
    private BaseStation baseStationList;
    private ArrayList<BaseStation> baseStationArrayList = new ArrayList<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;

    BaseStationSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
    }

    public void onClickBaseStationReadButton(){
        try{
            int i = 0;
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            baseStationCursor = db.query(DatabaseHelper.baseStationTable,
                    null,
                    null,
                    null,
                    null, null, null);
            if(baseStationCursor.moveToFirst()){
                do{
                    baseStationName.put(i, baseStationCursor.getString(baseStationCursor.getColumnIndexOrThrow(DatabaseHelper.stationName)));
                    mmsiData.put(i, baseStationCursor.getInt(baseStationCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));
                    isOriginData.put(i, baseStationCursor.getInt(baseStationCursor.getColumnIndexOrThrow(DatabaseHelper.isOrigin)));

                    i++;

                }while (baseStationCursor.moveToNext());
            }
            baseStationCursor.close();
            Toast.makeText(mContext, "Read Completed from DB", Toast.LENGTH_SHORT).show();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }
    }

    public void onClickBaseStationSyncButton() {
        for (int i = 0; i < mmsiData.size(); i++) {
            final int index = i;
            request = new StringRequest(Request.Method.POST, pushURL, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    try {
                        Log.d(TAG, "on receive");
                        JSONObject jsonObject = new JSONObject(response);
                        //Log.d(TAG, "on receive");
                        if (jsonObject.names().get(0).equals("success")) {
                            Toast.makeText(mContext, "SUCCESS " + jsonObject.getString("success"), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, "Error" + jsonObject.getString("error"), Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    HashMap<String, String> hashMap = new HashMap<String, String>();
                    hashMap.put(DatabaseHelper.stationName, (baseStationName.get(index) == null) ? "" : baseStationName.get(index));
                    hashMap.put(DatabaseHelper.mmsi, (mmsiData.get(index) == null) ? "" : mmsiData.get(index).toString());
                    hashMap.put(DatabaseHelper.isOrigin, (isOriginData.get(index) == null) ? "" : isOriginData.get(index).toString());

                    return hashMap;
                }
            };
            requestQueue.add(request);

        }
    }

    public void onClickBaseStationPullButton(){
        dbHelper = DatabaseHelper.getDbInstance(mContext);
        db = dbHelper.getReadableDatabase();
        db.execSQL("Delete from " + DatabaseHelper.baseStationTable);
        StringRequest pullRequest = new StringRequest(pullURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    parser.setInput(new StringReader(response));
                    int event = parser.getEventType();
                    String tag = "";
                    String value = "";
                    while (event != XmlPullParser.END_DOCUMENT) {
                        tag = parser.getName();
                        switch (event) {
                            case XmlPullParser.START_TAG:
                                if (tag.equals(DatabaseHelper.baseStationTable)) {
                                    baseStationList = new BaseStation(mContext);
                                    baseStationArrayList.add(baseStationList);
                                }
                                break;

                            case XmlPullParser.TEXT:
                                value = parser.getText();
                                break;

                            case XmlPullParser.END_TAG:

                                switch (tag) {

                                    case DatabaseHelper.stationName:
                                        baseStationList.setStationName(value);
                                        break;

                                    case DatabaseHelper.mmsi:
                                        baseStationList.setMmsi(Integer.parseInt(value));
                                        break;

                                    case DatabaseHelper.isOrigin:
                                        baseStationList.setOrigin(Integer.parseInt(value));


                                }
                                break;
                        }
                        event = parser.next();
                    }
                    for(BaseStation currentStn : baseStationArrayList){
                        currentStn.insertBaseStationInDB();
                    }
                    Toast.makeText(mContext, "Data Pulled from Server", Toast.LENGTH_SHORT).show();
                } catch (XmlPullParserException e) {
                    Log.d(TAG, "Error Parsing XML");
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.d(TAG, "IOException from Parser");
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        requestQueue.add(pullRequest);

    }
}

class BaseStation{

    private static final String TAG = "BaseStation";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private int mmsi;
    private int isOrigin;
    private Context appContext;
    ContentValues baseStation;
    private String stationName;

    public BaseStation(Context context){
            appContext = context;
            try {
            dbHelper = DatabaseHelper.getDbInstance(appContext);
            db = dbHelper.getReadableDatabase();
            } catch(SQLException e){
            Log.d(TAG, "Database Exception");
            e.printStackTrace();
            }
    }

    private void generateContentValues(){
        baseStation = new ContentValues();
        baseStation.put(DatabaseHelper.stationName, this.stationName);
        baseStation.put(DatabaseHelper.isOrigin, this.isOrigin);
        baseStation.put(DatabaseHelper.mmsi, this.mmsi);
    }

    public void insertBaseStationInDB(){
        generateContentValues();
        int result = db.update(DatabaseHelper.baseStationTable, baseStation, DatabaseHelper.mmsi + " = ?", new String[] {String.valueOf(mmsi)});
        if(result == 0){
            db.insert(DatabaseHelper.baseStationTable, null, baseStation);
            Log.d(TAG, "Station Added");
        } else{
            Log.d(TAG, "Station Updated");
        }
    }

    public int getIsOrigin() {
        return isOrigin;
    }

    public void setOrigin(int origin) {
        this.isOrigin = origin;
    }
    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public int getMmsi() {
        return mmsi;
    }

    public void setMmsi(int mmsi) {
        this.mmsi = mmsi;
    }
}

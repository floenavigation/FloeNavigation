package de.awi.floenavigation.synchronization;

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

import de.awi.floenavigation.helperclasses.DatabaseHelper;

public class BaseStationSync {
    private static final String TAG = "BaseStationSync";
    private Context mContext;

    private String pushURL = "";
    private String pullURL = "";
    private String deleteURL = "";

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private StringRequest request;

    private HashMap<Integer, String> baseStationName = new HashMap<>();
    private HashMap<Integer, Integer> mmsiData = new HashMap<>();
    private HashMap<Integer, Integer> isOriginData = new HashMap<>();

    private Cursor baseStationCursor;
    private BaseStation baseStationList;
    private ArrayList<BaseStation> baseStationArrayList = new ArrayList<>();
    private HashMap<Integer, Integer> deletedBaseStationData = new HashMap<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;
    private boolean dataPullCompleted;

    BaseStationSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
        dataPullCompleted = false;
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
                        JSONObject jsonObject = new JSONObject(response);
                        //Log.d(TAG, "on receive");
                        if (jsonObject.names().get(0).equals("success")) {
                            //Toast.makeText(mContext, "SUCCESS " + jsonObject.getString("success"), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "SUCCESS: " + jsonObject.getString("success"));
                        } else {
                            Toast.makeText(mContext, "Error" + jsonObject.getString("error"), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error: " + jsonObject.getString("error"));
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
        sendBSDeleteRequest();
    }

    public void onClickBaseStationPullButton(){
        dbHelper = DatabaseHelper.getDbInstance(mContext);
        db = dbHelper.getReadableDatabase();
        db.execSQL("Delete from " + DatabaseHelper.baseStationTable);
        db.execSQL("DELETE from " + DatabaseHelper.baseStationDeletedTable);
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
                    dataPullCompleted = true;
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

    public void setBaseUrl(String baseUrl, String port){
        pushURL = "http://" + baseUrl + ":" + port + "/BaseStation/pullStations.php";
        pullURL = "http://" + baseUrl + ":" + port + "/BaseStation/pushStations.php";
        deleteURL = "http://" + baseUrl + ":" + port + "/BaseStation/deleteStations.php";

    }

    public boolean getDataCompleted(){
        return dataPullCompleted;
    }

    private void sendBSDeleteRequest(){

        try{
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            Cursor deletedStationListCursor = db.query(DatabaseHelper.baseStationDeletedTable,
                    null,
                    null,
                    null,
                    null, null, null);
            int i = 0;
            if(deletedStationListCursor.moveToFirst()){
                do{
                    deletedBaseStationData.put(i, deletedStationListCursor.getInt(deletedStationListCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));
                    Log.d(TAG, "MMSI to be Deleted: " + deletedStationListCursor.getInt(deletedStationListCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));
                    i++;

                }while (deletedStationListCursor.moveToNext());
            }
            deletedStationListCursor.close();

        } catch (SQLException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }

        for(int j = 0; j < deletedBaseStationData.size(); j++){
            final int delIndex = j;
            request = new StringRequest(Request.Method.POST, deleteURL, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.names().get(0).equals("success")) {
                            //Toast.makeText(mContext, "SUCCESS " + jsonObject.getString("success"), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "SUCCESS: " + jsonObject.getString("success"));
                        } else {
                            Toast.makeText(mContext, "Error" + jsonObject.getString("error"), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error: " + jsonObject.getString("error"));
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }){
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    HashMap<String,String> hashMap = new HashMap<String, String>();
                    hashMap.put(DatabaseHelper.mmsi,(deletedBaseStationData.get(delIndex) == null)? "" : deletedBaseStationData.get(delIndex).toString());
                    //Log.d(TAG, "MMSI sent to be Deleted: " + deletedStationListData.get(delIndex) + " Index: " + String.valueOf(delIndex));
                    return hashMap;
                }
            };
            requestQueue.add(request);

        }

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

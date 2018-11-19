package de.awi.floenavigation.Synchronization;

import android.content.Context;
import android.database.Cursor;
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

public class StaticStationSync {

    private static final String TAG = "StaticStnSyncActivity";
    private Context mContext;

    private static final String URL = "http://192.168.137.1:80/StaticStation/pullStations.php";
    private static final String pullURL = "http://192.168.137.1:80/StaticStation/pushStations.php";
    private static final String deleteURL = "http://192.168.137.1:80/StaticStation/deleteStations.php";

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private StringRequest request;

    private HashMap<Integer, String> stationNameData = new HashMap<>();
    private HashMap<Integer, Double> alphaData = new HashMap<>();
    private HashMap<Integer, Double> distanceData = new HashMap<>();
    private HashMap<Integer, Double> xPositionData = new HashMap<>();
    private HashMap<Integer, Double> yPositionData = new HashMap<>();
    private HashMap<Integer, String> stationTypeData = new HashMap<>();

    private HashMap<Integer, String> deletedStaticStationData = new HashMap<Integer, String>();
    private Cursor staticStationCursor;
    private StaticStation staticStation;
    private ArrayList<StaticStation> staticStationList = new ArrayList<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;

    StaticStationSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
    }

    public void onClickStaticStationReadButton(){
        try{
            int i = 0;
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            staticStationCursor = db.query(DatabaseHelper.staticStationListTable,
                    null,
                    null,
                    null,
                    null, null, null);
            if(staticStationCursor.moveToFirst()){
                do{
                    stationNameData.put(i, staticStationCursor.getString(staticStationCursor.getColumnIndexOrThrow(DatabaseHelper.stationName)));
                    alphaData.put(i, staticStationCursor.getDouble(staticStationCursor.getColumnIndexOrThrow(DatabaseHelper.alpha)));
                    distanceData.put(i, staticStationCursor.getDouble(staticStationCursor.getColumnIndexOrThrow(DatabaseHelper.distance)));
                    xPositionData.put(i, staticStationCursor.getDouble(staticStationCursor.getColumnIndexOrThrow(DatabaseHelper.xPosition)));
                    yPositionData.put(i, staticStationCursor.getDouble(staticStationCursor.getColumnIndexOrThrow(DatabaseHelper.yPosition)));
                    stationTypeData.put(i, staticStationCursor.getString(staticStationCursor.getColumnIndexOrThrow(DatabaseHelper.stationType)));
                    i++;

                }while (staticStationCursor.moveToNext());
            }
            staticStationCursor.close();

            Cursor deletedStaticStationCursor = db.query(DatabaseHelper.staticStationDeletedTable,
                    null,
                    null,
                    null,
                    null, null, null);
            i = 0;
            if(deletedStaticStationCursor.moveToFirst()){
                do{
                    deletedStaticStationData.put(i, deletedStaticStationCursor.getString(deletedStaticStationCursor.getColumnIndexOrThrow(DatabaseHelper.staticStationName)));
                    Log.d(TAG, "Static Station to be Deleted: " + deletedStaticStationCursor.getString(deletedStaticStationCursor.getColumnIndexOrThrow(DatabaseHelper.staticStationName)));
                    i++;

                }while (deletedStaticStationCursor.moveToNext());
            }
            Toast.makeText(mContext, "Read Completed from DB", Toast.LENGTH_SHORT).show();
            deletedStaticStationCursor.close();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }

    }


    public void onClickStaticStationSyncButton(){
        for(int i = 0; i < stationNameData.size(); i++){
            final int index = i;
            request = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    try {
                        Log.d(TAG, "on receive");
                        JSONObject jsonObject = new JSONObject(response);
                        //Log.d(TAG, "on receive");
                        if(jsonObject.names().get(0).equals("success")){
                            Toast.makeText(mContext,"SUCCESS "+jsonObject.getString("success"),Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(mContext, "Error" +jsonObject.getString("error"), Toast.LENGTH_SHORT).show();
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
                    hashMap.put(DatabaseHelper.stationName,(stationNameData.get(index) == null)? "" : stationNameData.get(index));
                    hashMap.put(DatabaseHelper.alpha,(alphaData.get(index) == null)? "" : alphaData.get(index).toString());
                    hashMap.put(DatabaseHelper.distance,(distanceData.get(index) == null)? "" : distanceData.get(index).toString());
                    hashMap.put(DatabaseHelper.xPosition,(xPositionData.get(index) == null)? "" : xPositionData.get(index).toString());
                    hashMap.put(DatabaseHelper.yPosition,(yPositionData.get(index) == null)? "" : yPositionData.get(index).toString());
                    hashMap.put(DatabaseHelper.stationType,(stationTypeData.get(index) == null)? "" : stationTypeData.get(index));
                    return hashMap;
                }
            };
            requestQueue.add(request);

        }

        for(int j = 0; j < deletedStaticStationData.size(); j++){
            final int delIndex = j;
            request = new StringRequest(Request.Method.POST, deleteURL, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    try {
                        Log.d(TAG, "on receive");
                        JSONObject jsonObject = new JSONObject(response);
                        //Log.d(TAG, "on receive");
                        if(jsonObject.names().get(0).equals("success")){
                            Toast.makeText(mContext,"SUCCESS "+jsonObject.getString("success"),Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(mContext, "Error" +jsonObject.getString("error"), Toast.LENGTH_SHORT).show();
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
                    hashMap.put(DatabaseHelper.staticStationName, (deletedStaticStationData.get(delIndex) == null)? "" : deletedStaticStationData.get(delIndex));
                    //Log.d(TAG, "Static Station sent to be Deleted: " + deletedStaticStationData.get(delIndex) + " Index: " + String.valueOf(delIndex));
                    return hashMap;
                }
            };
            requestQueue.add(request);

        }
    }

    public void onClickStaticStationPullButton(){
        dbHelper = DatabaseHelper.getDbInstance(mContext);
        db = dbHelper.getReadableDatabase();
        db.execSQL("Delete from " + DatabaseHelper.staticStationListTable);
        db.execSQL("Delete from " + DatabaseHelper.staticStationDeletedTable);
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
                                if (tag.equals(DatabaseHelper.staticStationName)) {
                                    staticStation = new StaticStation(mContext);
                                    staticStationList.add(staticStation);
                                }
                                break;

                            case XmlPullParser.TEXT:
                                value = parser.getText();
                                break;

                            case XmlPullParser.END_TAG:

                                switch (tag) {

                                    case DatabaseHelper.staticStationName:
                                        staticStation.setStationName(value);
                                        break;

                                    case DatabaseHelper.alpha:
                                        staticStation.setAlpha(Double.valueOf(value));
                                        break;

                                    case DatabaseHelper.distance:
                                        staticStation.setDistance(Double.valueOf(value));
                                        break;

                                    case DatabaseHelper.xPosition:
                                        staticStation.setxPosition(Double.valueOf(value));
                                        break;

                                    case DatabaseHelper.yPosition:
                                        staticStation.setyPosition(Double.valueOf(value));
                                        break;

                                    case DatabaseHelper.stationType:
                                        staticStation.setStationType(value);
                                        break;

                                }
                                break;
                        }
                        event = parser.next();
                    }
                    for(StaticStation currentStn : staticStationList){
                        currentStn.insertStaticStationInDB();
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

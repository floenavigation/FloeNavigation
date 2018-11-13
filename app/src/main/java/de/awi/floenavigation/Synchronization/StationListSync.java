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

public class StationListSync {

    private static final String TAG = "StnListSyncActivity";
    private Context mContext;

    private static final String URL = "http://192.168.137.1:80/userControl.php";
    private static final String pullURL = "http://192.168.137.1:80/pushUsers.php";
    private static final String deleteURL = "http://192.168.137.1:80/deleteUser.php";

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private StringRequest request;

    private HashMap<Integer, String> stationNameData = new HashMap<>();
    private HashMap<Integer, Integer> mmsiData = new HashMap<>();


    private HashMap<Integer, Integer> deletedStationListData = new HashMap<>();
    private Cursor stationListCursor;
    private StationList stationList;
    private ArrayList<StationList> stationArrayList = new ArrayList<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;

    StationListSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
    }

    public void onClickStationListReadButton(){
        try{
            int i = 0;
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            stationListCursor = db.query(DatabaseHelper.stationListTable,
                    null,
                    null,
                    null,
                    null, null, null);
            if(stationListCursor.moveToFirst()){
                do{
                    stationNameData.put(i, stationListCursor.getString(stationListCursor.getColumnIndexOrThrow(DatabaseHelper.stationName)));
                    mmsiData.put(i, stationListCursor.getInt(stationListCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));

                    i++;

                }while (stationListCursor.moveToNext());
            }
            stationListCursor.close();

            Cursor deletedStationListCursor = db.query(DatabaseHelper.stationListDeletedTable,
                    null,
                    null,
                    null,
                    null, null, null);
            i = 0;
            if(deletedStationListCursor.moveToFirst()){
                do{
                    deletedStationListData.put(i, deletedStationListCursor.getInt(deletedStationListCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));
                    Log.d(TAG, "MMSI to be Deleted: " + deletedStationListCursor.getInt(deletedStationListCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));
                    i++;

                }while (deletedStationListCursor.moveToNext());
            }
            Toast.makeText(mContext, "Read Completed from DB", Toast.LENGTH_SHORT).show();
            deletedStationListCursor.close();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }

    }

    public void onClickStationListSyncButton(){
        for(int i = 0; i < mmsiData.size(); i++){
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
                    hashMap.put(DatabaseHelper.stationName,stationNameData.get(index));
                    hashMap.put(DatabaseHelper.mmsi,mmsiData.get(index).toString());

                    return hashMap;
                }
            };
            requestQueue.add(request);

        }

        for(int j = 0; j < deletedStationListData.size(); j++){
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
                    hashMap.put(DatabaseHelper.mmsi,deletedStationListData.get(delIndex).toString());
                    Log.d(TAG, "MMSI sent to be Deleted: " + deletedStationListData.get(delIndex) + " Index: " + String.valueOf(delIndex));
                    return hashMap;
                }
            };
            requestQueue.add(request);

        }
    }

    public void onClickStationListPullButton(){
        dbHelper = DatabaseHelper.getDbInstance(mContext);
        db = dbHelper.getReadableDatabase();
        db.execSQL("Delete from " + DatabaseHelper.stationListTable);
        db.execSQL("Delete from " + DatabaseHelper.stationListDeletedTable);
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
                                if (tag.equals(DatabaseHelper.stationListTable)) {
                                    stationList = new StationList(mContext);
                                    stationArrayList.add(stationList);
                                }
                                break;

                            case XmlPullParser.TEXT:
                                value = parser.getText();
                                break;

                            case XmlPullParser.END_TAG:

                                switch (tag) {

                                    case DatabaseHelper.stationName:
                                        stationList.setStationName(value);
                                        break;

                                    case DatabaseHelper.mmsi:
                                        stationList.setMmsi(Integer.parseInt(value));
                                        break;


                                }
                                break;
                        }
                        event = parser.next();
                    }
                    for(StationList currentStn : stationArrayList){
                        currentStn.insertStationInDB();
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

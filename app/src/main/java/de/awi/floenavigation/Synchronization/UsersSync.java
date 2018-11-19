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

public class UsersSync {

    private static final String TAG = "UsersSyncActivity";
    private Context mContext;

    private static final String URL = "http://192.168.137.1:80/Users/pullUsers.php";
    private static final String pullURL = "http://192.168.137.1:80/Users/pushUsers.php";
    private static final String deleteUserURL = "http://192.168.137.1:80/Users/deleteUsers.php";

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private StringRequest request;
    private HashMap<Integer, String> userNameData = new HashMap<>();
    private HashMap<Integer, String> userPasswordData = new HashMap<>();
    private HashMap<Integer, String> deletedUserData = new HashMap<>();
    private Cursor userCursor;
    private Users user;
    private ArrayList<Users> usersList = new ArrayList<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;

    UsersSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
    }

    public void onClickUserReadButton(){
        try{
            int i = 0;
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            userCursor = db.query(DatabaseHelper.usersTable,
                    null,
                    null,
                    null,
                    null, null, null);
            if(userCursor.moveToFirst()){
                do{
                    userNameData.put(i, userCursor.getString(userCursor.getColumnIndexOrThrow(DatabaseHelper.userName)));
                    userPasswordData.put(i, userCursor.getString(userCursor.getColumnIndexOrThrow(DatabaseHelper.password)));
                    i++;

                }while (userCursor.moveToNext());
            }
            userCursor.close();

            Cursor deletedUserCursor = db.query(DatabaseHelper.userDeletedTable,
                    null,
                    null,
                    null,
                    null, null, null);
            i = 0;
            if(deletedUserCursor.moveToFirst()){
                do{
                    deletedUserData.put(i, deletedUserCursor.getString(deletedUserCursor.getColumnIndexOrThrow(DatabaseHelper.userName)));
                    Log.d(TAG, "User to be Deleted: " + deletedUserCursor.getString(deletedUserCursor.getColumnIndexOrThrow(DatabaseHelper.userName)));
                    i++;

                }while (deletedUserCursor.moveToNext());
            }
            Toast.makeText(mContext, "Read Completed from DB", Toast.LENGTH_SHORT).show();
            deletedUserCursor.close();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }

    }

    public void onClickUserSyncButton(){
        for(int i = 0; i < userNameData.size(); i++){
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
                    hashMap.put(DatabaseHelper.userName,(userNameData.get(index) == null)? "" : userNameData.get(index));
                    hashMap.put(DatabaseHelper.password,(userPasswordData.get(index) == null)? "" : userPasswordData.get(index));
                    return hashMap;
                }
            };
            requestQueue.add(request);

        }

        for(int j = 0; j < deletedUserData.size(); j++){
            final int delIndex = j;
            request = new StringRequest(Request.Method.POST, deleteUserURL, new Response.Listener<String>() {

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
                    hashMap.put("username",(deletedUserData.get(delIndex) == null)? "" : deletedUserData.get(delIndex));
                    //Log.d(TAG, "User sent to be Deleted: " + deletedUserData.get(delIndex) + " Index: " + String.valueOf(delIndex));
                    return hashMap;
                }
            };
            requestQueue.add(request);

        }
    }

    public void onClickUserPullButton(){
        dbHelper = DatabaseHelper.getDbInstance(mContext);
        db = dbHelper.getReadableDatabase();
        db.execSQL("Delete from " + DatabaseHelper.usersTable);
        db.execSQL("Delete from " + DatabaseHelper.userDeletedTable);
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
                                if (tag.equals(DatabaseHelper.usersTable)) {
                                    user = new Users(mContext);
                                    usersList.add(user);
                                }
                                break;

                            case XmlPullParser.TEXT:
                                value = parser.getText();
                                break;

                            case XmlPullParser.END_TAG:

                                switch (tag) {

                                    case DatabaseHelper.userName:
                                        user.setUserName(value);
                                        break;

                                    case DatabaseHelper.password:
                                        user.setPassword(value);
                                        break;
                                }
                                break;
                        }
                        event = parser.next();
                    }
                    for(Users currentUser : usersList){
                        currentUser.insertUserInDB();
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
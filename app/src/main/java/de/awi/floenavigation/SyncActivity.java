package de.awi.floenavigation;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SyncActivity extends Activity {

    private static final String TAG = "SyncActivity";

    private static final String URL = "http://192.168.137.1:80/userControl.php";
    private static final String pullURL = "http://192.168.137.1:80/pushUsers.php";
    private static final String deleteUserURL = "http://192.168.137.1:80/deleteUser.php";

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private String username;
    private String password;
    private RequestQueue requestQueue;
    private StringRequest request;
    private HashMap<Integer, String> userNameData = new HashMap<>();
    private HashMap<Integer, String> userPasswordData = new HashMap<>();
    private HashMap<Integer, String> deletedUserData = new HashMap<>();
    private Cursor userCursor;
    private XmlPullParserFactory factory;
    private XmlPullParser parser;
    private Users user;
    private ArrayList<Users> usersList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestQueue = Volley.newRequestQueue(this);
        try {
            factory = XmlPullParserFactory.newInstance();
            parser = factory.newPullParser();
        } catch (XmlPullParserException e){
            Log.d(TAG, "Error create XML Parser");
            e.printStackTrace();
        }
        setContentView(R.layout.activity_sync);
    }

    public void onClickReadButton(View view) {
            try{
                int i = 0;
                dbHelper = DatabaseHelper.getDbInstance(this);
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
                Toast.makeText(this, "Read Completed from DB", Toast.LENGTH_SHORT).show();
                deletedUserCursor.close();
            } catch (SQLiteException e){
                Log.d(TAG, "Database Error");
                e.printStackTrace();
            }

    }

    public void onClickSyncButton(View view) {

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
                            Toast.makeText(getApplicationContext(),"SUCCESS "+jsonObject.getString("success"),Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(getApplicationContext(), "Error" +jsonObject.getString("error"), Toast.LENGTH_SHORT).show();
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
                    hashMap.put("username",userNameData.get(index));
                    hashMap.put("password",userPasswordData.get(index));
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
                            Toast.makeText(getApplicationContext(),"SUCCESS "+jsonObject.getString("success"),Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(getApplicationContext(), "Error" +jsonObject.getString("error"), Toast.LENGTH_SHORT).show();
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
                    hashMap.put("username",deletedUserData.get(delIndex));
                    Log.d(TAG, "User sent to be Deleted: " + deletedUserData.get(delIndex) + " Index: " + String.valueOf(delIndex));
                    return hashMap;
                }
            };
            requestQueue.add(request);

        }
    }


    public void onClickPullButton(View view) {
        dbHelper = DatabaseHelper.getDbInstance(this);
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
                                if (tag.equals("user")) {
                                    user = new Users(getApplicationContext());
                                    usersList.add(user);
                                }
                                break;

                            case XmlPullParser.TEXT:
                                value = parser.getText();
                                break;

                            case XmlPullParser.END_TAG:

                                switch (tag) {

                                    case "username":
                                        user.setUserName(value);
                                        break;

                                    case "password":
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
                    Toast.makeText(getApplicationContext(), "Data Pulled from Server", Toast.LENGTH_SHORT).show();
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

package de.awi.floenavigation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.CursorAdapter;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "FloeNavigation";
    private static final int DB_VERSION = 1;
    private static final String TAG = "DatabaseHelpler";

    //Database Tables Names
    private static final String fixedStationTable = "AIS_FIXED_STATION_POSITION";
    private static final String stationListTable = "AIS_STATION_LIST";
    private static final String mobileStationTable = "AIS_MOBILE_STATION_POSITION";
    private static final String usersTable = "USERS";
    private static final String sampleMeasurementTable = "SAMPLE_MEASUREMENT";
    private static final String deviceListTable = "DEVICE_LIST";
    private static final String waypointsTable = "WAYPOINTS";

    //Database Fields Names
    private static final  String stationName = "AIS_STATION_NAME";
    private static final String latitude = "LATITUDE";
    private static final String longitude = "LONGITUDE";
    private static final String xPosition = "X_POSITION";
    private static final String yPosition = "Y_POSITION";
    private static final String sog = "SPEED_OVER_GROUND";
    private static final String cog = "COURSE_OVER_GROUND";
    private static final String mmsi = "MMSI";
    private static final String deviceType = "DEVICE_TYPE";
    private static final String updateTime = "UPDTAE_TIME";
    private static final String isPredicted = "IS_POSITION_PREDICTED";
    private static final String userName = "USERNAME";
    private static final String password = "PASSWORD";
    private static final String deviceName = "DEVICE_NAME";
    private static final String label = "LABEL";




    DatabaseHelper(Context context){
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        //Create AIS Station List Table
        db.execSQL("CREATE TABLE "  + stationListTable + "(" + mmsi + " INTEGER PRIMARY KEY," +
                stationName + " TEXT NOT NULL );");

        //Create AIS Fixed Station Position Table
        db.execSQL("CREATE TABLE " + fixedStationTable + " (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                stationName + " TEXT NOT NULL," +
                latitude + " REAL," +
                longitude + " REAL," +
                xPosition + " REAL," +
                yPosition + " REAL," +
                deviceType + " TEXT," +
                updateTime + " TEXT," +
                sog + " REAL," +
                cog + " REAL," +
                isPredicted + " NUMERIC," +
                mmsi + " INTEGER NOT NULL," +
                "FOREIGN KEY (" + mmsi + ") REFERENCES " + stationListTable + "(" + mmsi + "));");

        //Create Users Table
        db.execSQL("CREATE TABLE " + usersTable + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                userName + " TEXT NOT NULL, " +
                password + " TEXT);");

        insertUser(db, "awi", "awi");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){

    }

    public boolean createTables(){
        SQLiteDatabase db = this.getWritableDatabase();
        try{
            //Create Mobile Station Table
            db.execSQL("CREATE TABLE " + mobileStationTable + " (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    stationName + " TEXT NOT NULL," +
                    latitude + " REAL," +
                    longitude + " REAL," +
                    xPosition + " REAL," +
                    yPosition + " REAL," +
                    deviceType + " TEXT," +
                    updateTime + " TEXT," +
                    mmsi + " INTEGER NOT NULL," +
                    "FOREIGN KEY (" + mmsi + ") REFERENCES " + stationListTable + "(" + mmsi + "));");

            //Create Sample/Measurement Table
            db.execSQL("CREATE TABLE " + sampleMeasurementTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    deviceName + " TEXT," +
                    label + " TEXT);");

            //Create DeviceList Table
            db.execSQL("CREATE TABLE " + deviceListTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    deviceName + " TEXT," +
                    deviceType + " TEXT," +
                    xPosition + " REAL," +
                    yPosition + "REAL);");

            //Create Waypoints Table
            db.execSQL("CREATE TABLE " + waypointsTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    label + " TEXT);");

            return  true;
        } catch(SQLiteException e){
            Log.d(TAG, "Database Unavailable");
            return  false;
        }

    }

    private void insertUser(SQLiteDatabase db, String name, String pass){
        ContentValues defaultUser = new ContentValues();
        defaultUser.put("USERNAME", name);
        defaultUser.put("PASSWORD", pass);
        db.insert("USERS", null, defaultUser);
    }

    public String searchPassword(String uname){
        SQLiteDatabase db = this.getReadableDatabase();
        //String query = "SELECT USERNAME, PASSWORD FROM USERS";
        Cursor cursor = db.query("USERS",
                new String[]{"USERNAME", "PASSWORD"},
                null, null, null, null, null);
        String user, pwd;
        pwd = "Not Found";

        if (cursor.moveToFirst()){


            do {

                user = cursor.getString(0);
                if (user.equals(uname)){
                    pwd = cursor.getString(1);

                    break;
                }
            } while(cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return pwd;
    }

}

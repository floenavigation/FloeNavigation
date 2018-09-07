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

    public static final int firstStationIndex = 0;
    public static final int secondStationIndex = 1;
    public static final int INITIALIZATION_SIZE = 2;

    //Database Tables Names
    public static final String fixedStationTable = "AIS_FIXED_STATION_POSITION";
    public static final String stationListTable = "AIS_STATION_LIST";
    public static final String mobileStationTable = "AIS_MOBILE_STATION_POSITION";
    public static final String usersTable = "USERS";
    public static final String sampleMeasurementTable = "SAMPLE_MEASUREMENT";
    public static final String deviceListTable = "DEVICE_LIST";
    public static final String waypointsTable = "WAYPOINTS";
    public static final String baseStationTable = "BASE_STATIONS";
    public static final String betaTable = "BETA";


    //Database Fields Names
    public static final  String stationName = "AIS_STATION_NAME";
    public static final String latitude = "LATITUDE";
    public static final String longitude = "LONGITUDE";
    public static final String xPosition = "X_POSITION";
    public static final String yPosition = "Y_POSITION";
    public static final String sog = "SPEED_OVER_GROUND";
    public static final String cog = "COURSE_OVER_GROUND";
    public static final String alpha = "ALPHA";
    public static final String beta = "BETA";
    public static final String packetType = "LAST_RECEIVED_PACKET_TYPE";
    public static final String mmsi = "MMSI";
    public static final String deviceType = "DEVICE_TYPE";
    public static final String updateTime = "UPDTAE_TIME";
    public static final String isPredicted = "IS_POSITION_PREDICTED";
    public static final String isLocationReceived = "IS_LOCATION_RECEIVED";
    public static final String userName = "USERNAME";
    public static final String password = "PASSWORD";
    public static final String deviceName = "DEVICE_NAME";
    public static final String label = "LABEL";

    //Initial Position of Setup Points in Custom Coordinate System
    public static final long station1InitialX = 0;
    public static final long station1InitialY = 0;
    public static final long station2InitialX = 500;
    public static final long station2InitialY = 0;




    DatabaseHelper(Context context){
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        //Create AIS Station List Table
        db.execSQL("CREATE TABLE "  + stationListTable + "(" + mmsi + " INTEGER PRIMARY KEY," +
                stationName + " TEXT NOT NULL );");

        //Create AIS Fixed Station Position Table
        db.execSQL("CREATE TABLE " + fixedStationTable + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                stationName + " TEXT, " +
                latitude + " REAL, " +
                longitude + " REAL, " +
                alpha + " REAL, " +
                xPosition + " REAL, " +
                yPosition + " REAL, " +
                deviceType + " TEXT, " +
                updateTime + " TEXT, " +
                sog + " REAL, " +
                cog + " REAL, " +
                packetType + " INTEGER, " +
                isPredicted + " NUMERIC, " +
                isLocationReceived + " NUMERIC, " +
                mmsi + " INTEGER NOT NULL, " +
                "FOREIGN KEY (" + mmsi + ") REFERENCES " + stationListTable + "(" + mmsi + "));");

        //Create Base Stations Table
        db.execSQL("CREATE TABLE " + baseStationTable + "(" + mmsi + " INTEGER PRIMARY KEY, " +
                stationName + " TEXT NOT NULL);");

        //Create Beta Table
        db.execSQL("CREATE TABLE " + betaTable + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                beta + " REAL NOT NULL, " +
                updateTime + " TEXT);");

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
            db.execSQL("CREATE TABLE " + mobileStationTable + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    stationName + " TEXT, " +
                    latitude + " REAL, " +
                    longitude + " REAL, " +
                    alpha + " REAL, " +
                    xPosition + " REAL, " +
                    yPosition + " REAL, " +
                    deviceType + " TEXT, " +
                    updateTime + " TEXT, " +
                    mmsi + " INTEGER NOT NULL);");

            //Create Sample/Measurement Table
            db.execSQL("CREATE TABLE " + sampleMeasurementTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    deviceName + " TEXT, " +
                    label + " TEXT);");

            //Create DeviceList Table
            db.execSQL("CREATE TABLE " + deviceListTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    deviceName + " TEXT, " +
                    deviceType + " TEXT, " +
                    xPosition + " REAL, " +
                    yPosition + "REAL);");

            //Create Waypoints Table
            db.execSQL("CREATE TABLE " + waypointsTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    label + " TEXT); ");

            return  true;
        } catch(SQLiteException e){
            Log.d(TAG, "Database Unavailable");
            return  false;
        }

    }

    private void insertUser(SQLiteDatabase db, String name, String pass){
        ContentValues defaultUser = new ContentValues();
        defaultUser.put(userName, name);
        defaultUser.put(password, pass);
        db.insert(usersTable, null, defaultUser);
    }

    public String searchPassword(String uname){
        SQLiteDatabase db = this.getReadableDatabase();
        //String query = "SELECT USERNAME, PASSWORD FROM USERS";
        Cursor cursor = db.query(usersTable,
                new String[]{userName, password},
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

    public  double[] readBaseCoordinatePointsLatLon(){
        double[] coordinates = new double[4];
        try {
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.query(fixedStationTable,
                    new String[]{latitude, longitude},
                    "(X_Position = ? AND Y_POSITION = ?) OR (X_POSITION = ? AND Y_POSITION = ? ",
                    new String[]{Long.toString(station1InitialX), Long.toString(station1InitialY), Long.toString(station2InitialX), Long.toString(station2InitialY)},
                    null, null, null);
            if (cursor.moveToFirst()) {
                int i = 0;
                do {

                    coordinates[i] = cursor.getDouble(cursor.getColumnIndex(latitude));
                    coordinates[i + 1] = cursor.getDouble(cursor.getColumnIndex(longitude));
                    i += 2;
                } while (cursor.moveToNext());
            }
            cursor.close();
            db.close();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Unavailable");
            e.printStackTrace();
        }
        return coordinates;
    }

}

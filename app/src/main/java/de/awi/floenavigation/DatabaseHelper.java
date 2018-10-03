package de.awi.floenavigation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "FloeNavigation";
    private static final int DB_VERSION = 1;
    private static final String TAG = "DatabaseHelper";

    private static DatabaseHelper dbInstance;

    public static final int firstStationIndex = 0;
    public static final int secondStationIndex = 1;
    public static final int INITIALIZATION_SIZE = 2;
    public static final int NUM_OF_BASE_STATIONS = 2;
    public static final int NUM_OF_DEVICES = 1234;
    public static final int IS_LOCATION_RECEIVED_INITIAL_VALUE = 0;
    public static final int IS_LOCATION_RECEIVED = 1;
    public static final double ORIGIN_DISTANCE = 0.0;
    public static final String error_threshold = "ERROR_THRESHOLD";
    public static final String prediction_accuracy_threshold = "PREDICTION_ACCURACY_THRESHOLD";


    //Database Tables Names
    public static final String fixedStationTable = "AIS_FIXED_STATION_POSITION";
    public static final String stationListTable = "AIS_STATION_LIST";
    public static final String mobileStationTable = "AIS_MOBILE_STATION_POSITION";
    public static final String usersTable = "USERS";
    public static final String sampleMeasurementTable = "SAMPLE_MEASUREMENT";
    public static final String deviceListTable = "DEVICE_LIST";
    public static final String waypointsTable = "WAYPOINTS";
    public static final String configParametersTable = "CONFIGURATION_PARAMETERS";
    public static final String baseStationTable = "BASE_STATIONS";
    public static final String betaTable = "BETA";
    public static final String staticStationListTable = "STATION_LIST";


    //Database Fields Names
    public static final String stationName = "AIS_STATION_NAME";
    public static final String latitude = "LATITUDE";
    public static final String longitude = "LONGITUDE";
    public static final String recvdLatitude = "RECEIVED_LATITUDE";
    public static final String recvdLongitude = "RECEIVED_LONGITUDE";
    public static final String xPosition = "X_POSITION";
    public static final String yPosition = "Y_POSITION";
    public static final String sog = "SPEED_OVER_GROUND";
    public static final String cog = "COURSE_OVER_GROUND";
    public static final String alpha = "ALPHA";
    public static final String beta = "BETA";
    public static final String packetType = "LAST_RECEIVED_PACKET_TYPE";
    public static final String predictionAccuracy = "PREDICTION_ACCURACY";
    public static final String mmsi = "MMSI";
    public static final String staticStationName = "STATION_NAME";
    public static final String stationType = "STATION_TYPE";
    public static final String distance = "DISTANCE";
    public static final String deviceType = "DEVICE_TYPE";
    public static final String updateTime = "UPDATE_TIME";
    public static final String isPredicted = "IS_POSITION_PREDICTED";
    public static final String isLocationReceived = "IS_LOCATION_RECEIVED";
    public static final String userName = "USERNAME";
    public static final String password = "PASSWORD";
    public static final String deviceID = "DEVICE_ID";
    public static final String deviceName = "DEVICE_NAME";
    public static final String deviceShortName = "DEVICE_SHORT_NAME";
    public static final String label = "LABEL";
    public static final String parameterName = "PARAMETER_NAME";
    public static final String parameterValue = "PARAMETER_VALUE";

    //Initial Position of Setup Points in Custom Coordinate System
    public static final long station1InitialX = 0;
    public static final long station1InitialY = 0;
    public static final long station2InitialX = 500;
    public static final long station2InitialY = 0;
    public static final double station1Alpha = 0.0;
    public static final double station2Alpha = 0.0;

    public static List<String> deviceNames;
    public static List<String> deviceShortNames;
    public static List<String> deviceIDs;
    public static List<String> deviceTypes;

    public static final String[] stationTypes = {
            "Tent",
            "Hut",
            "Mast",
            "Fixpoint",
            "Polarstern",
            "Pistenbully",
            "Hovercraft",
            "Scooter",
            "Pulka",
            "Buoy"
    };


    public DatabaseHelper(Context context){
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
                recvdLatitude + " REAL, " +
                recvdLongitude + " REAL, " +
                alpha + " REAL, " +
                distance + " REAL, " +
                xPosition + " REAL, " +
                yPosition + " REAL, " +
                stationType + " TEXT, " +
                updateTime + " TEXT, " +
                sog + " REAL, " +
                cog + " REAL, " +
                packetType + " INTEGER, " +
                isPredicted + " NUMERIC, " +
                predictionAccuracy + " NUMERIC, " +
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



    public static boolean createTables(SQLiteDatabase db){
        //SQLiteDatabase db = this.getWritableDatabase();
        try{
            //Create Mobile Station Table
            db.execSQL("CREATE TABLE " + mobileStationTable + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    stationName + " TEXT, " +
                    latitude + " REAL, " +
                    longitude + " REAL, " +
                    alpha + " REAL, " +
                    distance + " REAL, " +
                    xPosition + " REAL, " +
                    yPosition + " REAL, " +
                    updateTime + " TEXT, " +
                    mmsi + " INTEGER NOT NULL);");

            //Create Sample/Measurement Table
            db.execSQL("CREATE TABLE " + sampleMeasurementTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    deviceID + " TEXT," +
                    deviceName + " TEXT, " +
                    deviceShortName + " TEXT, " +
                    deviceType + " TEXT, " +
                    latitude + " REAL, " +
                    longitude + " REAL, " +
                    xPosition + " REAL, " +
                    yPosition + " REAL, " +
                    updateTime + " TEXT, " +
                    label + " TEXT);");

            //Create DeviceList Table
            db.execSQL("CREATE TABLE " + deviceListTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    deviceID + " TEXT," +
                    deviceName + " TEXT, " +
                    deviceShortName + " TEXT, " +
                    deviceType + " TEXT);");

            //Create StationList Table
            db.execSQL("CREATE TABLE " + staticStationListTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    staticStationName + " TEXT, " +
                    stationType + " TEXT, " +
                    xPosition + " REAL, " +
                    yPosition + " REAL, " +
                    alpha + " REAL, " +
                    distance + " REAL);");

            //Create Waypoints Table
            db.execSQL("CREATE TABLE " + waypointsTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    latitude + " REAL, " +
                    longitude + " REAL, " +
                    xPosition + " REAL, " +
                    yPosition + " REAL, " +
                    updateTime + " TEXT, " +
                    label + " TEXT); ");

            //Create Configuration Parameters Table
            db.execSQL("CREATE TABLE " + configParametersTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    parameterName + " TEXT, " +
                    parameterValue + " INTEGER); ");

            //Only for debugging purpose
            insertDeviceList(db);

            //Default config params
            insertDefaultConfigParams(db, error_threshold, 10);
            insertDefaultConfigParams(db, prediction_accuracy_threshold, 3);

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

    private static void insertDefaultConfigParams(SQLiteDatabase db, String name, int value){
        ContentValues defaultConfigParam = new ContentValues();
        defaultConfigParam.put(parameterName, name);
        defaultConfigParam.put(parameterValue, value);
        db.insert(configParametersTable, null, defaultConfigParam);
    }

    /******************Only for debugging purpose**************************/
    private static void insertDeviceList(SQLiteDatabase db){

        String[] deviceShortNames = {"2mBT", "3DCAM", "8-CTL", "AC-9", "AGSS"};
        String[] deviceLongNames = {"2 m Blake Trawl", "3D camera", "8-Channel Temperature Lance",
                "Absorption and beam attenuation", "Accoustic Geodetic Seafloor Station",};

        for(int index = 0; index < 5; index++) {
            ContentValues mContentValues = new ContentValues();
            mContentValues.put(deviceID, index);
            mContentValues.put(deviceName, deviceLongNames[index]);
            mContentValues.put(deviceShortName, deviceShortNames[index]);
            mContentValues.put(deviceType, stationTypes[index]);
            db.insert(deviceListTable, null, mContentValues);
        }
    }
    /********************************************/

    public String searchPassword(String uname, Context context){
        SQLiteOpenHelper dbHelper = getDbInstance(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
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
        //db.close();

        return pwd;
    }

    public  double[] readBaseCoordinatePointsLatLon(Context context){
        double[] coordinates = new double[4];
        try {
            SQLiteOpenHelper dbHelper = getDbInstance(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor cursor = db.query(fixedStationTable,
                    new String[]{latitude, longitude},
                    "(X_Position = ? AND Y_POSITION = ?) OR (X_POSITION = ? AND Y_POSITION = ?)",
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
            //db.close();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Unavailable");
            e.printStackTrace();
        }
        return coordinates;
    }

    public static synchronized DatabaseHelper getDbInstance(Context context){
        if (dbInstance == null){
            dbInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return dbInstance;
    }


    public static void loadDeviceList(Context mContext){


        deviceTypes = new ArrayList<String>();
        deviceIDs = new ArrayList<String>();
        deviceNames = new ArrayList<String>();
        deviceShortNames = new ArrayList<String>();

        try {
            SQLiteOpenHelper dbHelper = getDbInstance(mContext);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor mDeviceListCursor = db.query(deviceListTable, null,
                    null, null, null, null, null);
            if (mDeviceListCursor.moveToFirst()) {
                do {
                    deviceIDs.add(mDeviceListCursor.getString(mDeviceListCursor.getColumnIndex(deviceID)));
                    deviceNames.add(mDeviceListCursor.getString(mDeviceListCursor.getColumnIndex(deviceName)));
                    deviceShortNames.add(mDeviceListCursor.getString(mDeviceListCursor.getColumnIndex(deviceShortName)));
                    deviceTypes.add(mDeviceListCursor.getString(mDeviceListCursor.getColumnIndex(deviceType)));
                } while (mDeviceListCursor.moveToNext());
            }
            mDeviceListCursor.close();
        }catch (SQLException e){
            Log.d(TAG, "Database Unavailable");
            e.printStackTrace();
        }
    }

    public static ArrayAdapter<String> advancedSearchTextView(Context mContext){

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_dropdown_item_1line, deviceShortNames);
        return adapter;
    }

    public static ArrayList<String> getDeviceAttributes(String devShortName){

        int arrayIndex;
        ArrayList<String> selectedDeviceAttributes = new ArrayList<String>();
        for (arrayIndex = 0; arrayIndex < deviceShortNames.size(); arrayIndex++){
            if (deviceShortNames.get(arrayIndex).equals(devShortName)){
                selectedDeviceAttributes.add(deviceIDs.get(arrayIndex));
                selectedDeviceAttributes.add(deviceNames.get(arrayIndex));
                selectedDeviceAttributes.add(deviceTypes.get(arrayIndex));
                return selectedDeviceAttributes;
            }
        }
        Log.d(TAG, "Device attributes not found");
        return null;
    }







}

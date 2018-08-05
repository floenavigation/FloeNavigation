package de.awi.floenavigation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.CursorAdapter;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "FloeNavigation";
    private static final int DB_VERSION = 1;

    DatabaseHelper(Context context){
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        //Create AIS Station List Table
        db.execSQL("CREATE TABLE AIS_STATION_LIST (MMSI INTEGER PRIMARY KEY," +
                "AIS_STATION_NAME TEXT NOT NULL );");

        //Create AIS Fixed Station Position Table
        db.execSQL("CREATE TABLE AIS_FIXED_STATION_POSITION (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "AIS_STATION_NAME TEXT NOT NULL," +
                "LATITUDE REAL," +
                "LONGITUDE REAL," +
                "X_POSITION REAL," +
                "Y_POSITION REAL," +
                "DEVICE_TYPE TEXT," +
                "UPDATE_TIME TEXT," +
                "IS_POSITION_PREDICTED NUMERIC," +
                "MMSI INTEGER NOT NULL," +
                "FOREIGN KEY (MMSI) REFERENCES AIS_STATION_LIST (MMSI));");

        //Create Users Table
        db.execSQL("CREATE TABLE USERS (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "USERNAME TEXT NOT NULL, " +
                "PASSWORD TEXT);");

        insertUser(db, "awi", "awi");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){

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
        db.close();

        return pwd;
    }

}

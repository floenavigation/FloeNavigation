package de.awi.floenavigation.synchronization;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.awi.floenavigation.helperClasses.DatabaseHelper;

public class StaticStation {

    private static final String TAG = "STATIC_STATION";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private Context appContext;

    private String stationName;
    private String stationType;
    private double xPosition;
    private double yPosition;
    private double alpha;
    private double distance;


    private ContentValues staticStnContent;

    public StaticStation(Context context){
        appContext = context;
        try {
            dbHelper = DatabaseHelper.getDbInstance(appContext);
            db = dbHelper.getReadableDatabase();
        } catch(SQLException e){
            Log.d(TAG, "Database Exception");
            e.printStackTrace();
        }
    }

    private void generateContentValues() {
        staticStnContent = new ContentValues();
        staticStnContent.put(DatabaseHelper.staticStationName, this.stationName);
        staticStnContent.put(DatabaseHelper.alpha, this.alpha);
        staticStnContent.put(DatabaseHelper.distance, this.distance);
        staticStnContent.put(DatabaseHelper.xPosition, this.xPosition);
        staticStnContent.put(DatabaseHelper.yPosition, this.yPosition);
        staticStnContent.put(DatabaseHelper.stationType, this.stationType);
    }

    public void insertStaticStationInDB(){
        generateContentValues();
        int result = db.update(DatabaseHelper.staticStationListTable, staticStnContent, DatabaseHelper.staticStationName + " = ?", new String[] {this.stationName});
        if(result == 0){
            db.insert(DatabaseHelper.staticStationListTable, null, staticStnContent);
            Log.d(TAG, "Static Station Added");
        } else{
            Log.d(TAG, "Static Station Updated");
        }
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getxPosition() {
        return xPosition;
    }

    public void setxPosition(double xPosition) {
        this.xPosition = xPosition;
    }

    public double getyPosition() {
        return yPosition;
    }

    public void setyPosition(double yPosition) {
        this.yPosition = yPosition;
    }

    public String getStationType() {
        return stationType;
    }

    public void setStationType(String stationType) {
        this.stationType = stationType;
    }

}

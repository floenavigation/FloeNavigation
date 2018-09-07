package de.awi.floenavigation;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import org.w3c.dom.Text;
import org.xml.sax.DTDHandler;

import static de.awi.floenavigation.AIVDM.strbuildtodec;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class AISDecodingService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String TAG = "AISDecodingService";
    private Handler handler;
    private String packet = null;
    //private Callbacks sCallbacks;
    private AIVDM aivdmObj;
    private PostnReportClassA posObjA;
    private PostnReportClassB posObjB;
    private StaticVoyageData voyageDataObj;
    private StaticDataReport dataReportObj;

    private static final int POSITION_REPORT_CLASSA_TYPE_1 = 1;
    private static final int POSITION_REPORT_CLASSA_TYPE_2 = 2;
    private static final int POSITION_REPORT_CLASSA_TYPE_3 = 3;
    private static final int STATIC_VOYAGE_DATA_CLASSB = 5;
    private static final int POSITION_REPORT_CLASSB = 18;
    private static final int STATIC_DATA_CLASSA = 24;

    //Data to be decoded
    private long recvdMMSI;
    private double recvdLat;
    private double recvdLon;
    private float recvdSpeed;
    private float recvdCourse;
    private String recvdTimeStamp;
    private String recvdStationName;
    private BroadcastReceiver wifiReceiver;

    public AISDecodingService() {
        super("AISDecodingService");
        aivdmObj = new AIVDM();
        posObjA = new PostnReportClassA(); //1,2,3
        posObjB = new PostnReportClassB(); //18
        voyageDataObj = new StaticVoyageData(); //5
        dataReportObj = new StaticDataReport(); //24
    }



    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Service OnStart Command");

        /*synchronized (this){
            try{
                wait(1000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }*/
        try
        {
            packet = intent.getExtras().getString("AISPacket");
            SQLiteOpenHelper databaseHelper = new DatabaseHelper(this);
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            Cursor cursor_stnlist = db.query(DatabaseHelper.stationListTable,
                    new String[] {DatabaseHelper.mmsi, DatabaseHelper.stationName},
                    null, null, null, null, null);

            Cursor cursor_fixedstnlist = db.query(DatabaseHelper.fixedStationTable,
                    null, null, null, null, null, null);

            //Mobile Station Disabled for now. Uncomment later
           /* Cursor cursor_mobilestnlist = db.query(DatabaseHelper.mobileStationTable,
                    null,
                    null,
                    null,
                    null, null, null);*/

            int num = 0;

            if(packet != null) {
                Log.d(TAG, packet);
                String[] dataExtr = packet.split(",");
                aivdmObj.setData(dataExtr);
                StringBuilder binary = aivdmObj.decodePayload();
                num = (int) strbuildtodec(0, 5, 6, binary, int.class);
                msgDecoding(num, binary);
                Log.d(TAG, String.valueOf(recvdMMSI));
            }

            //if(recvdMMSI == 21100)


            if(cursor_stnlist.moveToFirst())
            {
                do{
                    int mmsi = cursor_stnlist.getInt(cursor_stnlist.getColumnIndex(DatabaseHelper.mmsi));
                    String aisStnName = cursor_stnlist.getString(cursor_stnlist.getColumnIndex(DatabaseHelper.stationName));

                    //Decoding logic

                    if(recvdMMSI == mmsi){

                        //Writing to the database table AISFIXEDSTATIONPOSITION
                        //More fields to be included
                        if (cursor_fixedstnlist.moveToFirst()) {
                            do {
                                //if((cursor_fixedstnlist.getInt(cursor_fixedstnlist.getColumnIndex(DatabaseHelper.mmsi))) == mmsi) {
                                ContentValues decodedValues = new ContentValues();
                                decodedValues.put(DatabaseHelper.stationName, recvdStationName);
                                decodedValues.put(DatabaseHelper.mmsi, recvdMMSI);
                                decodedValues.put(DatabaseHelper.isLocationReceived, 1);
                                if ((num != STATIC_VOYAGE_DATA_CLASSB) && (num != STATIC_DATA_CLASSA)) {
                                    decodedValues.put(DatabaseHelper.latitude, recvdLat);
                                    decodedValues.put(DatabaseHelper.longitude, recvdLon);
                                    decodedValues.put(DatabaseHelper.sog, recvdSpeed);
                                    decodedValues.put(DatabaseHelper.cog, recvdCourse);
                                    decodedValues.put(DatabaseHelper.updateTime, recvdTimeStamp);
                                    decodedValues.put(DatabaseHelper.isPredicted, 0);
                                }
                                int a = db.update(DatabaseHelper.fixedStationTable, decodedValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(recvdMMSI)});

                                break;
                                //}
                            } while (cursor_fixedstnlist.moveToNext());
                        }

                    } //Uncomment later
                    /*else{
                        if(cursor_mobilestnlist.moveToFirst()) {
                            do {

                                ContentValues decodedValues = new ContentValues();
                                decodedValues.put(DatabaseHelper.stationName, recvdStationName);
                                decodedValues.put(DatabaseHelper.mmsi, recvdMMSI);
                                if ((num != 5) && (num != 24)) {
                                    decodedValues.put(DatabaseHelper.latitude, recvdLat);
                                    decodedValues.put(DatabaseHelper.longitude, recvdLon);
                                    decodedValues.put(DatabaseHelper.sog, recvdSpeed);
                                    decodedValues.put(DatabaseHelper.cog, recvdCourse);
                                    decodedValues.put(DatabaseHelper.updateTime, recvdTimeStamp);
                                    decodedValues.put(DatabaseHelper.isPredicted, 0);
                                }
                                db.update(DatabaseHelper.mobileStationTable, decodedValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(recvdMMSI)});
                                break;

                            } while (cursor_mobilestnlist.moveToNext());
                        }

                           */

                }while(cursor_stnlist.moveToNext());

            }

            cursor_stnlist.close();
            cursor_fixedstnlist.close();
            //Uncomment later
            // cursor_mobilestnlist.close();
            db.close();
        }catch (SQLException e)
        {
            String text = "Database unavailable";
            Log.d(TAG, text);
            //showText(text);
        }
    }

    private void msgDecoding(int num, StringBuilder binary){


        switch(num)
        {
            case POSITION_REPORT_CLASSA_TYPE_1 :
            case POSITION_REPORT_CLASSA_TYPE_2 :
            case POSITION_REPORT_CLASSA_TYPE_3 :
                posObjA.setData(binary);
                recvdMMSI = posObjA.getMMSI();
                recvdLat = posObjA.getLatitude();
                recvdLon = posObjA.getLongitude();
                recvdSpeed = posObjA.getSpeed();
                recvdCourse = posObjA.getCourse();
                recvdTimeStamp = String.valueOf(posObjA.getSeconds());
                break;
            case STATIC_VOYAGE_DATA_CLASSB:
                voyageDataObj.setData(binary);
                recvdMMSI = voyageDataObj.getMMSI();
                recvdStationName = voyageDataObj.getVesselName();
                break;
            case POSITION_REPORT_CLASSB:
                posObjB.setData(binary);
                recvdMMSI = posObjB.getMMSI();
                recvdLat = posObjB.getLatitude();
                recvdLon = posObjB.getLongitude();
                recvdSpeed = posObjB.getSpeed();
                recvdCourse = posObjB.getCourse();
                recvdTimeStamp = String.valueOf(posObjB.getSeconds());
                break;
            case STATIC_DATA_CLASSA:
                dataReportObj.setData(binary);
                recvdMMSI = dataReportObj.getMMSI();
                recvdStationName = dataReportObj.getVesselName();
                break;
            default:
                recvdMMSI = 0;
                recvdLat = 0;
                recvdLon = 0;
                break;

        }


    }

    private void showText(final String text) {

        handler.post(new Runnable(){
            public void run()
            {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }
        });

    }
}

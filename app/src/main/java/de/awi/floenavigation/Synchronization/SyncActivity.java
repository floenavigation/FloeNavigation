package de.awi.floenavigation.Synchronization;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import de.awi.floenavigation.R;

public class SyncActivity extends Activity {

    private static final String TAG = "SyncActivity";



    private RequestQueue requestQueue;
    private XmlPullParserFactory factory;
    private XmlPullParser parser;

    private UsersSync usersSync;
    private FixedStationSync fixedStationSync;
    private StationListSync stationListSync;
    private WaypointsSync waypointsSync;

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

        usersSync = new UsersSync(this, requestQueue, parser);
        fixedStationSync = new FixedStationSync(this, requestQueue, parser);
        stationListSync = new StationListSync(this, requestQueue, parser);
        waypointsSync = new WaypointsSync(this, requestQueue, parser);
    }

    public void onClickUserReadButton(View view) {
        usersSync.onClickUserReadButton();
    }

    public void onClickUserSyncButton(View view) {
        usersSync.onClickUserSyncButton();
    }

    public void onClickUserPullButton(View view) {
        usersSync.onClickUserPullButton();
    }


    public void onClickFixedStationReadButton(View view) {
        fixedStationSync.onClickFixedStationReadButton();
    }

    public void onClickFixedStationSyncButton(View view) {
        fixedStationSync.onClickFixedStationSyncButton();
    }

    public void onClickfixedStationSyncPullButton(View view) {
        fixedStationSync.onClickFixedStationPullButton();
    }

    public void onClickStationListReadButton(View view) {
        stationListSync.onClickStationListReadButton();
    }

    public void onClickStationListSyncButton(View view) {
        stationListSync.onClickStationListSyncButton();
    }

    public void onClickStationListSyncPullButton(View view) {
        stationListSync.onClickStationListPullButton();
    }

    public void onClickWaypointsReadButton(View view) {
        waypointsSync.onClickWaypointsReadButton();
    }

    public void onClickWaypointsSyncButton(View view) {
        waypointsSync.onClickWaypointsSyncButton();
    }

    public void onClickWaypointsSyncPullButton(View view) {
        waypointsSync.onClickWaypointsPullButton();
    }
}

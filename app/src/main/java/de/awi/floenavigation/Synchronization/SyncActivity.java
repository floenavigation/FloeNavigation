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
    private BaseStationSync baseStationSync;
    private ConfigurationParameterSync parameterSync;
    private BetaSync betaSync;
    private SampleMeasurementSync sampleSync;
    private StaticStationSync staticStationSync;

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
        baseStationSync = new BaseStationSync(this, requestQueue, parser);
        parameterSync = new ConfigurationParameterSync(this, requestQueue, parser);
        betaSync = new BetaSync(this, requestQueue, parser);
        sampleSync = new SampleMeasurementSync(this, requestQueue, parser);
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

    public void onClickBaseStationReadButton(View view) {
        baseStationSync.onClickBaseStationReadButton();
    }

    public void onClickBaseStationSyncButton(View view) {
        baseStationSync.onClickBaseStationSyncButton();
    }

    public void onClickBaseStationPullButton(View view) {
        baseStationSync.onClickBaseStationPullButton();
    }

    public void onClickBetaReadButton(View view) {
        betaSync.onClickBetaReadButton();
    }

    public void onClickBetaSyncButton(View view) {
        betaSync.onClickBetaSyncButton();
    }


    public void onClickBetaPullButton(View view) {
        betaSync.onClickBetaPullButton();
    }


    public void onClickFixedStationReadButton(View view) {
        fixedStationSync.onClickFixedStationReadButton();
    }

    public void onClickFixedStationSyncButton(View view) {
        fixedStationSync.onClickFixedStationSyncButton();
    }

    public void onClickfixedStationPullButton(View view) {
        fixedStationSync.onClickFixedStationPullButton();
    }


    public void onClickStationListReadButton(View view) {
        stationListSync.onClickStationListReadButton();
    }

    public void onClickStationListSyncButton(View view) {
        stationListSync.onClickStationListSyncButton();
    }

    public void onClickStationListPullButton(View view) {
        stationListSync.onClickStationListPullButton();
    }

    public void onClickWaypointsReadButton(View view) {
        waypointsSync.onClickWaypointsReadButton();
    }

    public void onClickWaypointsSyncButton(View view) {
        waypointsSync.onClickWaypointsSyncButton();
    }

    public void onClickWaypointsPullButton(View view) {
        waypointsSync.onClickWaypointsPullButton();
    }


    public void onClickParameterReadButton(View view) {
        parameterSync.onClickParameterReadButton();
    }

    public void onClickParameterSyncButton(View view) {
        parameterSync.onClickParameterSyncButton();
    }

    public void onClickParameterPullButton(View view) {
        parameterSync.onClickParameterPullButton();
    }

    public void onClickSampleMeasurementReadButton(View view) {
        sampleSync.onClickSampleReadButton();
    }

    public void onClickSampleMeasurementSyncButton(View view) {
        sampleSync.onClickSampleSyncButton();
    }

    public void onClickDeviceListPullButton(View view) {
        sampleSync.onClickDeviceListPullButton();
    }


    public void onClickStaticStationReadButton(View view) {
        staticStationSync.onClickStaticStationReadButton();
    }

    public void onClickStaticStationSyncButton(View view) {
        staticStationSync.onClickStaticStationSyncButton();
    }

    public void onClickStaticStationPullButton(View view) {
        staticStationSync.onClickStaticStationPullButton();
    }




}

package de.awi.floenavigation.Synchronization;

import android.app.Activity;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.net.InetAddress;

import de.awi.floenavigation.AdminPageActivity;
import de.awi.floenavigation.AlphaCalculationService;
import de.awi.floenavigation.AngleCalculationService;
import de.awi.floenavigation.DatabaseHelper;
import de.awi.floenavigation.PredictionService;
import de.awi.floenavigation.R;
import de.awi.floenavigation.ValidationService;
import de.awi.floenavigation.aismessages.AISDecodingService;
import de.awi.floenavigation.initialsetup.GridSetupActivity;
import de.awi.floenavigation.initialsetup.SetupActivity;
import de.awi.floenavigation.network.NetworkService;

public class SyncActivity extends Activity {

    private static final String TAG = "SyncActivity";
    private static final String toastMsg = "Please wait until Sync Finishes";



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
    private EditText hostName, portNumber;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private boolean backButtonEnabled = true;
    private boolean syncUsers = false;
    private boolean syncFixedStations = false;
    private boolean syncStaticStations = false;
    private boolean syncSamples = false;
    private boolean syncWaypoints = false;
    private boolean syncParameters = false;

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
        staticStationSync = new StaticStationSync(this, requestQueue, parser);
    }

    public void onClickStartSync(View view) {
        String msg = "";
        TextView waitingMsg = findViewById(R.id.syncWaitingMsg);
        if(validateServerDetails()){
            String hostname = hostName.getText().toString();
            String port = portNumber.getText().toString();
            if(checkCheckBoxes()){
                backButtonEnabled = false;
                hideNavigationBar();
                findViewById(R.id.syncParametersSet).setVisibility(View.GONE);
                findViewById(R.id.syncWaitingView).setVisibility(View.VISIBLE);
                msg = "Contacting Server....";
                waitingMsg.setText(msg);
                if(pingRequest(hostname)) {
                    msg = "Stopping Services and Clearing Metadata";
                    waitingMsg.setText(msg);
                    stopServices();
                    clearMobileStationTable();
                    if(syncFixedStations){
                        msg = "Reading Fixed Stations from Database and Pushing it to the Server";
                        waitingMsg.setText(msg);
                        fixedStationSync.setBaseUrl(hostname, port);
                        fixedStationSync.onClickFixedStationReadButton();
                        fixedStationSync.onClickFixedStationSyncButton();
                        msg = "Clearing Database and Pulling Fixed Stations from the Server";
                        waitingMsg.setText(msg);
                        fixedStationSync.onClickFixedStationPullButton();

                        msg = "Reading Base Stations from Database and Pushing it to the Server";
                        waitingMsg.setText(msg);
                        baseStationSync.setBaseUrl(hostname, port);
                        baseStationSync.onClickBaseStationReadButton();
                        baseStationSync.onClickBaseStationSyncButton();
                        msg = "Clearing Database and Pulling Base Stations from the Server";
                        waitingMsg.setText(msg);
                        baseStationSync.onClickBaseStationPullButton();

                        msg = "Reading AIS Station List from Database and Pushing it to the Server";
                        waitingMsg.setText(msg);
                        stationListSync.setBaseUrl(hostname, port);
                        stationListSync.onClickStationListReadButton();
                        stationListSync.onClickStationListSyncButton();
                        msg = "Clearing Database and Pulling AIS Station List from the Server";
                        waitingMsg.setText(msg);
                        stationListSync.onClickStationListPullButton();

                        msg = "Reading Beta Table from Database and Pushing it to the Server";
                        waitingMsg.setText(msg);
                        betaSync.setBaseUrl(hostname, port);
                        betaSync.onClickBetaReadButton();
                        betaSync.onClickBetaSyncButton();
                        msg = "Clearing Database and Pulling Beta from the Server";
                        waitingMsg.setText(msg);
                        betaSync.onClickBetaPullButton();
                    }
                    if(syncUsers){
                        msg = "Reading Users from Database and Pushing it to the Server";
                        waitingMsg.setText(msg);
                        usersSync.setBaseUrl(hostname, port);
                        usersSync.onClickUserReadButton();
                        usersSync.onClickUserSyncButton();
                        msg = "Clearing Database and Pulling Users from the Server";
                        waitingMsg.setText(msg);
                        usersSync.onClickUserPullButton();
                    }
                    if(syncSamples){
                        msg = "Reading Samples from Database and Pushing it to the Server";
                        waitingMsg.setText(msg);
                        sampleSync.setBaseUrl(hostname, port);
                        sampleSync.onClickSampleReadButton();
                        sampleSync.onClickSampleSyncButton();
                        msg = "Clearing Database and Pulling Device List from the Server";
                        waitingMsg.setText(msg);
                        sampleSync.onClickDeviceListPullButton();
                    }
                    if(syncStaticStations){
                        msg = "Reading Static Stations from Database and Pushing it to the Server";
                        waitingMsg.setText(msg);
                        staticStationSync.setBaseUrl(hostname, port);
                        staticStationSync.onClickStaticStationReadButton();
                        staticStationSync.onClickStaticStationSyncButton();
                        msg = "Clearing Database and Pulling Static Stations from the Server";
                        waitingMsg.setText(msg);
                        staticStationSync.onClickStaticStationPullButton();
                    }
                    if(syncWaypoints){
                        msg = "Reading Waypoints from Database and Pushing it to the Server";
                        waitingMsg.setText(msg);
                        waypointsSync.setBaseUrl(hostname, port);
                        waypointsSync.onClickWaypointsReadButton();
                        waypointsSync.onClickWaypointsSyncButton();
                        msg = "Clearing Database and Pulling Waypoints from the Server";
                        waitingMsg.setText(msg);
                        waypointsSync.onClickWaypointsPullButton();
                    }
                    if(syncParameters){
                        msg = "Reading Configuration Parameters from Database and Pushing it to the Server";
                        waitingMsg.setText(msg);
                        parameterSync.setBaseUrl(hostname, port);
                        parameterSync.onClickParameterReadButton();
                        parameterSync.onClickParameterSyncButton();
                        msg = "Clearing Database and Pulling Configuration Parameters from the Server";
                        waitingMsg.setText(msg);
                        parameterSync.onClickParameterPullButton();
                    }
                    findViewById(R.id.syncProgressBar).setVisibility(View.GONE);
                    msg = "Sync Completed";
                    waitingMsg.setText(msg);
                    Button confirmBtn = findViewById(R.id.syncFinishBtn);
                    confirmBtn.setVisibility(View.VISIBLE);
                } else{
                    findViewById(R.id.syncParametersSet).setVisibility(View.VISIBLE);
                    findViewById(R.id.syncWaitingView).setVisibility(View.GONE);
                    Toast.makeText(this, "Could not contact the Server. Please check the Connection", Toast.LENGTH_SHORT).show();
                }

            } else{
                Toast.makeText(this, "Please select atleast one Table to Sync", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopServices(){
        Intent angleCalcServiceIntent = new Intent(this, AngleCalculationService.class);
        Intent alphaCalcServiceIntent = new Intent (this, AlphaCalculationService.class);
        Intent predictionServiceIntent = new Intent(this, PredictionService.class);
        Intent validationServiceIntent = new Intent(this, ValidationService.class);
        Intent decodoingServiceIntent = new Intent(this, AISDecodingService.class);
        Intent networkServiceIntent = new Intent(this, NetworkService.class);
        stopService(angleCalcServiceIntent);
        stopService(alphaCalcServiceIntent);
        stopService(predictionServiceIntent);
        stopService(validationServiceIntent);
        stopService(decodoingServiceIntent);
        stopService(networkServiceIntent);
    }

    private void clearMobileStationTable(){
        try {
            dbHelper = DatabaseHelper.getDbInstance(this);
            db = dbHelper.getReadableDatabase();
            db.execSQL("Delete from " + DatabaseHelper.mobileStationTable);
        } catch (SQLException e){
            Log.d(TAG, "Error Clearing Mobile Station Database");
            Toast.makeText(this, "Error Clearing Mobile Station Database", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private boolean validateServerDetails(){
        hostName = findViewById(R.id.serverHostname);
        portNumber = findViewById(R.id.serverPort);
        if (TextUtils.isEmpty(hostName.getText().toString())){
            Toast.makeText(this, "Invalid Host Name", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(portNumber.getText().toString())){
            Toast.makeText(this, "Invalid Port Number", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean checkCheckBoxes(){
        return syncUsers || syncFixedStations || syncStaticStations || syncSamples || syncWaypoints || syncParameters;
    }

    public void onClickSyncUser(View view) {
        syncUsers = !syncUsers;
    }

    public void onClickSyncFixedStations(View view) {
        syncFixedStations = !syncFixedStations;
    }

    public void onClickSyncStaticStations(View view) {
        syncStaticStations = !syncStaticStations;
    }

    public void onClickSyncSamples(View view) {
        syncSamples = !syncSamples;
    }

    public void onClickSyncWaypoints(View view) {
        syncWaypoints  = !syncWaypoints;
    }

    public void onClickSyncConfigParams(View view) {
        syncParameters = !syncParameters;
    }

    private boolean pingRequest(String host){

        boolean mExitValue = false;
        Log.d(TAG, "Hostname: " + host);

        try {
            mExitValue = InetAddress.getByName(host).isReachable(1000);
            Log.d(TAG, "Ping Result: " + mExitValue);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;

    }

    public void onClickFinishButton(View view) {
        Intent networkServiceIntent = new Intent(this, NetworkService.class);
        startService(networkServiceIntent);
        SetupActivity.runServices(this);
        Intent configActivity = new Intent(this, AdminPageActivity.class);
        startActivity(configActivity);
    }

    @Override
    public void onBackPressed(){

        if (backButtonEnabled){
            Intent mainIntent = new Intent(this, AdminPageActivity.class);
            startActivity(mainIntent);
        }else {
            Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
        }
    }

    private void hideNavigationBar() {

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }
}

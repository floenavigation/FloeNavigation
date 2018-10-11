package de.awi.floenavigation.deployment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import de.awi.floenavigation.ActionBarActivity;
import de.awi.floenavigation.FragmentChangeListener;
import de.awi.floenavigation.GPS_Service;
import de.awi.floenavigation.MainActivity;
import de.awi.floenavigation.R;


public class DeploymentActivity extends FragmentActivity implements FragmentChangeListener {

    private static final String TAG = "DeploymentActivity";
    //Action Bar Updates
    private BroadcastReceiver broadcastReceiver;
    private BroadcastReceiver aisPacketBroadcastReceiver;
    private boolean locationStatus = false;
    private boolean packetStatus = false;
    private final Handler statusHandler = new Handler();
    private MenuItem gpsIconItem, aisIconItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deployment);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        StationInstallFragment deviceFragment = new StationInstallFragment();
        this.replaceFragment(deviceFragment);

    }

    @Override
    protected void onStart() {
        super.onStart();
        actionBarUpdatesFunction();
    }

    private void actionBarUpdatesFunction() {

        /*****************ACTION BAR UPDATES*************************/
        if (broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    locationStatus = intent.getExtras().getBoolean(GPS_Service.locationStatus);
                }
            };
        }

        if (aisPacketBroadcastReceiver == null){
            aisPacketBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    packetStatus = intent.getExtras().getBoolean(GPS_Service.AISPacketStatus);
                }
            };
        }

        registerReceiver(aisPacketBroadcastReceiver, new IntentFilter(GPS_Service.AISPacketBroadcast));
        registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));

        Runnable gpsLocationRunnable = new Runnable() {
            @Override
            public void run() {
                if (locationStatus){
                    if (gpsIconItem != null)
                        gpsIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorGreen), PorterDuff.Mode.SRC_IN);
                }
                if (packetStatus){
                    if (aisIconItem != null)
                        aisIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorGreen), PorterDuff.Mode.SRC_IN);
                }

                statusHandler.postDelayed(this, ActionBarActivity.UPDATE_TIME);
            }
        };

        statusHandler.postDelayed(gpsLocationRunnable, ActionBarActivity.UPDATE_TIME);
        /******************************************/
    }

    @Override
    public void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frag_container, fragment, fragment.toString());
        fragmentTransaction.addToBackStack(fragment.toString());
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed(){
        Fragment frag = this.getSupportFragmentManager().findFragmentById(R.id.frag_container);
        if (frag instanceof StationInstallFragment){
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else{
            Toast.makeText(this, "Please finish Setup of the Station", Toast.LENGTH_LONG).show();
        }
    }

    /*****************ACTION BAR UPDATES*************************/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        int[] iconItems = {R.id.currentLocationAvail, R.id.aisPacketAvail};
        gpsIconItem = menu.findItem(iconItems[0]);
        gpsIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        aisIconItem = menu.findItem(iconItems[1]);
        aisIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        unregisterReceiver(aisPacketBroadcastReceiver);
        aisPacketBroadcastReceiver = null;
    }
    /******************************************/
}

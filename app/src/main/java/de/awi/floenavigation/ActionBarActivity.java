package de.awi.floenavigation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class ActionBarActivity extends Activity {

    private static final String TAG = "ActionBarActivity";
    private BroadcastReceiver broadcastReceiver;
    private BroadcastReceiver aisPacketBroadcastReceiver;
    private boolean locationStatus = false;
    private boolean packetStatus = false;
    private final Handler statusHandler = new Handler();
    private MenuItem gpsIconItem, aisIconItem;
    public static final int UPDATE_TIME = 50;
    public static final String colorGreen = "#00bfa5";
    public static final String colorRed = "#d32f2f";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Runnable gpsLocationRunnable = new Runnable() {
            @Override
            public void run() {
                if (locationStatus){
                    if (gpsIconItem != null)
                        gpsIconItem.getIcon().setColorFilter(Color.parseColor(colorGreen), PorterDuff.Mode.SRC_IN);
                }
                else {
                    if (gpsIconItem != null)
                        gpsIconItem.getIcon().setColorFilter(Color.parseColor(colorRed), PorterDuff.Mode.SRC_IN);
                }
                if (packetStatus){
                    if (aisIconItem != null)
                        aisIconItem.getIcon().setColorFilter(Color.parseColor(colorGreen), PorterDuff.Mode.SRC_IN);
                }else {
                    if (aisIconItem != null)
                        aisIconItem.getIcon().setColorFilter(Color.parseColor(colorRed), PorterDuff.Mode.SRC_IN);
                }

                statusHandler.postDelayed(this, UPDATE_TIME);
            }
        };

        statusHandler.postDelayed(gpsLocationRunnable, UPDATE_TIME);
    }

    public boolean onCreateOptionsMenu(Menu menu, boolean showLatlon) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        int[] iconItems = {R.id.currentLocationAvail, R.id.aisPacketAvail};
        gpsIconItem = menu.findItem(iconItems[0]);
        gpsIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        aisIconItem = menu.findItem(iconItems[1]);
        aisIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);

        if (showLatlon){
            MenuItem latLonFormat = menu.findItem(R.id.changeLatLonFormat);
            latLonFormat.setVisible(true);
        }

        return super.onCreateOptionsMenu(menu);
    }

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
    protected void onStart() {
        super.onStart();

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
                    Log.d(TAG, String.valueOf(packetStatus));
                }
            };
        }

        registerReceiver(aisPacketBroadcastReceiver, new IntentFilter(GPS_Service.AISPacketBroadcast));
        registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        unregisterReceiver(aisPacketBroadcastReceiver);
        aisPacketBroadcastReceiver = null;
    }


}

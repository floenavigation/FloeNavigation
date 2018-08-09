package de.awi.floenavigation;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Toast;

public class GPS_Service extends Service {

    private LocationManager locationManager;
    private LocationListener listener;

    public GPS_Service() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(getApplicationContext(), "startService", Toast.LENGTH_LONG).show();
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Intent intent = new Intent("location_updates");
                intent.putExtra("Latitude", location.getLatitude());
                intent.putExtra("Longitude", location.getLongitude());
                //Toast.makeText(getApplicationContext(),"Broadcast Sent", Toast.LENGTH_LONG).show();
                sendBroadcast(intent);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

            }
        };

        locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(locationManager != null)
        {
            locationManager.removeUpdates(listener);
            //locationManager = null;
        }
    }
}


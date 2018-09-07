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
import android.util.Log;
import android.widget.Toast;

public class GPS_Service extends Service {

    private LocationManager locationManager;
    private LocationListener listener;
    private double lat = 0.0;
    private double lon = 0.0;
    private static final String TAG = "GPS_SERVICE";
    public static final String GPSBroadcast = "GPSLocationUpdates";
    public static final String latitude = "LATITUDE";
    public static final String longitude = "LONGITUDE";
    private static final int updateInterval = 1000;
    LocationUpdates locationUpdates = new LocationUpdates();




    public GPS_Service() {
        //super(name);
        //Toast.makeText(getApplicationContext(), "startService", Toast.LENGTH_LONG).show();

    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(){
        Log.d(TAG, "GPS Service Started");
        listener =  new Listener();
        locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);

        new Thread(locationUpdates).start();

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

    private class Listener implements LocationListener{

        @Override
        public void onLocationChanged(Location location){

                locationUpdates.setLatitude(location.getLatitude());
                locationUpdates.setLongitude(location.getLongitude());

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
    }

    private class LocationUpdates implements Runnable {
        private double lat = 0.0;
        private double lon= 0.0;

        public void setLatitude(double lat) {
            this.lat = lat;
        }

        public void setLongitude(double lon) {
            this.lon = lon;
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(updateInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.d(TAG, "Thread Interrupted");
                }
                Intent broadcastIntent = new Intent(GPSBroadcast);
                broadcastIntent.putExtra(latitude, lat);
                broadcastIntent.putExtra(longitude, lon);
                //Toast.makeText(getApplicationContext(),"Broadcast Sent", Toast.LENGTH_LONG).show();
                sendBroadcast(broadcastIntent);
            }
        }
    }
}


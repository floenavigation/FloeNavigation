package de.awi.floenavigation.aismessages;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import de.awi.floenavigation.aismessages.AISDecodingService;

public class AISMessageReceiver implements Runnable {

    private static final String TAG = "AISMessageReceiver";

    private String dstAddress;
    private int dstPort;
    private TelnetClient client;
    private String packet;
    private BufferedReader bufferedReader;
    StringBuilder responseString;
    boolean isConnected = false;
   // private NetworkMonitor monitor;
    private Context context;
    private boolean mDisconnectFlag = false;
    private BroadcastReceiver reconnectReceiver;

    public AISMessageReceiver(String addr, int port, Context con){
        this.dstAddress = addr;
        this.dstPort = port;
        this.context = con;
        reconnectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getExtras()!= null) {
                    Log.d(TAG, "BraodCastReceived: " + String.valueOf( intent.getExtras().getBoolean("mDisconnectFlag")));
                    mDisconnectFlag = intent.getExtras().getBoolean("mDisconnectFlag");
                }
            }
        };

    }

    @Override
    public void run(){
        context.registerReceiver(reconnectReceiver, new IntentFilter("Reconnect"));
        responseString = new StringBuilder();
        try {

            client = new TelnetClient();

            client.connect(dstAddress, dstPort);



            bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            /*Intent serviceIntent = new Intent(context, AISDecodingService.class);
            context.startService(serviceIntent);*/

            do{

                isConnected =  client.isConnected();
                while(bufferedReader.read() != -1) {
                    //Log.d(TAG, "ConnectionStatus: " + String.valueOf(client.isConnected()));
                    //Log.d(TAG, "DisconnectFlag Value: " + String.valueOf(mDisconnectFlag));
                    responseString.append(bufferedReader.readLine());
                    if (responseString.toString().contains("AIVDM") || responseString.toString().contains("AIVDO")) {
                        packet = responseString.toString();
                        //responseString.setLength(0);
                        Intent serviceIntent = new Intent(context, AISDecodingService.class);
                        serviceIntent.putExtra("AISPacket", packet);
                        context.startService(serviceIntent);
                    }
                    responseString.setLength(0);
                    /*Intent intent = new Intent("RECEIVED_PACKET");
                    intent.putExtra("AISPACKET", packet);
                    this.context.sendBroadcast(intent);*/
                    //Log.d(TAG, packet);

                    /*if(mDisconnectFlag){
                        //client.disconnect();
                        //Log.d(TAG, "DisconnectFlag: " + String.valueOf(client.isConnected()));
                        break;
                    }*/
                }
            } while (isConnected);


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public boolean isConnected(){
        return client.isConnected();
    }

}
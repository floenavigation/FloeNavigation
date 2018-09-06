package de.awi.floenavigation;


import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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

    public AISMessageReceiver(String addr, int port, Context con){
        this.dstAddress = addr;
        this.dstPort = port;
        this.context = con;
    }

    @Override
    public void run(){

        responseString = new StringBuilder();
        try {

            client = new TelnetClient();

            client.connect(dstAddress, dstPort);


            isConnected =  client.isConnected();
            bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            /*Intent serviceIntent = new Intent(context, AISDecodingService.class);
            context.startService(serviceIntent);*/

            do{
                while(bufferedReader.read() != -1) {
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

                    if(NetworkMonitor.mdisconnectFlag){
                        client.disconnect();
                        break;
                    }
                }
            } while (isConnected);


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public boolean isConnected(){
        return isConnected;
    }

}

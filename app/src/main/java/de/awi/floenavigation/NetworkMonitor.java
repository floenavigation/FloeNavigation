package de.awi.floenavigation;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NetworkMonitor implements Runnable {
    //final static String IPADDRESS = "192.168.0.102";
    boolean success = false;
    boolean prevSuccess = false;
    Context appContext;
    AISMessageReceiver aisMessage;
    Thread aisMessageThread;
    private static final String TAG = "NetworkMonitor";
    //public static volatile boolean mdisconnectFlag = false;

    public NetworkMonitor(Context con){
        this.appContext = con;
        aisMessage = new AISMessageReceiver(GridSetupActivity.dstAddress,GridSetupActivity.dstPort, con);
        aisMessageThread = new Thread(aisMessage);
    }

    public void run(){

        while(true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            success = pingRequest("/system/bin/ping -c 1 " + GridSetupActivity.dstAddress);

            /*if(success != prevSuccess){
                if(!aisMessageThread.isAlive()){

                    aisMessageThread.start();
                    prevSuccess = true;
                    mdisconnectFlag = false;
                }
            }
            else if(!success){
                mdisconnectFlag = true; //to disconnect the client
            }*/
            Intent intent = new Intent();
            intent.setAction("Reconnect");
            Log.d(TAG, "Success Value: " + String.valueOf(success));
            if(success){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(!aisMessageThread.isAlive()){

                    aisMessageThread.start();
                    //prevSuccess = true;

                    intent.putExtra("mDisconnectFlag", false);
                    appContext.sendBroadcast(intent);
                }
            }
            else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(aisMessageThread.isAlive()) {
                    //mdisconnectFlag = true; //to disconnect the client
                    intent.putExtra("mDisconnectFlag", true);
                    appContext.sendBroadcast(intent);
                    //aisMessageThread.interrupt();
                }
            }


        }
    }

    private boolean pingRequest(String Instr){

        Runtime runtime = Runtime.getRuntime();
        int counter = 0;
        int maxCount = 5;
        Process  mIpAddrProcess;
        int mExitValue;
        try
        {
            do{
                mIpAddrProcess = runtime.exec(Instr);
                mExitValue = mIpAddrProcess.waitFor();
                ++counter;
            }while(counter < maxCount);

            BufferedReader stdInput;
            stdInput = new BufferedReader(new InputStreamReader(mIpAddrProcess.getInputStream()));

            String s;
            String res = "";
            while ((s = stdInput.readLine()) != null) {
                res += s + "\n";
            }
            Log.d("pingmsg", res);

            mIpAddrProcess.destroy();

            if(mExitValue==0){
                return true;
            }else{
                return false;
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();

        }
        catch (IOException e)
        {
            e.printStackTrace();

        }
        return false;
    }

    public boolean isSuccess() {
        return success;
    }
}

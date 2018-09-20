package de.awi.floenavigation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import de.awi.floenavigation.deployment.DeploymentActivity;
import de.awi.floenavigation.network.NetworkService;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Start Network Monitor Service
        Intent networkServiceIntent = new Intent(this, NetworkService.class);
        startService(networkServiceIntent);



        setContentView(R.layout.activity_main);




    }

    public void onClickListener(View view){

    }

    public void onClickDeploymentBtn(View view){
        Intent deploymentIntent = new Intent(this, DeploymentActivity.class);
        startActivity(deploymentIntent);
    }

    public void onClickAdminBtn(View view){
        Intent intent = new Intent(this, LoginPage.class);
        startActivity(intent);
    }
}

package de.awi.floenavigation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import de.awi.floenavigation.deployment.DeploymentActivity;
import de.awi.floenavigation.network.NetworkService;
import de.awi.floenavigation.sample_measurement.SampleMeasurementActivity;

public class MainActivity extends Activity {

    private static boolean networkSetup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Start Network Monitor Service

        if (!networkSetup) {
            Intent networkServiceIntent = new Intent(this, NetworkService.class);
            startService(networkServiceIntent);
            networkSetup = true;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("NetworkState", networkSetup);
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

    public void onClickSampleMeasureBtn(View view) {
        Intent sampleMeasureIntent = new Intent(this, SampleMeasurementActivity.class);
        startActivity(sampleMeasureIntent);
    }

    public void onClickGridButton(View view) {
        Intent gridActivityIntent = new Intent(this, GridActivity.class);
        startActivity(gridActivityIntent);
    }
}

package de.awi.floenavigation.initialsetup;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import de.awi.floenavigation.AdminPageActivity;
import de.awi.floenavigation.FragmentChangeListener;
import de.awi.floenavigation.GPS_Service;
import de.awi.floenavigation.R;


public class GridSetupActivity extends FragmentActivity implements FragmentChangeListener {

    private boolean configSetupStep;
    private static final String TAG = "GridSetupActivity";

    public static final String dstAddress = "192.168.0.1";
    public static final int dstPort = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid_setup);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        if (savedInstanceState != null){
            configSetupStep = savedInstanceState.getBoolean("SetupStep");
        }
        checkPermission();
        if (!configSetupStep) {
            configSetupStep = true;
            MMSIFragment mmsiFragment = new MMSIFragment();
            this.replaceFragment(mmsiFragment);
        } //else if (configSetupStep == 1){
           // CoordinateFragment coordinateFragment = new CoordinateFragment();
           // this.replaceFragment(coordinateFragment);
        //}

    }

    private void checkPermission(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                ActivityCompat.requestPermissions(this,
                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.INTERNET},
                        CoordinateFragment.GPS_REQUEST_CODE);
            }
            return;
        }
        Intent intent = new Intent(this, GPS_Service.class);
        startService(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        switch (requestCode){
            case CoordinateFragment.GPS_REQUEST_CODE:
                checkPermission();
                break;
            default:
                break;
        }
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
    public void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("SetupStep", configSetupStep);
    }

    @Override
    public void onBackPressed(){
        Fragment frag = this.getSupportFragmentManager().findFragmentById(R.id.frag_container);
        if (frag instanceof MMSIFragment){
            Intent intent = new Intent(this, AdminPageActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else{
            Toast.makeText(this, "Please finish Setup", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        stopService(new Intent(this, GPS_Service.class));
    }

}

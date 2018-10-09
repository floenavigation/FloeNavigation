package de.awi.floenavigation.deployment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
import android.view.WindowManager;
import android.widget.Toast;

import de.awi.floenavigation.FragmentChangeListener;
import de.awi.floenavigation.MainActivity;
import de.awi.floenavigation.R;

public class DeploymentActivity extends FragmentActivity implements FragmentChangeListener {

    private static final String TAG = "DeploymentActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deployment);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        StationInstallFragment deviceFragment = new StationInstallFragment();
        this.replaceFragment(deviceFragment);
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
}

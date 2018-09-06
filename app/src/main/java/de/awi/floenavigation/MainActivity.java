package de.awi.floenavigation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent serviceIntent = new Intent(this, NetworkService.class);
        startService(serviceIntent);
        setContentView(R.layout.activity_main);


    }

    public void onClickListener(View view){

    }

    public void onClickAdminBtn(View view){
        Intent intent = new Intent(this, LoginPage.class);
        startActivity(intent);
    }
}

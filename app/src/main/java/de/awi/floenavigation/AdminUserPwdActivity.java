package de.awi.floenavigation;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class AdminUserPwdActivity extends Activity {

    private static final String TAG = "AdminUserPwdActivity";
    private EditText newusernameView, newpwdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_pwd);

        newusernameView = findViewById(R.id.newusername_id);
        newpwdView = findViewById(R.id.newpassword_id);
    }

    public void onClickNewUserConfirm(View view) {
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            String newUserName = newusernameView.getText().toString();
            String newPassword = newpwdView.getText().toString();

            DatabaseHelper.insertUser(db, newUserName, newPassword);
            Toast.makeText(getApplicationContext(), "New User Credentials Saved", Toast.LENGTH_LONG).show();

        } catch(SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }
    }


}

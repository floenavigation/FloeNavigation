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

import java.util.regex.Pattern;

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

            if (validateNewUserCredentials(newUserName, newPassword)){
                DatabaseHelper.insertUser(db, newUserName, newPassword);
                Toast.makeText(getApplicationContext(), "New User Credentials Saved", Toast.LENGTH_LONG).show();
            }



        } catch(SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }
    }

    private boolean validateNewUserCredentials(String newUserName, String newPassword) {
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        String receivedPassword = databaseHelper.searchPassword(newUserName, getApplicationContext());

        if (!receivedPassword.equals("Not Found")){
            Toast.makeText(getApplicationContext(), "User Name already present", Toast.LENGTH_LONG).show();
            return false;
        }

        Pattern pattern = Pattern.compile("[A-Za-z0-9_]+");
        boolean validUserName = (newUserName != null) && pattern.matcher(newUserName).matches();
        boolean validPassword = (newPassword != null) && pattern.matcher(newPassword).matches();
        if (!validUserName){
            Toast.makeText(getApplicationContext(), "Invalid user name", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!validPassword){
            Toast.makeText(getApplicationContext(), "Invalid Password", Toast.LENGTH_LONG).show();
            return false;
        }


        return true;
    }


}

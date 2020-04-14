package com.example.scan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class LoggedInActivity extends AppCompatActivity {

    private boolean doublePressToExit = false;
    private Toast toast = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logged_in);
        Log.d("Log", " UserName : " + FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);


        (findViewById(R.id.signOut)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            }
        });

        (findViewById(R.id.scanCode)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ASyncT startHotspot = new ASyncT();
                startHotspot.execute();
                System.out.println("I am here yooo!");
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) !=
                        PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(com.example.scan.LoggedInActivity.this, new String[] {Manifest.permission.CAMERA},
                            50); }
                else
                    startActivity(new Intent(getApplicationContext(), ScannerActivity.class));
            }
        });
    }


    @Override
    public void onBackPressed(){        // todo: Check working after adding multiple activites to the activity stack
        if (doublePressToExit){
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        doublePressToExit = true;
        toast.setText("Press again to exit.");
        toast.show();
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run(){
                doublePressToExit = false;
            }
        }, 2000);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        toast.cancel();
    }

}


class ASyncT extends AsyncTask<Void, Void, Void> {
    @Override
    protected Void doInBackground(Void ...params){
        // todo : Add logic to start hotspot with random username and password
        return null;
    }
}
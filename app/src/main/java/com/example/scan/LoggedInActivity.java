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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logged_in);
        Log.d("Log", " UserName : " + FirebaseAuth.getInstance().getCurrentUser().getDisplayName());

        (findViewById(R.id.signOut)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
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
                    ActivityCompat.requestPermissions(com.example.scan.LoggedInActivity.this,
                            new String[] {Manifest.permission.CAMERA},
                            50);
                }
                else
                    startActivity(new Intent(getApplicationContext(), ScannerActivity.class));
            }
        });
    }



    ///////////////////////////////////////////
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 50: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    startActivity(new Intent(getApplicationContext(), ScannerActivity.class));
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    System.out.println("Permission denied by the user!!");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
    //////////////////////////////////////////



    @Override
    public void onBackPressed(){        // todo: Check working after adding multiple activites to the activity stack
        if (doublePressToExit){
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        Toast.makeText(this, "Press again to exit.", Toast.LENGTH_SHORT).show();
        doublePressToExit = true;
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run(){
                doublePressToExit = false;
            }
        }, 2000);
    }

}


class ASyncT extends AsyncTask<Void, Void, Void> {
    @Override
    protected Void doInBackground(Void ...params){
        // todo : Add logic to start hotspot with random username and password
        return null;
    }
}
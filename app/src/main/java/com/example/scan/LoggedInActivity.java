package com.example.scan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

public class LoggedInActivity extends AppCompatActivity{

    private boolean doublePressToExit = false;
    private Toast toast = null;
    private String username = null;
    private String bikeIP = null;
    private int totalDistance = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logged_in);

        toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);


        (findViewById(R.id.signOut)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            }

        });


        (findViewById(R.id.scanButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {        // todo : Add all permissions at app startup
                if (bikeIP == null) {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) !=
                            PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(com.example.scan.LoggedInActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                50);
                    } else {
                        startActivity(new Intent(getApplicationContext(), ScannerActivity.class));
                    }
                } else{
                    Log.d("Bike", bikeIP);
                    Snackbar.make(findViewById(R.id.constraintLayout), "You already have rented a bike.", Snackbar.LENGTH_LONG).show();
                }
            }});
    }

    // Checking the result of Permission Request
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if ( requestCode == 50) {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // camera-related task you need to do.
                    startActivity(new Intent(getApplicationContext(), ScannerActivity.class));
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("Scanner","Permission denied by the user!!");
                }
        }
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
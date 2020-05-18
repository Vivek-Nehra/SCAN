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
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

public class LoggedInActivity extends AppCompatActivity {

    private boolean doublePressToExit = false;
    private Toast toast = null;
    private int QR_CODE_SCAN = 10;
    private String bikeIP = null;

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

        (findViewById(R.id.floatingActionButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bikeIP == null) {
                    ASyncT startHotspot = new ASyncT();
                    startHotspot.execute();
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) !=
                            PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(com.example.scan.LoggedInActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                50);
                    } else {
                        startActivityForResult(new Intent(getApplicationContext(), ScannerActivity.class), QR_CODE_SCAN);
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
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 50: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // camera-related task you need to do.
                    startActivityForResult(new Intent(getApplicationContext(), ScannerActivity.class), QR_CODE_SCAN);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("Scanner","Permission denied by the user!!");
                }
                return;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == QR_CODE_SCAN){
            if (resultCode == RESULT_OK){
                bikeIP = data.getStringExtra("IP");
                Log.d("Scanner", bikeIP);
                startActivity(new Intent(getApplicationContext(), BikeControllerActivity.class));
            } else {
                Log.d("Scanner", "QR code not obtained");
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


class ASyncT extends AsyncTask<Void, Void, Void> {
    @Override
    protected Void doInBackground(Void ...params){
        // todo : Add logic to start hotspot with random username and password
        return null;
    }
}
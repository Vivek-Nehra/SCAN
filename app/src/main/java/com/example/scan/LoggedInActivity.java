package com.example.scan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.scan.util.CreateHTTPPostRequest;
import com.example.scan.util.HotspotManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONException;
import org.json.JSONObject;

public class LoggedInActivity extends AppCompatActivity implements HotspotManager.OnHotspotEnabledListener {

    private boolean doublePressToExit = false;
    private Toast toast = null;
    private int QR_CODE_SCAN = 10;
    private String bikeIP = null;
    private String hotspotName = null;
    private String hotspotPassword = null;
    private HotspotManager hotspot;
    private PostRequest request;
    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logged_in);
        toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);

        hotspot = new HotspotManager((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE),this);
        pd = new ProgressDialog(this);
        pd.setCanceledOnTouchOutside(false);
        pd.setMessage("Please Wait");

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
            public void onClick(View view) {        // todo : Add all permissions at app startup
                if (bikeIP == null) {
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

    @Override
    public void OnHotspotEnabled(boolean enabled, @Nullable WifiConfiguration wifiConfiguration){
        if (enabled) {
            Log.d("Hotspot", "Hotspot Started");
            hotspotPassword = wifiConfiguration.preSharedKey;
            hotspotName = wifiConfiguration.SSID;

            Log.d("Hotspot", "Password: " + hotspotPassword);
            Log.d("Hotspot", "ID: " + hotspotName);

            if (bikeIP != null) {
                request = new PostRequest();
                request.execute(hotspotName, hotspotPassword, bikeIP);
            }
        }
        else{
            Log.d("Hotspot", "Hotspot turned off");
        }
    }
    private class PostRequest extends CreateHTTPPostRequest {
        @Override
        protected void onPreExecute(){
           if (isCancelled()){
               return;
           }
            pd.show();
        }
        @Override
        protected void onPostExecute(String result) {
            pd.dismiss();
            if (isCancelled()){
                return;
            }
            try {
                JSONObject resultJson = new JSONObject(result);
                result = resultJson.getString("message");
                Log.d("Request",result);
                toast = Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT);
                toast.show();
                if (result.contains("Error")){
                    Log.d("Request", "Error sending request");
                    bikeIP = null;
                    hotspot.turnOffHotspot();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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
                    startActivityForResult(new Intent(getApplicationContext(), ScannerActivity.class), QR_CODE_SCAN);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("Scanner","Permission denied by the user!!");
                }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == QR_CODE_SCAN){
            if (resultCode == RESULT_OK){
                bikeIP = data.getStringExtra("IP");
                Log.d("Scanner", bikeIP != null ? bikeIP : "Empty");
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(com.example.scan.LoggedInActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            50);
                } else {
                    hotspot.turnOnHotspot(LoggedInActivity.this);       // todo : Ensure no Memory leaks
                }
//                startActivity(new Intent(getApplicationContext(), BikeControllerActivity.class));
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
    public void onStop(){
        super.onStop();
        if (request != null){
            request.cancel(true);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        toast.cancel();
    }

}
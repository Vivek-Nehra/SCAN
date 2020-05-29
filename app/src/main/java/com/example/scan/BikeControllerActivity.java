package com.example.scan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.scan.util.CreateHTTPGetRequest;
import com.example.scan.util.CreateHTTPPostRequest;
import com.example.scan.util.HotspotManager;
import com.example.scan.util.Sockets;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import static com.example.scan.util.Constants.CONNECTION_ESTABLISHED;
import static com.example.scan.util.Constants.CONNECTION_FAILED;
import static com.example.scan.util.Constants.CONNECTION_UNKNOWN;
import static com.example.scan.util.Constants.connectionEstablished;
import static com.example.scan.util.Constants.totalTime;

// TODO: BikeControllerActivity will implement "OnMapReadyCallback" interface as well (Done). @ Aditya
public class BikeControllerActivity extends AppCompatActivity implements HotspotManager.OnHotspotEnabledListener, OnMapReadyCallback {
    private ConstraintLayout bikeDashboard, checkConnection;
    private String bikeIP;
    private HotspotManager hotspot;
    private String hotspotName = null;
    private String hotspotPassword = null;
    private ProgressDialog pd;
    private String androidIP = null;
    private PostRequest postRequest;
    private GetRequest getRequest;
    private Toast toast = null;
    private Handler updateUI;
    private Runnable runHandler, runTimer;
    private Sockets socketConnection = null;
    private boolean doublePressToExit = false;
    private TextView timer;
    private static boolean isActivityRunning;


    // TODO: Variables defined as follows. @ Aditya
    /////////////////////////////////////////////////////////////////////////////////////////////
    private static final String TAG = BikeControllerActivity.class.getSimpleName();
    private GoogleMap map;
    private CameraPosition cameraPosition;


    private FusedLocationProviderClient fusedLocationProviderClient;

    private final LatLng defaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    /////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_controller);

        // TODO: Written within onCreate method. @Aditya
        /////////////////////////////////////////////////////////////////////////////////////////
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map); // TODO: Fragment id is "map" (uncomment in bike_control_dashboard.xml file)!.
        mapFragment.getMapAsync(this);
        ////////////////////////////////////////////////////////////////////////////////////////

        checkConnection = findViewById(R.id.check_connection);
        bikeDashboard = findViewById(R.id.bike_control_dashboard);
        isActivityRunning = true;

        hotspot = new HotspotManager((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE), this);
        socketConnection = new Sockets();

        toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
        pd = new ProgressDialog(this);
        pd.setCanceledOnTouchOutside(false);
        pd.setMessage("Please Wait");

        timer = findViewById(R.id.time);
        updateUI = new Handler();
        runTimer = new Runnable() {
            @Override
            public void run() {
                int hours = totalTime / 3600;
                int minutes = (totalTime % 3600) / 60;
                int seconds = totalTime % 60;
                timer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            }
        };


        (findViewById(R.id.retryButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectionEstablished = CONNECTION_UNKNOWN;
                finish();
                overridePendingTransition(0, 0);
                startActivity(getIntent());
                overridePendingTransition(0, 0);
            }
        });

        (findViewById(R.id.lock)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SocketResponse().execute("192.168.43.21", "Lock");
            }
        });
        (findViewById(R.id.unlock)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SocketResponse().execute("192.168.43.21", "Unlock");
            }
        });
        (findViewById(R.id.release)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(BikeControllerActivity.this)
                        .setMessage("This will end your ride. \n Are you sure?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                pd.setMessage("Ending Trip");
                                pd.show();
                                new SocketResponse().execute(bikeIP, "Release");
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Perform Your Task Here--When No is pressed
                                dialog.cancel();
                            }
                        }).show();
            }
        });
    }


    // TODO: Remaining methods @Aditya
    ///////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = map;
        this.map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.map_contents,
                        (FrameLayout) findViewById(R.id.map), false); // TODO: uncomment fragment in bike_control_dashboard.xml

                TextView title = infoWindow.findViewById(R.id.title);
                title.setText(marker.getTitle());

                TextView snippet = infoWindow.findViewById(R.id.snippet);
                snippet.setText(marker.getSnippet());

                return infoWindow;
            }
        });

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }

    private void getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }


    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // LOOK AT onRequestPermissionMethod as well !! //////////////////////////////////////////////
    // END/////////////////////////









    @Override
    public void onStart(){
        super.onStart();
        bikeIP = getIntent().getStringExtra("IP");

        if (connectionEstablished != CONNECTION_ESTABLISHED) {
            bikeDashboard.setVisibility(View.GONE);
            checkConnection.setVisibility(View.GONE);

            Log.d("Hotspot", "Starting Hotspot");
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(com.example.scan.BikeControllerActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        25);
            } else {
                hotspot.turnOnHotspot(BikeControllerActivity.this);
            }
        } else {
            bikeDashboard.setVisibility(View.VISIBLE);
            checkConnection.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if ( requestCode == 25) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hotspot.turnOnHotspot(BikeControllerActivity.this);
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Log.d("Hotspot","Permission denied by the user!!");
            }
        }// TODO: This elseif statement and updateLocationUI() added !!
        else if(requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        }
        updateLocationUI();
    }

    public String getDeviceIP() {
        try {
            for (Enumeration<NetworkInterface> ni = NetworkInterface.getNetworkInterfaces(); ni.hasMoreElements();) {
                NetworkInterface interfaceVar = ni.nextElement();
                for (Enumeration<InetAddress> enumIP = interfaceVar.getInetAddresses(); enumIP.hasMoreElements();) {
                    InetAddress myaddress = enumIP.nextElement();
                    if (!myaddress.isLoopbackAddress() && myaddress instanceof Inet4Address) {
                        //String deviceIP = Formatter.formatIpAddress(myaddress.hashCode());
                        String deviceIP = myaddress.getHostAddress();
                        return deviceIP;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("TAG", ex.toString());
        }
        return null;
    }

    @Override
    public void OnHotspotEnabled(boolean enabled, @Nullable WifiConfiguration wifiConfiguration){
        if (enabled) {
            Log.d("Hotspot", "Hotspot Started");
            Log.d("Hotspot", "Config: " + wifiConfiguration);
            hotspotPassword = wifiConfiguration.preSharedKey;
            hotspotName = wifiConfiguration.SSID;

            Log.d("Hotspot", "Password: " + hotspotPassword);
            Log.d("Hotspot", "ID: " + hotspotName);

            androidIP = getDeviceIP();
            Log.d("Hotspot", "My IP: " + androidIP);

            if (bikeIP != null) {
                postRequest = new BikeControllerActivity.PostRequest();
                postRequest.execute(hotspotName, hotspotPassword, bikeIP, androidIP);
            }
        }
        else{
            Log.d("Hotspot", "Hotspot turned off");
        }
    }

    private void onPostResponse(){
        if (androidIP != null) {
            getRequest = new GetRequest();
            getRequest.execute(androidIP);
            socketConnection.startServerSocket();

            checkConnection.setVisibility(View.VISIBLE);
            final ImageView retry = findViewById(R.id.retryButton);
            retry.setVisibility(View.INVISIBLE);
            bikeDashboard.setVisibility(View.INVISIBLE);

            runHandler = new Runnable() {
                private int count = 0;
                private boolean isConnected = false;
                @Override
                public void run() {
                    TextView connectingText = findViewById(R.id.connectionTextBox);
                    Log.d("Bike", "Running Handler" + count);
                    if (connectionEstablished == CONNECTION_UNKNOWN) {
                        String text = "Connecting ";
                        for(int j=0;j<count%3 +1;j++)   text += ".";
                        connectingText.setText(text);
                        count++;
                        if (count == 30) {
                            cancelAsyncTasks();
                            connectingText.setText("Timeout occurred. Retry.");
                            Log.d("Bike", "Timeoout");
                            hotspot.turnOffHotspot();
                            retry.setVisibility(View.VISIBLE);
                            socketConnection.closeAllConnectionsAndThreads();
                        }
                        else {
                            updateUI.postDelayed(this,1000);
                        }
                    }
                    if (connectionEstablished == CONNECTION_FAILED) {
                        connectingText.setText("Connection Failed. Make Sure you are close to the bike or Try Again later.");
                        Log.d("Bike", "Not Connected.");
                        hotspot.turnOffHotspot();
                        retry.setVisibility(View.VISIBLE);
                        socketConnection.closeAllConnectionsAndThreads();
                    }
                    if (connectionEstablished == CONNECTION_ESTABLISHED) {
                        cancelAsyncTasks();
                        socketConnection.closeAllConnectionsAndThreads();
                        if (!isConnected){
                            connectingText.setText("Connected ^_^ ");
                            Log.d("Bike", "Connected");
                            updateUI.postDelayed(this,1000);
                            isConnected = true;
                        } else {
                            checkConnection.setVisibility(View.GONE);
                            bikeDashboard.setVisibility(View.VISIBLE);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    while (connectionEstablished == CONNECTION_ESTABLISHED)
                                    {
                                        if (isActivityRunning){
                                            runOnUiThread(runTimer);
                                        }
                                        try {
                                            Thread.sleep(1000);
                                            totalTime++;
                                        } catch (InterruptedException e) {
                                            System.out.println("Timer thread exception");
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }).start();
                        }
                    }
                }
            };
            updateUI.post(runHandler);
        }
    }

    private class SocketResponse extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String ...params){
            String ip = params[0];
            String msg = params[1];
            String response = socketConnection.sendClientMessage(ip,msg);
            Log.d("Socket Data Received", response);
            return response;
        }

        @Override
        protected void onPostExecute(String result){
            toast = Toast.makeText(getApplicationContext(),result,Toast.LENGTH_LONG);
            if (pd.isShowing()){
                pd.dismiss();
            }
            if (result.contains("Release")){
                connectionEstablished = CONNECTION_UNKNOWN;
                // Update time and other values to database

                totalTime = 0;
                startActivity(new Intent(getApplicationContext(),LoggedInActivity.class));
                finish();
            }
            toast.show();
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
                    hotspot.turnOffHotspot();
                    startActivity(new Intent(getApplicationContext(),LoggedInActivity.class));
                    finish();
                    return;
                }
                else {
                    onPostResponse();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetRequest extends CreateHTTPGetRequest {

        @Override
        protected void onPostExecute(String result) {
            if (isCancelled()){
                return;
            }
            try {
                JSONObject resultJson = new JSONObject(result);
                result = resultJson.getString("message");
                Log.d("Request",result);
                if (result.contains("Can't connect")){
                    Log.d("Request", "Not able to connect");
                    connectionEstablished = CONNECTION_FAILED;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void cancelAsyncTasks(){
        if (postRequest != null){
            postRequest.cancel(true);
        }
        if (getRequest != null){
            getRequest.cancel(true);
        }
    }

    @Override
    public void onBackPressed(){
        if (connectionEstablished != CONNECTION_ESTABLISHED) {
            if (doublePressToExit) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            doublePressToExit = true;
            toast.setText("Press again to exit.");
            toast.show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (connectionEstablished != CONNECTION_ESTABLISHED) {
                        doublePressToExit = false;
                    }
                }
            }, 2000);
        }
        else{
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        cancelAsyncTasks();
        if (socketConnection != null){
            socketConnection.closeAllConnectionsAndThreads();
        }
        if (updateUI != null && runHandler != null){
            updateUI.removeCallbacks(runHandler);
        }
        runTimer = null;
        isActivityRunning = false;
        toast.cancel();
    }
}


//    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//    FirebaseDatabase scanDB = FirebaseDatabase.getInstance();
//    DatabaseReference userRef = scanDB.getReference().child("Users").child(currentUser.getUid());
//    userRef.child("currentBike").setValue(bikeIP);
//    userRef.child("currentBike").setValue(null);
package com.example.scan;

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
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.scan.util.CreateHTTPGetRequest;
import com.example.scan.util.CreateHTTPPostRequest;
import com.example.scan.util.HotspotManager;
import com.example.scan.util.PreviousRides;
import com.example.scan.util.Sockets;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.scan.util.Constants.CONNECTION_ESTABLISHED;
import static com.example.scan.util.Constants.CONNECTION_FAILED;
import static com.example.scan.util.Constants.CONNECTION_UNKNOWN;
import static com.example.scan.util.Constants.connectionStatus;
import static com.example.scan.util.Constants.serverUnreachable;
import static com.example.scan.util.Constants.totalTime;


public class BikeControllerActivity extends AppCompatActivity implements HotspotManager.OnHotspotEnabledListener, OnMapReadyCallback {
    private ConstraintLayout bikeDashboard, checkConnection, syncConnection, tripDetails;
    private String bikeIP;
    private HotspotManager hotspot;
    private String hotspotName = null;
    private String hotspotPassword = null;
    private ProgressDialog pd;
    private String androidIP = null;
    private PostRequest postRequest;
    private GetRequest getRequest;
    private Toast toast = null;
    private Handler updateUI, updateTimer, syncConnectionStatus;
    private Runnable runHandler, syncStatus;
    private Sockets socketConnection = null;
    private boolean doublePressToExit = false;
    private static boolean isActivityRunning;

    private DatabaseReference userRef;
    private Thread timerThread;

    private static final String TAG = BikeControllerActivity.class.getSimpleName();
    private GoogleMap map;
    private CameraPosition cameraPosition;


    private FusedLocationProviderClient fusedLocationProviderClient;

    private final LatLng defaultLocation = new LatLng(28.644800, 77.216721); // Delhi
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;


    private Location lastKnownLocation;
    private Location currentLocation;
    private double distance = 0.0d;

    private String rideDate = null;
    private String rideTime = null;
    private String bikeName = "Bike1097";

    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_controller);

        checkConnection = findViewById(R.id.check_connection);
        bikeDashboard = findViewById(R.id.bike_control_dashboard);
        syncConnection = findViewById(R.id.sync_connection);
        tripDetails = findViewById(R.id.trip_details);

        hotspot = new HotspotManager((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE), this);
        socketConnection = new Sockets();

        toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
        pd = new ProgressDialog(this);
        pd.setCanceledOnTouchOutside(false);
        pd.setMessage("Please Wait");


        updateUI = new Handler();

        updateTimer = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                String string = bundle.getString("Time");
                final TextView timer = (findViewById(R.id.time));
                Log.d("Timer", "Setting Time: " + timer.getText());
                timer.setText(string);
                timer.invalidate();
            }
        };

        timerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (connectionStatus == CONNECTION_ESTABLISHED) {
                    if (Thread.interrupted()){
                        Log.d("Thread", "Stopping Thread :" + Thread.currentThread().getName());
                        break;
                    }
                    Log.d("Thread", Thread.currentThread().getName() + ": " + BikeControllerActivity.isActivityRunning);
                    if (BikeControllerActivity.isActivityRunning) {
                        int hours = totalTime / 3600;
                        int minutes = (totalTime % 3600) / 60;
                        int seconds = totalTime % 60;
                        Message msg = updateTimer.obtainMessage();
                        Bundle bundle = new Bundle();
                        Log.d("Timer", "Updating Timer:" + totalTime);
                        bundle.putString("Time", String.format("%02d:%02d:%02d", hours, minutes, seconds));
                        msg.setData(bundle);
                        updateTimer.sendMessage(msg);
                    }
                    try {
                        Thread.sleep(1000);
                        totalTime++;
                    } catch (InterruptedException e) {
                        System.out.println("Caught in Sleep");
                        e.printStackTrace();
                    }
                }
            }
        });

        syncConnectionStatus = new Handler();
        syncStatus = new Runnable() {
            @Override
            public void run() {
                new Sockets().sendMessage(bikeIP, "Checking Connection");
                if (serverUnreachable >= 2){
                    checkConnection.setVisibility(View.INVISIBLE);
                    bikeDashboard.setVisibility(View.INVISIBLE);
                    syncConnection.setVisibility(View.VISIBLE);
                    tripDetails.setVisibility(View.INVISIBLE);
                    Log.d("Connection", "Disconnected");
                }
                else{
                    checkConnection.setVisibility(View.INVISIBLE);
                    bikeDashboard.setVisibility(View.VISIBLE);
                    syncConnection.setVisibility(View.INVISIBLE);
                    tripDetails.setVisibility(View.INVISIBLE);
                }
//                syncConnectionStatus.postDelayed(this, 3000);
            }
        };

        (findViewById(R.id.retryButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectionStatus = CONNECTION_UNKNOWN;
                finish();
                overridePendingTransition(0, 0);
                startActivity(getIntent());
                overridePendingTransition(0, 0);
            }
        });

        (findViewById(R.id.lock)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SocketResponse().execute(bikeIP, "Lock");
            }
        });
        (findViewById(R.id.unlock)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SocketResponse().execute(bikeIP, "Unlock");
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

        (findViewById(R.id.closeBtn)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                connectionStatus = CONNECTION_UNKNOWN;
                // Update time and other values to database
                userRef.child("currentBike").setValue(null);

                totalTime = 0;
                hotspot.turnOffHotspot();
                startActivity(new Intent(getApplicationContext(), LoggedInActivity.class));
                finish();
            }
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase scanDB = FirebaseDatabase.getInstance();
        userRef = scanDB.getReference().child("Users").child(currentUser.getUid());

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        bikeIP = getIntent().getStringExtra("IP");

        if (connectionStatus != CONNECTION_ESTABLISHED) {
            bikeDashboard.setVisibility(View.GONE);
            checkConnection.setVisibility(View.GONE);
            syncConnection.setVisibility(View.GONE);
            tripDetails.setVisibility(View.GONE);

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
            syncConnection.setVisibility(View.GONE);
            tripDetails.setVisibility(View.GONE);
//            syncConnectionStatus.postDelayed(syncStatus,3000);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 25) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hotspot.turnOnHotspot(BikeControllerActivity.this);
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.

                Log.d("Hotspot", "Permission denied by the user!!");
            }
        }
        locationPermissionGranted = false;
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        }

    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;

//        this.map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
//
//            @Override
//            public View getInfoWindow(Marker arg0) {
//                return null;
//            }
//
//            @Override
//            public View getInfoContents(Marker marker) {
//                View infoWindow = getLayoutInflater().inflate(R.layout.activity_maps,
//                        (FrameLayout) findViewById(R.id.map), false);
//
//                return infoWindow;
//            }
//        });

        getLocationPermission();

        updateLocationUI();

        getOriginLocation();

        // Tracking the device.
        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                getDeviceLocation();
            }}, 0, 10000);
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

    public static double calculate_distance(double lat1, double lat2, double lon1, double lon2) {

        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Haversine formula for distance calculation.
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2),2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Radius of earth in kilometers.
        double r = 6371;
        // Return value in Metres.
        return(c * r * 1000);
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
                            currentLocation = task.getResult();

                            if (currentLocation != null && lastKnownLocation!=null) {
//                                TextView cords = findViewById(R.id.cord);
//                                String rst = ""+currentLocation.getLatitude() + "\n"+ currentLocation.getLongitude();
                                Double lat1 = lastKnownLocation.getLatitude();
                                Double lon1 = lastKnownLocation.getLongitude();
                                Double lat2 = currentLocation.getLatitude();
                                Double lon2 = currentLocation.getLongitude();

//                                cords.setText(rst);
                                Double moved = calculate_distance(lat1,lat2,lon1,lon2);
                                distance+=moved;
                                TextView distanceView = findViewById(R.id.distance);
                                int km = (int)distance/1000;
                                int m = (int)distance % 1000;
                                distanceView.setText(String.format("%02d.%03d km", km, m));

                                TextView speedView = findViewById(R.id.speed);
                                speedView.setText((totalTime == 0 ? "0" : String.format("%.2f", distance/totalTime)) + "m/s");
//                                if(distance<=1000.0){
//                                    TextView distanceView = findViewById(R.id.distance);
//                                    distanceView.setText(Double.toString(distance)+"m");
//                                }
//                                else {
//                                    TextView distanceView = findViewById(R.id.distanceView);
//                                    distanceView.setText(Double.toString(distance/1000.0)+"Km");
//                                }
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(currentLocation.getLatitude(),
                                                currentLocation.getLongitude()), DEFAULT_ZOOM));
                                lastKnownLocation = currentLocation;
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

    private void getOriginLocation() {
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the origin location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
//                                TextView cords = findViewById(R.id.cord);
//                                String rst = Double.toString(lastKnownLocation.getLatitude()) + "\n"+ Double.toString(lastKnownLocation.getLongitude());
//                                cords.setText(rst);
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                                // Adding marker at starting location.
                                map.addMarker(new MarkerOptions()
                                        .position(new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude()))
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
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


    public String getDeviceIP() {
        try {
            for (Enumeration<NetworkInterface> ni = NetworkInterface.getNetworkInterfaces(); ni.hasMoreElements(); ) {
                NetworkInterface interfaceVar = ni.nextElement();
                for (Enumeration<InetAddress> enumIP = interfaceVar.getInetAddresses(); enumIP.hasMoreElements(); ) {
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
    public void OnHotspotEnabled(boolean enabled, @Nullable WifiConfiguration wifiConfiguration) {
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
        } else {
            Log.d("Hotspot", "Hotspot turned off");
        }
    }

    private void onPostResponse() {
        if (androidIP != null) {
            getRequest = new GetRequest();
            getRequest.execute(androidIP);
            socketConnection.startServerSocket();

            checkConnection.setVisibility(View.VISIBLE);
            final ImageView retry = findViewById(R.id.retryButton);
            retry.setVisibility(View.INVISIBLE);
            bikeDashboard.setVisibility(View.INVISIBLE);
            syncConnection.setVisibility(View.INVISIBLE);
            tripDetails.setVisibility(View.INVISIBLE);

            runHandler = new Runnable() {
                private int count = 0;
                private boolean isConnected = false;

                @Override
                public void run() {
                    TextView connectingText = findViewById(R.id.connectionTextBox);
                    Log.d("Bike", "Running Handler" + count);
                    if (connectionStatus == CONNECTION_UNKNOWN) {
                        String text = "Connecting ";
                        for (int j = 0; j < count % 3 + 1; j++) text += ".";
                        connectingText.setText(text);
                        count++;
                        if (count == 30) {
                            cancelAsyncTasks();
                            connectingText.setText("Timeout occurred. Retry.");
                            Log.d("Bike", "Timeoout");
                            hotspot.turnOffHotspot();
                            retry.setVisibility(View.VISIBLE);
                            socketConnection.closeAllConnectionsAndThreads();
                        } else {
                            updateUI.postDelayed(this, 1000);
                        }
                    }
                    if (connectionStatus == CONNECTION_FAILED) {
                        connectingText.setText("Connection Failed. Make Sure you are close to the bike or Try Again later.");
                        Log.d("Bike", "Not Connected.");
                        hotspot.turnOffHotspot();
                        retry.setVisibility(View.VISIBLE);
                        socketConnection.closeAllConnectionsAndThreads();
                    }
                    if (connectionStatus == CONNECTION_ESTABLISHED) {
                        cancelAsyncTasks();
                        socketConnection.closeAllConnectionsAndThreads();
                        if (!isConnected) {
                            connectingText.setText("Connected ^_^ ");
                            Log.d("Bike", "Connected");
                            updateUI.postDelayed(this, 1000);
                            isConnected = true;
                        } else {
                            checkConnection.setVisibility(View.GONE);
                            bikeDashboard.setVisibility(View.VISIBLE);
                            userRef.child("currentBike").setValue(bikeIP);
                            Date c = Calendar.getInstance().getTime();
                            SimpleDateFormat df1 = new SimpleDateFormat("dd-MMM-yyyy");
                            SimpleDateFormat df2 = new SimpleDateFormat("hh:mm a");
                            rideDate = df1.format(c);
                            rideTime = df2.format(c);
                            Log.d("ride", rideDate + " " + rideTime);
                            timerThread.start();

                            syncConnectionStatus.postDelayed(syncStatus,3000);
                        }
                    }
                }
            };
            updateUI.post(runHandler);
        }
    }

    private class SocketResponse extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String ip = params[0];
            String msg = params[1];
            String response = socketConnection.sendClientMessage(ip, msg);
            Log.d("Socket Data Received", response);
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            if (pd.isShowing()) {
                pd.dismiss();
            }
            TextView status = findViewById(R.id.status);

            if (result.contains("Lock")){
                status.setText("Locked");
            }
            else if (result.contains("Unlock")){
                status.setText("Unlocked");
            }
            else if (result.contains("Release")) {
                if (result.contains("Error")){
                    Toast.makeText(getApplicationContext(), "Please get close to a Dock Point and Retry", Toast.LENGTH_LONG).show();
                } else {

                    TextView distanceSummary = findViewById(R.id.dist1);
                    TextView timeSummary = findViewById(R.id.time2);
                    int km = (int)distance/1000;
                    int m = (int)distance % 1000;
                    String rideDistance = String.format("%02d.%03d km", km, m);
                    distanceSummary.setText(rideDistance);

                    int hours = totalTime / 3600;
                    int minutes = (totalTime % 3600) / 60;
                    int seconds = totalTime % 60;

                    String rideTotalTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                    timeSummary.setText(rideTotalTime);

                    PreviousRides ride = new PreviousRides(bikeName, rideDate, rideTime, rideDistance, rideTotalTime );
                    DatabaseReference rideRef = userRef.child("rides").push();
                    userRef.child("currentBike").setValue(null);
                    rideRef.setValue(ride);

                    status.setText("Released");
                    tripDetails.setVisibility(View.VISIBLE);
                    bikeDashboard.setVisibility(View.VISIBLE);
                    bikeDashboard.setAlpha(0.5f);
                    checkConnection.setVisibility(View.INVISIBLE);
                    syncConnection.setVisibility(View.INVISIBLE);
                }
            }
            else {
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
            }
        }
    }


    private class PostRequest extends CreateHTTPPostRequest {
        @Override
        protected void onPreExecute() {
            if (isCancelled()) {
                return;
            }
            pd.show();
        }

        @Override
        protected void onPostExecute(String result) {
            pd.dismiss();
            if (isCancelled()) {
                return;
            }
            try {
                JSONObject resultJson = new JSONObject(result);
                result = resultJson.getString("message");
                Log.d("Request", result);
                toast = Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT);
                toast.show();
                if (result.contains("Error")) {
                    Log.d("Request", "Error sending request");
                    hotspot.turnOffHotspot();
                    startActivity(new Intent(getApplicationContext(), LoggedInActivity.class));
                    finish();
                    return;
                } else {
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
            if (isCancelled()) {
                return;
            }
            try {
                JSONObject resultJson = new JSONObject(result);
                result = resultJson.getString("message");
                Log.d("Request", result);
                if (result.contains("Can't connect")) {
                    Log.d("Request", "Not able to connect");
                    connectionStatus = CONNECTION_FAILED;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void cancelAsyncTasks() {
        if (postRequest != null) {
            postRequest.cancel(true);
        }
        if (getRequest != null) {
            getRequest.cancel(true);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        BikeControllerActivity.isActivityRunning = true;
    }

    @Override
    public void onStop(){
        super.onStop();
//        BikeControllerActivity.isActivityRunning = false; // todo : Uncomment later

        if (socketConnection != null) {
            socketConnection.closeAllConnectionsAndThreads();
        }
        if (updateUI != null && runHandler != null) {
            updateUI.removeCallbacks(runHandler);
        }
        if (syncConnectionStatus != null && syncStatus != null){
            syncConnectionStatus.removeCallbacks(syncStatus);
        }
    }

    @Override
    public void onBackPressed() {
        if (connectionStatus != CONNECTION_ESTABLISHED) {
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
                    if (connectionStatus != CONNECTION_ESTABLISHED) {
                        doublePressToExit = false;
                    }
                }
            }, 2000);
        } else {
//            super.onBackPressed();
            moveTaskToBack(true);
//            startActivity(new Intent(getApplicationContext(),LoggedInActivity.class));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelAsyncTasks();
        if (socketConnection != null) {
            socketConnection.closeAllConnectionsAndThreads();
        }
        if (updateUI != null && runHandler != null) {
            updateUI.removeCallbacks(runHandler);
        }
        BikeControllerActivity.isActivityRunning = false;
        toast.cancel();
    }
}
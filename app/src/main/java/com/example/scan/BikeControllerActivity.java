package com.example.scan;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.scan.util.CreateHTTPGetRequest;
import com.example.scan.util.CreateHTTPPostRequest;
import com.example.scan.util.HotspotManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class BikeControllerActivity extends AppCompatActivity implements HotspotManager.OnHotspotEnabledListener {
    private int CONNECTION_UNKNOWN = 0, CONNECTION_ESTABLISHED = 1, CONNECTION_FAILED = -1;
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
    private int connectionEstablished = CONNECTION_UNKNOWN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_controller);

        bikeDashboard = findViewById(R.id.check_connection);
        checkConnection = findViewById(R.id.bike_control_dashboard);

        hotspot = new HotspotManager((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE),this);

        toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
        pd = new ProgressDialog(this);
        pd.setCanceledOnTouchOutside(false);
        pd.setMessage("Please Wait");

        (findViewById(R.id.retryButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                overridePendingTransition(0, 0);
                startActivity(getIntent());
                overridePendingTransition(0, 0);
            }
        });

    }

    @Override
    public void onStart(){
        super.onStart();
        bikeDashboard.setVisibility(View.VISIBLE);
        checkConnection.setVisibility(View.GONE);

        bikeIP = getIntent().getStringExtra("IP");
        hotspot.turnOnHotspot(BikeControllerActivity.this);
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

            final Handler updateUI= new Handler();
            updateUI.postDelayed(new Runnable() {
                private int count = 0;
                @Override
                public void run() {
                    TextView connectingText = findViewById(R.id.connectionTextBox);
                    ImageButton retry = findViewById(R.id.retryButton);
                    retry.setVisibility(View.INVISIBLE);
                    if (connectionEstablished == CONNECTION_UNKNOWN) {
                        String text = "Connecting ";
                        for(int j=0;j<count%3 +1;j++)   text += ".";
                        connectingText.setText(text);
                        count++;
                        if (count == 30) {
                            connectingText.setText("Timeout occurred. Retry.");
                            retry.setVisibility(View.VISIBLE);
                            updateUI.removeCallbacks(this);
                        }
                    }
                    if (connectionEstablished == CONNECTION_FAILED) {
                        connectingText.setText("Connection Failed. Retry.");
                        retry.setVisibility(View.VISIBLE);
                    }
                    if (connectionEstablished == CONNECTION_ESTABLISHED) {
                        cancelAsyncTasks();
                        connectingText.setText("Connected ... ");
                        retry.setVisibility(View.INVISIBLE);
                        updateUI.removeCallbacks(this);
                    }
                }
            },1000);

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
                Log.d("Request", " Get Async Task cancelled");
                return;
            }
            try {
                JSONObject resultJson = new JSONObject(result);
                result = resultJson.getString("message");
                Log.d("Request",result);
                if (result.contains("Error")){
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
    public void onStop(){
        super.onStop();
        cancelAsyncTasks();
        toast.cancel();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        cancelAsyncTasks();
        toast.cancel();
    }
}

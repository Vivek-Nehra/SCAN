package com.example.scan.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.scan.LoggedInActivity;


public class HotspotManager {
    private final WifiManager wifiManager;
    private final OnHotspotEnabledListener onHotspotEnabledListener;
    private WifiManager.LocalOnlyHotspotReservation mReservation;
    private Toast toast = null;

    public interface OnHotspotEnabledListener{
        void OnHotspotEnabled(boolean enabled, @Nullable WifiConfiguration wifiConfiguration);
    }

    //call with Hotspotmanager(getApplicationContext().getSystemService(Context.WIFI_SERVICE),this) in an activity that implements the Hotspotmanager.OnHotspotEnabledListener
    public HotspotManager(WifiManager wifiManager, OnHotspotEnabledListener onHotspotEnabledListener) {
        this.wifiManager = wifiManager;
        this.onHotspotEnabledListener = onHotspotEnabledListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void turnOnHotspot(final Context context) {
        wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

            @Override
            public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                super.onStarted(reservation);
                mReservation = reservation;
                onHotspotEnabledListener.OnHotspotEnabled(true, mReservation.getWifiConfiguration());

            }

            @Override
            public void onStopped() {
                super.onStopped();
            }

            @Override
            public void onFailed(int reason) {
                super.onFailed(reason);
                if (reason == 3) {
                    Log.d("msg", "reason 3 yayyyy");
                    AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                    alertDialog.setTitle("Alert");
                    alertDialog.setMessage("Disable Hotspot to continue");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
//                    toast = Toast.makeText(context, "Disable Hotspot for App to Work", Toast.LENGTH_SHORT);
                }
            }
        }, new Handler());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void turnOffHotspot() {
        if (mReservation != null) {
            mReservation.close();
            onHotspotEnabledListener.OnHotspotEnabled(false, null);
        }
    }
}

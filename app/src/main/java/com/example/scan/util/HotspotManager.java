package com.example.scan.util;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;


public class HotspotManager {
    private final WifiManager wifiManager;
    private final OnHotspotEnabledListener onHotspotEnabledListener;
    private WifiManager.LocalOnlyHotspotReservation mReservation;

    public interface OnHotspotEnabledListener{
        void OnHotspotEnabled(boolean enabled, @Nullable WifiConfiguration wifiConfiguration);
    }8

    //call with Hotspotmanager(getApplicationContext().getSystemService(Context.WIFI_SERVICE),this) in an activity that implements the Hotspotmanager.OnHotspotEnabledListener
    public HotspotManager(WifiManager wifiManager, OnHotspotEnabledListener onHotspotEnabledListener) {
        this.wifiManager = wifiManager;
        this.onHotspotEnabledListener = onHotspotEnabledListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void turnOnHotspot() {
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

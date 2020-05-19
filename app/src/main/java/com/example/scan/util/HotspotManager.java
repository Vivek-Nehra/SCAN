package com.example.scan.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
    }


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

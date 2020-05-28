package com.example.scan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;

public class ScannerActivity extends AppCompatActivity {

    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private TextView textScan;
    private BarcodeDetector barcodeDetector;
    private boolean processingScannedField;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);


        Camera.Parameters params;
        Camera camera;
        processingScannedField = false;


        // camera setup and qr code detection.
        surfaceView = (SurfaceView)findViewById(R.id.camerapreview);
        textScan = (TextView)findViewById(R.id.textscan);

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE).build();
        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640, 480).setAutoFocusEnabled(true).build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    cameraSource.start(holder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                catch (Exception e){
                    TextView textView = (TextView)findViewById(R.id.textscan);
                    textView.setText("Please check your camera permissions");
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                cameraSource.stop();
            }
        });



        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                Log.d("Scanner","Over");
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {

                // When QRCode is detected.
                final SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                // to get IP address corresponding to scanned QR Code.
                if(qrCodes.size()!=0 && !processingScannedField){
                    processingScannedField = true;
                    textScan.post(new Runnable() {
                        @Override
                        public void run() {
                            Vibrator vibrator = (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                            assert vibrator != null;
                            vibrator.vibrate(100);
                            textScan.setText("Processing QR Code");
                            String qrCode = qrCodes.valueAt(0).displayValue;
                            DatabaseReference piRef = FirebaseDatabase.getInstance().getReference().child("pi").child(qrCode);
                            //piRef.child("status").setValue("Active");
                            piRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    String ip = dataSnapshot.child("ipaddress").getValue(String.class);
                                    if(ip!=null) {
                                        Log.d("Scan","IP: " + ip);
                                        processingScannedField = false;
                                        cameraSource.stop();
                                        textScan.setText("Bike Found");

                                        Intent scannedData = new Intent(getApplicationContext(), BikeControllerActivity.class);
                                        scannedData.putExtra("IP", ip);
                                        startActivity(scannedData);
                                        finish();
                                    }
                                    processingScannedField = false;
                                    textScan.setText("Focus on QR Code");

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Log.e("Error", "Unable to retrieve IP address.");
                                    processingScannedField = false;
                                }
                            });
                        }
                    });
                }
            }
        });
    }
}

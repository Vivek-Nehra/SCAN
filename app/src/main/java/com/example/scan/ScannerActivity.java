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
import com.google.firebase.database.Query;
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

        // returns a reference to firebase Json tree.
        final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        final DatabaseReference raspRef = rootRef.child("raspberries");


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

                            Query ipquery = raspRef.child("configurations");

                            ipquery.addListenerForSingleValueEvent(new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    Iterable<DataSnapshot> dataSnapshotIterable = dataSnapshot.getChildren();

                                    for (DataSnapshot p : dataSnapshotIterable) {
                                        String piname = p.getKey();
                                        String qrcode = qrCodes.valueAt(0).displayValue;
                                        assert piname != null;
                                        // Obtained IP address
                                        if(piname.equals(qrcode)) {
                                            String ipvalue = p.getValue(String.class);
                                            processingScannedField = false;
                                            cameraSource.stop();
                                            textScan.setText("Bike Found");
                                            Intent returnData = new Intent();
                                            returnData.putExtra("IP", ipvalue);
                                            setResult(RESULT_OK, returnData);
                                            finish();
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    setResult(RESULT_CANCELED);
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // When the user hits the back button set the resultCode
        // as Activity.RESULT_CANCELED to indicate a failure
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}

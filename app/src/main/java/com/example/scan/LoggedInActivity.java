package com.example.scan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;


public class LoggedInActivity extends AppCompatActivity  {

    private boolean doublePressToExit = false;
    private Toast toast = null;
    private String username = null;
    private String bikeIP = null;
    private Uri filePath;
    private final int PICK_IMAGE_REQUEST = 71;
    private Bitmap user_image;

    private StorageReference storageReference;
    private FirebaseUser currentUser;
    private StorageReference ref;
    private DatabaseReference userRef;

    TextView profileName;
    de.hdodenhof.circleimageview.CircleImageView profileImage;



    @Override
    protected void onStart() {

        super.onStart();
        getUserData();
        getUserImage();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logged_in);

        toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
        storageReference = FirebaseStorage.getInstance().getReference();;
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        ref = storageReference.child("images/"+ currentUser.getUid());
        profileName = findViewById(R.id.profile_name);
        profileImage = findViewById(R.id.profile_image);

        //getUserData();
        System.out.println("Logged In started again");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase scanDB = FirebaseDatabase.getInstance();
        userRef = scanDB.getReference().child("Users").child(currentUser.getUid());

        (findViewById(R.id.profile_image)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();
            }

        });

        (findViewById(R.id.signOut)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            }

        });

        (findViewById(R.id.cardView1)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bikeIP == null){
                    Snackbar.make(findViewById(R.id.constraintLayout), "Please rent a bike first", Snackbar.LENGTH_LONG).show();
                } else{
                    Intent intent = new Intent(getApplicationContext(),BikeControllerActivity.class);
                    intent.putExtra("IP", bikeIP);
                    startActivity(intent);
                }
            }
        });


        (findViewById(R.id.scanButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {        // todo : Add all permissions at app startup
                if (bikeIP == null) {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) !=
                            PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(com.example.scan.LoggedInActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                50);
                    } else {
                        startActivity(new Intent(getApplicationContext(), ScannerActivity.class));
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
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if ( requestCode == 50) {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // camera-related task you need to do.
                    startActivity(new Intent(getApplicationContext(), ScannerActivity.class));
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("Scanner","Permission denied by the user!!");
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
//        if(userdata!=null){
//            userdata.cancel(true);
//        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null )
        {
            filePath = data.getData();
            uploadImage();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                profileImage.setImageBitmap(bitmap);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void uploadImage() {
        if(filePath != null)
        {
            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Toast.makeText(LoggedInActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(LoggedInActivity.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler dataHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            profileName.setText(username);

            //super.handleMessage(msg);
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler imgHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            profileImage.setImageBitmap(user_image);
        }
    };


    protected void getUserData() {
        new Thread(new Runnable() { // todo: Use handler only
            @Override
            public void run() {

                userRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        username = dataSnapshot.child("username").getValue(String.class);
                        bikeIP = dataSnapshot.child("currentBike").getValue(String.class);
                        Log.d("handler", username);
                        Message msg = dataHandler.obtainMessage();
                        dataHandler.sendMessage(msg);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("Error", "Unable to load User");
                    }
                });
            }
        }).start();
    }

    protected void getUserImage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final File localFile = File.createTempFile("Images", "bmp");
                    ref.getFile(localFile).addOnSuccessListener(new OnSuccessListener< FileDownloadTask.TaskSnapshot >() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                user_image = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                                Log.d("handler", "User_image Loaded");
                                Message msg = dataHandler.obtainMessage();
                                imgHandler.sendMessage(msg);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("UserData", "Unable to load image");
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
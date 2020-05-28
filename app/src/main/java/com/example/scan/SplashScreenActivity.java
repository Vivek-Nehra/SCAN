package com.example.scan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        final FirebaseAuth mauth = FirebaseAuth.getInstance();

        new Handler().postDelayed(new Runnable(){
            public void run(){
                if(mauth.getCurrentUser() != null && mauth.getCurrentUser().isEmailVerified()){
                    Log.d("Log", "Logging User in : " + mauth.getCurrentUser().getDisplayName() + " " + mauth.getCurrentUser().getEmail());
                    startActivity(new Intent(getApplicationContext(), LoggedInActivity.class));
                }
                else {
                    startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
                }
                finish();
            }
    }, 1000);
    }
}

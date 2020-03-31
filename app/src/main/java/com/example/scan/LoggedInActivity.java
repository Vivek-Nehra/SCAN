package com.example.scan;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class LoggedInActivity extends AppCompatActivity {

    private boolean doublePressToExit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logged_in);
        Log.d("Log", " UserName : " + FirebaseAuth.getInstance().getCurrentUser().getDisplayName());

        (findViewById(R.id.signOut)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        });

        (findViewById(R.id.scanCode)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ASyncT startHotspot = new ASyncT();
                startHotspot.execute();
            }
        });
    }

    @Override
    public void onBackPressed(){        // todo: Check working after adding multiple activites to the activity stack
        if (doublePressToExit){
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        Toast.makeText(this, "Press again to exit.", Toast.LENGTH_SHORT).show();
        doublePressToExit = true;
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run(){
                doublePressToExit = false;
            }
        }, 2000);
    }
}


class ASyncT extends AsyncTask<Void, Void, Void> {
    @Override
    protected Void doInBackground(Void ...params){
        // todo : Add logic to start hotspot with random username and password
        return null;
    }
}
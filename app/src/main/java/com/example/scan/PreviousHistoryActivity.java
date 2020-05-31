package com.example.scan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.scan.util.PreviousRides;
import com.example.scan.util.RideAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class PreviousHistoryActivity extends AppCompatActivity {

    private ListView ridesListView;
    private ArrayList<PreviousRides> rides = new ArrayList<PreviousRides>();

    RideAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_previous_history);
        ridesListView = (ListView) findViewById(R.id.rideHistory);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getRideHistory();
        adapter = new RideAdapter(this, rides);
        ridesListView.setAdapter(adapter);
    }


    private void getRideHistory(){
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase scanDB = FirebaseDatabase.getInstance();
        DatabaseReference userRef = scanDB.getReference().child("Users").child(currentUser.getUid()).child("rides");
        Query orderedData = userRef.orderByChild("date");
        orderedData.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                rides.clear();

                for (DataSnapshot rideSnapshot: dataSnapshot.getChildren()){
                    PreviousRides ride = rideSnapshot.getValue(PreviousRides.class);
                    rides.add(ride);
                }
                Collections.reverse(rides);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("rides", "Unable to retrieve past history");

            }
        });
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        startActivity(new Intent(getApplicationContext(),LoggedInActivity.class));
        finish();
    }
}

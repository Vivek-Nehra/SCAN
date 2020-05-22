package com.example.scan.util;

public class User {

    public String username;
    public String email;
    public String currentBike;
    public int totalDistance;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String username, String email ) {
        this.username = username;
        this.email = email;
        this.currentBike = null;
        this.totalDistance = 0;
    }

}


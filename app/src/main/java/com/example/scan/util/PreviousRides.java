package com.example.scan.util;

public class PreviousRides {

    public String bikeName;
    public String date;
    public String startTime;
    public int distance;
    public String duration;

    public PreviousRides(){
    }

    public PreviousRides(String bikeName, String date, String startTime, int distance, String duration){
            this.bikeName = bikeName;
            this.date = date;
            this.startTime = startTime;
            this.distance = distance;
            this.duration = duration;
    }

}

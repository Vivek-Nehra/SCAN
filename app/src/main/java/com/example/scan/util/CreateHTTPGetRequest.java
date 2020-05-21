package com.example.scan.util;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;



public class CreateHTTPGetRequest extends AsyncTask<String, Void, String>{
    @Override
    protected String doInBackground(String... params) {

        String androidIP = params[0];

        try {
            String urlString = "https://scan-app2020.herokuapp.com/scanapp/android_status?androidIP=" + androidIP;
            URL url = new URL(urlString);

            for (int i=0; i<10;i++) {
                if (isCancelled()){
                    return "{\"message\": \"Try Again.\"}";
                }
                HttpURLConnection http = (HttpURLConnection) url.openConnection();
                http.connect();
                int statusCode = http.getResponseCode();
                Log.d("StatusCode","Value: " + statusCode);

                if(statusCode == 500) {
                    Log.d("500","Checking status again..");
                    if (http != null) {
                        http.disconnect();
                    }
                    SystemClock.sleep(3000);
                }
                else {
                    try {

                        InputStream in = new BufferedInputStream(http.getInputStream());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        StringBuilder result = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line);
                        }
                        Log.d("GET","Result from server" + result);

                        return result.toString();

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (http != null) {
                            http.disconnect();
                        }
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "{\"message\": \"Try Again.\"}";
    }
}

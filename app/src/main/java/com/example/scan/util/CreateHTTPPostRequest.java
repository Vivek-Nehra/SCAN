package com.example.scan.util;

import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class CreateHTTPPostRequest extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {
        String name = params[0];
        String pass = params[1];
        String bikeIP = params[2];
        String androidIP = params[3];


        try {
            URL url = new URL("https://scan-app2020.herokuapp.com/scanapp/receive");
            HttpURLConnection http = (HttpURLConnection) url.openConnection();

            Map<String, String> arguments = new HashMap<>();
            arguments.put("name", name);
            arguments.put("pass", pass);
            arguments.put("bikeIP", bikeIP);
            arguments.put("androidIP", androidIP);
            StringJoiner sj = new StringJoiner("&");

            for (Map.Entry<String, String> entry : arguments.entrySet())
                sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "="
                        + URLEncoder.encode(entry.getValue(), "UTF-8"));
            byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
            int length = out.length;

            http.setFixedLengthStreamingMode(length);
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            http.connect();

            try (OutputStream os = http.getOutputStream()) {
                os.write(out);
                os.flush();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            int statusCode = http.getResponseCode();

            Log.d("StatusCode","Value: " + statusCode);

            try {

                InputStream in = new BufferedInputStream(http.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                return result.toString();

            } catch (IOException e) {
                if(statusCode == 500) {
                    InputStream in = new BufferedInputStream(http.getErrorStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    return result.toString();
                }
                e.printStackTrace();
            } finally {
                if (http != null) {
                    http.disconnect();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "{\"message\": \"Error. Try Again.\"}";
    }
}
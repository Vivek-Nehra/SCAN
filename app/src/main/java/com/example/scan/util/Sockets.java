package com.example.scan.util;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Sockets {
    private boolean end = false;

    private void startServerSocket() {

        Thread thread = new Thread(new Runnable() {

            private String stringData = null;
            @Override
            public void run() {

                try {

                    ServerSocket ss = new ServerSocket(9002);

                    while (!end) {
                        //Server is waiting for client here.
                        Socket s = ss.accept();
                        // To read the data received from client.
                        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                        // readLine() takes \n as EOF therefore, msg with /n is sent from client.
                        stringData = input.readLine();

                        // To send the data from server to client.
                        // use conditional statements to send specific data based on received data.
                        PrintWriter output = new PrintWriter(s.getOutputStream(), true);

                        // Reverse connection establishment.
                        if(stringData.equals("Talk")){
                            output.println("Okay");
                        }
                        else if(stringData.contains("IP")){
                            output.println("Start Your Server");
                            String ip = stringData.substring(2);
                            String msg = "Hello, I have become a Client now\n";
                            output.close();
                            s.close();
                            ss.close();
                            end = true;
                            // At this point pi has become server and ready to accept messages from the app.
                            sendMessage(ip,msg);
                            continue;
                        }
                        else{
                            output.println("FROM SERVER - Acknowledgement ");
                        }
                        output.close();
                        s.close();
                    }
                    ss.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });
        thread.start();
    }

    // App as a Client.
    private void sendMessage(final String ip,final String msg) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    System.out.println("Received IP is : "+ip);
                    Socket s = new Socket(ip, 9008);

                    OutputStream out = s.getOutputStream();

                    PrintWriter output = new PrintWriter(out);

                    output.println(msg);
                    output.flush();
                    BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    final String st = input.readLine();

                    System.out.println(st + "Message received from Server");

                    output.close();
                    out.close();
                    s.close();
                } catch (IOException e) {
                    System.out.println("Cannot connect to Server");
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }
}

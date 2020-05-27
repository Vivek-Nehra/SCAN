package com.example.scan.util;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static com.example.scan.util.Constants.CONNECTION_ESTABLISHED;
import static com.example.scan.util.Constants.CONNECTION_UNKNOWN;

public class Sockets {
    private Thread serverThread = null, clientThread = null;
    private ServerSocket serverSocket = null;
    private Socket clientSocket = null;

    public void startServerSocket() {

        serverThread = new Thread(new Runnable() {

            private String stringData = null;

            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(9002);

//                    while (!end) {
                    //Server is waiting for client here.
                    System.out.println("Waiting for Data from Bike");
                    Socket s = serverSocket.accept();

                    // To read the data received from client.
                    BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    // readLine() takes \n as EOF therefore, msg with /n is sent from client.
                    stringData = input.readLine();
                    System.out.println("Data received:" + stringData);

                    // To send the data from server to client.
                    // use conditional statements to send specific data based on received data.
                    PrintWriter output = new PrintWriter(s.getOutputStream(), true);

                    // Reverse connection establishment.
                    if (stringData.equals("Connection Established")) {
                        output.println("FROM SERVER - OK");
                        Constants.connectionEstablished = CONNECTION_ESTABLISHED;
                    } else {
                        output.println("FROM SERVER - Acknowledgement ");
                    }
                    output.close();
                    s.close();
//                    }
                    serverSocket.close();
                } catch (IOException e) {
                    System.out.println("Server already running");
                    e.printStackTrace();
                }
            }
        });
        serverThread.start();
    }

    public String sendClientMessage(String ip, String msg) {
        clientSocket = new Socket();
        try {
            System.out.println("Received IP is : " + ip);
            clientSocket.connect(new InetSocketAddress(ip, 9008), 3000);
            clientSocket.setSoTimeout(5000);

            OutputStream out = clientSocket.getOutputStream();

            PrintWriter output = new PrintWriter(out);

            output.println(msg);
            output.flush();
            BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            final String st = input.readLine();

            System.out.println(st + "Message received from Server");

            output.close();
            out.close();
            clientSocket.close();
            return st;
        } catch (IOException e) {
            System.out.println("Cannot connect to Server");
            if (e instanceof SocketTimeoutException) {
                Constants.connectionEstablished = CONNECTION_UNKNOWN;
            }
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return "Cannot Connect to Server";
        }
    }

    // App as a Client.
    public void sendMessage(final String ip, final String msg) {

        clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                sendClientMessage(ip, msg);
            }
        });
        clientThread.start();
    }

    public void closeAllConnectionsAndThreads() {
        try {
            System.out.println("Closing all Threads and sockets");
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
            if (serverThread != null && serverThread.isAlive()) {
                serverThread.interrupt();
            }
            if (clientThread != null && clientThread.isAlive()) {
                clientThread.interrupt();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalThreadStateException e) {
            e.printStackTrace();
        }
    }
}
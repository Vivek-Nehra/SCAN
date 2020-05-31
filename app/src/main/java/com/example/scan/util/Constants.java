package com.example.scan.util;

public class Constants {
    public static final String CONNECTION_UNKNOWN = "CONNECTION_UNKNOWN", CONNECTION_ESTABLISHED = "CONNECTION_ESTABLISHED",
            CONNECTION_FAILED = "CONNECTION_FAILED";
    public static String connectionStatus = CONNECTION_ESTABLISHED;
    public static int totalTime = 0;
    public static int serverUnreachable = 0; // Timeout at 2
}

package com.blogspot.tndev1403.antitheft.Stored.Config;

public class Config {
    /* Default value here */
    public final static int DISCONNECT_TYPE = -1;
    public final static int WARNING_TYPE = DISCONNECT_TYPE;
    public final static int SAFE_TYPE = 0;
    public final static int DANGER_TYPE = 1;
    public final static int NOTIFICATION_REQUEST_CODE = 14031998;
    /* Default setting */
    public static int VIBRATE_TIME = 500; // (miliseconds)
    public static long[][] VIBRATE_WAVE = {{50, 100, 200, 300, 400, 1000},{4}};
    /* Firebase */
    public final static String REFERENCE_STRING = "Laze";
}

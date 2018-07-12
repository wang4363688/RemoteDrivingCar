package com.android.remotedriving;

/**
 * Created by qbr on 2018/4/24.
 */

public class Instructions {

//    public static final String back = "2";

    public static final String P = "5";
    public static final String R = "6";
    public static final String N = "7";
    public static final String D1 = "8";
    public static final String D2 = "9";
    public static final String D1_W = "81";
    public static final String D1_D = "84";
    public static final String D1_A = "83";
    public static final String D2_W = "91";
    public static final String D2_D = "94";
    public static final String D2_A = "93";
    public static final String R_W = "61";//BACK
    public static final String R_D = "64";//RIGHT BACK
    public static final String R_A = "63";//LEFT BACK
    public static final String brake = "0";
    public static final String disconnect = "99";
    public static final String HOST = "10.210.26.205";

    public static final float stight_minired = 1.0f;
    public static final float turn_minired = 0.8f;
    public static final float stight_bigred = 0.8f;
    public static final float turn_bigred = 1.0f;
    public static final float stight_yellow = 0.6f;
    public static final float turn_yellow = 0.8f;

    public class MotorVoltage {
        public float straight_vol;  // input-voltage-level of the motor.
        public float turn_vol;
    }



//    public static final int STATE_CONNECTED = 9;









}

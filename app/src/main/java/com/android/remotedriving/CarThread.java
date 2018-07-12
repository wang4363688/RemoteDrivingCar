package com.android.remotedriving;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Switch;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by qbr on 2018/6/5.
 */

public class CarThread extends Thread {
    Message mes;
    Handler mhandler;
    private static final int CAR_PORT = 9001;
    private static double EARTH_RADIUS = 6378.137;
    private DatagramSocket client = null;
    private DatagramPacket packet;
    private DatagramPacket packet2;
    private DatagramSocket carsocket;
    private InetAddress NEXT_CAR_address;
    private static final String NEXT_CAR_HOST = "1.112.199.126";
    private LocationClient mLocationClient;
    private ExecutorService executor;


    //字节
    String reply = null;
    byte[] b = new byte[1024];
    JSONObject jsonObj = new JSONObject();
    int State = 0, Rec = 0;
    double c = 0, d = 0, la = 0, lo = 0;
    float h = 0, m =0;

    public CarThread(Context context, Handler handler) {
        this.mhandler = handler;
//        mLocationClient = new LocationClient(context);
//        mLocationClient.registerLocationListener(new MyLocationListener());
//        executor = Executors.newSingleThreadExecutor();
//        requestLocation();
        try {
            NEXT_CAR_address = InetAddress.getByName(NEXT_CAR_HOST);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        super.run();

        try {
            client = new DatagramSocket(CAR_PORT);

            while (true) {
                packet2 = new DatagramPacket(b, b.length);
                client.receive(packet2);
                reply = new String(b, 0, packet2.getLength());
                System.out.println("前车发来的信息。。。。。。。。。。。。。。。。。。。。。。。。。。。。。" + reply);
                jsonObj = new JSONObject(reply);
//                la = jsonObj.getDouble("latitude");
//                lo = jsonObj.getDouble("longitude");
                Rec = jsonObj.getInt("State");
                //判断状态
                mes = mhandler.obtainMessage();
//                mes.obj = instruction(c, la, d, lo, State);
                mes.obj = Rec_command(Rec);
                System.out.println("判断的信息。。。。。。。。。。。。。。。。。。。。。。。。。。。。。" + mes.obj);
                mhandler.sendMessage(mes);
            }
        }catch (Exception e) {
            e.printStackTrace();
            mes = mhandler.obtainMessage();
            mes.obj = "0";
            mhandler.sendMessage(mes);
        }
    }

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    public String Rec_command(int rec){
        String is ="0";
        switch (rec){
            case 0://brake
                is = Instructions.brake;
                break;
            case 1:// R_W
                is = Instructions.R_W;
                break;
            case 2://R_D
                is = Instructions.R_D;
                break;
            case 3://R_A
                is = Instructions.R_A;
                break;
            case 4://D1_W
                is = Instructions.D1_W;
                break;
            case 5://D1_D
                is = Instructions.D1_D;
                break;
            case 6://D1_A
                is = Instructions.D1_A;
                break;
            case 7://D2_W
                is = Instructions.D2_W;
                break;
            case 8://D2_D
                is = Instructions.D2_D;
                break;
            case 9://D2_A
                is = Instructions.D2_A;
                break;
            case  99:
                is = Instructions.brake;
                break;
            default:
                is = Instructions.brake;
                break;
        }
        return is;
    }



    public String instruction(double lat1, double lat2, double lng1, double lng2, int state){
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000d) / 10000d;
        s = s*1000;
        if(s >= 1.00){
            return Instructions.D1_W;
        }else {
            return  Instructions.brake;
        }
//        return "0";
    }

    public void exit(){
        mLocationClient.stop();
    }


    private void requestLocation(){
        initLocation();
        mLocationClient.start();
    }

    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setScanSpan(500);
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
    }
    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            c = bdLocation.getLatitude();
            d = bdLocation.getLongitude();
            h = bdLocation.getSpeed();
            m = bdLocation.getDirection();
            JSONObject jsonObjSend;
            jsonObjSend = new JSONObject();
            try {
                jsonObjSend.put("latitude", c);
                jsonObjSend.put("longitude", d);
                jsonObjSend.put("speed", h);
                jsonObjSend.put("direction", m);
                jsonObjSend.put("State", State);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            final byte[] Gps = jsonObjSend.toString().getBytes();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        carsocket = new DatagramSocket();
                        DatagramPacket packet_CAR= new DatagramPacket(Gps, Gps.length, NEXT_CAR_address, CAR_PORT);
                        carsocket.send(packet_CAR);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

}

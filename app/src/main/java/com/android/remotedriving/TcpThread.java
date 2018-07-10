package com.android.remotedriving;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by qbr on 2018/4/24.
 */

public class TcpThread extends Thread {
    Message mes;
    Handler mhandler;
//    private static final String TCP_HOST = "2001:da8:215:6a01::f651";
    private static final String TCP_HOST = "10.112.192.176";
    private static final int TCP_PORT = 8889;
    InetAddress address;
    //Socket客户端
    DatagramSocket client = null;
    DatagramPacket packet;
    DatagramPacket packet2;

    private String IMEI = "0";

    //字节
    String reply = null;
    String s ="ok";
    byte[] b = new byte[1024];

    public static String filename = "log_tcp";

    public TcpThread(Context context, Handler handler){
        this.mhandler=handler;
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Activity.TELEPHONY_SERVICE);
        if (tm != null) {
            IMEI = tm.getDeviceId();
        }
    }
    @Override
    public void run() {
        // TODO Auto-generated method stub
        super.run();

        try {
            address = InetAddress.getByName(TCP_HOST);
            byte[] data = IMEI.getBytes();
            packet = new DatagramPacket(data, data.length, address, TCP_PORT);
            client = new DatagramSocket();
            client.send(packet);
//            mes = mhandler.obtainMessage();
//            mes.what = Instructions.STATE_CONNECTED;
//            mhandler.sendMessage(mes);
            while(true) {
                try {
//                    is = client.getInputStream();
//                    l = is.read(b);
//                    reply = new String(b, 0, l);
//                    a = Integer.parseInt(reply);
//                    writelog("control:"+reply , filename);
                    packet2 = new DatagramPacket(b, b.length);
                    client.receive(packet2);
                    reply = new String(b, 0, packet2.getLength());
                    System.out.println("reply:"+reply);
                    mes = mhandler.obtainMessage();
//                    mes.what = a;
                    mes.obj = reply;
                    mhandler.sendMessage(mes);
//                    os = client.getOutputStream();
//                    os.write(s.getBytes());
//                    os.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                    writelog(e.toString(),filename);
                    mes = mhandler.obtainMessage();
//                    mes.what = a;
                    mes.obj = "0";
                    mhandler.sendMessage(mes);
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    public void writelog(String log, String filename){
        SimpleDateFormat sdf4 = new SimpleDateFormat("HH:mm:ss.SSS");
        String str4 = sdf4.format(new Date());
        String time4 = str4;
        String str = time4+"\t"+log+"\n";
        try {
            if (Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                File sdDire = Environment.getExternalStorageDirectory();
                FileOutputStream outFileStream = new FileOutputStream(
                        sdDire.getCanonicalPath() + "/" + filename + ".txt", true);
                outFileStream.write(str.getBytes());
                outFileStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

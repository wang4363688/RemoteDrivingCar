package com.android.remotedriving;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import org.json.JSONException;
import org.json.JSONObject;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.spi.Log;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.sleep;

public class FleetActivity extends IOIOActivity implements SurfaceHolder.Callback,
        Camera.PreviewCallback, Encode.IEncoderListener {

    private Camera camera;
    private boolean isPreview = false;
    private SurfaceView surfaceView;

    private static final String VLC_HOST = "10.30.176.175";
    private static final String NEXT_CAR_HOST = "1.112.171.47";
    //    private static final String VLC_HOST = "10.210.26.205";
    private static final int VLC_PORT = 5501;
    private static final int GPS_PORT = 8901;
    private static final int CAR_PORT = 9001;
    private String mGps = "GPS";
    private String IMEI = "0";
    private int State = 0;
    private int Orientation = 101;
    int width = 320;
    int height = 240;
    private int displayOrientation = 90;
    private InetAddress address;
    private DatagramSocket socket;
    private DatagramSocket statesocket;
    private DatagramSocket gpssocket;
    private InetAddress NEXT_CAR_address;
    private ExecutorService executor;
    private ExecutorService executor_1;
    private Encode encode;
    private LocationClient mLocationClient;
    public static String filename = "log_gps";

    private ProgressBar progressBar1_;
    private TextView textView2_;
    private int echoSeconds;
    private int echoDistanceCm;


    Handler mMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.obj.toString()) {
//                case Instructions.STATE_CONNECTED:
//                    toast("Connected Successfully");
//                    break;
                case Instructions.brake:
                    SetState(0);
                    break;
                case Instructions.R:
                    if(Orientation != 100)
                    {
                        Orientation = 100;
                        stopPreview();
                        startPreview();
                    }
                    break;
                case Instructions.R_W:
                    SetState(1);
                    break;
                case Instructions.R_D:
                    SetState(2);
                    break;
                case Instructions.R_A:
                    SetState(3);
                    break;
                case Instructions.D1_W:
                    SetState(4);
                    break;
                case Instructions.D1:
                    if(Orientation == 100)
                    {
                        Orientation = 101;
                        stopPreview();
                        startPreview();
                    }
                    break;
                case Instructions.D1_D:
                    SetState(5);
                    break;
                case Instructions.D1_A:
                    SetState(6);
                    break;
                case Instructions.D2:
                    if(Orientation == 100)
                    {
                        Orientation = 101;
                        stopPreview();
                        startPreview();
                    }
                    break;
                case Instructions.D2_W:
                    SetState(7);
                    break;
                case Instructions.D2_D:
                    SetState(8);
                    break;
                case Instructions.D2_A:
                    SetState(9);
                    break;
                case Instructions.disconnect:
                    SetState(99);
                    break;
                default:
                    SetState(0);
                    break;
            }

        }
    };

    private void SetState(int i) {
        State = i;
    }

    TcpThread tcpThread;
    CarThread carThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = (SurfaceView) findViewById(R.id.camera_surfaceview);
        surfaceView.getHolder().setFixedSize(width, height);
        surfaceView.getHolder().setKeepScreenOn(true);
        surfaceView.getHolder().addCallback(this);

		/* ultrasonic sensor */
        progressBar1_ = (ProgressBar) findViewById(R.id.progressBar1);
        textView2_ = (TextView) findViewById(R.id.textView2);

        TelephonyManager tm = (TelephonyManager) getSystemService(Activity.TELEPHONY_SERVICE);
        if (tm != null) {
            IMEI = tm.getDeviceId();
        }
        tcpThread=new TcpThread(getApplicationContext(), mMessageHandler);
        carThread=new CarThread(getApplicationContext(), mMessageHandler);
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        requestLocation();

        try {
            address = InetAddress.getByName(VLC_HOST);
            socket = new DatagramSocket();
            NEXT_CAR_address = InetAddress.getByName(NEXT_CAR_HOST);
            statesocket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
        tcpThread.start();
        carThread.start();
        executor = Executors.newSingleThreadExecutor();
        executor_1 = Executors.newSingleThreadExecutor();
    }

    private void startPreview() {
        if (camera == null&& Orientation!=100) {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        }else {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }

        try {
            Camera.Parameters parameters = camera.getParameters();
//            parameters.setPreviewFrameRate(20);//设置帧率
            parameters.setPreviewSize(width, height);//设置分辨率
            parameters.setPictureSize(width, height);
            parameters.setPreviewFormat(ImageFormat.NV21);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            parameters.setSceneMode(Camera.Parameters.SCENE_MODE_ACTION);
            camera.setParameters(parameters);


            Camera.Size previewSize = parameters.getPreviewSize();
            int size = previewSize.width * previewSize.height;
            size = size * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;

            if (encode == null) {
//                encode = new Encode(previewSize.width, previewSize.height,
//                        125000, 10, this);
                encode = new Encode(previewSize.width, previewSize.height,
                        125000, 10, this);
            }

            camera.addCallbackBuffer(new byte[size]);
            camera.setPreviewDisplay(surfaceView.getHolder());
            camera.setDisplayOrientation(displayOrientation);
            camera.setPreviewCallbackWithBuffer(this);
            camera.startPreview();
            isPreview = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopPreview() {
        if (camera != null) {
            if (isPreview) {
                isPreview = false;
                camera.setPreviewCallbackWithBuffer(null);
                camera.stopPreview();
            }
            camera.release();
            camera = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (isPreview) {
            stopPreview();
        }

        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        encode.encoderYUV420(data);
        camera.addCallbackBuffer(data);
    }

    @Override
    public void onH264(final byte[] data) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramPacket packet = new DatagramPacket(data, 0, data.length, address, VLC_PORT);
                    socket.send(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (encode != null) {
            encode.releaseMediaCodec();
            encode = null;
        }
    }

    private boolean isQuit = false;

    @Override
    public void onBackPressed() {
        if (!isQuit) {
            Toast.makeText(FleetActivity.this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            isQuit = true;
            //这段代码意思是,在两秒钟之后isQuit会变成false
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        isQuit = false;
                    }
                }
            }).start();
        } else {
            stopPreview();
            mLocationClient.stop();
            carThread.exit();
            finish();
        }
    }

    private void requestLocation(){
        initLocation();
        mLocationClient.start();
    }

    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setScanSpan(1000);
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
    }

    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            StringBuilder currentPosition = new StringBuilder();
            currentPosition.append("纬度：").append(bdLocation.getLatitude()).append("\n");
            currentPosition.append("经度：").append(bdLocation.getLongitude()).append("\n");
            currentPosition.append("速度：").append(bdLocation.getSpeed()).append("\n");
            currentPosition.append("方向：").append(bdLocation.getDirection());
            mGps = currentPosition.toString();
//            writelog(mGps,filename);
//            System.out.println(mGps);
            JSONObject jsonObjSend;
            jsonObjSend = new JSONObject();
            try {
                jsonObjSend.put("imei", IMEI);
                jsonObjSend.put("gps", new String(Base64.encode(mGps.getBytes(),Base64.NO_WRAP)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JSONObject jsonObjSendNext;
            jsonObjSendNext = new JSONObject();
            try {
                jsonObjSendNext.put("State", State);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            final byte[] Gps = jsonObjSend.toString().getBytes();
            final byte[] Next = jsonObjSendNext.toString().getBytes();
            executor_1.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        gpssocket = new DatagramSocket();
                        DatagramPacket packet = new DatagramPacket(Gps, Gps.length, address, GPS_PORT);
                        DatagramPacket packet_CAR = new DatagramPacket(Next, Next.length, NEXT_CAR_address, CAR_PORT);
                        gpssocket.send(packet);
                        statesocket.send(packet_CAR);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    class Looper extends BaseIOIOLooper {
        private PwmOutput pwm1,pwm2, pwm3, pwm4;
        private DigitalOutput triggerPin_, led_;
        private PulseInput echoPin_;
        //  private DigitalOutput D1A, D2A, D1B, D2B;
        /**
         * Called every time a connection with IOIO has been established.
         * Typically used to open pins.
         *
         * @throws ConnectionLostException
         *             When IOIO connection is lost.
         *
         *
         */
        @Override
        protected void setup() throws ConnectionLostException {
            showVersions(ioio_, "IOIO connected!");
//            executor = Executors.newSingleThreadExecutor();
            led_ = ioio_.openDigitalOutput(0, true);
            echoPin_ = ioio_.openPulseInput(7, PulseInput.PulseMode.POSITIVE);
            triggerPin_ = ioio_.openDigitalOutput(6);

            pwm1 = ioio_.openPwmOutput(1, 100);
            pwm2 = ioio_.openPwmOutput(2, 100);
            pwm3 = ioio_.openPwmOutput(3, 100);
            pwm4 = ioio_.openPwmOutput(4, 100);

//            tcpThread.start();
        }

        /**
         * Called repetitively while the IOIO is connected.
         *
         * @throws ConnectionLostException
         *             When IOIO connection is lost.
         * @throws InterruptedException
         * 				When the IOIO thread has been interrupted.
         *
         * @see ioio.lib.util.IOIOLooper#loop()
         */
        @Override// 档位和 小车行进的要分开
        public void loop() throws ConnectionLostException, InterruptedException {

            try {
                // read HC-SR04 ultrasonic sensor
                triggerPin_.write(false);
                sleep(5);
                triggerPin_.write(true);
                sleep(1);
                triggerPin_.write(false);
                echoSeconds = (int) (echoPin_.getDuration() * 1000 * 1000);
                echoDistanceCm = echoSeconds / 29 / 2;
				/* update UI */
                updateViews();

                if(echoDistanceCm >50 ){
                    State = 7;
                } else {
                    State = 0;
                }

                sleep(20);
            } catch (InterruptedException e) {
                ioio_.disconnect();

            } catch (ConnectionLostException e) {
                throw e;

            }

            switch (State){
                case 0://brake
                    pwm1.setDutyCycle(0.0f);
                    pwm2.setDutyCycle(0.0f);
                    pwm3.setDutyCycle(0.0f);
                    pwm4.setDutyCycle(0.0f);

                    Thread.sleep(100);
                    break;
                case 1:// R_W
                    pwm1.setDutyCycle(0.0f);
                    pwm2.setDutyCycle(0.0f);
                    pwm3.setDutyCycle(0.0f);
                    pwm4.setDutyCycle(1.0f);
                    Thread.sleep(100);
                    break;
                case 2://R_D
                    pwm1.setDutyCycle(0.0f);
                    pwm2.setDutyCycle(1.0f);
                    pwm3.setDutyCycle(0.0f);
                    pwm4.setDutyCycle(1.0f);
                    Thread.sleep(100);
                    break;
                case 3://R_A
                    pwm1.setDutyCycle(1.0f);
                    pwm2.setDutyCycle(0.0f);
                    pwm3.setDutyCycle(0.0f);
                    pwm4.setDutyCycle(1.0f);
                    break;
                case 4://D1_W
                    pwm1.setDutyCycle(0.0f);
                    pwm2.setDutyCycle(0.0f);
                    pwm3.setDutyCycle(0.7f);
                    pwm4.setDutyCycle(0.0f);
                    Thread.sleep(100);
                    break;
                case 5://D1_D
                    pwm1.setDutyCycle(0.0f);
                    pwm2.setDutyCycle(1.0f);
                    pwm3.setDutyCycle(0.7f);
                    pwm4.setDutyCycle(0.0f);
                    Thread.sleep(100);
                    break;
                case 6://D1_A
                    pwm1.setDutyCycle(1.0f);
                    pwm2.setDutyCycle(0.0f);
                    pwm3.setDutyCycle(0.7f);
                    pwm4.setDutyCycle(0.0f);
                    Thread.sleep(100);
                    break;
                case 7://D2_W
                    pwm1.setDutyCycle(0.0f);
                    pwm2.setDutyCycle(0.0f);
                    pwm3.setDutyCycle(1.0f);
                    pwm4.setDutyCycle(0.0f);
                    Thread.sleep(100);
                    break;
                case 8://D2_D
                    pwm1.setDutyCycle(0.0f);
                    pwm2.setDutyCycle(1.0f);
                    pwm3.setDutyCycle(1.0f);
                    pwm4.setDutyCycle(0.0f);
                    Thread.sleep(100);
                    break;
                case 9://D2_A
                    pwm1.setDutyCycle(1.0f);
                    pwm2.setDutyCycle(0.0f);
                    pwm3.setDutyCycle(1.0f);
                    pwm4.setDutyCycle(0.0f);
                    Thread.sleep(100);
                    break;
                case  99:
                    pwm1.setDutyCycle(0.0f);
                    pwm2.setDutyCycle(0.0f);
                    pwm3.setDutyCycle(0.0f);
                    pwm4.setDutyCycle(0.0f);
                    Thread.sleep(100);
//                    executor.shutdown();
                    break;
                default:
                    pwm1.setDutyCycle(0.0f);
                    pwm2.setDutyCycle(0.0f);
                    pwm3.setDutyCycle(0.0f);
                    pwm4.setDutyCycle(0.0f);
                    Thread.sleep(100);
                    break;
            }



        }

        /**
         * Called when the IOIO is disconnected.
         *
         * @see ioio.lib.util.IOIOLooper#disconnected()
         */
        @Override
        public void disconnected() {
            toast("IOIO Lost");
            writelog("dk", filename);

        }

        /**
         * Called when the IOIO is connected, but has an incompatible firmware version.
         *
         *
         */
        @Override
        public void incompatible() {

        }
    }

    /**
     * A method to create our IOIO thread.
     *
     * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
     */
    @Override
    protected IOIOLooper createIOIOLooper() {
        return new Looper();
    }

    private void showVersions(IOIO ioio, String title) {
        toast(String.format("%s\n" +
                        "IOIOLib: %s\n" +
                        "Application firmware: %s\n" +
                        "Bootloader firmware: %s\n" +
                        "Hardware: %s",
                title,
                ioio.getImplVersion(IOIO.VersionType.IOIOLIB_VER),
                ioio.getImplVersion(IOIO.VersionType.APP_FIRMWARE_VER),
                ioio.getImplVersion(IOIO.VersionType.BOOTLOADER_VER),
                ioio.getImplVersion(IOIO.VersionType.HARDWARE_VER)));
    }
    private void toast(final String message) {
        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateViews() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView2_.setText(String.valueOf(echoDistanceCm));
                progressBar1_.setProgress(echoDistanceCm);
            }
        });
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

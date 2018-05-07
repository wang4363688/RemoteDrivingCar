package com.android.remotedriving;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
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

public class MainActivity extends IOIOActivity implements SurfaceHolder.Callback,
    Camera.PreviewCallback, Encode.IEncoderListener {

    private Camera camera;
    private boolean isPreview = false;
    private SurfaceView surfaceView;

    private static final String TAG = "MainActivity";
    private static final String VLC_HOST = "2001:da8:215:6a01::f651";
//    private static final String VLC_HOST = "10.112.57.170";
//    private static final String VLC_HOST = "2001:da8:215:6a01::b0ed";
    private static final int VLC_PORT = 5500;
    public int State = 0;
    int width = 320;
    int height = 240;
    private int displayOrientation = 90;
    private InetAddress address;
    private DatagramSocket socket;
    private ExecutorService executor;
    private Encode encode;
    public static String filename = "log_tcp";


    Handler mMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.obj.toString()) {
//                case Instructions.STATE_CONNECTED:
//                    toast("Connected Successfully");
//                    break;
                case Instructions.forward:
                    SetState(1);
                    break;
                case Instructions.back:
                    SetState(2);
                    break;
                case Instructions.left:
                    SetState(3);
                    break;
                case Instructions.right:
                    SetState(4);
                    break;
                case Instructions.brake:
                    SetState(0);
                    break;
                case Instructions.P:
                    break;
                case Instructions.R:
                    break;
                case Instructions.N:
                    break;
                case Instructions.D:
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

    TcpThread tcpThread=new TcpThread(mMessageHandler);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = (SurfaceView) findViewById(R.id.camera_surfaceview);
        surfaceView.getHolder().setFixedSize(width, height);
        surfaceView.getHolder().setKeepScreenOn(true);
        surfaceView.getHolder().addCallback(this);

        try {
            address = InetAddress.getByName(VLC_HOST);
            socket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        tcpThread.start();
        executor = Executors.newSingleThreadExecutor();


    }

    private void startPreview() {
        if (camera == null) {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        }

        try {
            Camera.Parameters parameters = camera.getParameters();
//            parameters.setPreviewFrameRate(15);//设置帧率
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
                encode = new Encode(previewSize.width, previewSize.height,
                        125000, 10, this);
            }

            camera.addCallbackBuffer(new byte[size]);
            camera.setPreviewDisplay(surfaceView.getHolder());
            camera.setDisplayOrientation(displayOrientation);
//            camera.setDisplayOrientation(90);
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

    class Looper extends BaseIOIOLooper {
        private DigitalOutput D1A, D1B, D2A, D2B;

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
            D1A = ioio_.openDigitalOutput(1, false);
            D1B = ioio_.openDigitalOutput(2, false);
            D2A = ioio_.openDigitalOutput(3, false);
            D2B = ioio_.openDigitalOutput(4, false);
//            executor = Executors.newSingleThreadExecutor();
            tcpThread.start();

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
        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            switch (State){
                case 1:
                    D1A.write(false);
                    D1B.write(false);
                    D2A.write(true);
                    D2B.write(false);
                    Thread.sleep(100);
                    break;
                case 2:
                    D1A.write(false);
                    D1B.write(false);
                    D2A.write(false);
                    D2B.write(true);
                    Thread.sleep(100);
                    break;
                case 3:
                    D1A.write(true);
                    D1B.write(false);
                    D2A.write(true);
                    D2B.write(false);
                    Thread.sleep(100);
                    break;
                case 4:
                    D1A.write(false);
                    D1B.write(true);
                    D2A.write(true);
                    D2B.write(false);
                    Thread.sleep(100);
                    break;
                case 0:
                    D1A.write(false);
                    D1B.write(false);
                    D2A.write(false);
                    D2B.write(false);
                    Thread.sleep(100);
                    break;
                default:
                    D1A.write(false);
                    D1B.write(false);
                    D2A.write(false);
                    D2B.write(false);
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

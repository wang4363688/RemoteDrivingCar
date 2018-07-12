package com.android.remotedriving;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by qbr on 2018/6/25.
 */

public class Welcome extends Activity {
    private Timer timer;
    private TimerTask task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_welcome);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        final Intent it = new Intent(Welcome.this, Selection.class);

        timer = new Timer();

        task = new TimerTask() {
            @Override
            public void run() {
                startActivity(it);
                finish();
            }
        };
        timer.schedule(task, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        task.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    1);}
    }

}

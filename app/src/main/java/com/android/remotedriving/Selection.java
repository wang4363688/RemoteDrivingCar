package com.android.remotedriving;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

/**
 * Created by qbr on 2018/6/25.
 */

public class Selection extends Activity implements View.OnClickListener{
    private ImageButton button_fleet, button_control;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);
        button_control = (ImageButton) findViewById(R.id.button_control);
        button_fleet = (ImageButton) findViewById(R.id.button_fleet);
        button_control.setOnClickListener(this);
        button_fleet.setOnClickListener(this);

    }
    // 新加 by wh
    public void Dialog(View view, final Class<?> cls) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请选择车辆");
        final String[] items = {"小红车", "黄车", "大红车"};
        //创建单选对话框
        //第一个参数:单选对话框中显示的条目所在的字符串数组
        //第二个参数:默认选择的条目的下标(-1表示默认没有选择任何条目)
        //第三个参数:设置事件监听
        builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            //which:用户所选的条目的下标；dialog:触发这个方法的对话框
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(
                        Selection.this,
                        "您选择的车辆是:" + items[which],
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();    //关闭对话框
                Intent intent = new Intent(Selection.this, cls);
                intent.putExtra("car",items[which]);  // 车型传给下一个activity
                startActivity(intent);
            }
        });
        builder.show();          //显示单选对话框
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button_control:
                Dialog(view, MainActivity.class);
                break;
            case R.id.button_fleet:
                Dialog(view, FleetActivity.class);
                break;
        }
    }

    /** 上次点击返回键的时间 */
    private long lastBackPressed;
    /** 两次点击的间隔时间 */
    private static final int QUIT_INTERVAL = 2000;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode== KeyEvent.KEYCODE_BACK && event.getRepeatCount()==0) {
            long backPressed = System.currentTimeMillis();
            if (backPressed - lastBackPressed > QUIT_INTERVAL) {
                lastBackPressed = backPressed;
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}

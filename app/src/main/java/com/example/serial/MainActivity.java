package com.example.serial;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.serial.serialPort.IUGComTaskEvent;
import com.example.serial.serialPort.UGComBean;
import com.example.serial.serialPort.UGComEventHandler;
import com.example.serial.serialPort.UGSerailManager;

public class MainActivity extends AppCompatActivity {
    private UGComEventHandler ugCommandHandler;
    private UGSerailManager serialJob;
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn = findViewById(R.id.btn);
        Button btn1 = findViewById(R.id.btn1);
        //
        serialJob = new UGSerailManager();
        serialJob.serialPortInit("/dev/ttyS4", 9600);

        serialJob.eventHandler.setUgComTaskEvent(new IUGComTaskEvent() {
            @Override
            public void onOpen(int address, int door) {
                Log.i(TAG, "开门：" + address + "-" + door);
            }

            @Override
            public void onClose(int address, int door) {
                Log.i(TAG, "关门：" + address + "-" + door);
            }

            @Override
            public void onStatus(int address, byte[] status) {
                Log.i(TAG, "状态：" + address);
            }

            @Override
            public void oVersion(String sv, String hw) {
                Log.i(TAG, "版本：");
            }

            @Override
            public void onError(int address, byte[] status) {
                Log.i(TAG, "错误：");
            }
        });
        //

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UGSerailManager.UGComTask task = serialJob.createTask();
                UGComBean bean = task.open(1, 9); //1号从机  第12路
                task.setComBean(bean);
                serialJob.excuteTask(task);
            }
        });

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }


}
package com.example.serial.serialPort;

import com.kongqw.serialportlibrary.SerialPortManager;

public interface ISerialManager<T> {

    //初始化
    void serialPortInit(String deviceName,int baudRate);

    //串口测试
    boolean serialPortTest();

    //串口接收数据的监听
    void setSerialPortDataListener();

}

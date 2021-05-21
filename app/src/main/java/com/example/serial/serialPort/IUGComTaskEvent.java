package com.example.serial.serialPort;

public interface IUGComTaskEvent {
    void onOpen(int address,int door); //开门响应

    void onClose(int address,int door); //关门响应

    void onStatus(int address,byte[] status); //查询锁状态响应

    void oVersion(String sv, String hw); //查询版本号响应

    void onError(int address,byte[] status); //故障响应
}

package com.example.serial.serialPort;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class UGComBean {
    public int start; // 帧头 ---1byte
    public int address; //从机地址 即某块锁板单片机的编号 ---1byte
    public int type; //帧类型   0 命令帧 1 响应帧 2 通知帧 ---1byte
    public int command; //指令代码：0xBD 串口 0x11 版本 0x13 查询 0x12 0x0004 开锁 ---1byte
    public int paramlen; //参数长度 ---2byte
    public byte[] param; //指令参数内容
    public int check; // 校验和 ---1byte
    public int end;//帧尾 --1byte
    public static final String TAG = "COM_BEAN";

    public UGComBean(byte[] data) {
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        try {
            this.start = ds.readUnsignedByte();
            this.address = ds.readUnsignedByte();
            this.type = ds.readUnsignedByte();
            this.command = ds.readUnsignedByte();
            this.paramlen = ds.readUnsignedShort();
            if (this.paramlen == 0) {
                this.param = new byte[0];
            } else {
                this.param = new byte[this.paramlen];
                ds.read(this.param, 0, this.paramlen);
            }
            this.check = ds.readUnsignedByte();
            this.end = ds.readUnsignedByte();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    // 发送数据 自动计算校验位和数据长度
    public UGComBean(int address, int command, byte[] param) {
        this.start = 0xAB;
        this.address = address;
        this.type = 0x00;
        this.command = command;
        this.paramlen = param.length;
        this.param = param;
        this.end = (byte) 0xBA;
    }

    //打包数据帧
    public byte[] pack() {
        try {
            ByteArrayOutputStream bin = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bin);
            out.writeByte(this.start);
            out.writeByte(this.address);
            out.writeByte(this.type);
            out.writeByte(this.command);
            out.writeShort(this.paramlen);
            out.write(this.param);
            byte[] bytCheck = bin.toByteArray();
            out.writeByte(CRC8(bytCheck));
            out.writeByte(this.end);
            return bin.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public static int CRC8(byte[] data) {
        int len = data.length;
        if (len > 0) {
            short crc = 0x0000;
            for (int i = 0; i < len; i++) {
                crc += data[i];
            }

            return crc & 0x00FF;
        }
        return 0;
    }

    public DataInputStream getParam() {
        return new DataInputStream(new ByteArrayInputStream(this.param));
    }
}

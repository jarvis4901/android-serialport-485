package com.example.serial.serialPort;

import android.annotation.SuppressLint;

import java.io.DataInputStream;

public class UGComEventHandler {

    private IUGComTaskEvent mEvent;

    public void setUgComTaskEvent(IUGComTaskEvent mEvent) {
        this.mEvent = mEvent;
    }


    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    //   响 应
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++


    /**
     * 查询版本信息响应
     *
     * @param bean
     */
    public void versionResp(UGComBean bean, DataInputStream ds) {
        try {
            @SuppressLint("DefaultLocale")
            String sv = String.format("V%.1f", (float) ds.readUnsignedByte() / 10);
            @SuppressLint("DefaultLocale")
            String hw = String.format("V%.1f", (float) ds.readUnsignedByte() / 10);
//            mcStatus.onVersion(sv, hw);
            mEvent.oVersion(sv, hw);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 开锁响应
     *
     * @param bean
     */
    public void openResp(UGComBean bean, DataInputStream ds) {
        try {
            int status = ds.readInt();
            for (byte i = 0; i < (bean.paramlen * 8); i++) {
                if (((status >> i) & 0x01) > 0) {
                    mEvent.onOpen(bean.address, i);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 关锁响应
     *
     * @param bean
     */
    public void closeResp(UGComBean bean, DataInputStream ds) {
        try {
            int status = ds.readInt();
            for (byte i = 0; i < (bean.paramlen * 8); i++) {
                if (((status >> i) & 0x01) == 0) {
                    mEvent.onClose(bean.address, i);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 查询锁状态响应
     *
     * @param bean
     */
    public void statusResp(UGComBean bean, DataInputStream ds) {
        try {
            byte[] ret = new byte[bean.paramlen * 8 - 1];
            int status = ds.readInt();
            for (byte i = 0; i < (bean.paramlen * 8); i++) {
                ret[i] = (byte) ((status >> i) & 0x01);
            }
            mEvent.onStatus(bean.address, ret);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 故障响应
     *
     * @param bean
     */
    public void errorResp(UGComBean bean, DataInputStream ds) {
        try {
            byte[] ret = new byte[bean.paramlen * 8 - 1];
            int status = ds.readInt();
            for (byte i = 0; i < (bean.paramlen * 8); i++) {
                ret[i] = (byte) ((status >> i) & 0x01);
            }
            mEvent.onError(bean.address, ret);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

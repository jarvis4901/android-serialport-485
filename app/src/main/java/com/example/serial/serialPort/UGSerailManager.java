package com.example.serial.serialPort;

import android.util.Log;

import com.kongqw.serialportlibrary.Device;
import com.kongqw.serialportlibrary.SerialPortFinder;
import com.kongqw.serialportlibrary.SerialPortManager;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UGSerailManager implements ISerialManager {
    public Boolean isConnected = false;  //是否已连接
    public Thread thread;  //线程
    public File device;
    public String deviceName;
    public int baudRate;
    public SerialPortManager serialPortManager = new SerialPortManager();
    public UGComEventHandler eventHandler;


    private ExecutorService mSingleThreadPool;
    //串口接受到的阻塞消息队列
    //用于判断发送一个指令后是否在3秒内收到响应帧，若未收到响应帧则表示指令执行失败
    private ArrayBlockingQueue respQueue;
    public ByteBuffer receiveData;                //串口数据缓冲区

    public static final String TAG = "UGSerailManager";

    public UGSerailManager() {
        respQueue = new ArrayBlockingQueue(5);
        //线程池管理
        //newSingleThreadExecutor保证线程池内同时只有一个活动的线程
        mSingleThreadPool = Executors.newSingleThreadExecutor();
        eventHandler = new UGComEventHandler();
    }

    /**
     * 开始一个任务
     *
     * @param task
     */
    public void excuteTask(UGComTask task) {
//        UGComTask task = new UGComTask();
        if (task == null || task.bean == null) {
            return;
        }
        mSingleThreadPool.execute(task);
    }

    /**
     * 创建一个任务
     *
     * @return
     */
    public UGComTask createTask() {
        return new UGComTask();
    }

    /**
     * 串口接收到数据 验证其有效性后 针对不同的command进行不同的响应处理
     * 有可能是通知帧也有可能是响应帧
     * 只有主动发送指令动作帧(如测试帧、开锁帧)后的响应帧需要进入respQueue中进行超时或异常处理  *1
     * 其它的都是被动想用不需要进入respQueue队列
     *
     * @param bean
     */
    public void commandDispatcher(UGComBean bean) {
        try {
            DataInputStream ds = bean.getParam();
            switch (bean.command) {
                case 0xBD: //测试帧响应
                    respQueue.add(bean);  //*1 进入respQueue队列
//                    eventHandler.closeResp(bean,ds);
                    break;
                case 0x11: //查看驱动板硬件程序版本响应
                    eventHandler.versionResp(bean, ds);
                    break;
                case 0x12://锁控制
                    if (bean.type == 0x01) {
                        //开锁响应
                        respQueue.add(bean); //*1 进入respQueue队列
                        eventHandler.openResp(bean, ds);
                    } else if (bean.type == 0x02) {
                        //关锁响应（通知帧）
                        eventHandler.closeResp(bean, ds);
                    }
                    break;
                case 0x13: //锁状态响应
                    eventHandler.statusResp(bean, ds);
                    break;
                case 0xEE: //故障
                    eventHandler.errorResp(bean, ds);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 监听串口接收到的数据
     */
    @Override
    public void setSerialPortDataListener() {
        serialPortManager.setOnSerialPortDataListener(new OnSerialPortDataListener() {
            @Override
            public void onDataReceived(byte[] bytes) {
                UGComBean bean = byteDataConvertToBean(bytes);
                if (bean != null) {
                    Log.e(TAG, "接收帧：" + bytesToHex(bean.pack()));
                    commandDispatcher(bean);
                    receiveData.clear();
                }
            }

            @Override
            public void onDataSent(byte[] bytes) {

            }
        });

    }


    /**
     * 检验接收到的数据的有效性，并转化为bean类 无效返回的是null 有效则返回bean
     *
     * @param bytes
     * @return UGComBean
     */
    private UGComBean byteDataConvertToBean(byte[] bytes) {
        try {
            receiveData.put(bytes);
            byte[] buffArray = receiveData.array();
            int buffLen = receiveData.position();
            ByteArrayInputStream bs = new ByteArrayInputStream(buffArray);
            DataInputStream ds = new DataInputStream(bs);
            if (buffLen < 8) {
                // 数据长度小于8 数据分包
                return null;
            }

            if (ds.readUnsignedByte() != 0xAB) {
                //检查帧头
                receiveData.clear();
                return null;
            }
            ds.readUnsignedByte(); //address
            ds.readUnsignedByte(); // 帧类型
            ds.readUnsignedByte(); //command
            int pamalen = ds.readUnsignedShort();
            if (pamalen > 10) {
                // 接收到的数据会不知道什么原因造成错误，长度值错误
                receiveData.clear();
                return null;
            }
            // 判断是否接收完整
            if (buffLen < pamalen + 8) {
                // 数据分包
                return null;
            }
            byte[] pama = new byte[pamalen];
            ds.read(pama, 0, pamalen);
            int cks = ds.readUnsignedByte();
            int end = ds.readUnsignedByte();
            // 验证帧尾
            if (end != 0xBA) {
                receiveData.clear();
                return null;
            }
            // 验证校验和
            byte[] bytCheck = new byte[pamalen + 6];
            System.arraycopy(buffArray, 0, bytCheck, 0, pamalen + 6); //复制字节数组数组
            if (UGComBean.CRC8(bytCheck) != cks) {
                receiveData.clear();
                return null;
            }
            return new UGComBean(buffArray);
        } catch (Exception e) {
            e.printStackTrace();
            receiveData.clear();
        }
        return null;
    }

    /**
     * 测试串口连接是否成功
     *
     * @return
     */
    @Override
    public boolean serialPortTest() {
        UGComBean comBean = new UGComBean(0x01, 0xBD, new byte[0]);
        try {
            boolean send = serialPortManager.sendBytes(comBean.pack());
            UGComBean bean = (UGComBean) respQueue.poll(3, TimeUnit.SECONDS);
            if (null == bean) {
                isConnected = false;
                serialPortManager.closeSerialPort();
            } else if (bean.command == 0xBD && bean.paramlen == 1) {
                //测试帧响应结果正确
                isConnected = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 串口初始化
     *
     * @param deviceName 设备名
     * @param baudRate   波特率
     */
    public void serialPortInit(String deviceName, int baudRate) {
        this.deviceName = deviceName;
        this.baudRate = baudRate;
//        this.serialPortManager =new SerialPortManager();
        this.receiveData = ByteBuffer.allocate(100);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //查看串口列表
                    SerialPortFinder serialPortFinder = new SerialPortFinder();
                    final ArrayList<Device> devices = serialPortFinder.getDevices();
                    for (int i = devices.size() - 1; i >= 0; i--) {
                        if (isConnected) {
                            //已经连接
                            continue;
                        }
                        device = devices.get(i).getFile();
                        String dn = device.getPath();
                        if (!deviceName.equals(dn)) {
                            continue;
                        }
                        boolean isOpen = serialPortManager.openSerialPort(device, baudRate);
                        if (isOpen) {
                            setSerialPortDataListener();

                            serialPortTest();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }


    private String bytesToHex(byte[] bytes) {
        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) { // 使用String的format方法进行转换
            buf.append(String.format("%02X ", b & 0xff));
        }
        return buf.toString();
    }


    /**
     * ##################
     * 发送数据的任务类
     * ##################
     */
    public class UGComTask implements Runnable {

        private UGComBean bean;

        public UGComTask() {
            //
        }

        //        public UGComTask(UGComBean bean) {
//            this.bean = bean;
//        }
//
        public void setComBean(UGComBean bean) {
            this.bean = bean;
        }


        //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //   执 行 动 作
        //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

        /**
         * 开锁
         *
         * @param address 锁板编号（从机地址）
         * @param door    锁孔编号（门编号）
         * @return
         */
        public UGComBean open(int address, int door) {
            byte[] data = new byte[4];
            try {
                ByteArrayOutputStream bin = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bin);
                out.writeInt(0x01 << (door - 1));
                data = bin.toByteArray();
                return new UGComBean(address, 0x12, data);
            } catch (IOException e) {
                //data = new byte[4];
            }
            return null;
        }

        /**
         * 开某一组（某一块锁版）的所有锁
         *
         * @param address 锁板编号（从机地址）
         * @return
         */
        public UGComBean openAll(int address) {
            try {
                //AB 01 00 12 0004 00 FF FF FF BF BA
                byte[] data = new byte[]{(byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff};

                return new UGComBean(address, 0x12, data);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * 查询硬件版本
         *
         * @return
         */
        public UGComBean version() {
            return new UGComBean(1, 0x11, new byte[0]);
        }

        /**
         * 查询某一块锁板的锁孔状态
         *
         * @param address 锁板编号（从机地址）
         * @return
         */
        public UGComBean status(int address) {
            return new UGComBean(address, 0x13, new byte[0]);
        }


        /**
         * 发送数据
         *
         * @param data
         * @return
         */
        public boolean sendData(byte[] data) {
            if (isConnected) {
                return serialPortManager.sendBytes(data);
                //todo:发送失败重新发送
            } else {
                if (!thread.isAlive()) {
                    //重新连接
                    serialPortInit(deviceName, baudRate);
                }
            }
            return false;
        }


        /**
         * 等待串口响应的数据
         * respQueue 为阻塞消息队列
         */
        private void waitResult() {
            try {
                //三秒内未接受到数据则视为发送的指令未能正确执行
                UGComBean bean = (UGComBean) respQueue.poll(3000, TimeUnit.MILLISECONDS);
                Log.i("task:", "收到消息：");
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("task:", "超时");
            }
        }

        @Override
        public void run() {
            sendData(bean.pack());
            waitResult();
            Log.e("task:", "结束");
        }
    }


}

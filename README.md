# android 485协议通讯 示例

##### 硬件端：

硬件为主控芯片为stm32的单片机，每块单片机上控制24路锁开关，可通过485外扩。

通讯协议为485标准协议，实现的功能有：某一路的开锁动作，监听某一路的锁关闭动作，查询某块板子的24路的所有锁的状态等

##### android端：

串口通讯库：https://github.com/cl-6666/serialPort

由于485半双工通讯模式的限制，必须保证同时只有一条指令下发给单片机

设想的实现结果是：在某条指令的响应帧到达android之后再进行下一条指令的下发，针对这一问题采用了当前的处理方案：

线程池newSingleThreadExecutor模式保证同时只有一个活动的线程，每一个指令的下发任务包装在一个线程类中，结合ArrayBlockingQueue阻塞队列实现对某条指令的响应监控，队列接收到消息后再进行下一个指令的下发

```java
//UGSerialManager.java

//发送指令后
respQueue.poll(3000, TimeUnit.MILLISECONDS);//respQueue为ArrayBlockingQueue 3s内respQueue内接收到响应消息则视为下发指令正常，没收到则视为异常
```



```java
//UGSerialManager.java

//串口监听数据回调
respQueue.add(bean); //相应数据进入到respQueue队列中
```


package com.whiteskycn.tv.projectorlauncher.common;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.util.Log;

import com.whiteskycn.tv.projectorlauncher.utils.SharedPreferencesUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.net.ssl.SSLContext;

import static com.whiteskycn.tv.projectorlauncher.home.model.HomeModel.INTENT_EXTRA_LOGIN;
import static com.whiteskycn.tv.projectorlauncher.home.model.HomeModel.INTENT_EXTRA_LOGIN_VALUE;
import static com.whiteskycn.tv.projectorlauncher.home.model.HomeModel.INTENT_LOGIN_URI;

/**
 * Created by lei on 17-11-14.
 */

public class MQTTService extends Service
{
    public final String TAG = this.getClass().getSimpleName();
    
    private MqttAndroidClient mMqttClient;
    private MqttConnectOptions mConnectOpt;
    
    private String protocol = "ssl://";
    private String host = "mqtt-sh1.whiteskycn.com";
    private String port = "1883";
    private String userName = null;
    private String passWord = null;
    private String mMqttTopic = "whiteskycn";
    private String clientId = "whiteskyClient";

    private boolean mMqttCleanSession = true;
    private boolean mMqttMsgRetained = false;
    private int mMqttTimeout_s = 10;
    private int mMqttHeartBeat_s = 20;
    private int mMqttQos = 1;
    private int mMqttReconnectDelay_ms = 100000;

    private final int MSG_NONE = 0;

    // mqtt network event
    private final int MSG_MQTT_RECONNECT = 100;

    // command
    private final int MSG_DEVICE_LOGIN_DONE = 200;
    private final int MSG_DEVICE_REBOOT = 201;
    private final int MSG_DEVICE_INSTALL_APK = 202;
    private final int MSG_DEVICE_REMOVE_APK = 203;
    private final int MSG_DEVICE_OTA_UPDATE = 204;

    // request
    private final int MSG_REQUEST_STATUS = 300;
    private final int MSG_REQUEST_APKLIST = 301;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (mMqttClient == null)
        {
            loadPropValue();
            mqttClientInit();
        }
        doMqttConnect();

        super.onStartCommand(intent, flags, startId);
        return START_STICKY;//保证服务被意外杀死后会重启
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        Log.i(TAG, "onDestroy");
        try
        {
            mMqttClient.disconnect();
            mMqttClient = null;
        }
        catch (MqttException e)
        {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void loadPropValue()
    {
        protocol = SystemProperties.get("persist.whitesky.mqtt.protocol", protocol);
        host = SystemProperties.get("persist.whitesky.mqtt.host", host);
        port = SystemProperties.get("persist.whitesky.mqtt.port", port);
        //todo
        //clientId = new String(WsdSerialnum.read()).toUpperCase();
        Log.d(TAG, "clientId = " + clientId);
        mMqttTopic = clientId;
    }
    
    private void mqttClientInit()
    {
        // 服务器地址（协议+地址+端口号）
        String uri = protocol + host + ":" + port;
        Log.d(TAG, "mqtt_uri = " + uri);
        mMqttClient = new MqttAndroidClient(this, uri, mMqttTopic);
        // 设置MQTT监听并且接受消息
        mMqttClient.setCallback(mqttCallback);

        mConnectOpt = new MqttConnectOptions();
        // 清除缓存
        mConnectOpt.setCleanSession(mMqttCleanSession);
        // 设置超时时间，单位：秒
        mConnectOpt.setConnectionTimeout(mMqttTimeout_s);
        // 心跳包发送间隔，单位：秒
        mConnectOpt.setKeepAliveInterval(mMqttHeartBeat_s);

        // 用户名
        if (userName != null) {
            mConnectOpt.setUserName(userName);
        }
        // 密码
        if (passWord != null) {
            mConnectOpt.setPassword(passWord.toCharArray());
        }

        if (protocol.contains("ssl"))
        {
            try
            {
                mConnectOpt.setSocketFactory(SSLContext.getDefault().getSocketFactory());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        // last will message
        long timeNow = System.currentTimeMillis() / 1000;
        String lastMessage = "This device is disconnected and the time when I connected successfully is " + timeNow;
        if (!lastMessage.isEmpty() && !mMqttTopic.isEmpty())
        {
            // 最后的遗嘱
            try
            {
                mConnectOpt.setWill(mMqttTopic, lastMessage.getBytes(), mMqttQos, mMqttMsgRetained);
            }
            catch (Exception e)
            {
                Log.i(TAG, "Exception Occured", e);
                iMqttActionListener.onFailure(null, e);
            }
        }
    }

    /** 连接MQTT服务器 */
    private void doMqttConnect()
    {
        if (mMqttClient!=null && !mMqttClient.isConnected() && isNetworkActive())
        {
            try
            {
                Log.i(TAG, "doMqttConnect . . .");
                mMqttClient.connect(mConnectOpt, null, iMqttActionListener);
            }
            catch (MqttException e)
            {
                e.printStackTrace();
            }
        }
        
    }

    private IMqttActionListener iMqttActionListener = new IMqttActionListener()
    {
        @Override
        public void onSuccess(IMqttToken arg0)
        {
            Log.i(TAG, "mqtt connect success");
            try
            {
                mMqttClient.subscribe(mMqttTopic, mMqttQos);
                mMqttClient.publish(mMqttTopic, "The connection is successful!".getBytes(), mMqttQos, mMqttMsgRetained);
            }
            catch (MqttException e)
            {
                e.printStackTrace();
            }
        }
        
        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1)
        {
            Log.i(TAG, "mqtt connect fail!!!");
            arg1.printStackTrace();

            Message msg = mqttHandler.obtainMessage();
            msg.what = MSG_MQTT_RECONNECT;
            mqttHandler.sendMessageDelayed(msg,mMqttReconnectDelay_ms);

        }
    };
    
    // MQTT监听并且接受消息
    private MqttCallback mqttCallback = new MqttCallback()
    {
        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage)
            throws Exception
        {
            
            String msg = new String(mqttMessage.getPayload());
            
            Log.i(TAG, "messageArrived:" + msg);
            Log.i(TAG, topic + ";mMqttQos:" + mqttMessage.getQos() + ";retained:" + mqttMessage.isRetained());

            parserMqttMessage(msg);
        }
        
        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0)
        {
            Log.i(TAG, "deliveryComplete:" + arg0);
        }
        
        @Override
        public void connectionLost(Throwable arg0)
        {
            //Log.e(TAG, "connectionLost:" + arg0.getMessage());
            //Log.e(TAG, arg0.getCause().toString() + arg0.getStackTrace());

            Message msg = mqttHandler.obtainMessage();
            msg.what = MSG_MQTT_RECONNECT;
            mqttHandler.sendMessageDelayed(msg,mMqttReconnectDelay_ms);
        }
    };
    
    /** 判断网络是否连接 */
    private boolean isNetworkActive()
    {
        ConnectivityManager connectivityManager =
            (ConnectivityManager)this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable())
        {
            String name = info.getTypeName();
            Log.i(TAG, "active network is：" + name);
            return true;
        }
        else
        {
            Log.i(TAG, "no active network on the device!");
            return false;
        }
    }

    public boolean publish(String topicName, byte[] payload, int qos) {
        boolean flag = false;
        if (mMqttClient != null && mMqttClient.isConnected()) {
            Log.d(TAG,"Publishing to topic \"" + topicName + "\" qos " + qos);
            // Create and configure a message
            MqttMessage message = new MqttMessage(payload);
            message.setQos(qos);
            // Send the message to the server, control is not returned until
            // it has been delivered to the server meeting the specified
            // quality of service.
            try {
                mMqttClient.publish(topicName, message);
                flag = true;
            } catch (MqttException e) {
            }
        }
        return flag;
    }

    public boolean publish(byte[] payload, int qos) {
        boolean flag = false;
        if (mMqttClient != null && mMqttClient.isConnected()) {
            Log.d(TAG,"Publishing to topic \"" + mMqttTopic + "\" qos " + qos);
            // Create and configure a message
            MqttMessage message = new MqttMessage(payload);
            message.setQos(qos);
            // Send the message to the server, control is not returned until
            // it has been delivered to the server meeting the specified
            // quality of service.
            try {
                mMqttClient.publish(mMqttTopic, message);
                flag = true;
            } catch (MqttException e) {
            }
        }
        return flag;
    }

    public boolean publish(String payload, int qos) {
        boolean flag = false;
        if (mMqttClient != null && mMqttClient.isConnected()) {
            Log.d(TAG,"Publishing to topic \"" + mMqttTopic + "\" qos " + qos);
            // Create and configure a message
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(qos);
            // Send the message to the server, control is not returned until
            // it has been delivered to the server meeting the specified
            // quality of service.
            try {
                mMqttClient.publish(mMqttTopic, message);
                flag = true;
            } catch (MqttException e) {
            }
        }
        return flag;
    }

    private void parserMqttMessage(String mqttMessage)
    {
        Message message = mqttHandler.obtainMessage();
        message.what = MSG_NONE;
        
        if (mqttMessage.indexOf("command") == 20)
        { // command 用于管理端向设备发送控制命令
            if (mqttMessage.contains("logindone"))
            {
                message.what = MSG_DEVICE_LOGIN_DONE;
            }
            else if (mqttMessage.contains("reboot"))
            {
                message.what = MSG_DEVICE_REBOOT;
            }
            else if (mqttMessage.contains("installapk"))
            {
                message.what = MSG_DEVICE_INSTALL_APK;
            }
            else if (mqttMessage.contains("removeapk"))
            {
                message.what = MSG_DEVICE_REMOVE_APK;
            }
            else if (mqttMessage.contains("otaupdate"))
            {
                message.what = MSG_DEVICE_OTA_UPDATE;
            }
            mqttHandler.sendMessage(message);
        }
        else if (mqttMessage.indexOf("request") == 20)
        { // request 用于管理端向设备发送请求
            if (mqttMessage.contains("status"))
            {
                message.what = MSG_REQUEST_STATUS;
            }
            else if (mqttMessage.contains("apklis"))
            {
                message.what = MSG_REQUEST_APKLIST;
            }
        }
        mqttHandler.sendMessage(message);
    }
    
    private Handler mqttHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MSG_MQTT_RECONNECT:
                    Log.i(TAG, "mqtt reconnect!");
                    doMqttConnect();
                    break;
                case MSG_DEVICE_LOGIN_DONE:
                    SharedPreferencesUtil shared = new SharedPreferencesUtil(getApplicationContext(), Contants.CONFIG);
                    shared.putBoolean(Contants.IS_ACTIVATE, true);
                    shared.putBoolean(Contants.IS_SETUP_PASS, true);
                    
                    Intent intent = new Intent(INTENT_LOGIN_URI);
                    intent.putExtra(INTENT_EXTRA_LOGIN, INTENT_EXTRA_LOGIN_VALUE);
                    sendBroadcast(intent);
                    break;
                case MSG_DEVICE_REBOOT:
                    PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
                    powerManager.reboot("mqtt request");
                    break;
                case MSG_DEVICE_INSTALL_APK:
                    break;
                case MSG_DEVICE_REMOVE_APK:
                    break;
                case MSG_DEVICE_OTA_UPDATE:
                    break;
                default:
                    Log.i(TAG, "unknown msg " + msg.what);
                    break;
            }
            super.handleMessage(msg);
        }
    };
}

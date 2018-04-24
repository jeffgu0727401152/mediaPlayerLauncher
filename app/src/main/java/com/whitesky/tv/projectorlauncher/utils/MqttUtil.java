package com.whitesky.tv.projectorlauncher.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;

import com.whitesky.tv.projectorlauncher.admin.DeviceInfoActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.net.ssl.SSLContext;

/**
 * Created by jeff on 18-2-13.
 */

public class MqttUtil {
    private final String TAG = this.getClass().getSimpleName();

    private final static String MQTT_DEFAULT_PROTOCOL = "ssl://";
    private final static String MQTT_DEFAULT_HOST = "mqtt-sh1.whiteskycn.com";
    private final static String MQTT_DEFAULT_PORT = "1883";
    private final static String MQTT_DEFAULT_TOPIC = "whiteskyClient";

    private static MqttUtil instance;

    private MqttAndroidClient mClient;
    private MqttConnectOptions mConOpt;

    private String protocol = MQTT_DEFAULT_PROTOCOL;
    private String host = MQTT_DEFAULT_HOST;
    private String port = MQTT_DEFAULT_PORT;
    private String userName = "";
    private String passWord = "";
    private String mTopic = MQTT_DEFAULT_TOPIC;
    private String clientId = MQTT_DEFAULT_TOPIC;

    private boolean mMqttCleanSession = true;
    private boolean mMqttMsgRetained = false;
    private int mMqttTimeout_s = 10;
    private int mMqttHeartBeat_s = 60;
    private int mMqttRetryDelay_ms = 20*1000;
    private int mMqttMsgQos = 0;

    public interface MqttMessageCallback
    {
        public void onDistributeMessage(String msg);
    }

    private MqttMessageCallback mMsgCallback = null;

    // mqtt network event
    private final int MQTT_RECONNECT = 0;


    public void setMsgCallBack(MqttMessageCallback cb)
    {
        mMsgCallback = cb;
    }

    private MqttUtil(Context context) {
        clientId = DeviceInfoActivity.getSysSn(context);
        loadPropValue();
        mqttClientInit(context);
    }

    public static MqttUtil getInstance(Context context) {
        if (null == instance) {
            synchronized (MqttUtil.class) {
                instance = new MqttUtil(context);
            }
        }
        return instance;
    }

    /**
     * 连接服务器
     * MqttService有自己的重连机制，在断线情况下会重连，但是首次连接失败后，需要再调用connect方法
     */
    public void connect() {
        if (mClient !=null && !mClient.isConnected()) {
            try {
                mClient.connect(mConOpt, null, iMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG,"already connected!");
        }
    }

    public void disConnect() {
        if (null == mClient) {
            return;
        }
        try {
            mClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "disconnected");
    }

    private void loadPropValue()
    {
        protocol = SystemProperties.get("oemdata.whitesky.mqtt.protocol", MQTT_DEFAULT_PROTOCOL);
        host = SystemProperties.get("oemdata.whitesky.mqtt.host", MQTT_DEFAULT_HOST);
        port = SystemProperties.get("oemdata.whitesky.mqtt.port", MQTT_DEFAULT_PORT);
        mTopic = clientId;

        Log.d(TAG, "clientId = " + clientId);
    }

    private void mqttClientInit(Context context) {
        String uri = protocol + host + ":" + port;
        Log.d(TAG, "uri = " + uri);

        mClient = new MqttAndroidClient(context, uri, clientId);
        mClient.setCallback(mqttCallback);

        mConOpt = new MqttConnectOptions();
        mConOpt.setCleanSession(mMqttCleanSession);
        mConOpt.setConnectionTimeout(mMqttTimeout_s);
        mConOpt.setKeepAliveInterval(mMqttHeartBeat_s);
        mConOpt.setAutomaticReconnect(true);

        if (!userName.isEmpty() && !passWord.isEmpty()) {
            mConOpt.setUserName(userName);
            mConOpt.setPassword(passWord.toCharArray());
        }

        if (protocol.contains("ssl")) {
            try {
                mConOpt.setSocketFactory(SSLContext.getDefault().getSocketFactory());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String lastMessage = "device(" + clientId + ") disconnected";
        if (!lastMessage.isEmpty() && !mTopic.isEmpty()) {
            try {
                mConOpt.setWill(mTopic, lastMessage.getBytes(), mMqttMsgQos, mMqttMsgRetained);
            } catch (Exception e) {
                Log.i(TAG, "Exception Occured", e);
                iMqttActionListener.onFailure(null, e);
            }
        }
    }

    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken arg0) {
            Log.i(TAG, "connect success");
            try {
                mClient.subscribe(mTopic, mMqttMsgQos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            Log.i(TAG, "connect fail!");
            arg1.printStackTrace();

            Message msg = mqttUtilHandler.obtainMessage();
            msg.what = MQTT_RECONNECT;
            mqttUtilHandler.sendMessageDelayed(msg, mMqttRetryDelay_ms);
        }
    };

    // MQTT监听并且接受消息
    private MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage) {
            String msg = new String(mqttMessage.getPayload());
            Log.d(TAG, "messageArrived:" + msg);
            if (mMsgCallback!=null) {
                mMsgCallback.onDistributeMessage(msg);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {
            Log.d(TAG, "deliveryComplete:" + arg0);
        }

        @Override
        public void connectionLost(Throwable arg0) {
            Message msg = mqttUtilHandler.obtainMessage();
            msg.what = MQTT_RECONNECT;
            mqttUtilHandler.sendMessageDelayed(msg, mMqttRetryDelay_ms);
        }
    };

    public boolean publish(String topic, byte[] payload, int qos) {
        boolean flag = false;
        if (mClient != null && mClient.isConnected()) {
            Log.d(TAG,"Publishing to topic \"" + topic + "\" qos " + qos);
            MqttMessage message = new MqttMessage(payload);
            message.setQos(qos);
            try {
                mClient.publish(topic, message);
                flag = true;
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        return flag;
    }

    public boolean publish(byte[] payload, int qos) {
        boolean flag = false;
        if (mClient != null && mClient.isConnected()) {
            Log.d(TAG,"Publishing to topic \"" + mTopic + "\" qos " + qos);
            MqttMessage message = new MqttMessage(payload);
            message.setQos(qos);
            try {
                mClient.publish(mTopic, message);
                flag = true;
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        return flag;
    }

    public boolean publish(String payload, int qos) {
        boolean flag = false;
        if (mClient != null && mClient.isConnected()) {
            Log.d(TAG,"Publishing to topic \"" + mTopic + "\" qos " + qos);
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(qos);
            try {
                mClient.publish(mTopic, message);
                flag = true;
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        return flag;
    }

    private Handler mqttUtilHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MQTT_RECONNECT:
                    Log.i(TAG, "mqtt reconnect!");
                    connect();
                    break;
                default:
                    Log.i(TAG, "unknown msg " + msg.what);
                    break;
            }
            super.handleMessage(msg);
        }
    };
}

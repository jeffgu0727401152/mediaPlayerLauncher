package com.whiteskycn.tv.projectorlauncher.common;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import com.whiteskycn.tv.projectorlauncher.utils.MqttUtil;
import com.whiteskycn.tv.projectorlauncher.utils.SharedPreferencesUtil;
import com.whiteskycn.wsd.android.NativeCertification;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * Created by lei on 17-12-1.
 */

public class MqttSslService extends Service implements MqttUtil.MqttMessageCallback
{
    public static final String INTENT_LOGIN_URI = "com.whiteskycn.tv.login";

    private final static int MSG_NONE = 0;
    // command
    private final static int MSG_DEVICE_LOGIN_DONE = 200;
    private final static int MSG_DEVICE_REBOOT = 201;
    private final static int MSG_DEVICE_INSTALL_APK = 202;
    private final static int MSG_DEVICE_REMOVE_APK = 203;
    private final static int MSG_DEVICE_OTA_UPDATE = 204;
    // request
    private final static int MSG_REQUEST_STATUS = 300;
    private final static int MSG_REQUEST_APKLIST = 301;

    private final String TAG = this.getClass().getSimpleName();
    private final String keyStorePassword = "wxgh#2561";


    // MQTT Util callback begin
    @Override
    public void onDistributeMessage(String msg) {
        Log.d("TAG","on distributeMessage:" + msg);
        parserMqttMessage(msg);
        // todo 使用eventbus
    }
    // MQTT Util callback end


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        initSSL();
        MqttUtil.getInstance(this).setMsgCallBack(this);
        MqttUtil.getInstance(this).connect();
        return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
    
    private void initSSL() {
        try {
            byte[] bufferPkcs12 = NativeCertification.getPkcs12();
            if (bufferPkcs12.length > 0) {
                InputStream kmin = new ByteArrayInputStream(bufferPkcs12);
                KeyStore kmkeyStore = KeyStore.getInstance("PKCS12");
                kmkeyStore.load(kmin, keyStorePassword.toCharArray());
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
                kmf.init(kmkeyStore, keyStorePassword.toCharArray());

                // Create an SSLContext that uses our TrustManager
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(kmf.getKeyManagers(), null, null);

                SSLContext.setDefault(context);
                Log.d(TAG, "init SSLContext done!");
            } else {
                Log.e(TAG,"init SSLContext fail!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parserMqttMessage(String mqttMessage)
    {
        Message message = mqttUtilHandler.obtainMessage();
        message.what = MSG_NONE;

        if (mqttMessage.indexOf("command") == 20) { // command 用于管理端向设备发送控制命令
            if (mqttMessage.contains("logindone")) {
                message.what = MSG_DEVICE_LOGIN_DONE;

            } else if (mqttMessage.contains("reboot")) {
                message.what = MSG_DEVICE_REBOOT;

            } else if (mqttMessage.contains("installapk")) {
                message.what = MSG_DEVICE_INSTALL_APK;

            } else if (mqttMessage.contains("removeapk")) {
                message.what = MSG_DEVICE_REMOVE_APK;

            } else if (mqttMessage.contains("otaupdate")) {
                message.what = MSG_DEVICE_OTA_UPDATE;
            }

        } else if (mqttMessage.indexOf("request") == 20) { // request 用于管理端向设备发送请求
            if (mqttMessage.contains("status")) {
                message.what = MSG_REQUEST_STATUS;

            } else if (mqttMessage.contains("apklis")) {
                message.what = MSG_REQUEST_APKLIST;
            }
        }

        mqttUtilHandler.sendMessage(message);
    }

    private Handler mqttUtilHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MSG_DEVICE_LOGIN_DONE:
                    Log.i(TAG, "mqtt login done!");
                    SharedPreferencesUtil shared = new SharedPreferencesUtil(getApplicationContext(), Contants.CONFIG);
                    shared.putBoolean(Contants.IS_ACTIVATE, true);
                    shared.putBoolean(Contants.IS_SETUP_PASS, true);

                    Intent intent = new Intent(INTENT_LOGIN_URI);
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

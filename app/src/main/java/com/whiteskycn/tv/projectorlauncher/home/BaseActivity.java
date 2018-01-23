package com.whiteskycn.tv.projectorlauncher.home;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.support.annotation.Nullable;
import android.util.Log;

import com.whiteskycn.tv.projectorlauncher.common.InitSSLSocketFactory;

import static com.whiteskycn.tv.projectorlauncher.home.model.HomeModel.INTENT_EXTRA_LOGIN;
import static com.whiteskycn.tv.projectorlauncher.home.model.HomeModel.INTENT_EXTRA_LOGIN_VALUE;
import static com.whiteskycn.tv.projectorlauncher.home.model.HomeModel.INTENT_LOGIN_URI;


/**
 * Created by xiaoxuan on 2017/12/12.
 */
public class BaseActivity extends Activity implements MqttMessageCallback {
    private final String TAG = this.getClass().getSimpleName();

    public final String IS_POWER_ON_PROPERTY = "whitesky.launcher.1stinit";

    public final String IS_TVAPK_OPENED_PROPERTY = "persist.whitesky.tvapk.isopened";

    private MqttRequestReceiver mMqttReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //SystemClock.setCurrentTimeMillis(1514736000000L);
        startService(new Intent(getApplicationContext(), InitSSLSocketFactory.class));

        // 注册接受mqttservice传来的intent广播接收器
        mMqttReceiver = new MqttRequestReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(INTENT_LOGIN_URI);
        registerReceiver(mMqttReceiver, intentFilter);
        mMqttReceiver.setMsgCallback(this);

        if (isFirstPowerOn() && SystemProperties.getBoolean(IS_TVAPK_OPENED_PROPERTY, false)) {
            new Handler().post(new Runnable() {
                public void run() {
                    ComponentName componentName =
                            new ComponentName("com.mstar.tv.tvplayer.ui", "com.mstar.tv.tvplayer.ui.RootActivity");
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    intent.setComponent(componentName);
                    intent.putExtra("isPowerOn", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(intent);
                }
            });
        }
    }

    public boolean isFirstPowerOn() {
        if (!SystemProperties.getBoolean(IS_POWER_ON_PROPERTY, false)) {
            SystemProperties.set(IS_POWER_ON_PROPERTY, "true");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void distributeMessage(String msg) {
        if (msg.equals(INTENT_EXTRA_LOGIN_VALUE)) {
            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
            // 如果Activity已经运行到了Task，再次跳转不会在运行这个Activity xiaxoaun 2017.12.13
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else {
            Log.d(TAG,"no case to handle " + msg);
        }

    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(mMqttReceiver);
        super.onDestroy();
    }

    private class MqttRequestReceiver extends BroadcastReceiver {
        private MqttMessageCallback msgCallback;

        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra(INTENT_EXTRA_LOGIN);
            if (msgCallback!=null) {
                msgCallback.distributeMessage(msg);
            }
        }

        public void setMsgCallback(MqttMessageCallback callback) {
            this.msgCallback = callback;
        }
    }
}

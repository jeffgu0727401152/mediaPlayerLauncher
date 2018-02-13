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

import com.whiteskycn.tv.projectorlauncher.common.MqttSslService;

import static com.whiteskycn.tv.projectorlauncher.common.MqttSslService.INTENT_LOGIN_URI;


/**
 * Created by xiaoxuan on 2017/12/12.
 */
public class BaseActivity extends Activity {
    private final String TAG = this.getClass().getSimpleName();

    public final String IS_POWER_ON_PROPERTY = "whitesky.launcher.1stinit";

    public final String IS_TVAPK_OPENED_PROPERTY = "persist.whitesky.tvapk.isopened";

    private MqttRequestReceiver mMqttReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //SystemClock.setCurrentTimeMillis(1514736000000L);
        startService(new Intent(getApplicationContext(), MqttSslService.class));

        // 注册接受mqttservice传来的intent广播接收器
        mMqttReceiver = new MqttRequestReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(INTENT_LOGIN_URI);
        registerReceiver(mMqttReceiver, intentFilter);

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
    protected void onDestroy() {
        this.unregisterReceiver(mMqttReceiver);
        super.onDestroy();
    }

    private class MqttRequestReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(INTENT_LOGIN_URI)) {
                Intent homeIntent = new Intent(getApplicationContext(), HomeActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(homeIntent);
            } else {
                Log.d(TAG,"no case to handle " + action);
            }
        }
    }
}

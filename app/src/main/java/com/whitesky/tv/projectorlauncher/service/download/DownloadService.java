package com.whitesky.tv.projectorlauncher.service.download;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;


/**
 * Created by jeff on 18-3-13.
 */

public class DownloadService extends Service {
    public static final String ACTION_START = "com.whitesky.tv.DOWNLOAD_START";
    public static final String ACTION_STOP = "com.whitesky.tv.DOWNLOAD_STOP";
    public static final String ACTION_UPDATE = "com.whitesky.tv.DOWNLOAD_UPDATE";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("TAG", intent.getAction().toString());
        Log.i("action", intent.getAction().toString());
        //获得activity传来的参数
        if (ACTION_START.equals(intent.getAction())) {
            Toast.makeText(getApplicationContext(),"123",Toast.LENGTH_SHORT).show();
        } else if (ACTION_STOP.equals(intent.getAction())) {
            Toast.makeText(getApplicationContext(),"999",Toast.LENGTH_SHORT).show();

        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}


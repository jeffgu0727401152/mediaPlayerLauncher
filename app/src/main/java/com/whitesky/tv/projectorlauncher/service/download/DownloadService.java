package com.whitesky.tv.projectorlauncher.service.download;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.application.MainApplication;
import com.whitesky.tv.projectorlauncher.common.Contants;
import com.whitesky.tv.projectorlauncher.home.HomeActivity;
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBeanDao;


/**
 * Created by jeff on 18-3-13.
 */

public class DownloadService extends Service {
    private static final String TAG = DownloadService.class.getSimpleName();

    private final static int DOWNLOAD_SERVICE_ID = 1002;

    public static final String ACTION_DOWNLOAD_START = "com.whitesky.tv.DOWNLOAD_START";
    public static final String ACTION_DOWNLOAD_PAUSE = "com.whitesky.tv.DOWNLOAD_PAUSE";
    public static final String ACTION_DOWNLOAD_CANCEL = "com.whitesky.tv.DOWNLOAD_CANCEL";

    @Override
    public void onCreate() {
        //前台通知提高优先级
        Intent notificationIntent = new Intent(this,HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0);

        Notification.Builder builer = new Notification.Builder(this);
        builer.setContentTitle("Download Service");     //设置通知的标题
        builer.setContentText("service running...");    //设置通知的内容
        builer.setSmallIcon(R.mipmap.ic_launcher);      //设置通知的图标
        builer.setContentIntent(pendingIntent);         //设置点击通知后的操作

        Notification notification = builer.build();
        startForeground(DOWNLOAD_SERVICE_ID, notification);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //获得activity传来的参数
        if (ACTION_DOWNLOAD_START.equals(intent.getAction())) {

            Toast.makeText(getApplicationContext(),"ACTION_DOWNLOAD_START",Toast.LENGTH_SHORT).show();

            String path = intent.getStringExtra("path");
            MediaBean needDownload = new MediaBeanDao(getApplicationContext()).queryById(path);
            DownloadManager.getInstance(getApplicationContext()).registerObserver(needDownload.getUrl(), mCallback);

            Log.d(TAG,"needDownload:"+needDownload.toString());

            DownloadManager.getInstance(getApplicationContext()).download(needDownload);

        } else if (ACTION_DOWNLOAD_PAUSE.equals(intent.getAction())) {

            Toast.makeText(getApplicationContext(),"ACTION_DOWNLOAD_PAUSE",Toast.LENGTH_SHORT).show();

        } else if (ACTION_DOWNLOAD_CANCEL.equals(intent.getAction())) {

            Toast.makeText(getApplicationContext(),"ACTION_DOWNLOAD_CANCEL", Toast.LENGTH_SHORT).show();

        }

        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy()
    {
        DownloadManager.getInstance(getApplicationContext()).removeAllObserver();
        super.onDestroy();

        Log.d(TAG,"service onDestroy auto restart!");
        Intent localIntent = new Intent();
        localIntent.setClass(this, DownloadService.class);
        this.startService(localIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendIntentToActivityIfForeground(MediaBean bean) {
        if (((MainApplication)getApplication()).isMediaActivityForeground) {
            Log.w(TAG,"send a intent to MediaActivity to update UI");
            Intent intent=new Intent(Contants.ACTION_DOWNLOAD_STATE_UPDATE);
            Bundle bundle = new Bundle();
            bundle.putParcelable(Contants.EXTRA_DOWNLOAD_STATE_CONTEXT, bean);
            intent.putExtras(bundle);
            sendBroadcast(intent);
        } else {
            Log.w(TAG,"MediaActivity is not foreground, do nothing");
        }
    }

    private DownloadCallback mCallback = new DownloadCallback();

    private class DownloadCallback implements DownloadObserver {
        @Override
        public void onStop(MediaBean bean) {
            Log.d(TAG,"onStop");
        }

        @Override
        public void onStart(MediaBean bean) {
            Log.d(TAG,"onStart");
            sendIntentToActivityIfForeground(bean);
        }

        @Override
        public void onProgress(MediaBean bean) {
            Log.d(TAG,"onProgress " + bean.getDownloadProgress());
            sendIntentToActivityIfForeground(bean);
        }

        @Override
        public void onPrepare(MediaBean bean) {
            Log.d(TAG,"onPrepare");
            sendIntentToActivityIfForeground(bean);
        }

        @Override
        public void onFinish(MediaBean bean) {
            Log.d(TAG,"onFinish");
            sendIntentToActivityIfForeground(bean);
        }

        @Override
        public void onError(MediaBean bean) {
            Log.d(TAG,"onError");
            sendIntentToActivityIfForeground(bean);
        }

        @Override
        public void onDelete(MediaBean bean) {
            Log.d(TAG,"onDelete");
        }

        @Override
        public void onEnqueue(MediaBean bean) {
            Log.d(TAG,"onEnqueue");
        }
    }
}


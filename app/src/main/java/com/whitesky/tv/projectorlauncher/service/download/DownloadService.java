package com.whitesky.tv.projectorlauncher.service.download;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.application.MainApplication;
import com.whitesky.tv.projectorlauncher.common.Contants;
import com.whitesky.tv.projectorlauncher.home.HomeActivity;
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBeanDao;
import com.whitesky.tv.projectorlauncher.utils.MediaScanUtil;


/**
 * Created by jeff on 18-3-13.
 */

public class DownloadService extends Service {
    private static final String TAG = DownloadService.class.getSimpleName();

    private final static int DOWNLOAD_SERVICE_ID = 1002;

    public static final String ACTION_DOWNLOAD_START = "com.whitesky.tv.DOWNLOAD_START";
    public static final String ACTION_DOWNLOAD_PAUSE = "com.whitesky.tv.DOWNLOAD_PAUSE";
    public static final String ACTION_DOWNLOAD_START_PAUSE = "com.whitesky.tv.DOWNLOAD_START_PAUSE";
    public static final String ACTION_DOWNLOAD_CANCEL = "com.whitesky.tv.DOWNLOAD_CANCEL";

    MediaScanUtil downloadFileDurationScanner = new MediaScanUtil();

    @Override
    public void onCreate() {
        //前台通知提高优先级
        Intent notificationIntent = new Intent(this,HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("Download Service");     //设置通知的标题
        builder.setContentText("service running...");    //设置通知的内容
        builder.setSmallIcon(R.mipmap.ic_launcher);      //设置通知的图标
        builder.setContentIntent(pendingIntent);         //设置点击通知后的操作

        Notification notification = builder.build();
        startForeground(DOWNLOAD_SERVICE_ID, notification);

        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetReceiver, mFilter);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String path = intent.getStringExtra("path");
        if (ACTION_DOWNLOAD_START.equals(intent.getAction())) {

            MediaBean bean = new MediaBeanDao(getApplicationContext()).queryByPath(path);
            DownloadManager.getInstance().download(bean,mCallback);
            Log.d(TAG,"download:"+bean.toString());

        } else if (ACTION_DOWNLOAD_PAUSE.equals(intent.getAction())) {

            MediaBean bean = new MediaBeanDao(getApplicationContext()).queryByPath(path);
            DownloadManager.getInstance().pause(bean);
            Log.d(TAG,"pause:"+bean.toString());

        } else if (ACTION_DOWNLOAD_START_PAUSE.equals(intent.getAction())) {

            MediaBean bean = new MediaBeanDao(getApplicationContext()).queryByPath(path);
            if (bean.getDownloadState()==MediaBean.STATE_DOWNLOAD_NONE
                    || bean.getDownloadState() == MediaBean.STATE_DOWNLOAD_PAUSED
                    || bean.getDownloadState() == MediaBean.STATE_DOWNLOAD_ERROR) {
                DownloadManager.getInstance().download(bean, mCallback);
                Log.d(TAG,"download:"+bean.toString());
            } else {
                DownloadManager.getInstance().pause(bean);
                Log.d(TAG,"pause:"+bean.toString());
            }

        } else if (ACTION_DOWNLOAD_CANCEL.equals(intent.getAction())) {

            MediaBean bean = new MediaBeanDao(getApplicationContext()).queryByPath(path);
            DownloadManager.getInstance().cancel(bean);
            Log.d(TAG,"cancel:"+bean.toString());
        }

        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(mNetReceiver);
        Log.d(TAG,"service onDestroy auto restart!");
        Intent localIntent = new Intent();
        localIntent.setClass(this, DownloadService.class);
        this.startService(localIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendResultToActivity(MediaBean bean) {
        new MediaBeanDao(getApplicationContext()).createOrUpdate(bean);
        if (((MainApplication)getApplication()).isMediaActivityForeground) {
            Intent intent=new Intent(Contants.ACTION_DOWNLOAD_STATE_UPDATE);
            Bundle bundle = new Bundle();
            bundle.putParcelable(Contants.EXTRA_DOWNLOAD_STATE_CONTEXT, bean);
            intent.putExtras(bundle);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        } else {
            //Log.w(TAG,"MediaActivity is not foreground, do nothing");
        }
    }

    private BroadcastReceiver mNetReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                Log.i(TAG, "network status change!");
                ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
                if(info != null && info.isAvailable()) {
                    String name = info.getTypeName();
                    Log.d(TAG, "resume all download, network on " + name);
                    for (MediaBean tmp : new MediaBeanDao(getApplicationContext()).selectItemsDownloading()) {
                        DownloadManager.getInstance().download(tmp,mCallback);
                    }
                } else {
                    Log.d(TAG, "no network!");
                }
            }
        }
    };

    private DownloadObserver mCallback = new DownloadObserver();

    private class DownloadObserver implements DownloadCallback {

        @Override
        public void onStateChange(MediaBean bean) {
            Log.d(TAG,bean.getUrl() + " onStateChange");
            sendResultToActivity(bean);
        }

        @Override
        public void onProgress(MediaBean bean) {
            Log.d(TAG,bean.getUrl() + " onProgress " + bean.getDownloadProgress());
            sendResultToActivity(bean);
        }

        @Override
        public void onFinish(MediaBean bean) {
            Log.d(TAG,bean.getUrl() + " onFinish");
            bean.setDuration(downloadFileDurationScanner.getMediaDuration(bean.getPath()));
            sendResultToActivity(bean);
        }

        @Override
        public void onError(MediaBean bean) {
            Log.d(TAG,bean.getUrl() + " onError");
            sendResultToActivity(bean);

            ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
            if(info != null && info.isAvailable()) {
                String name = info.getTypeName();
                Log.d(TAG, bean.getUrl() + "network is ok, retry download now!");
                DownloadManager.getInstance().download(bean,mCallback);
            }
        }
    }
}


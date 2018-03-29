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
import com.whitesky.tv.projectorlauncher.admin.DeviceInfoActivity;
import com.whitesky.tv.projectorlauncher.application.MainApplication;
import com.whitesky.tv.projectorlauncher.common.Contants;
import com.whitesky.tv.projectorlauncher.home.HomeActivity;
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBeanDao;
import com.whitesky.tv.projectorlauncher.utils.AppUtil;
import com.whitesky.tv.projectorlauncher.utils.FileUtil;
import com.whitesky.tv.projectorlauncher.utils.MediaScanUtil;
import com.whitesky.tv.projectorlauncher.utils.PathUtil;
import com.whitesky.tv.projectorlauncher.utils.ShellUtil;
import com.whitesky.tv.projectorlauncher.utils.ToastUtil;

import java.io.File;
import java.util.List;

import static com.whitesky.tv.projectorlauncher.common.Contants.UPDATE_APK_DOWNLOAD_PATH;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_UNKNOWN;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_NONE;


/**
 * Created by jeff on 18-3-13.
 */

public class DownloadService extends Service {
    private static final String TAG = DownloadService.class.getSimpleName();
    private final static int DOWNLOAD_SERVICE_ID = 1002;

    public static final int APK_SIZE_MAX = 100*1024*1024; // 限制apk大小100m，防止/mnt/sdcard/下空间不够

    public static final String ACTION_MEDIA_DOWNLOAD_START = "com.whitesky.tv.MEDIA_DOWNLOAD_START";
    public static final String ACTION_MEDIA_DOWNLOAD_PAUSE = "com.whitesky.tv.MEDIA_DOWNLOAD_PAUSE";
    public static final String ACTION_MEDIA_DOWNLOAD_CANCEL = "com.whitesky.tv.MEDIA_DOWNLOAD_CANCEL";
    public static final String ACTION_MEDIA_DOWNLOAD_START_PAUSE = "com.whitesky.tv.MEDIA_DOWNLOAD_START_PAUSE";
    public static final String ACTION_MEDIA_DOWNLOAD_CANCEL_ALL = "com.whitesky.tv.MEDIA_DOWNLOAD_CANCEL_ALL";

    public static final String ACTION_APK_DOWNLOAD_START = "com.whitesky.tv.APK_DOWNLOAD_START";
    public static final String ACTION_APK_DOWNLOAD_CANCEL = "com.whitesky.tv.APK_DOWNLOAD_CANCEL";

    public static final String EXTRA_KEY_URL = "extra.com.whitesky.tv.url";

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

        String url = intent.getStringExtra(EXTRA_KEY_URL);

        if (ACTION_MEDIA_DOWNLOAD_CANCEL_ALL.equals(intent.getAction())) {
            DownloadManager.getInstance().cancelAll();

        } else if (ACTION_APK_DOWNLOAD_START.equals(intent.getAction())) {
            DownloadManager.getInstance().pauseAll();

            MediaBean apk = new MediaBean("ota.apk",0, MEDIA_UNKNOWN, MediaBean.SOURCE_CLOUD_FREE, UPDATE_APK_DOWNLOAD_PATH,0,0L);
            apk.setUrl(url);
            apk.setDownloadState(STATE_DOWNLOAD_NONE);

            // 因为下载apk的不会储存数据库,关机再启动不会继续任务,所以假如上次下载到一半,则磁盘上存在一个一半的下载临时文件
            // 如果一年半载后服务器上的apk已经更新,这个时候推送更新会导致文件断点续传机制触发,下载下来的文件是错误的
            // 所以下载apk之前首先要检查目前存在不存在任务,如果不存在任务,则直接删除临时文件一次.
            if (DownloadManager.getInstance().contains(apk.getUrl())) {
                PathUtil.getDownloadTempFileByUrl(new File(apk.getPath()).getParent(), apk.getUrl()).delete();
            }

            DownloadManager.getInstance().download(apk, mApkDownloadCallback);
            Log.d(TAG,"download:"+apk.toString());

        } else if (ACTION_MEDIA_DOWNLOAD_START.equals(intent.getAction())) {

            MediaBean bean = queryMediaBeanByUrl(url);
            DownloadManager.getInstance().download(bean, mMediaDownloadCallback);
            Log.d(TAG,"download:"+bean.toString());

        } else if (ACTION_MEDIA_DOWNLOAD_PAUSE.equals(intent.getAction())) {

            MediaBean bean = queryMediaBeanByUrl(url);
            DownloadManager.getInstance().pause(bean);
            Log.d(TAG,"pause:"+bean.toString());

        } else if (ACTION_MEDIA_DOWNLOAD_START_PAUSE.equals(intent.getAction())) {

            MediaBean bean = queryMediaBeanByUrl(url);
            if (bean.getDownloadState()==MediaBean.STATE_DOWNLOAD_NONE
                    || bean.getDownloadState() == MediaBean.STATE_DOWNLOAD_PAUSED
                    || bean.getDownloadState() == MediaBean.STATE_DOWNLOAD_ERROR) {
                DownloadManager.getInstance().download(bean, mMediaDownloadCallback);
                Log.d(TAG,"download:"+bean.toString());
            } else {
                DownloadManager.getInstance().pause(bean);
                Log.d(TAG,"pause:"+bean.toString());
            }

        } else if (ACTION_MEDIA_DOWNLOAD_CANCEL.equals(intent.getAction())) {

            MediaBean bean = queryMediaBeanByUrl(url);
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

    private MediaBean queryMediaBeanByUrl(String url) {
        List<MediaBean> beans = new MediaBeanDao(getApplicationContext()).queryByUrl(url);
        if (beans==null || beans.isEmpty()) {
            return null;
        }

        if (beans.size()>1) {
            Log.w(TAG,"has the same url!");
            for (MediaBean bean:beans) {
                Log.w(TAG,bean.toString());
            }
            Log.w(TAG,"has the same url!");
        }

        return beans.get(0);
    }

    private void sendResultToMediaActivity(MediaBean bean) {
        new MediaBeanDao(getApplicationContext()).createOrUpdate(bean);

        Intent intent=new Intent(Contants.ACTION_DOWNLOAD_STATE_UPDATE);
        Bundle bundle = new Bundle();
        bundle.putParcelable(Contants.EXTRA_DOWNLOAD_STATE_CONTEXT, bean);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void sendProgressToOtaActivity(MediaBean bean) {
        Intent intent=new Intent(Contants.ACTION_DOWNLOAD_OTA_PROGRESS);
        Bundle bundle = new Bundle();
        bundle.putParcelable(Contants.EXTRA_DOWNLOAD_STATE_CONTEXT, bean);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void sendFailToOtaActivity(MediaBean bean) {
        Intent intent=new Intent(Contants.ACTION_DOWNLOAD_OTA_INSTALL_FAILED);
        Bundle bundle = new Bundle();
        bundle.putParcelable(Contants.EXTRA_DOWNLOAD_STATE_CONTEXT, bean);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }


    private BroadcastReceiver mNetReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                Log.i(TAG, "network status change!");
                ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

                // 恢复网络的时候重新启动下载
                if(info != null && info.isAvailable()) {
                    String name = info.getTypeName();
                    Log.d(TAG, "resume all download, network on " + name);
                    for (MediaBean tmp : new MediaBeanDao(getApplicationContext()).selectItemsDownloading()) {
                        tmp.setDownloadState(STATE_DOWNLOAD_NONE);
                        DownloadManager.getInstance().download(tmp, mMediaDownloadCallback);
                    }
                } else {
                    Log.d(TAG, "no network!");
                }
            }
        }
    };

    private ApkDownloadObserver mApkDownloadCallback = new ApkDownloadObserver();

    private class ApkDownloadObserver implements DownloadCallback {

        @Override
        public void onStateChange(MediaBean bean) {
            Log.d(TAG,bean.getUrl() + " onStateChange");
        }

        @Override
        public void onProgress(MediaBean bean) {
            Log.d(TAG,bean.getUrl() + " onProgress " + bean.getDownloadProgress());
            sendProgressToOtaActivity(bean);
        }

        @Override
        public void onFinish(MediaBean bean) {
            Log.d(TAG,bean.getUrl() + " onFinish");
            String apkPkgName = AppUtil.getApkPackageName(getApplicationContext(),UPDATE_APK_DOWNLOAD_PATH);
            String apkVersionName = AppUtil.getApkVersionName(getApplicationContext(),UPDATE_APK_DOWNLOAD_PATH);
            String apkSignature = AppUtil.getAPKSignature(UPDATE_APK_DOWNLOAD_PATH);
            int apkVersionCode = AppUtil.getApkVersionCode(getApplicationContext(),UPDATE_APK_DOWNLOAD_PATH);

            String mySignature = AppUtil.getSignature(getApplicationContext());
            int myVersionCode = DeviceInfoActivity.getVersionCode(getApplicationContext());

            // 包名字一致，签名一致，版本号高于本地，才真的去做升级
            if (apkPkgName!=null && apkPkgName.equals(getPackageName())
                    && mySignature.equals(apkSignature)
                    && apkVersionCode >= myVersionCode) {

                if (((MainApplication)getApplication()).isBusyInFormat || ((MainApplication)getApplication()).isBusyInCopy) {
                    Log.w(TAG,"can not update because device is in sata disk format/copy file to internal!");
                    sendFailToOtaActivity(bean);
                } else {
                    ToastUtil.showToast(getApplicationContext(), getResources().getString(R.string.str_update_file_download_ready));
                    ShellUtil.execCommand("pm install -r " + UPDATE_APK_DOWNLOAD_PATH,false,false);
                }

            } else {
                Log.w(TAG,"can not update because apk not as our expect! " +
                        " apkPkgName:" + apkPkgName +
                        " apkVersionCode:"+ apkVersionCode);
                sendFailToOtaActivity(bean);
            }
        }

        @Override
        public void onError(MediaBean bean) {
            Log.d(TAG,bean.getUrl() + " onError");

            ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
            if(info != null && info.isAvailable()) {
                String name = info.getTypeName();
                Log.d(TAG, bean.getUrl() + "network is ok, retry download now!");

                // 发生错误自动重新下载
                bean.setDownloadState(STATE_DOWNLOAD_NONE);
                DownloadManager.getInstance().download(bean, mApkDownloadCallback);
            }
        }
    }

    private MediaDownloadObserver mMediaDownloadCallback = new MediaDownloadObserver();

    private class MediaDownloadObserver implements DownloadCallback {

        @Override
        public void onStateChange(MediaBean bean) {
            Log.d(TAG,bean.getUrl() + " onStateChange");
            sendResultToMediaActivity(bean);
        }

        @Override
        public void onProgress(MediaBean bean) {
            Log.d(TAG,bean.getUrl() + " onProgress " + bean.getDownloadProgress());
            sendResultToMediaActivity(bean);
        }

        @Override
        public void onFinish(MediaBean bean) {
            Log.d(TAG,bean.getUrl() + " onFinish");
            bean.setDuration(downloadFileDurationScanner.getMediaDuration(bean.getPath()));
            sendResultToMediaActivity(bean);
        }

        @Override
        public void onError(MediaBean bean) {
            Log.d(TAG,bean.getUrl() + " onError");
            sendResultToMediaActivity(bean);

            ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
            if(info != null && info.isAvailable()) {
                String name = info.getTypeName();
                Log.d(TAG, bean.getUrl() + "network is ok, retry download now!");
                DownloadManager.getInstance().download(bean, mMediaDownloadCallback);
            }
        }
    }
}


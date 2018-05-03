package com.whitesky.tv.projectorlauncher.service.mqtt;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.admin.DeviceInfoActivity;
import com.whitesky.tv.projectorlauncher.application.MainApplication;
import com.whitesky.tv.projectorlauncher.common.Contants;
import com.whitesky.tv.projectorlauncher.common.HttpConstants;
import com.whitesky.tv.projectorlauncher.home.HomeActivity;
import com.whitesky.tv.projectorlauncher.media.DeleteTask;
import com.whitesky.tv.projectorlauncher.media.MediaActivity;
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBeanDao;
import com.whitesky.tv.projectorlauncher.media.db.PlayBean;
import com.whitesky.tv.projectorlauncher.media.db.PlayBeanDao;
import com.whitesky.tv.projectorlauncher.service.download.DownloadService;
import com.whitesky.tv.projectorlauncher.service.mqtt.bean.FileListPushBean;
import com.whitesky.tv.projectorlauncher.service.mqtt.bean.DeviceInfoResponseBean;
import com.whitesky.tv.projectorlauncher.service.mqtt.bean.MediaListPushBean;
import com.whitesky.tv.projectorlauncher.service.mqtt.bean.MediaListResponseBean;
import com.whitesky.tv.projectorlauncher.service.mqtt.bean.PlayModePushBean;
import com.whitesky.tv.projectorlauncher.service.mqtt.bean.QRcodeResultBean;
import com.whitesky.tv.projectorlauncher.service.mqtt.bean.VersionCheckResultBean;
import com.whitesky.tv.projectorlauncher.utils.FileUtil;
import com.whitesky.tv.projectorlauncher.utils.MqttUtil;
import com.whitesky.tv.projectorlauncher.utils.SharedPreferencesUtil;
import com.whitesky.tv.projectorlauncher.utils.ShellUtil;
import com.whitesky.tv.projectorlauncher.utils.ToastUtil;
import com.wsd.android.NativeCertification;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.widget.AdapterView.INVALID_POSITION;
import static com.whitesky.tv.projectorlauncher.common.Contants.MASS_STORAGE_PATH;
import static com.whitesky.tv.projectorlauncher.common.Contants.PROJECT_NAME;
import static com.whitesky.tv.projectorlauncher.common.HttpConstants.LOGIN_STATUS_NOT_YET;
import static com.whitesky.tv.projectorlauncher.common.HttpConstants.QRCODE_GET_STATUS_SUCCESS;
import static com.whitesky.tv.projectorlauncher.common.HttpConstants.VERSION_CHECK_STATUS_NO_AVAILABLE;
import static com.whitesky.tv.projectorlauncher.common.HttpConstants.VERSION_CHECK_STATUS_SUCCESS;
import static com.whitesky.tv.projectorlauncher.media.MediaActivity.MEDIA_REPLAY_ALL;
import static com.whitesky.tv.projectorlauncher.media.MediaActivity.MEDIA_REPLAY_ONE;
import static com.whitesky.tv.projectorlauncher.media.MediaActivity.MEDIA_REPLAY_SHUFFLE;
import static com.whitesky.tv.projectorlauncher.media.MediaActivity.needShowQRcode;
import static com.whitesky.tv.projectorlauncher.media.MediaActivity.savePlayIndexToConfig;
import static com.whitesky.tv.projectorlauncher.media.MediaActivity.saveQRcodeUrlToConfig;
import static com.whitesky.tv.projectorlauncher.media.MediaActivity.saveReplayModeToConfig;
import static com.whitesky.tv.projectorlauncher.media.MediaActivity.saveShowMaskToConfig;
import static com.whitesky.tv.projectorlauncher.media.MediaActivity.saveShowQRcodeToConfig;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.SOURCE_LOCAL;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_DOWNLOADED;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_NONE;
import static com.whitesky.tv.projectorlauncher.service.download.DownloadService.APK_SIZE_MAX;
import static com.whitesky.tv.projectorlauncher.service.download.DownloadService.EXTRA_KEY_URL;
import static java.lang.Thread.sleep;

/**
 * Created by lei on 17-12-1.
 */

public class MqttSslService extends Service implements MqttUtil.MqttMessageCallback
{
    private final static String TAG = MqttSslService.class.getSimpleName();
    private final static int MQTT_SERVICE_ID = 1001;

    private final String STR_MQTT_MSG_TYPE_CMD  = "command~~~~~";
    private final String STR_MQTT_MSG_TYPE_REQ  = "request~~~~~";

    private final String STR_MQTT_MSG_TYPE_RET  = "return~~~~~~";
    private final String STR_MQTT_MSG_TYPE_PUSH = "push~~~~~~~~";

    private final String STR_MQTT_CMD_ACTION_LOGINDONE  = "logindone";
    private final String STR_MQTT_CMD_ACTION_REBOOT     = "reboot";
    private final String STR_MQTT_CMD_ACTION_STANDBY    = "standby";
    private final String STR_MQTT_CMD_ACTION_OTA        = "otaupdate";
    private final String STR_MQTT_CMD_ACTION_PREVIOUS   = "previous";
    private final String STR_MQTT_CMD_ACTION_NEXT       = "next";
    private final String STR_MQTT_CMD_ACTION_QRCODE_ENABLE   = "controlqrcode:1";
    private final String STR_MQTT_CMD_ACTION_QRCODE_DISABLE  = "controlqrcode:0";

    private final String STR_MQTT_REQ_ACTION_INFO       = "info";      // 请求回应设备当前信息（开关机情况，开机时间，剩余磁盘容量，机顶盒软件版本信息）
    private final String STR_MQTT_REQ_ACTION_SHARELIST  = "sharelist"; // 请求回应设备当前硬盘中 共享中已下载的文件列表
    private final String STR_MQTT_REQ_ACTION_LOCALLIST  = "locallist"; // 本地 U盘导入的私有文件列表
    private final String STR_MQTT_REQ_ACTION_PLAYLIST   = "playlist";  // 当前的播放列表
    private final String STR_MQTT_REQ_ACTION_RAW        = "rawcommand";// 请求执行命令并返回执行结果

    private final String STR_MQTT_PUSH_ACTION_PLAYLIST  = "setlist";   // 设置播放列表
    private final String STR_MQTT_PUSH_ACTION_PLAYMODE  = "setmode";   // 设置播放模式
    private final String STR_MQTT_PUSH_ACTION_DOWNLOAD  = "download";  // 设置下载任务
    private final String STR_MQTT_PUSH_ACTION_DELETE    = "delete";    // 设置删除
    private final String STR_MQTT_PUSH_ACTION_LOCALDELETE  = "localdelete";   // 设置删除
    private final String STR_MQTT_PUSH_ACTION_WAKEUPGROUP  = "wakeup";   // 网络唤醒局域网其他设备

    private final int MQTT_DEFAULT_PUBLISH_QOS = 0;


    private final static int MSG_NONE = 0;
    // command
    private final static int MSG_CMD_LOGIN_DONE = 200;
    private final static int MSG_CMD_REBOOT = 201;
    private final static int MSG_CMD_STANDBY = 202;
    private final static int MSG_CMD_OTA = 203;
    public final static int MSG_CMD_PREVIOUS = 204;
    public final static int MSG_CMD_NEXT = 205;
    public final static int MSG_CMD_QRCODE_ENABLE = 206;
    public final static int MSG_CMD_QRCODE_DISABLE = 207;

    // request
    private final static int MSG_REQUEST_INFO = 300;
    private final static int MSG_REQUEST_PLAYLIST = 301;
    private final static int MSG_REQUEST_LOCALLIST = 302;
    private final static int MSG_REQUEST_SHARELIST = 303;
    private final static int MSG_REQUEST_RAW = 304;

    private final static int MSG_PUSH_PLAYLIST = 400;
    private final static int MSG_PUSH_PLAYMODE = 401;
    private final static int MSG_PUSH_DELETE = 402;
    private final static int MSG_PUSH_DOWNLOAD = 403;
    private final static int MSG_PUSH_WAKEUP = 404;

    private final String keyStorePassword = "wxgh#2561";

    private String SN = "";
    private ExecutorService mHeartBeatWorker = Executors.newSingleThreadExecutor();
    private ExecutorService mGetQRcodeWorker = Executors.newSingleThreadExecutor();

    private boolean mHeartBeatRunnig = false;
    private boolean mGetQRcodeRunnig = false;
    private final static int HEARTBEAT_INTERVAL_S = 60 * 3;
    private final static int GET_QRCODE_URL_INTERVAL_S = 60 * 60;

    private ArrayList<FileListPushBean> mNeedToDownloadList = null;
    private ArrayList<MediaListPushBean> mNeedToPlayList = null;

    // MQTT Util callback +++
    @Override
    public void onDistributeMessage(String msg) {

        if (msg.indexOf(SN) == 0) {
            parserMqttMessage(msg);
        } else {
            Log.e(TAG,"this msg is not for this device sn!!!");
        }

    }
    // MQTT Util callback ---


    @Override
    public void onCreate() {
        //前台通知提高优先级
        Intent notificationIntent = new Intent(this,HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0);

        //新建Builer对象
        Notification.Builder builer = new Notification.Builder(this);
        builer.setContentTitle("MQTT Service");         //设置通知的标题
        builer.setContentText("service running...");    //设置通知的内容
        builer.setSmallIcon(R.mipmap.ic_launcher);      //设置通知的图标
        builer.setContentIntent(pendingIntent);         //设置点击通知后的操作

        Notification notification = builer.build(); //将Builder对象转变成普通的notification
        startForeground(MQTT_SERVICE_ID, notification);  //让Service变成前台Service,并在系统的状态栏显示出来

        SN = DeviceInfoActivity.getSysSn(this);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        initSSL();
        MqttUtil.getInstance(this).setMsgCallBack(this);
        MqttUtil.getInstance(this).connect();

        reportVersionAndGetUpdateInfo(getApplicationContext(),null);

        if (mHeartBeatRunnig==false) {
            heartBeatThread();
        }

        if (needShowQRcode(getApplicationContext()) && mGetQRcodeRunnig ==false) {
            getQRcodeUrlThread();
        }

        return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    public void onDestroy()
    {
        mHeartBeatRunnig = false;
        if (mHeartBeatWorker!=null) {
            mHeartBeatWorker.shutdownNow();
            mHeartBeatWorker = null;
        }

        mGetQRcodeRunnig = false;
        if (mGetQRcodeWorker !=null) {
            mGetQRcodeWorker.shutdownNow();
            mGetQRcodeWorker = null;
        }

        super.onDestroy();

        Log.d(TAG,"service onDestroy auto restart!");
        Intent localIntent = new Intent();
        localIntent.setClass(this, MqttSslService.class); // 销毁时重新启动Service
        this.startService(localIntent);
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

    private void heartBeatThread() {
        mHeartBeatWorker.execute(new Runnable()
        {
            @Override
            public void run()
            {
                mHeartBeatRunnig = true;
                OkHttpClient mClient = new OkHttpClient();
                if (HttpConstants.URL_HEARTBEAT.contains("https")) {
                    try {
                        mClient = new OkHttpClient.Builder()
                                .sslSocketFactory(SSLContext.getDefault().getSocketFactory())
                                .build();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    mClient = new OkHttpClient();
                }

                Request request = new Request.Builder()
                        .url(HttpConstants.URL_HEARTBEAT + "?sn=" + SN )
                        .get()
                        .build();


                int loopCount = 0;

                while (mHeartBeatRunnig) {
                    try {
                        if (loopCount==0) {
                            Call call = mClient.newCall(request);
                            Response response = call.execute();

                            if (!response.isSuccessful()) {
                                throw new IOException("Unexpected code " + response);
                            } else if (response.code() == HttpConstants.HTTP_STATUS_SUCCESS) {
                                Log.d(TAG, "heartbeat success");
                            } else {
                                Log.e(TAG, "heartbeat response http code undefine! " + response.toString());

                            }
                        }

                        sleep(1000);
                        if (loopCount < HEARTBEAT_INTERVAL_S){
                            loopCount++;
                        } else {
                            loopCount = 0;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void getQRcodeUrlThread() {
        mGetQRcodeWorker.execute(new Runnable() {
            @Override
            public void run() {
                mGetQRcodeRunnig = true;
                int loopCount = 0;
                while (mGetQRcodeRunnig) {
                    try {
                        if (loopCount==0 && needShowQRcode(getApplicationContext())) {
                            getControlQrcode(getApplicationContext(), new getQRcodeCallback() {
                                @Override
                                public void qrCodeRequestDone(boolean ret, QRcodeResultBean.Result result) {

                                    if (ret == true) {

                                        saveQRcodeUrlToConfig(getApplicationContext(), result.getUrl());

                                        if (((MainApplication) getApplication()).isMediaActivityForeground) {
                                            Intent intent = new Intent(Contants.ACTION_CMD_QRCODE_CONTROL);
                                            Bundle bundle = new Bundle();
                                            bundle.putString(Contants.EXTRA_MQTT_ACTION_CONTEXT, result.getUrl());
                                            intent.putExtras(bundle);
                                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                                        }
                                    }
                                }
                            });
                        }

                        sleep(1000);
                        if (loopCount < GET_QRCODE_URL_INTERVAL_S) {
                            loopCount++;
                        } else {
                            loopCount = 0;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public interface requestVersionCallback{
        // ret==false表示没有与服务器正常连接, result==null表示没有发现更新
        void versionRequestDone(boolean ret, VersionCheckResultBean.Result result);
    }

    public static void reportVersionAndGetUpdateInfo(final Context context, final requestVersionCallback callback) {
        OkHttpClient mClient = new OkHttpClient();
        if (HttpConstants.URL_CHECK_VERSION.contains("https")) {
            try {
                mClient = new OkHttpClient.Builder()
                        .sslSocketFactory(SSLContext.getDefault().getSocketFactory())
                        .build();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mClient = new OkHttpClient();
        }

        FormBody body = new FormBody.Builder()
                .add("project", PROJECT_NAME)
                .add("appVer", DeviceInfoActivity.getVersionName(context))
                .add("sysVer", SystemProperties.get("ro.build.version.incremental", "0.0.0"))
                .add("sn", DeviceInfoActivity.getSysSn(context))
                .build();
        Request request = new Request.Builder().url(HttpConstants.URL_CHECK_VERSION).post(body).build();
        Call call = mClient.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            // 失败
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                if (callback!=null) callback.versionRequestDone(false,null);
            }

            // 成功
            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {
                if (!response.isSuccessful()) {
                    if (callback!=null) callback.versionRequestDone(false,null);
                    Log.e(TAG,"response is not success!" + response.toString());

                } else if (response.code() == HttpConstants.HTTP_STATUS_SUCCESS) {
                    String htmlBody = response.body().string();

                    VersionCheckResultBean serverResponse = null;

                    try {
                        serverResponse = new Gson().fromJson(htmlBody, VersionCheckResultBean.class);
                    } catch (IllegalStateException e) {
                        serverResponse = null;
                        e.printStackTrace();
                    }

                    if (serverResponse != null) {
                        if (serverResponse.getStatus().equals(VERSION_CHECK_STATUS_SUCCESS)) {
                            Log.i(TAG, "get version response have available!");
                            if (callback!=null) callback.versionRequestDone(true, serverResponse.getResult());
                        } else if (serverResponse.getStatus().equals(VERSION_CHECK_STATUS_NO_AVAILABLE)) {
                            Log.i(TAG,"get version response not have available!");
                            if (callback!=null) callback.versionRequestDone(true,null);
                        } else {
                            Log.d(TAG,"get version response unknown status " + serverResponse.getStatus());
                            if (callback!=null) callback.versionRequestDone(true,null);
                        }
                    } else {
                        Log.e(TAG, "get version response unknown json! " + htmlBody);
                        if (callback!=null) callback.versionRequestDone(true,null);
                    }

                } else {
                    Log.e(TAG, "onResponse http undefine code " + response.code());
                    if (callback!=null) callback.versionRequestDone(false,null);
                }
            }
        });
    }

    public interface getQRcodeCallback {
        void qrCodeRequestDone(boolean ret, QRcodeResultBean.Result result);
    }

    public static void getControlQrcode(final Context context, final getQRcodeCallback callback) {
        OkHttpClient mClient = new OkHttpClient();
        if (HttpConstants.URL_CHECK_VERSION.contains("https")) {
            try {
                mClient = new OkHttpClient.Builder()
                        .sslSocketFactory(SSLContext.getDefault().getSocketFactory())
                        .build();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mClient = new OkHttpClient();
        }

        FormBody body = new FormBody.Builder()
                .add("sn", DeviceInfoActivity.getSysSn(context))
                .build();
        Request request = new Request.Builder().url(HttpConstants.URL_GET_QRCODE).post(body).build();
        Call call = mClient.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            // 失败
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                if (callback!=null) callback.qrCodeRequestDone(false,null);
            }

            // 成功
            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {
                if (!response.isSuccessful()) {
                    if (callback!=null) callback.qrCodeRequestDone(false,null);
                    Log.e(TAG,"response is not success!" + response.toString());

                } else if (response.code() == HttpConstants.HTTP_STATUS_SUCCESS) {
                    String htmlBody = response.body().string();

                    QRcodeResultBean serverResponse = null;

                    try {
                        serverResponse = new Gson().fromJson(htmlBody, QRcodeResultBean.class);
                    } catch (IllegalStateException e) {
                        serverResponse = null;
                        e.printStackTrace();
                    }

                    if (serverResponse != null) {
                        if (serverResponse.getStatus().equals(QRCODE_GET_STATUS_SUCCESS)) {
                            Log.d(TAG, "get QRcode response success");
                            if (callback!=null) callback.qrCodeRequestDone(serverResponse.getResult()==null?false:true, serverResponse.getResult());
                        } else if (serverResponse.getStatus().equals(LOGIN_STATUS_NOT_YET)) {
                            Log.d(TAG,"onResponse device not login yet,need user login this device!");
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    ToastUtil.showToast(context,context.getResources().getString(R.string.str_media_cloud_file_need_login_toast));
                                }
                            });
                            if (callback!=null) callback.qrCodeRequestDone(false,null);
                        } else {
                            Log.d(TAG,"onResponse unknown status " + serverResponse.getStatus());
                            if (callback!=null) callback.qrCodeRequestDone(false,null);
                        }
                    } else {
                        Log.e(TAG, "server response null! " + htmlBody);
                        if (callback!=null) callback.qrCodeRequestDone(false,null);
                    }

                } else {
                    Log.e(TAG, "onResponse http undefine code " + response.code());
                    if (callback!=null) callback.qrCodeRequestDone(false,null);
                }
            }
        });
    }

    private class MqttDeleteMediaCallback implements DeleteTask.DeleteTaskListener {
        @Override
        public void onDeleteStartCallback(){
            ((MainApplication)getApplication()).isBusyInDelete = true;
        }


        @Override
        public void onDeleteOneCallback(MediaBean bean){
            // 从播放列表数据库移除相关条目
            new PlayBeanDao(getApplicationContext()).delete(new ArrayList<PlayBean>(bean.getPlayBeans()));

            if (bean.getSource()==SOURCE_LOCAL) {          // 本地文件
                // 从数据库删除
                new MediaBeanDao(getApplicationContext()).delete(bean);
                // 从磁盘删除
                FileUtil.deleteFile(bean.getPath());

            } else {                                       // 云端文件

                int downloadState = bean.getDownloadState();

                if (downloadState == STATE_DOWNLOAD_DOWNLOADED) {

                    bean.setDownloadProgress(0);
                    bean.setDownloadState(STATE_DOWNLOAD_NONE);
                    bean.setDuration(0);

                    // 更新数据库
                    new MediaBeanDao(getApplicationContext()).update(bean);
                    // 从磁盘删除
                    FileUtil.deleteFile(bean.getPath());

                } else if (downloadState == STATE_DOWNLOAD_NONE) {

                    // 没有开始下载的云端条目,啥都不做

                } else {

                    Intent intent = new Intent().setAction(DownloadService.ACTION_MEDIA_DOWNLOAD_CANCEL);
                    intent.putExtra(EXTRA_KEY_URL, bean.getUrl());
                    startService(intent);
                }
            }
        }

        @Override
        public void onAllDeleteDoneCallback(int deleteCount) {
            ToastUtil.showToast(getApplicationContext(), getResources().getString(R.string.str_media_file_delete_toast) + deleteCount);
            ((MainApplication)getApplication()).isBusyInDelete = false;
        }
    }

    private void mqttDeleteMediaFileInThread(Deque<MediaBean> deleteDeque) {
        DeleteTask.DeleteTaskParam param = new DeleteTask.DeleteTaskParam();
        param.deleteQueue = new ArrayDeque<>(deleteDeque);
        param.callback = new MqttDeleteMediaCallback();
        new DeleteTask(getApplicationContext(),false).execute(param);
    }

    private void parserMqttMessage(String mqttMessage) {
        Message message = mqttServiceHandler.obtainMessage();
        message.what = MSG_NONE;

        if (mqttMessage.indexOf(STR_MQTT_MSG_TYPE_CMD) == 20) {
            // command 用于管理端向设备发送控制命令

            if (mqttMessage.indexOf(STR_MQTT_CMD_ACTION_LOGINDONE) == 32) {
                message.what = MSG_CMD_LOGIN_DONE;

            } else if (mqttMessage.indexOf(STR_MQTT_CMD_ACTION_REBOOT) == 32) {
                message.what = MSG_CMD_REBOOT;

            } else if (mqttMessage.indexOf(STR_MQTT_CMD_ACTION_STANDBY) == 32) {
                message.what = MSG_CMD_STANDBY;

            } else if (mqttMessage.indexOf(STR_MQTT_CMD_ACTION_OTA) == 32) {
                message.what = MSG_CMD_OTA;

            } else if (mqttMessage.indexOf(STR_MQTT_CMD_ACTION_PREVIOUS) == 32) {
                message.what = MSG_CMD_PREVIOUS;

            } else if (mqttMessage.indexOf(STR_MQTT_CMD_ACTION_NEXT) == 32) {
                message.what = MSG_CMD_NEXT;

            } else if (mqttMessage.indexOf(STR_MQTT_CMD_ACTION_QRCODE_ENABLE) == 32) {
                message.what = MSG_CMD_QRCODE_ENABLE;

            } else if (mqttMessage.indexOf(STR_MQTT_CMD_ACTION_QRCODE_DISABLE) == 32) {
                message.what = MSG_CMD_QRCODE_DISABLE;

            } else {
                Log.e(TAG,"unknown msg action(" + mqttMessage.substring(32,64) + ")!");
            }

        } else if (mqttMessage.indexOf(STR_MQTT_MSG_TYPE_REQ) == 20) {
            // request 用于管理端向设备发送请求

            if (mqttMessage.indexOf(STR_MQTT_REQ_ACTION_INFO) == 32) {
                message.what = MSG_REQUEST_INFO;

            } else if (mqttMessage.indexOf(STR_MQTT_REQ_ACTION_SHARELIST) == 32) {
                message.what = MSG_REQUEST_SHARELIST;

            } else if (mqttMessage.indexOf(STR_MQTT_REQ_ACTION_LOCALLIST) == 32) {
                message.what = MSG_REQUEST_LOCALLIST;

            }else if (mqttMessage.indexOf(STR_MQTT_REQ_ACTION_PLAYLIST) == 32) {
                message.what = MSG_REQUEST_PLAYLIST;

            } else if (mqttMessage.indexOf(STR_MQTT_REQ_ACTION_RAW) == 32) {
                message.what = MSG_REQUEST_RAW;

            } else {
                Log.e(TAG,"unknown msg action(" + mqttMessage.substring(32,64) + ")!");
            }

        } else if (mqttMessage.indexOf(STR_MQTT_MSG_TYPE_PUSH) == 20) {

            if (mqttMessage.indexOf(STR_MQTT_PUSH_ACTION_PLAYLIST) == 32) {
                message.what = MSG_PUSH_PLAYLIST;

            } else if (mqttMessage.indexOf(STR_MQTT_PUSH_ACTION_PLAYMODE) == 32) {
                message.what = MSG_PUSH_PLAYMODE;

            } else if (mqttMessage.indexOf(STR_MQTT_PUSH_ACTION_DELETE) == 32) {
                message.what = MSG_PUSH_DELETE;

            } else if (mqttMessage.indexOf(STR_MQTT_PUSH_ACTION_DOWNLOAD) == 32) {
                message.what = MSG_PUSH_DOWNLOAD;

            } else if (mqttMessage.indexOf(STR_MQTT_PUSH_ACTION_LOCALDELETE) == 32) {
                message.what = MSG_PUSH_DELETE;

            } else if (mqttMessage.indexOf(STR_MQTT_PUSH_ACTION_WAKEUPGROUP) == 32) {
                message.what = MSG_PUSH_WAKEUP;

            } else {
                Log.e(TAG,"unknown msg action(" + mqttMessage.substring(32,64) + ")!");
            }

        } else if (mqttMessage.indexOf(STR_MQTT_MSG_TYPE_RET) == 20) {
            // device直接publish到自己的sn来回应服务器的request
            // 服务器会订阅所有sn的主题以此获得request的返回
            // 忽略本device对服务器的return
            Log.d(TAG,"do nothing when receive action(" + mqttMessage.substring(20,32) + ")!");
            return;

        } else {

            Log.e(TAG,"unknown msg type(" + mqttMessage.substring(20,32) + ")!");
            return;

        }

        if (mustIgnoreThisMessage(message.what)) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            formatter.setTimeZone(TimeZone.getTimeZone("GMT+08:00"));
            String timeNow = formatter.format(new Date());
            Log.i(TAG, "must ignore message(" + message.what + ")" + " because of device busy at " + timeNow);
            return;
        }

        message.obj = mqttMessage;
        mqttServiceHandler.sendMessage(message);
    }

    private boolean mustIgnoreThisMessage(int msgWhat) {
        // 机器格式化硬盘的时候是白名单,只允许远程查询机器信息,而且机器信息中的硬盘容量不返回
        if (((MainApplication)getApplication()).isBusyInFormat) {
            switch (msgWhat) {
                case MSG_REQUEST_INFO:
                case MSG_PUSH_WAKEUP:
                    return false;
                default:
                    return true;
            }
        }

        if (((MainApplication)getApplication()).isBusyInCopy
                || ((MainApplication)getApplication()).isBusyInDelete) {
            switch (msgWhat) {
                case MSG_REQUEST_INFO:
                case MSG_REQUEST_PLAYLIST:
                case MSG_REQUEST_LOCALLIST:
                case MSG_REQUEST_SHARELIST:
                case MSG_PUSH_DOWNLOAD:
                case MSG_PUSH_WAKEUP:
                    return false;
                default:
                    return true;
            }
        }

        return false;
    }

    private void callMediaDownload(MediaBean bean) {
        if (bean.getDownloadState()!=STATE_DOWNLOAD_DOWNLOADED) {
            Intent intent = new Intent().setAction(DownloadService.ACTION_MEDIA_DOWNLOAD_START);
            intent.putExtra(EXTRA_KEY_URL, bean.getUrl());
            Log.i(TAG, "mqtt call download:" + bean.toString());
            getApplicationContext().startService(intent);
        }
    }

    private void savePlaylistAndStartActivityToPlay(List<PlayBean> playlist) {

        new PlayBeanDao(getApplication()).deleteAll();
        new PlayBeanDao(getApplication()).createOrUpdate(playlist);

        savePlayIndexToConfig(getApplicationContext(), INVALID_POSITION);

        if (!playlist.isEmpty()) {
            // 立刻开始播放
            for (PlayBean bean : playlist) {
                callMediaDownload(bean.getMedia());
            }
            startActivity(new Intent(getApplicationContext(), MediaActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }

    }

    private Handler mqttServiceHandler = new Handler() {
        public void handleMessage(Message msg) {
            String rawStr = (String) msg.obj;
            Gson gson = new Gson();
            String jsonStr;
            List<MediaListResponseBean> responseDataList = new ArrayList<MediaListResponseBean>();
            List<PlayBean> pList;
            Deque<MediaBean> dDeque;

            if (msg.what!=MSG_NONE) {
                ToastUtil.showToast(getApplicationContext(), getResources().getString(R.string.str_media_receive_mqtt_toast) + "(" + msg.what + ")");
                Log.i(TAG,"receive mqtt cmd:"+msg.what);
            }

            switch (msg.what) {

                case MSG_CMD_LOGIN_DONE:
                    SharedPreferencesUtil shared = new SharedPreferencesUtil(getApplicationContext(), Contants.PREF_CONFIG);
                    shared.putBoolean(Contants.IS_ACTIVATE, true);

                    Intent homeIntent = new Intent(getApplicationContext(), HomeActivity.class);
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                    break;

                case MSG_CMD_REBOOT:
                    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    powerManager.reboot("mqtt request");
                    break;

                case MSG_CMD_STANDBY:
                    homeIntent = new Intent(getApplicationContext(), HomeActivity.class);
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                    break;

                case MSG_CMD_OTA:
                    reportVersionAndGetUpdateInfo(getApplicationContext(),new requestVersionCallback(){
                        @Override
                        public void versionRequestDone(boolean ret, VersionCheckResultBean.Result result) {
                            if (ret && result!=null) {
                                Log.i(TAG, "push ota version = " + result.getAppVer());
                                Log.i(TAG, "push ota size = " + result.getSize());

                                //boolean needDownload = serverVersionGreaterThanDeviceVersion(result.getAppVer(),DeviceInfoActivity.getVersionName(getApplicationContext()));
                                boolean needDownload = true;
                                if (result.getSize()<APK_SIZE_MAX && needDownload) {
                                    Intent intent = new Intent().setAction(DownloadService.ACTION_APK_DOWNLOAD_START);
                                    intent.putExtra(EXTRA_KEY_URL, result.getUrl());
                                    getApplicationContext().startService(intent);
                                } else {
                                    Log.e(TAG,"server update apk not as our required!");
                                }
                            }
                        }
                    });
                    break;

                case MSG_CMD_PREVIOUS:
                case MSG_CMD_NEXT:

                    if (((MainApplication)getApplication()).isMediaActivityForeground) {

                        Intent intent=new Intent(Contants.ACTION_CMD_PLAY_CONTROL);
                        Bundle bundle = new Bundle();
                        bundle.putInt(Contants.EXTRA_MQTT_ACTION_CONTEXT, msg.what);
                        intent.putExtras(bundle);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                    } else {
                        startActivity(new Intent(getApplicationContext(), MediaActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                    break;

                case MSG_CMD_QRCODE_ENABLE:
                    saveShowQRcodeToConfig(getApplicationContext(),true);
                    getQRcodeUrlThread();
                    break;

                case MSG_CMD_QRCODE_DISABLE:
                    mGetQRcodeRunnig = false;
                    saveShowQRcodeToConfig(getApplicationContext(),false);
                    saveQRcodeUrlToConfig(getApplicationContext(),"");

                    if (((MainApplication)getApplication()).isMediaActivityForeground) {
                        Intent intent=new Intent(Contants.ACTION_CMD_QRCODE_CONTROL);
                        Bundle bundle = new Bundle();
                        bundle.putString(Contants.EXTRA_MQTT_ACTION_CONTEXT, "");
                        intent.putExtras(bundle);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    }
                    break;

                case MSG_REQUEST_RAW:
                    //ToastUtil.showToast(getApplicationContext(), rawStr.substring(100));
                    Log.i(TAG,"exec cmd:" + rawStr.substring(100));
                    ShellUtil.CommandResult result = ShellUtil.execCommand(rawStr.substring(100),false, true);
                    String cmdRet = "";
                    if (result.successMsg!=null) {
                        cmdRet = result.successMsg;
                    }
                    rawStr = rawStr.replace(STR_MQTT_MSG_TYPE_REQ, STR_MQTT_MSG_TYPE_RET);
                    MqttUtil.getInstance(getApplicationContext()).publish(rawStr.substring(0,100) + cmdRet,MQTT_DEFAULT_PUBLISH_QOS);
                    break;

                case MSG_REQUEST_INFO:
                    DeviceInfoResponseBean info = new DeviceInfoResponseBean();

                    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                    ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                    am.getMemoryInfo(memoryInfo);
                    long availMemorySize = memoryInfo.availMem;
                    long totalMemorySize = memoryInfo.totalMem;

                    info.setUpTime(SystemClock.elapsedRealtime());
                    info.setAppVer(DeviceInfoActivity.getVersionName(getApplicationContext()));
                    info.setSysVer(SystemProperties.get("ro.build.description", "4.4.2"));
                    info.setFreeMemory(availMemorySize);
                    info.setTotalMemory(totalMemorySize);
                    if (((MainApplication)getApplication()).isBusyInFormat) {
                        // 正在格式化的时候容量返回 0;
                        info.setFreeCapacity(0);
                        info.setTotalCapacity(0);
                    } else {
                        info.setFreeCapacity(FileUtil.getAvailableCapacity(MASS_STORAGE_PATH));
                        info.setTotalCapacity(FileUtil.getTotalCapacity(MASS_STORAGE_PATH));
                    }
                    info.setPlaying(((MainApplication)getApplication()).isMediaActivityFullScreenPlaying ==true?1:0);

                    jsonStr = gson.toJson(info);
                    rawStr = rawStr.replace(STR_MQTT_MSG_TYPE_REQ, STR_MQTT_MSG_TYPE_RET);
                    MqttUtil.getInstance(getApplicationContext()).publish(rawStr + jsonStr,MQTT_DEFAULT_PUBLISH_QOS);
                    break;

                case MSG_REQUEST_SHARELIST:
                    responseDataList.clear();
                    for (MediaBean m:new MediaBeanDao(getApplicationContext()).selectDownloadedItemsFromCloud())
                    {
                        responseDataList.add(new MediaListResponseBean(m));
                    }
                    jsonStr = gson.toJson(responseDataList);
                    rawStr = rawStr.replace(STR_MQTT_MSG_TYPE_REQ, STR_MQTT_MSG_TYPE_RET);
                    MqttUtil.getInstance(getApplicationContext()).publish(rawStr + jsonStr,MQTT_DEFAULT_PUBLISH_QOS);
                    break;

                case MSG_REQUEST_PLAYLIST:
                    pList = new PlayBeanDao(getApplicationContext()).selectAll();
                    responseDataList.clear();
                    for (int i = 0; i < pList.size(); i++) {
                        responseDataList.add(new MediaListResponseBean(pList.get(i)));
                    }

                    jsonStr = gson.toJson(responseDataList);

                    rawStr = rawStr.replace(STR_MQTT_MSG_TYPE_REQ, STR_MQTT_MSG_TYPE_RET);
                    MqttUtil.getInstance(getApplicationContext()).publish(rawStr + jsonStr,MQTT_DEFAULT_PUBLISH_QOS);

                    break;

                case MSG_REQUEST_LOCALLIST:
                    responseDataList.clear();
                    for (MediaBean m:new MediaBeanDao(getApplicationContext()).selectItemsLocalImport())
                    {
                        responseDataList.add(new MediaListResponseBean(m));
                    }
                    jsonStr = gson.toJson(responseDataList);
                    rawStr = rawStr.replace(STR_MQTT_MSG_TYPE_REQ, STR_MQTT_MSG_TYPE_RET);
                    MqttUtil.getInstance(getApplicationContext()).publish(rawStr + jsonStr,MQTT_DEFAULT_PUBLISH_QOS);
                    break;

                case MSG_PUSH_PLAYLIST:
                    Type type = new TypeToken<List<MediaListPushBean>>(){ }.getType();
                    ArrayList<MediaListPushBean> playCloudList = null;
                    try {
                        playCloudList = gson.fromJson(rawStr.substring(100), type);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }

                    if (playCloudList == null) {
                        Log.w(TAG,"json parse error!");
                        return;
                    }

                    if (((MainApplication)getApplication()).isMediaActivityForeground) {

                        Intent intent=new Intent(Contants.ACTION_PUSH_PLAYLIST);
                        Bundle bundle = new Bundle();
                        bundle.putParcelableArrayList(Contants.EXTRA_MQTT_ACTION_CONTEXT, playCloudList);
                        intent.putExtras(bundle);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                    } else {

                        pList = new ArrayList<>();
                        // 这边需要处理，本地数据库中没有云端条目，而云端却发送过来让你下载的情况
                        boolean needSync = DataListCovert.covertCloudPushToPlayList(getApplicationContext(), pList, playCloudList);
                        if (needSync) {
                            if (mNeedToPlayList!=null) {
                                Log.w(TAG,"mNeedToPlayList already has a cloud sync http request");
                            } else {
                                mNeedToPlayList = playCloudList;
                                MediaActivity.loadMediaListFromCloud(getApplicationContext(), new MediaActivity.cloudListGetCallback() {
                                    @Override
                                    public void cloudSyncDone(boolean result) {
                                        ArrayList<PlayBean> pList = new ArrayList<>();
                                        boolean stillNeedSync = DataListCovert.covertCloudPushToPlayList(getApplicationContext(), pList, mNeedToPlayList);
                                        if (stillNeedSync) {
                                            Log.w(TAG, "we have sync with cloud,but still missing some item!");
                                        }
                                        mNeedToPlayList = null;
                                        savePlaylistAndStartActivityToPlay(pList);
                                    }
                                });
                            }
                        } else {
                            savePlaylistAndStartActivityToPlay(pList);
                        }
                    }

                    break;

                case MSG_PUSH_PLAYMODE:
                    PlayModePushBean playMode = null;
                    try {
                        playMode = gson.fromJson(rawStr.substring(100), PlayModePushBean.class);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }

                    if (playMode==null) {
                        Log.w(TAG,"json parse error!");
                        return;
                    }

                    if (((MainApplication)getApplication()).isMediaActivityForeground) {

                        Intent intent=new Intent(Contants.ACTION_PUSH_PLAYMODE);
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(Contants.EXTRA_MQTT_ACTION_CONTEXT, playMode);
                        intent.putExtras(bundle);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                    } else {

                        if (playMode.getPlayMode()==MEDIA_REPLAY_SHUFFLE
                                || playMode.getPlayMode()==MEDIA_REPLAY_ONE
                                || playMode.getPlayMode()==MEDIA_REPLAY_ALL) {
                            saveReplayModeToConfig(getApplicationContext(), playMode.getPlayMode());
                        }

                        if (playMode.getMask()==0 || playMode.getMask()==1) {
                            saveShowMaskToConfig(getApplicationContext(),playMode.getMask()==0?false:true);
                        }

                        startActivity(new Intent(getApplicationContext(), MediaActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                    break;

                case MSG_PUSH_DOWNLOAD:
                    type = new TypeToken<List<FileListPushBean>>(){ }.getType();
                    ArrayList<FileListPushBean> downloadList = null;
                    try {
                        downloadList = gson.fromJson(rawStr.substring(100), type);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }

                    if (downloadList == null) {
                        Log.w(TAG,"json parse error!");
                        return;
                    }

                    dDeque = new ArrayDeque<>();
                    // 这边需要处理，本地数据库中没有云端条目，而云端却发送过来让你下载的情况
                    boolean needSync = DataListCovert.covertCloudFileListToMediaBeanList(getApplicationContext(),dDeque,downloadList);
                    if (needSync) {

                        if (((MainApplication)getApplication()).isMediaActivityForeground) {
                            // 交给MediaActivity同步，同步结束后下载这些文件
                            Intent intent=new Intent(Contants.ACTION_PUSH_DOWNLOAD_NEED_SYNC);
                            Bundle bundle = new Bundle();
                            bundle.putParcelableArrayList(Contants.EXTRA_MQTT_ACTION_CONTEXT, downloadList);
                            intent.putExtras(bundle);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                        } else {

                            if (mNeedToDownloadList!=null) {
                                Log.w(TAG,"mNeedToDownloadList already has a cloud sync http request");
                            } else {
                                mNeedToDownloadList = downloadList;
                                MediaActivity.loadMediaListFromCloud(getApplicationContext(), new MediaActivity.cloudListGetCallback() {
                                    @Override
                                    public void cloudSyncDone(boolean result) {
                                        Deque<MediaBean> dDeque = new ArrayDeque<>();
                                        boolean stillNeedSync = DataListCovert.covertCloudFileListToMediaBeanList(getApplicationContext(), dDeque, mNeedToDownloadList);
                                        if (stillNeedSync == true) {
                                            Log.w(TAG,"we have sync with cloud,but still missing some item!");
                                        }
                                        mNeedToDownloadList = null;
                                        while (!dDeque.isEmpty()) {
                                            callMediaDownload(dDeque.pop());
                                        }
                                    }
                                });
                            }
                        }

                    } else {

                        while (!dDeque.isEmpty()) {
                            callMediaDownload(dDeque.pop());
                        }

                    }
                    break;

                case MSG_PUSH_DELETE:
                    type = new TypeToken<List<FileListPushBean>>(){}.getType();
                    ArrayList<FileListPushBean> deleteList = null;
                    try {
                        deleteList = gson.fromJson(rawStr.substring(100), type);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }

                    if (deleteList == null) {
                        Log.w(TAG,"json parse error!");
                        return;
                    }

                    if (((MainApplication)getApplication()).isMediaActivityForeground) {

                        Intent intent=new Intent(Contants.ACTION_PUSH_DELETE);
                        Bundle bundle = new Bundle();
                        bundle.putParcelableArrayList(Contants.EXTRA_MQTT_ACTION_CONTEXT, deleteList);
                        intent.putExtras(bundle);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                    } else {
                        dDeque = new ArrayDeque<>();
                        // 删除文件不用管本地数据库中没有的, 而服务器传过来的条目的删除
                        DataListCovert.covertCloudFileListToMediaBeanList(getApplicationContext(),dDeque,deleteList);
                        mqttDeleteMediaFileInThread(dDeque);
                    }
                    break;

                case MSG_PUSH_WAKEUP:
                    String[] macList = rawStr.substring(100).split(",");
                    if (macList!=null && macList.length>0) {
                        WakeupGroupThread wakeTrigger = new WakeupGroupThread();
                        for (String mac:macList) {
                            String needWakeupMac = mac.replace("[","").replace("\"","");
                            Log.d(TAG,"will wake up mac:" + needWakeupMac);
                            wakeTrigger.add(needWakeupMac);
                        }
                        wakeTrigger.run();
                    }
                    break;

                default:
                    Log.i(TAG, "unknown msg " + msg.what);
                    break;
            }
            super.handleMessage(msg);
        }
    };

}

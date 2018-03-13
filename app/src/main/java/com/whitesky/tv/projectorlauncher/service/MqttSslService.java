package com.whitesky.tv.projectorlauncher.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.admin.DeviceInfoActivity;
import com.whitesky.tv.projectorlauncher.application.MainApplication;
import com.whitesky.tv.projectorlauncher.common.Contants;
import com.whitesky.tv.projectorlauncher.common.HttpConstants;
import com.whitesky.tv.projectorlauncher.home.HomeActivity;
import com.whitesky.tv.projectorlauncher.media.MediaActivity;
import com.whitesky.tv.projectorlauncher.media.bean.PlayListBean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBeanDao;
import com.whitesky.tv.projectorlauncher.media.DataCovert;
import com.whitesky.tv.projectorlauncher.utils.FileUtil;
import com.whitesky.tv.projectorlauncher.utils.MqttUtil;
import com.whitesky.tv.projectorlauncher.utils.SharedPreferencesUtil;
import com.whitesky.tv.projectorlauncher.utils.ToastUtil;
import com.wsd.android.NativeCertification;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.whitesky.tv.projectorlauncher.common.Contants.CONFIG_PLAYLIST;
import static com.whitesky.tv.projectorlauncher.common.Contants.LOCAL_MASS_STORAGE_PATH;
import static com.whitesky.tv.projectorlauncher.common.Contants.PROJECT_NAME;
import static com.whitesky.tv.projectorlauncher.common.HttpConstants.VERSION_CHECK_STATUS_SUCCESS;
import static com.whitesky.tv.projectorlauncher.media.MediaActivity.saveReplayModeToConfig;
import static com.whitesky.tv.projectorlauncher.media.MediaActivity.saveShowMaskToConfig;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.MEDIA_REPLAY_ALL;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.MEDIA_REPLAY_ONE;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.MEDIA_REPLAY_SHUFFLE;
import static java.lang.Thread.sleep;

/**
 * Created by lei on 17-12-1.
 */

public class MqttSslService extends Service implements MqttUtil.MqttMessageCallback
{
    private final static int SERVICE_ID = 1001;

    private final String STR_MQTT_MSG_TYPE_CMD  = "command~~~~~";
    private final String STR_MQTT_MSG_TYPE_REQ  = "request~~~~~";

    private final String STR_MQTT_MSG_TYPE_RET  = "return~~~~~~";
    private final String STR_MQTT_MSG_TYPE_PUSH = "push~~~~~~~~";

    private final String STR_MQTT_CMD_ACTION_LOGINDONE  = "logindone";
    private final String STR_MQTT_CMD_ACTION_REBOOT     = "reboot";
    private final String STR_MQTT_CMD_ACTION_OTA        = "otaupdate";

    private final String STR_MQTT_REQ_ACTION_INFO       = "info";// 请求回应设备当前信息（开关机情况，开机时间，剩余磁盘容量，机顶盒软件版本信息，地理位置）
    private final String STR_MQTT_REQ_ACTION_SHARELIST  = "sharelist";//请求回应设备当前硬盘中 共享中已下载的文件列表
    private final String STR_MQTT_REQ_ACTION_LOCALLIST  = "locallist";//本地U盘导入的私有文件列表
    private final String STR_MQTT_REQ_ACTION_PLAYLIST   = "playlist";// 当前的播放列表

    private final String STR_MQTT_PUSH_ACTION_PLAYLIST  = "setlist";// 当前的播放列表
    private final String STR_MQTT_PUSH_ACTION_PLAYMODE  = "setmode";// 当前的播放模式


    private final static int MSG_NONE = 0;

    // command
    private final static int MSG_CMD_LOGIN_DONE = 200;
    private final static int MSG_CMD_REBOOT = 201;
    private final static int MSG_CMD_OTA = 202;
    // request
    private final static int MSG_REQUEST_INFO = 300;
    private final static int MSG_REQUEST_PLAYLIST = 301;
    private final static int MSG_REQUEST_LOCALLIST = 302;
    private final static int MSG_REQUEST_SHARELIST = 303;

    private final static int MSG_PUSH_PLAYLIST = 400;
    private final static int MSG_PUSH_PLAYMODE = 401;

    private final String TAG = this.getClass().getSimpleName();
    private final String keyStorePassword = "wxgh#2561";

    private final String SN = DeviceInfoActivity.getSysSN();
    private ExecutorService mHeartBeatWorker = Executors.newSingleThreadExecutor();

    private boolean mHeartBeatRunnig = false;
    private final static int HEARTBEAT_INTERVAL_S = 60 * 3;

    // MQTT Util callback +++
    @Override
    public void onDistributeMessage(String msg) {

        if (msg.indexOf(SN) == 0) {
            parserMqttMessage(msg);
        } else {
            Log.e(TAG,"this msg is not for this device sn!!!");
        }
        // todo 考虑使用eventbus?
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
        startForeground(SERVICE_ID, notification);  //让Service变成前台Service,并在系统的状态栏显示出来

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        initSSL();
        MqttUtil.getInstance(this).setMsgCallBack(this);
        MqttUtil.getInstance(this).connect();

        reportVersionInfo();

        heartBeat();

        Log.d(TAG,"~~debug~~service onStartCommand!");

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

        super.onDestroy();

        Log.d(TAG,"~~debug~~service onDestroy!");

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

    private void heartBeat() {
        mHeartBeatWorker.execute(new Runnable()
        {
            @Override
            public void run()
            {
                mHeartBeatRunnig = true;
                OkHttpClient mClient = new OkHttpClient();
                if (HttpConstants.URL_HEARTBEAT.contains("https"))
                {
                    try
                    {
                        mClient =
                                new OkHttpClient.Builder().sslSocketFactory(SSLContext.getDefault().getSocketFactory())
                                        .build();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    mClient = new OkHttpClient();
                }

                Request request = new Request.Builder()
                        .url(HttpConstants.URL_HEARTBEAT + "?sn=" + SN )
                        .get()
                        .build();


                int loopCount = 0;

                while (mHeartBeatRunnig) {

                    try {

                        sleep(1000);

                        if (loopCount<HEARTBEAT_INTERVAL_S){

                            loopCount++;

                        } else {

                            loopCount = 0;

                            Call call = mClient.newCall(request);
                            Response response = call.execute();

                            if (!response.isSuccessful()) {
                                throw new IOException("Unexpected code " + response);
                            } else if (response.code() == HttpConstants.HTTP_STATUS_SUCCESS) {

                                Log.d(TAG, "heartBeat success");

                            } else {

                                Log.e(TAG, "heartBeat response http code undefine!");

                            }
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Exception in heartBeat thread" + e);
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void reportVersionInfo() {
        OkHttpClient mClient = new OkHttpClient();
        if (HttpConstants.URL_CHECK_VERSION.contains("https")) {
            try {
                mClient =
                        new OkHttpClient.Builder().sslSocketFactory(SSLContext.getDefault().getSocketFactory())
                                .build();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mClient = new OkHttpClient();
        }

        FormBody body =
                new FormBody.Builder()
                        .add("project", PROJECT_NAME)
                        .add("appVer", DeviceInfoActivity.getVersionName(getApplicationContext()))
                        .add("sysVer", SystemProperties.get("ro.build.description", "4.4.2"))
                        .add("sn", SN)
                        .build();
        Request request = new Request.Builder().url(HttpConstants.URL_CHECK_VERSION).post(body).build();
        Call call = mClient.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            // 失败
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            // 成功
            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);

                } else if (response.code() == HttpConstants.HTTP_STATUS_SUCCESS) {
                    String htmlBody = response.body().string();

                    VersionCheckResultBean serverResponse = null;

                    try {
                        serverResponse = new Gson().fromJson(htmlBody, VersionCheckResultBean.class);
                    } catch (IllegalStateException e) {
                        serverResponse = null;
                        Log.e(TAG, "Gson parse error!" + e);
                    }

                    if (serverResponse != null) {
                        if (serverResponse.getStatus().equals(VERSION_CHECK_STATUS_SUCCESS)) {
                            if (serverResponse.getResult() != null) {
                                Log.d(TAG, "VersionInfo success");
                            }
                            // todo 检查版本是否为null,不是再比较版本,然后再download
                            Log.d(TAG, htmlBody);
//                                    Log.d(TAG, result.getResult().getAppVer());
//                                    Log.d(TAG, result.getResult().getSysVer());
//                                    Log.d(TAG, "size:" + result.getResult().getSize());
//                                    Log.d(TAG, "time:" + result.getResult().getCreatedAt());
//                                    Log.d(TAG, result.getResult().getUrl());
                        }
                    }

                } else {
                    Log.e(TAG, "VersionInfo response http code undefine!");
                }
            }
        });
    }

    private void parserMqttMessage(String mqttMessage) {
        Message message = mqttUtilHandler.obtainMessage();
        message.what = MSG_NONE;

        if (mqttMessage.indexOf(STR_MQTT_MSG_TYPE_CMD) == 20) {
            // command 用于管理端向设备发送控制命令

            if (mqttMessage.contains(STR_MQTT_CMD_ACTION_LOGINDONE)) {
                message.what = MSG_CMD_LOGIN_DONE;

            } else if (mqttMessage.contains(STR_MQTT_CMD_ACTION_REBOOT)) {
                message.what = MSG_CMD_REBOOT;

            } else if (mqttMessage.contains(STR_MQTT_CMD_ACTION_OTA)) {
                message.what = MSG_CMD_OTA;
            } else {
                Log.e(TAG,"unknown msg action(" + mqttMessage.substring(32,64) + ")!");
            }

        } else if (mqttMessage.indexOf(STR_MQTT_MSG_TYPE_REQ) == 20) {
            // request 用于管理端向设备发送请求

            if (mqttMessage.contains(STR_MQTT_REQ_ACTION_INFO)) {
                message.what = MSG_REQUEST_INFO;
            } else if (mqttMessage.contains(STR_MQTT_REQ_ACTION_SHARELIST)) {
                message.what = MSG_REQUEST_SHARELIST;
            } else if (mqttMessage.contains(STR_MQTT_REQ_ACTION_LOCALLIST)) {
                message.what = MSG_REQUEST_LOCALLIST;
            }else if (mqttMessage.contains(STR_MQTT_REQ_ACTION_PLAYLIST)) {
                message.what = MSG_REQUEST_PLAYLIST;
            } else {
                Log.e(TAG,"unknown msg action(" + mqttMessage.substring(32,64) + ")!");
            }

        } else if (mqttMessage.indexOf(STR_MQTT_MSG_TYPE_PUSH) == 20) {

            if (mqttMessage.contains(STR_MQTT_PUSH_ACTION_PLAYLIST)) {
                message.what = MSG_PUSH_PLAYLIST;
            } else if (mqttMessage.contains(STR_MQTT_PUSH_ACTION_PLAYMODE)) {
                message.what = MSG_PUSH_PLAYMODE;
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

        message.obj = mqttMessage;
        mqttUtilHandler.sendMessage(message);
    }

    private Handler mqttUtilHandler = new Handler() {
        public void handleMessage(Message msg) {
            String rawStr = (String) msg.obj;
            Gson gson = new Gson();
            String jsonStr;
            List<MediaListResponseBean> responseDataList = new ArrayList<MediaListResponseBean>();
            List<PlayListBean> pList;

            if (msg.what!=MSG_NONE) {
                ToastUtil.showToast(getApplicationContext(), getResources().getString(R.string.str_media_receive_mqtt_toast));
                Log.i(TAG,"receive mqtt cmd:"+msg.what);
            }

            switch (msg.what) {

                case MSG_CMD_LOGIN_DONE:
                    SharedPreferencesUtil shared = new SharedPreferencesUtil(getApplicationContext(), Contants.PREF_CONFIG);
                    shared.putBoolean(Contants.IS_ACTIVATE, true);
                    shared.putBoolean(Contants.IS_SETUP_PASS, true);

                    Intent homeIntent = new Intent(getApplicationContext(), HomeActivity.class);
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                    break;

                case MSG_CMD_REBOOT:
                    PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
                    powerManager.reboot("mqtt request");
                    break;

                case MSG_CMD_OTA:
                    // todo ota
                    break;

                case MSG_REQUEST_INFO:
                    DeviceInfoResponseBean info = new DeviceInfoResponseBean();

                    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                    ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
                    am.getMemoryInfo(memoryInfo);
                    long availMemorySize = memoryInfo.availMem;
                    long totalMemorySize = memoryInfo.totalMem;

                    info.setUpTime(SystemClock.elapsedRealtime());
                    info.setAppVer(DeviceInfoActivity.getVersionName(getApplicationContext()));
                    info.setSysVer(SystemProperties.get("ro.build.description", "4.4.2"));
                    info.setFreeMemory(availMemorySize);
                    info.setTotalMemory(totalMemorySize);
                    info.setFreeCapacity(FileUtil.getAvailableCapacity(LOCAL_MASS_STORAGE_PATH));
                    info.setTotalCapacity(FileUtil.getTotalCapacity(LOCAL_MASS_STORAGE_PATH));
                    info.setPlaying(((MainApplication)getApplication()).isMediaActivityFullScreenPlaying ==true?1:0);

                    jsonStr = gson.toJson(info);
                    rawStr = rawStr.replace(STR_MQTT_MSG_TYPE_REQ, STR_MQTT_MSG_TYPE_RET);
                    MqttUtil.getInstance(getApplicationContext()).publish(rawStr + jsonStr,2);
                    break;

                case MSG_REQUEST_SHARELIST:
                    break;

                case MSG_REQUEST_PLAYLIST:
                    SharedPreferencesUtil config = new SharedPreferencesUtil(getApplicationContext(), Contants.PREF_CONFIG);
                    jsonStr = config.getString(CONFIG_PLAYLIST, "[]");
                    Type type = new TypeToken<List<PlayListBean>>() {
                    }.getType();

                    pList = gson.fromJson(jsonStr, type);
                    responseDataList.clear();
                    for (int i = 0; i < pList.size(); i++) {
                        responseDataList.add(new MediaListResponseBean(pList.get(i)));
                    }

                    jsonStr = gson.toJson(responseDataList);

                    rawStr = rawStr.replace(STR_MQTT_MSG_TYPE_REQ, STR_MQTT_MSG_TYPE_RET);
                    MqttUtil.getInstance(getApplicationContext()).publish(rawStr + jsonStr,2);

                    Log.d(TAG,"PLAYLIST~~~~"+rawStr + jsonStr);
                    break;

                case MSG_REQUEST_LOCALLIST:
                    responseDataList.clear();
                    for (MediaBean m:new MediaBeanDao(getApplicationContext()).selectItemsLocalImport())
                    {
                        responseDataList.add(new MediaListResponseBean(m));
                    }
                    jsonStr = gson.toJson(responseDataList);
                    rawStr = rawStr.replace(STR_MQTT_MSG_TYPE_REQ, STR_MQTT_MSG_TYPE_RET);
                    MqttUtil.getInstance(getApplicationContext()).publish(rawStr + jsonStr,2);

                    Log.d(TAG,"LOCALLIST~~~~"+rawStr + jsonStr);
                    break;

                case MSG_PUSH_PLAYLIST:
                    type = new TypeToken<List<MediaListPushBean>>(){ }.getType();
                    ArrayList<MediaListPushBean> pushList = gson.fromJson(rawStr.substring(100), type);

                    if (((MainApplication)getApplication()).isMediaActivityForeground) {

                        Intent intent=new Intent(Contants.ACTION_PUSH_PLAYLIST);
                        Bundle bundle = new Bundle();
                        bundle.putParcelableArrayList(Contants.EXTRA_PUSH_CONTEXT, pushList);
                        intent.putExtras(bundle);
                        sendBroadcast(intent);

                    } else {

                        pList = new ArrayList<>();
                        DataCovert.covertPlayList(getApplicationContext(), pList, pushList);
                        MediaActivity.savePlaylistToConfig(getApplication(), pList);
                        if (!pList.isEmpty()) {
                            // 立刻开始播放
                            startActivity(new Intent(getApplicationContext(), MediaActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        }
                    }

                    break;

                case MSG_PUSH_PLAYMODE:
                    PlayModePushBean playMode = gson.fromJson(rawStr.substring(100),PlayModePushBean.class);

                    if (((MainApplication)getApplication()).isMediaActivityForeground) {

                        Intent intent=new Intent(Contants.ACTION_PUSH_PLAYMODE);
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(Contants.EXTRA_PUSH_CONTEXT, playMode);
                        intent.putExtras(bundle);
                        sendBroadcast(intent);

                    } else {

                        if (playMode.getPlayMode()==MEDIA_REPLAY_SHUFFLE
                                || playMode.getPlayMode()==MEDIA_REPLAY_ONE
                                || playMode.getPlayMode()==MEDIA_REPLAY_ALL) {

                            saveReplayModeToConfig(getApplicationContext(),playMode.getPlayMode());
                            saveShowMaskToConfig(getApplicationContext(),playMode.getMask()==0?false:true);
                            startActivity(new Intent(getApplicationContext(), MediaActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

                        } else {
                            Log.e(TAG, "unknown replay mode (" + playMode.getPlayMode() + ")");
                        }
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

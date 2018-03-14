package com.whitesky.tv.projectorlauncher.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.admin.DeviceInfoActivity;
import com.whitesky.tv.projectorlauncher.application.MainApplication;
import com.whitesky.tv.projectorlauncher.common.Contants;
import com.whitesky.tv.projectorlauncher.common.HttpConstants;
import com.whitesky.tv.projectorlauncher.home.HomeActivity;
import com.whitesky.tv.projectorlauncher.media.adapter.AllMediaListAdapter;
import com.whitesky.tv.projectorlauncher.media.adapter.PlayListAdapter;
import com.whitesky.tv.projectorlauncher.media.adapter.UsbMediaListAdapter;
import com.whitesky.tv.projectorlauncher.media.bean.AllMediaListBean;
import com.whitesky.tv.projectorlauncher.media.bean.CloudListBean;
import com.whitesky.tv.projectorlauncher.media.bean.PlayListBean;
import com.whitesky.tv.projectorlauncher.media.bean.UsbMediaListBean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBeanDao;
import com.whitesky.tv.projectorlauncher.service.download.DownloadService;
import com.whitesky.tv.projectorlauncher.service.mqtt.MediaListPushBean;
import com.whitesky.tv.projectorlauncher.service.mqtt.PlayModePushBean;
import com.whitesky.tv.projectorlauncher.utils.FileUtil;
import com.whitesky.tv.projectorlauncher.utils.MediaScanUtil;
import com.whitesky.tv.projectorlauncher.utils.SharedPreferencesUtil;
import com.whitesky.tv.projectorlauncher.utils.ToastUtil;
import com.whitesky.tv.projectorlauncher.utils.ViewUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.whitesky.tv.projectorlauncher.common.Contants.CONFIG_MEDIA_LIST_ORDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.CONFIG_PLAYLIST;
import static com.whitesky.tv.projectorlauncher.common.Contants.CONFIG_REPLAY_MODE;
import static com.whitesky.tv.projectorlauncher.common.Contants.CONFIG_SHOW_MASK;
import static com.whitesky.tv.projectorlauncher.common.Contants.COPY_TO_USB_MEDIA_EXPORT_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.LOCAL_MASS_STORAGE_PATH;
import static com.whitesky.tv.projectorlauncher.common.Contants.LOCAL_MEDIA_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.LOCAL_SATA_MOUNT_PATH;
import static com.whitesky.tv.projectorlauncher.common.Contants.MEDIA_LIST_ORDER_DEFAULT;
import static com.whitesky.tv.projectorlauncher.common.Contants.MEDIA_LIST_ORDER_DURATION;
import static com.whitesky.tv.projectorlauncher.common.Contants.MEDIA_LIST_ORDER_NAME;
import static com.whitesky.tv.projectorlauncher.common.Contants.MEDIA_LIST_ORDER_SOURCE;
import static com.whitesky.tv.projectorlauncher.common.Contants.USB_DEVICE_DEFAULT_SEARCH_MEDIA_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.mMountExceptList;
import static com.whitesky.tv.projectorlauncher.common.HttpConstants.LOGIN_STATUS_NOT_YET;
import static com.whitesky.tv.projectorlauncher.common.HttpConstants.LOGIN_STATUS_SUCCESS;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_IDLE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.ID_LOCAL;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_PICTURE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_VIDEO;

/**
 * Created by jeff on 18-1-16.
 */
public class MediaActivity extends Activity
        implements View.OnClickListener,
        PictureVideoPlayer.OnMediaEventListener,
        RadioGroup.OnCheckedChangeListener {

    private final String TAG = this.getClass().getSimpleName();

    private final int MSG_USB_PLUG_IN = 0;
    private final int MSG_USB_PLUG_OUT = 1;
    private final int MSG_USB_PARTITION_SWITCH = 2;

    private final int MSG_LOCAL_MEDIA_DATABASE_UPDATE = 10;
    private final int MSG_MEDIA_LIST_CLEAN_LOCAL = 11;
    private final int MSG_USB_MEDIA_LIST_UPDATE = 12;
    private final int MSG_USB_MEDIA_LIST_CLEAN = 13;

    private final int MSG_MEDIA_DATABASE_UI_SYNC = 20;
    private final int MSG_USB_MEDIA_SCAN_DONE = 21;

    private final int MSG_USB_COPY_TO_INTERNAL = 40;
    private final int MSG_MEDIA_LIST_ITEM_DELETE = 41;
    private final int MSG_MEDIA_LIST_ITEM_PREVIEW = 42;
    private final int MSG_MEDIA_LIST_ITEM_DOWNLOAD = 43;

    private final int MSG_USB_LIST_SELECTED_CHANGE = 44;
    private final int MSG_MEDIA_LIST_SELECTED_CHANGE = 45;

    // 媒体播放事件
    private final int MSG_MEDIA_PLAY_COMPLETE = 100;
    private final int MSG_MEDIA_PLAY_STOP = 102;

    private static final String BUNDLE_KEY_STORAGE_PATH = "storagePath";

    private static final String BUNDLE_KEY_MEDIA_TYPE = "type";
    private static final String BUNDLE_KEY_MEDIA_SIZE = "size";
    private static final String BUNDLE_KEY_MEDIA_DURATION = "duration";
    private static final String BUNDLE_KEY_MEDIA_PATH = "path";
    private static final String BUNDLE_KEY_MEDIA_NAME = "name";


    private final int USB_COPY_BUFFER_SIZE = 1024*1024;     // 拷贝文件缓冲区长度,可调参数

    private MediaScanUtil mLocalMediaScanner;
    private MediaScanUtil mUsbMediaScanner;

    private PictureVideoPlayer mPlayer;

    private Button mMultiCopyToLocalBtn;
    private Button mMultiDeleteLocalBtn;
    private Button mMultiAddToPlayListBtn;
    private Button mMultiDownloadBtn;
    private Button mMultiCopyToUsbBtn;

    private Button mLocalMediaListRefreshBtn;
    private Button mCloudMediaListRefreshBtn;

    private TextView mMediaInfoNameTextView;
    private TextView mMediaInfoTypeTextView;
    private TextView mMediaInfoWidthHeightTextView;
    private TextView mMediaInfoSizeTextView;
    private TextView mMediaInfoBpsTextView;

    private RadioGroup mReplayModeRadioGroup;
    private RadioButton mReplayAllRadioButton;
    private RadioButton mReplayOneRadioButton;
    private RadioButton mReplayShuffleRadioButton;

    private RadioGroup mMediaListOrderRadioGroup;
    private RadioButton mMediaListOrderNameRadioButton;
    private RadioButton mMediaListOrderDurationRadioButton;
    private RadioButton mMediaListOrderSourceRadioButton;

    private int mOriginPlayerMarginTop = 0;
    private int mOriginPlayerMarginLeft = 0;

    private List<PlayListBean> mPlayListBeans = new ArrayList<PlayListBean>();
    private PlayListAdapter mPlayListAdapter;  // 除开onCreate与onResume外,其他所有对于playlist数据的操作都通过adapter做,好触发onPlaylistItemEvent
    private DragListView mDragPlayListView;

    private List<AllMediaListBean> mAllMediaListBeans = new ArrayList<AllMediaListBean>();
    private AllMediaListAdapter mAllMediaListAdapter;
    private ListView mAllMediaListView;

    private List<UsbMediaListBean> mUsbMediaListBeans = new ArrayList<UsbMediaListBean>();
    private UsbMediaListAdapter mUsbMediaListAdapter;
    private ListView mUsbMediaListView;

    private CheckBox mAllMediaListCheckBox;
    private CheckBox mUsbMediaListCheckBox;

    private TextView mUsbListTitle;
    private Deque<String> mUsbMediaCopyDeque = new ArrayDeque<String>();                        //需要复制的文件
    private Deque<AllMediaListBean> mMediaDeleteDeque = new ArrayDeque<AllMediaListBean>();               //需要删除的文件
    private Deque<String> mMediaSameDeque = new ArrayDeque<String>();                           //复制中重复冲突的文件
    private Deque<String> mCopyDoneDeque = new ArrayDeque<String>();                           //复制完成的文件

    private TextView mUsbCapacityTextView;
    private TextView mLocalCapacityTextView;
    ExecutorService mCapacityUpdateService = Executors.newSingleThreadExecutor();;          //用于更新容量的线程
    ExecutorService mCopyDoneUpdateService = Executors.newSingleThreadExecutor();;          //用于复制后的列表

    private Spinner mUsbPartitionSpinner;
    private ArrayAdapter<String> mUsbPartitionAdapter;

    private final BroadcastReceiver mqttEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Contants.ACTION_PUSH_PLAYLIST)) {

                ArrayList<MediaListPushBean> pushList = intent.getParcelableArrayListExtra(Contants.EXTRA_PUSH_CONTEXT);

                if (pushList==null) {
                    Log.e(TAG,"mqtt receive a error format pushlist!");
                    return;
                }

                mPlayer.mediaStop();

                DataCovert.covertPlayList(getApplicationContext(),mPlayListBeans,pushList);

                mPlayListAdapter.refresh();
                savePlaylistToConfig();

                if (mPlayListBeans.size()>0) {
                    mPlayer.fullScreenSwitch(true);
                    mPlayer.mediaPlay(0);
                }

            } else if (action.equals(Contants.ACTION_PUSH_PLAYMODE)) {

                PlayModePushBean pushReq = intent.getParcelableExtra(Contants.EXTRA_PUSH_CONTEXT);

                if (pushReq==null) {
                    Log.e(TAG,"mqtt receive a error format push play mode!");
                    return;
                }

                saveReplayModeToConfig(getApplicationContext(),pushReq.getPlayMode());
                loadReplayMode();

                saveShowMaskToConfig(getApplicationContext(),pushReq.getMask()==0?false:true);
                mPlayer.getMaskController().showDefaultMask();
            }
        }
    };

    private final BroadcastReceiver usbMountEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                mHandler.removeMessages(MSG_USB_PLUG_IN);
                Message msg = new Message();
                msg.what = MSG_USB_PLUG_IN;
                Bundle bundle = new Bundle();
                bundle.putString(BUNDLE_KEY_STORAGE_PATH, intent.getData().getPath());
                msg.setData(bundle);
                mHandler.sendMessageDelayed(msg, 500);
                ToastUtil.showToast(context, getResources().getString(R.string.str_media_usb_device_plug_in_toast) + intent.getData().getPath());

            } else if (action.equals(Intent.ACTION_MEDIA_REMOVED) || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                mHandler.removeMessages(MSG_USB_PLUG_OUT);
                Message msg = new Message();
                msg.what = MSG_USB_PLUG_OUT;
                Bundle bundle = new Bundle();
                bundle.putString(BUNDLE_KEY_STORAGE_PATH, intent.getData().getPath());
                msg.setData(bundle);
                mHandler.sendMessageDelayed(msg, 500);
                ToastUtil.showToast(context, getResources().getString(R.string.str_media_usb_device_plug_out_toast) + intent.getData().getPath());
            }
        }
    };

    // 媒体播放的回调函数=============开始
    @Override
    public void onMediaPlayFullScreenSwitch(boolean fullScreen) {
        Log.d(TAG, "MediaPlayFullScreenSwitch fullScreen:" + fullScreen);

        if (fullScreen) {

            ((MainApplication)getApplication()).isMediaActivityFullScreenPlaying = true;

            LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) mPlayer.getLayoutParams();
            llp.topMargin = 0;
            llp.leftMargin = 0;
            mPlayer.setLayoutParams(llp);
        } else {

            ((MainApplication)getApplication()).isMediaActivityFullScreenPlaying = false;

            LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) mPlayer.getLayoutParams();
            llp.topMargin = mOriginPlayerMarginTop;
            llp.leftMargin = mOriginPlayerMarginLeft;
            mPlayer.setLayoutParams(llp);
        }
    }

    @Override
    public void onMediaPlayStop() {
        // 播放完毕
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_MEDIA_PLAY_STOP;
        mHandler.sendMessage(msg);
    }

    @Override
    public void onMediaPlayCompletion() {
        // 播放完毕
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_MEDIA_PLAY_COMPLETE;
        mHandler.sendMessage(msg);
    }

    @Override
    public void onMediaPlayInfoUpdate(String name, String mimeType, int width, int height, long size, int bps) {
        // 在UI显示播放信息
        setMediaInfoUI(name, mimeType, width, height, size, bps);

        for (PlayListBean bean : mPlayListAdapter.getListDatas()) {
            bean.setPlaying(false);
        }

        if (!mPlayer.isPreview()) {
            mPlayer.getCurPlaylistBean().setPlaying(true);
        }

        mPlayListAdapter.refresh();
    }

    @Override
    public void onMediaPlayError(int error, int position) {
        //todo error handler,在播放列表里标记出来
        Log.e(TAG,"~~debug~~ onMediaPlayError");
    }
    // 媒体播放的回调函数=============结束


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch(group.getId()) {
            case R.id.rg_media_replay_mode:

                int replayMode = PictureVideoPlayer.MEDIA_REPLAY_MODE_DEFAULT;

                if (checkedId == mReplayOneRadioButton.getId()) {
                    replayMode = PictureVideoPlayer.MEDIA_REPLAY_ONE;
                } else if (checkedId == mReplayAllRadioButton.getId()) {
                    replayMode = PictureVideoPlayer.MEDIA_REPLAY_ALL;
                } else if (checkedId == mReplayShuffleRadioButton.getId()) {
                    replayMode = PictureVideoPlayer.MEDIA_REPLAY_SHUFFLE;
                }

                if (mPlayer != null) {
                    mPlayer.setReplayMode(replayMode);
                }

                saveReplayModeToConfig(getApplicationContext(),replayMode);

                break;

            case R.id.rg_media_list_order_mode:
                int orderMode = MEDIA_LIST_ORDER_SOURCE;
                if (checkedId == mMediaListOrderNameRadioButton.getId()) {
                    orderMode = MEDIA_LIST_ORDER_NAME;
                } else if (checkedId == mMediaListOrderDurationRadioButton.getId()) {
                    orderMode = MEDIA_LIST_ORDER_DURATION;
                } else if (checkedId == mMediaListOrderSourceRadioButton.getId()) {
                    orderMode = MEDIA_LIST_ORDER_SOURCE;
                }

                saveMediaOrderModeToConfig(getApplicationContext(),orderMode);

                Message msg = mHandler.obtainMessage();
                msg.what = MSG_MEDIA_DATABASE_UI_SYNC;
                mHandler.sendMessage(msg);

                break;

            default:
                Log.e(TAG,"unknown RadioGroup onCheckedChanged!");
                break;
        }


    }

    public static void saveMediaOrderModeToConfig(Context context, int orderMode) {
        SharedPreferencesUtil config = new SharedPreferencesUtil(context, Contants.PREF_CONFIG);
        config.putInt(CONFIG_MEDIA_LIST_ORDER, orderMode);
    }

    public static int loadMediaOrderModeFromConfig(Context context) {
        SharedPreferencesUtil config = new SharedPreferencesUtil(context, Contants.PREF_CONFIG);
        return config.getInt(CONFIG_MEDIA_LIST_ORDER, MEDIA_LIST_ORDER_DEFAULT);
    }

    private void loadMediaOrder() {
        int orderMode = loadMediaOrderModeFromConfig(getApplicationContext());
        int checkId = mMediaListOrderNameRadioButton.getId();

        switch (orderMode) {
            case MEDIA_LIST_ORDER_NAME:
                checkId = mMediaListOrderNameRadioButton.getId();
                break;
            case MEDIA_LIST_ORDER_DURATION:
                checkId = mMediaListOrderDurationRadioButton.getId();
                break;
            case MEDIA_LIST_ORDER_SOURCE:
                checkId = mMediaListOrderSourceRadioButton.getId();
                break;
            default:
                break;
        }

        mMediaListOrderRadioGroup.check(checkId);
    }


    public static void saveReplayModeToConfig(Context context, int replayMode) {
        SharedPreferencesUtil config = new SharedPreferencesUtil(context, Contants.PREF_CONFIG);
        config.putInt(CONFIG_REPLAY_MODE, replayMode);
    }

    public static int loadReplayModeFromConfig(Context context) {
        SharedPreferencesUtil config = new SharedPreferencesUtil(context, Contants.PREF_CONFIG);
        return config.getInt(CONFIG_REPLAY_MODE, PictureVideoPlayer.MEDIA_REPLAY_MODE_DEFAULT);
    }

    public static void saveShowMaskToConfig(Context context, boolean showMask) {
        SharedPreferencesUtil config = new SharedPreferencesUtil(context, Contants.PREF_CONFIG);
        config.putBoolean(CONFIG_SHOW_MASK,showMask);
    }

    private void loadReplayMode() {
        int playMode = loadReplayModeFromConfig(getApplicationContext());
        int checkId = mReplayAllRadioButton.getId();
        switch (playMode) {
            case PictureVideoPlayer.MEDIA_REPLAY_ONE:
                checkId = mReplayOneRadioButton.getId();
                break;
            case PictureVideoPlayer.MEDIA_REPLAY_ALL:
                checkId = mReplayAllRadioButton.getId();
                break;
            case PictureVideoPlayer.MEDIA_REPLAY_SHUFFLE:
                checkId = mReplayShuffleRadioButton.getId();
                break;
            default:
                break;
        }
        mReplayModeRadioGroup.check(checkId);

        if (mPlayer != null) {
            mPlayer.setReplayMode(playMode);
        }
    }

    private void loadMediaListFromDatabase() {
        // 初始化本地媒体列表
        if (new MediaBeanDao(MediaActivity.this).selectAll().isEmpty())
        {   // 如果数据库为空,则扫描一次本地媒体文件
            Log.i(TAG, "empty media database, so scan and download media list");
            String localMediaStorePath = LOCAL_MASS_STORAGE_PATH + File.separator + LOCAL_MEDIA_FOLDER;
            FileUtil.createDir(localMediaStorePath);
            int ret = mLocalMediaScanner.safeScanning(localMediaStorePath);
            if (ret==-2) {
                Log.e(TAG,"LOCAL_MASS_STORAGE_PATH not found!");
                ToastUtil.showToast(getApplicationContext(),"LOCAL_MASS_STORAGE_PATH not found");
            }
            loadMediaListFromCloud();
        } else {
            // 如果有数据库,则从数据库获取
            for (MediaBean m:new MediaBeanDao(MediaActivity.this).selectAll())
            {
                Log.i(TAG, "has media database, so get media list from media database");
                mAllMediaListBeans.add(new AllMediaListBean(m));
            }

            if (mAllMediaListAdapter!=null) {
                mAllMediaListAdapter.refresh();
            }
        }
    }

    private void loadMediaListFromCloud() {
        // 初始化云端媒体列表
        OkHttpClient mClient = new OkHttpClient();
        if (HttpConstants.URL_GET_SHARE_LIST.contains("https")) {
            try {
                mClient = new OkHttpClient.Builder()
                        .sslSocketFactory(SSLContext.getDefault().getSocketFactory())
                        .build();
            } catch (Exception e) {
                Log.e(TAG,"Exception in load media list from cloud " + e.toString());
            }
        } else {
            mClient = new OkHttpClient();
        }

        FormBody body = new FormBody.Builder()
                .add("sn", DeviceInfoActivity.getSysSN())
                .build();

        Request request = new Request.Builder().url(HttpConstants.URL_GET_SHARE_LIST).post(body).build();
        Call call = mClient.newCall(request);

        // 只要收到返回,就让用户看到界面有一个刷新的效果
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCloudMediaListRefreshBtn.setEnabled(false);
            }
        });

        call.enqueue(new okhttp3.Callback() {
            // 失败
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCloudMediaListRefreshBtn.setEnabled(true);
                    }
                });

                Log.e(TAG,"onFailure" + e.toString());
            }

            // 成功
            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {
                // 只要收到返回,就让用户看到界面有一个刷新的效果
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCloudMediaListRefreshBtn.setEnabled(true);
                        mAllMediaListAdapter.clear();
                        mAllMediaListAdapter.refresh();
                    }
                });

                if (!response.isSuccessful()) {
                    throw new IOException("onResponse unexpected return code " + response);

                } else if (response.code() == HttpConstants.HTTP_STATUS_SUCCESS) {
                    String htmlBody = response.body().string();

                    CloudListBean cloudList;
                    try {
                        cloudList = new Gson().fromJson(htmlBody, CloudListBean.class);
                    } catch (IllegalStateException e) {
                        cloudList = null;
                        Log.e(TAG, "except in json parse!" + e.toString());
                    }

                    if  (cloudList != null) {
                        if (cloudList.getStatus().equals(LOGIN_STATUS_SUCCESS)) {
                            Log.d(TAG,"onResponse get "+cloudList.getResult().size() + " media info(s) from cloud");
                            new MediaBeanDao(MediaActivity.this).deleteItemsFromCloud();

                            if (!cloudList.getResult().isEmpty()) {
                                List<MediaBean> listCloud = new ArrayList<>();
                                DataCovert.covertMediaList(MediaActivity.this, listCloud, cloudList.getResult());
                                new MediaBeanDao(MediaActivity.this).insert(listCloud);
                            }
                        } else if (cloudList.getStatus().equals(LOGIN_STATUS_NOT_YET)) {
                            Log.d(TAG,"onResponse device not login yet,need user login this device!");
                        } else {
                            Log.d(TAG,"onResponse unknown status!");
                        }
                    } else {
                        Log.e(TAG, "cloud media list json parse error! " + htmlBody);
                    }


                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_MEDIA_DATABASE_UI_SYNC;
                    mHandler.sendMessage(msg);

                } else {
                    Log.e(TAG, "onResponse response http code undefine!");
                }
            }
        });
    }

    public void savePlaylistToConfig() {
        savePlaylistToConfig(this,mPlayListBeans);
    }

    public static void savePlaylistToConfig(Context context, List<PlayListBean> pList) {
        SharedPreferencesUtil config = new SharedPreferencesUtil(context, Contants.PREF_CONFIG);
        Gson gson = new Gson();
        String jsonStr = gson.toJson(pList);
        config.putString(CONFIG_PLAYLIST, jsonStr);
    }

    public static boolean hasPlaylistConfig(Context context) {
        SharedPreferencesUtil config = new SharedPreferencesUtil(context, Contants.PREF_CONFIG);
        String jsonStr = config.getString(CONFIG_PLAYLIST, "[]");
        if (jsonStr.equals("[]")) {
            return false;
        } else {
            return true;
        }
    }

    private void loadPlaylistFromConfig() {
        Gson gson = new Gson();
        SharedPreferencesUtil config = new SharedPreferencesUtil(this, Contants.PREF_CONFIG);
        String jsonStr = config.getString(CONFIG_PLAYLIST, "[]");
        Type type = new TypeToken<List<PlayListBean>>() {
        }.getType();

        List<PlayListBean> data = gson.fromJson(jsonStr, type);
        mPlayListBeans.clear();
        for (int i = 0; i < data.size(); i++) {
            mPlayListBeans.add(data.get(i));
        }

        if (mPlayListAdapter != null) {
            mPlayListAdapter.refresh();
        }
    }

    private void initView() {
        setContentView(R.layout.activity_media);

        mPlayer = (com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer) findViewById(R.id.pictureVideoPlayer_playArea);

        mMultiCopyToLocalBtn = (Button) findViewById(R.id.bt_media_multi_copy_to_left);
        mMultiDeleteLocalBtn = (Button) findViewById(R.id.bt_media_multi_delete);
        mMultiAddToPlayListBtn = (Button) findViewById(R.id.bt_media_multi_add);
        mMultiDownloadBtn = (Button) findViewById(R.id.bt_media_multi_download);
        mMultiCopyToUsbBtn = (Button) findViewById(R.id.bt_media_multi_copy_to_right);

        mLocalMediaListRefreshBtn = (Button) findViewById(R.id.bt_media_local_list_refresh);
        mCloudMediaListRefreshBtn = (Button) findViewById(R.id.bt_media_cloud_list_refresh);

        mMediaInfoNameTextView = (TextView) findViewById(R.id.tv_media_play_info_name);
        mMediaInfoTypeTextView = (TextView) findViewById(R.id.tv_media_play_info_type);
        mMediaInfoWidthHeightTextView = (TextView) findViewById(R.id.tv_media_play_info_wh);
        mMediaInfoSizeTextView = (TextView) findViewById(R.id.tv_media_play_info_size);
        mMediaInfoBpsTextView = (TextView) findViewById(R.id.tv_media_play_info_bps);

        mReplayModeRadioGroup = (RadioGroup) findViewById(R.id.rg_media_replay_mode);
        mReplayAllRadioButton = (RadioButton) findViewById(R.id.rb_media_replay_all);
        mReplayOneRadioButton = (RadioButton) findViewById(R.id.rb_media_replay_one);
        mReplayShuffleRadioButton = (RadioButton) findViewById(R.id.rb_media_replay_shuffle);

        mMediaListOrderRadioGroup = (RadioGroup) findViewById(R.id.rg_media_list_order_mode);
        mMediaListOrderNameRadioButton = (RadioButton) findViewById(R.id.rb_media_order_name);
        mMediaListOrderDurationRadioButton = (RadioButton) findViewById(R.id.rb_media_order_duration);
        mMediaListOrderSourceRadioButton = (RadioButton) findViewById(R.id.rb_media_order_source);

        mAllMediaListView = (ListView) findViewById(R.id.lv_media_all_list);
        mUsbMediaListView = (ListView) findViewById(R.id.lv_media_usb_list);
        mDragPlayListView = (DragListView) findViewById(R.id.lv_media_playList);

        mLocalCapacityTextView = (TextView) findViewById(R.id.tv_media_all_list_capacity);
        mUsbCapacityTextView = (TextView) findViewById(R.id.tv_media_usb_list_capacity);
        mUsbPartitionSpinner = (Spinner) findViewById(R.id.sp_media_usb_partition_spinner);

        mUsbListTitle = (TextView) findViewById(R.id.tv_media_usb_list_name);

        mAllMediaListCheckBox = (CheckBox) findViewById(R.id.cb_media_all_list_check);
        mUsbMediaListCheckBox = (CheckBox) findViewById(R.id.cb_media_usb_list_check);
    }

    private void prepareListView() {
        // usb设备选择列表
        mUsbPartitionSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                // spinner由空到有第一个会跳过来选中一次
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_USB_PARTITION_SWITCH;
                msg.arg1 = position;
                msg.obj = mUsbPartitionAdapter.getItem(position).toString();
                mHandler.sendMessage(msg);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_USB_PARTITION_SWITCH;
                msg.arg1 = AdapterView.INVALID_POSITION;
                msg.obj = "none";
                mHandler.sendMessage(msg);
            }
        });
        mUsbPartitionAdapter = new ArrayAdapter<String>(this, R.layout.item_usb_partition_spinner, R.id.tv_partition_idx);
        mUsbPartitionSpinner.setAdapter(mUsbPartitionAdapter);

        // 云端本地的全媒体列表
        mAllMediaListAdapter = new AllMediaListAdapter(getApplicationContext(), mAllMediaListBeans);
        mAllMediaListAdapter.setOnALlMediaListItemListener(new AllMediaListAdapter.OnAllMediaListItemEventListener() {
            @Override
            public void doItemDelete(int position) {
                mMediaDeleteDeque.clear();
                mMediaDeleteDeque.push(mAllMediaListAdapter.getItem(position));
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_MEDIA_LIST_ITEM_DELETE;
                mHandler.sendMessage(msg);
            }

            @Override
            public void doItemPreview(int position) {
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_MEDIA_LIST_ITEM_PREVIEW;
                msg.obj = mAllMediaListAdapter.getItem(position).getMediaData();
                mHandler.sendMessage(msg);
            }

            @Override
            public void doItemDownLoad(int position) {
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_MEDIA_LIST_ITEM_DOWNLOAD;
                msg.obj = mAllMediaListAdapter.getItem(position).getMediaData();
                mHandler.sendMessage(msg);
            }

            @Override
            public void itemSelectedChange() {
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_MEDIA_LIST_SELECTED_CHANGE;
                mHandler.sendMessage(msg);
            }
        });
        mAllMediaListView.setAdapter(mAllMediaListAdapter);
        mAllMediaListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 如果是双击
                if (ViewUtil.isFastDoubleClick()) {
                    MediaBean mediaItem = mAllMediaListBeans.get(position).getMediaData();
                    if (mediaItem.isDownload()) {
                        mPlayListAdapter.addItem(new PlayListBean(mediaItem));
                    } else {
                        ToastUtil.showToast(MediaActivity.this, getResources().getString(R.string.str_media_cloud_file_need_download_toast));
                    }
                }
            }
        });

        // usb设备媒体列表
        mUsbMediaListAdapter = new UsbMediaListAdapter(getApplicationContext(), mUsbMediaListBeans);
        mUsbMediaListAdapter.setOnUsbItemEventListener(new UsbMediaListAdapter.OnUsbItemEventListener() {
            @Override
            public void doItemCopy(int position) {
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_USB_COPY_TO_INTERNAL;
                mUsbMediaCopyDeque.clear();
                mUsbMediaCopyDeque.push(mUsbMediaListAdapter.getItem(position).getPath());
                mHandler.sendMessage(msg);
            }

            @Override
            public void itemSelectedChange() {
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_USB_LIST_SELECTED_CHANGE;
                if (mUsbMediaListAdapter.hasItemSelected()) {
                    msg.arg1 = 1;
                } else {
                    msg.arg1 = 0;
                }
                mHandler.sendMessage(msg);
            }
        });

        mUsbMediaListView.setAdapter(mUsbMediaListAdapter);
        mUsbMediaListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 如果是双击
                if (ViewUtil.isFastDoubleClick()) {
                    Log.i(TAG, "double click usb media list!");
                    // 预留了双击的处理,但是目前没有做任何操作
                }
            }
        });

        // 播放列表
        mPlayListAdapter = new PlayListAdapter(getApplicationContext(), mPlayListBeans);
        mPlayListAdapter.setOnPlaylistItemEventListener(new PlayListAdapter.OnPlaylistItemEventListener() {
            @Override
            public void onPlaylistChange() {
                savePlaylistToConfig();
            }

            @Override
            public void onScaleChange(int position, int scaleType) {
                if (mPlayer.getPlayState()!= MEDIA_IDLE
                        && mPlayer.getPlayState()!= PictureVideoPlayer.MediaPlayState.MEDIA_PLAY_COMPLETE
                        && !mPlayer.isPreview()) {
                    if (mPlayer.getCurPlaylistBean().equals(mPlayListAdapter.getItem(position))) {
                        // 播放的position可能会因为改变顺序的原因而与现在的playlist不对对应.所以使用Bean比较
                        mPlayer.changeScaleNow(scaleType);
                    }
                }
            }
        });

        mDragPlayListView.setAdapter(mPlayListAdapter);
        mDragPlayListView.setDragItemListener(new DragListView.SimpleAnimationDragItemListener() {
            private Rect mFrame = new Rect();
            private boolean mIsSelected;

            @Override
            public boolean canDrag(View dragView, int x, int y) {
                // 获取可拖拽的图标
                View dragger = dragView.findViewById(R.id.iv_media_playlist_move);
                if (dragger == null || dragger.getVisibility() != View.VISIBLE) {
                    return false;
                }
                float tx = x - ViewUtil.getX(dragView);
                float ty = y - ViewUtil.getY(dragView);
                dragger.getHitRect(mFrame);
                if (mFrame.contains((int) tx, (int) ty)) { //点击的点在拖拽图标位置
                    return true;
                }
                return false;
            }

            @Override
            public void beforeDrawingCache(View dragView) {
                mIsSelected = dragView.isSelected();
                View drag = dragView.findViewById(R.id.iv_media_playlist_move);
                dragView.setSelected(true);
                if (drag != null) {
                    drag.setSelected(true);
                }
            }

            @Override
            public Bitmap afterDrawingCache(View dragView, Bitmap bitmap) {
                dragView.setSelected(mIsSelected);
                View drag = dragView.findViewById(R.id.iv_media_playlist_move);
                if (drag != null) {
                    drag.setSelected(false);
                }
                return bitmap;
            }

            @Override
            public boolean canExchange(int srcPosition, int position) {
                boolean result = mPlayListAdapter.exchange(srcPosition, position);
                return result;
            }
        });

        mDragPlayListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 如果是双击
                if (ViewUtil.isFastDoubleClick()) {
                    Log.i(TAG, "double click play list!");
                    mPlayer.mediaStop();
                    mPlayer.mediaPlay(position);
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();

        LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) mPlayer.getLayoutParams();
        mOriginPlayerMarginTop = llp.topMargin;
        mOriginPlayerMarginLeft = llp.leftMargin;

        mAllMediaListCheckBox.setOnClickListener(this);
        mUsbMediaListCheckBox.setOnClickListener(this);

        mMultiCopyToLocalBtn.setOnClickListener(this);
        mMultiDeleteLocalBtn.setOnClickListener(this);
        mMultiAddToPlayListBtn.setOnClickListener(this);
        mMultiDownloadBtn.setOnClickListener(this);
        mMultiCopyToUsbBtn.setOnClickListener(this);

        mLocalMediaListRefreshBtn.setOnClickListener(this);
        mCloudMediaListRefreshBtn.setOnClickListener(this);

        mReplayModeRadioGroup.setOnCheckedChangeListener(this);
        mMediaListOrderRadioGroup.setOnCheckedChangeListener(this);

        prepareListView();

        mPlayer.setPlayList(mPlayListBeans);
        mPlayer.setOnMediaEventListener(this);

        //递归扫描sd卡根目录
        mLocalMediaScanner = new MediaScanUtil();
        mLocalMediaScanner.setNeedDuration(true);
        mLocalMediaScanner.setMediaFileScanListener(new MediaScanUtil.MediaFileScanListener() {
            @Override
            public void onMediaScanBegin() {
                Log.i(TAG, "local media scan begin!");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLocalMediaListRefreshBtn.setEnabled(false);
                    }
                });

                Message msg = mHandler.obtainMessage();
                msg.what = MSG_MEDIA_LIST_CLEAN_LOCAL;
                mHandler.sendMessage(msg);
            }

            @Override
            public void onFindMedia(int type, String name, String extension, String path, int duration, long size) {
                if (type == MEDIA_PICTURE || type == MEDIA_VIDEO) {
                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_LOCAL_MEDIA_DATABASE_UPDATE;
                    Bundle b = new Bundle();
                    b.putInt(BUNDLE_KEY_MEDIA_TYPE, type);
                    b.putLong(BUNDLE_KEY_MEDIA_SIZE, size);
                    b.putInt(BUNDLE_KEY_MEDIA_DURATION, duration);
                    b.putString(BUNDLE_KEY_MEDIA_PATH, path);
                    b.putString(BUNDLE_KEY_MEDIA_NAME, name);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                }
            }

            @Override
            public void onMediaScanDone() {
                Log.i(TAG, "local media scan done!");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLocalMediaListRefreshBtn.setEnabled(true);
                    }
                });

                Message msg = mHandler.obtainMessage();
                msg.what = MSG_MEDIA_DATABASE_UI_SYNC;
                mHandler.sendMessage(msg);
            }
        });


        mUsbMediaScanner = new MediaScanUtil();
        mUsbMediaScanner.setNeedSize(true);
        mUsbMediaScanner.setMediaFileScanListener(new MediaScanUtil.MediaFileScanListener() {
            @Override
            public void onMediaScanBegin() {
                Log.i(TAG, "usb media scan begin!");
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_USB_MEDIA_LIST_CLEAN;
                mHandler.sendMessage(msg);
            }

            @Override
            public void onFindMedia(int type, String name, String extension, String path, int duration, long size) {
                if (type == MEDIA_PICTURE || type == MEDIA_VIDEO) {
                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_USB_MEDIA_LIST_UPDATE;
                    Bundle b = new Bundle();
                    b.putInt(BUNDLE_KEY_MEDIA_TYPE, type);
                    b.putString(BUNDLE_KEY_MEDIA_NAME, name);
                    b.putString(BUNDLE_KEY_MEDIA_PATH, path);
                    b.putLong(BUNDLE_KEY_MEDIA_SIZE, size);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                }
            }

            @Override
            public void onMediaScanDone() {
                Log.i(TAG, "usb media scan done!");
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_USB_MEDIA_SCAN_DONE;
                mHandler.sendMessage(msg);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        LinearLayout layout = (LinearLayout) findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.shape_background);

        // mqtt更改了配置,并将mediaActivity叫起来
        loadPlaylistFromConfig();
        loadReplayMode();
        loadMediaOrder();

        loadMediaListFromDatabase();
        updateMultiActionButtonState();

        // 监听usb插拔事件
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        usbFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        usbFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        usbFilter.addDataScheme("file");
        registerReceiver(usbMountEventReceiver, usbFilter);

        // 监听mqtt控制命令
        IntentFilter mqttFilter = new IntentFilter();
        mqttFilter.addAction(Contants.ACTION_PUSH_PLAYLIST);
        mqttFilter.addAction(Contants.ACTION_PUSH_PLAYMODE);
        registerReceiver(mqttEventReceiver, mqttFilter);


        mPlayer.setStartRightNow(true);

        // 主动枚举一次usb设备,防止在此activity无法接受usbReceiver的时候有u盘设备插上
        discoverMountDevice();

        // 开线程去查询本地容量
        updateCapacity(true, LOCAL_MASS_STORAGE_PATH);

        ((MainApplication)getApplication()).isMediaActivityForeground = true;
        ((MainApplication)getApplication()).mFirstInitDone = true;
    }

    @Override
    protected void onPause() {
        if (mPlayer != null) {
            mPlayer.mediaStop();
        }
        unregisterReceiver(usbMountEventReceiver);
        unregisterReceiver(mqttEventReceiver);

        super.onPause();

        ((MainApplication)getApplication()).isMediaActivityForeground = false;
    }

    @Override
    protected void onDestroy() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }

        if (mCapacityUpdateService != null) {
            mCapacityUpdateService.shutdownNow();
            mCapacityUpdateService = null;
        }

        if (mCopyDoneUpdateService != null) {
            mCopyDoneUpdateService.shutdownNow();
            mCopyDoneUpdateService = null;
        }

        mLocalMediaScanner.release();
        mUsbMediaScanner.release();
        mLocalMediaScanner = null;
        mUsbMediaScanner = null;

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mPlayer.isFullScreen()) {
            new AlertDialog.Builder(this).setIcon(R.drawable.img_media_warning)
                    .setTitle(R.string.str_media_full_screen_quite)
                    .setPositiveButton(R.string.str_media_dialog_button_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mPlayer.fullScreenSwitch(false);
                        }
                    }).setNegativeButton(R.string.str_media_dialog_button_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            }).show();
        } else {
            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
            startActivity(intent);
            this.finish();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.bt_media_local_list_refresh:
                String localMediaStorePath = LOCAL_MASS_STORAGE_PATH + File.separator + LOCAL_MEDIA_FOLDER;
                FileUtil.createDir(localMediaStorePath);
                int ret = mLocalMediaScanner.safeScanning(localMediaStorePath);
                if (ret==-2) {
                    Log.e(TAG,"LOCAL_MASS_STORAGE_PATH not found!");
                    ToastUtil.showToast(getApplicationContext(),"LOCAL_MASS_STORAGE_PATH not found!");
                }
                break;

            case R.id.bt_media_cloud_list_refresh:
                loadMediaListFromCloud();
                break;

            case R.id.cb_media_all_list_check:
                for (AllMediaListBean data : mAllMediaListBeans) {
                    data.setSelected(mAllMediaListCheckBox.isChecked());
                }
                mAllMediaListAdapter.refresh();
                updateMultiActionButtonState();
                break;

            case R.id.cb_media_usb_list_check:
                for (UsbMediaListBean data : mUsbMediaListBeans) {
                    data.setSelected(mUsbMediaListCheckBox.isChecked());
                }
                mMultiCopyToLocalBtn.setEnabled(mUsbMediaListAdapter.hasItemSelected());
                mUsbMediaListAdapter.refresh();
                break;


            case R.id.bt_media_multi_copy_to_left:
                mUsbMediaCopyDeque.clear();
                for (UsbMediaListBean data : mUsbMediaListAdapter.getListDatas()) {
                    if (data.isSelected()) {
                        mUsbMediaCopyDeque.add(data.getPath());
                    }
                }

                if (!mUsbMediaCopyDeque.isEmpty()) {
                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_USB_COPY_TO_INTERNAL;
                    mHandler.sendMessage(msg);
                }
                break;

            case R.id.bt_media_multi_delete:
                mMediaDeleteDeque.clear();
                for (AllMediaListBean data : mAllMediaListBeans) {
                    if (data.isSelected()) {
                        mMediaDeleteDeque.push(data);
                        // 这边只加入等待删除的列表,从播放列表以及数据库删除记录的动作,在处理msg的地方做
                    }
                }

                Message msg = mHandler.obtainMessage();
                msg.what = MSG_MEDIA_LIST_ITEM_DELETE;
                mHandler.sendMessage(msg);
                break;

            case R.id.bt_media_multi_add:
                for (AllMediaListBean data : mAllMediaListBeans) {
                    if (data.isSelected()) {
                        if (data.getMediaData().isDownload()) {
                            mPlayListAdapter.addItem(new PlayListBean(data.getMediaData()));
                        } else {
                            ToastUtil.showToast(MediaActivity.this, getResources().getString(R.string.str_media_cloud_file_need_download_toast));
                        }
                    }
                }
                break;

            case R.id.bt_media_multi_download:
                break;

            case R.id.bt_media_multi_copy_to_right:
                mUsbMediaCopyDeque.clear();
                for (AllMediaListBean data : mAllMediaListAdapter.getListDatas()) {
                    if (data.isSelected()) {
                        if (data.getMediaData().getSource()==MediaBean.SOURCE_LOCAL) {
                            mUsbMediaCopyDeque.add(data.getMediaData().getPath());
                        } else {
                            ToastUtil.showToast(MediaActivity.this, getResources().getString(R.string.str_media_cloud_file_could_not_export_toast));
                        }
                    }
                }

                if (!mUsbMediaCopyDeque.isEmpty()) {
                    copyInternalFileToUsb();
                }
                break;

            default:
                break;
        }
    }

    private void updateMultiActionButtonState()
    {
        if (mAllMediaListAdapter.hasItemSelected()) {
            // 额外需要检查是否存在usb设备
            if (mUsbPartitionSpinner.getSelectedItemPosition()!=AdapterView.INVALID_POSITION) {
                mMultiCopyToUsbBtn.setEnabled(true);
            } else {
                mMultiCopyToUsbBtn.setEnabled(false);
            }
            mMultiAddToPlayListBtn.setEnabled(true);
            mMultiDeleteLocalBtn.setEnabled(true);
            mMultiDownloadBtn.setEnabled(true);
        } else {
            mMultiCopyToUsbBtn.setEnabled(false);
            mMultiAddToPlayListBtn.setEnabled(false);
            mMultiDeleteLocalBtn.setEnabled(false);
            mMultiDownloadBtn.setEnabled(false);
        }

        if (mUsbMediaListAdapter.hasItemSelected()) {
            mMultiCopyToLocalBtn.setEnabled(true);
        } else {
            mMultiCopyToLocalBtn.setEnabled(false);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_USB_PLUG_IN:
                    String storagePath = msg.getData().getString(BUNDLE_KEY_STORAGE_PATH);

                    if (LOCAL_MASS_STORAGE_PATH.equals(storagePath)) {
                        //挂载了硬盘设备,很可能是开机,直接触发播放
                        Log.i(TAG, "mount sata device, power on complete, start play media");
                        if (mPlayer.getPlayState()==MEDIA_IDLE) {
                            mPlayer.fullScreenSwitch(true);
                            mPlayer.mediaPlay(0);
                        }
                    } else {
                        discoverMountDevice();
                    }
                    break;

                case MSG_USB_PLUG_OUT:
                    storagePath = msg.getData().getString(BUNDLE_KEY_STORAGE_PATH);
                    String currentSelect = "";
                    if (mUsbPartitionSpinner.getSelectedItem()!=null) {
                        currentSelect = mUsbPartitionSpinner.getSelectedItem().toString();
                    }

                    discoverMountDevice();

                    if (mUsbPartitionSpinner.getSelectedItem()!=null) {
                        if (!currentSelect.equals(mUsbPartitionSpinner.getSelectedItem().toString()))
                        {
                            msg = mHandler.obtainMessage();
                            msg.what = MSG_USB_PARTITION_SWITCH;
                            msg.arg1 = mUsbPartitionSpinner.getSelectedItemPosition();
                            msg.obj = mUsbPartitionSpinner.getSelectedItem().toString();
                            mHandler.sendMessage(msg);
                        }
                    }
                    break;

                case MSG_USB_PARTITION_SWITCH:
                    // 处理 用户选择usb设备 或 插拔事件导致usb设备选择改变
                    // 首先将容量信息消除,等待线程查询成功后再发消息更新
                    mUsbCapacityTextView.setText(R.string.str_media_wait_capacity);

                    if (msg.arg1 == AdapterView.INVALID_POSITION) {
                        // 没有usb设备了
                        // mUsbListTitle.setText("无USB设备");
                        mUsbMediaListAdapter.clear();
                        mUsbMediaListAdapter.refresh();
                    } else if (msg.arg1 < mUsbPartitionAdapter.getCount()) {
                        final String currentPath = (String) msg.obj;
                        //mUsbListTitle.setText(currentPath);

                        // 开线程去查询currentPath容量
                        updateCapacity(false,currentPath);

                        // 由扫描来更新usb media列表
                        int ret = mUsbMediaScanner.safeScanning(currentPath + File.separator + USB_DEVICE_DEFAULT_SEARCH_MEDIA_FOLDER);
                        if (ret==-2) {
                            // 根本没有这个目录
                            mUsbMediaListAdapter.clear();
                            mUsbMediaListAdapter.refresh();
                        }
                    }
                    updateMultiActionButtonState();
                    break;

                case MSG_LOCAL_MEDIA_DATABASE_UPDATE:
                    Bundle b = msg.getData();
                    int type = b.getInt(BUNDLE_KEY_MEDIA_TYPE);
                    String name = b.getString(BUNDLE_KEY_MEDIA_NAME);
                    String path = b.getString(BUNDLE_KEY_MEDIA_PATH);
                    int duration = b.getInt(BUNDLE_KEY_MEDIA_DURATION);
                    long size = b.getLong(BUNDLE_KEY_MEDIA_SIZE);

                    MediaBean data = new MediaBean(name, ID_LOCAL, type, MediaBean.SOURCE_LOCAL, path, duration, size, true);
                    new MediaBeanDao(MediaActivity.this).insert(data);
                    break;

                case MSG_USB_MEDIA_LIST_UPDATE:
                    b = msg.getData();
                    name = b.getString(BUNDLE_KEY_MEDIA_NAME);
                    type = b.getInt(BUNDLE_KEY_MEDIA_TYPE);
                    size = b.getLong(BUNDLE_KEY_MEDIA_SIZE);
                    path = b.getString(BUNDLE_KEY_MEDIA_PATH);

                    UsbMediaListBean UsbMedia = new UsbMediaListBean(name, type, path, size);
                    mUsbMediaListAdapter.addItem(UsbMedia);
                    mUsbMediaListAdapter.refresh();
                    break;

                case MSG_USB_MEDIA_LIST_CLEAN:
                    mUsbMediaListAdapter.clear();
                    mUsbMediaListAdapter.refresh();
                    break;

                case MSG_MEDIA_LIST_CLEAN_LOCAL:
                    mAllMediaListAdapter.clear();
                    mAllMediaListAdapter.refresh();
                    new MediaBeanDao(MediaActivity.this).deleteItemsLocalImport();
                    break;

                case MSG_USB_COPY_TO_INTERNAL:
                    if (!mUsbMediaCopyDeque.isEmpty()) {
                        copyUsbFileToInternal();
                    }
                    break;

                case MSG_USB_LIST_SELECTED_CHANGE:
                    if (msg.arg1==1) {
                        mMultiCopyToLocalBtn.setEnabled(true);
                    } else {
                        mMultiCopyToLocalBtn.setEnabled(false);
                    }
                    break;

                case MSG_MEDIA_LIST_SELECTED_CHANGE:
                    updateMultiActionButtonState();
                    break;


                case MSG_MEDIA_LIST_ITEM_DELETE:
                    if (!mMediaDeleteDeque.isEmpty()) {
                        deleteInternalMediaFile();
                    }
                    break;

                case MSG_MEDIA_LIST_ITEM_DOWNLOAD:
                    Intent intent = new Intent().setAction(DownloadService.ACTION_START);
                    Log.i("TAG",intent.getAction().toString());
                    startService(intent);
//todo download
                    break;

                case MSG_MEDIA_LIST_ITEM_PREVIEW:
                    MediaBean previewItem = (MediaBean)msg.obj;
                    mPlayer.mediaStop();
                    mPlayer.mediaPreview(previewItem);
                    break;

                case MSG_MEDIA_DATABASE_UI_SYNC:
                    mAllMediaListAdapter.clear();

                    int orderMode = loadMediaOrderModeFromConfig(getApplicationContext());

                    List<MediaBean> allDataItems = new ArrayList<>();
                    if (orderMode==MEDIA_LIST_ORDER_NAME) {
                        allDataItems = new MediaBeanDao(MediaActivity.this).selectAllByNameOrder(true);
                    } else if (orderMode==MEDIA_LIST_ORDER_DURATION) {
                        allDataItems = new MediaBeanDao(MediaActivity.this).selectAllByDurationOrder(true);
                    } else if (orderMode== MEDIA_LIST_ORDER_SOURCE) {
                        allDataItems = new MediaBeanDao(MediaActivity.this).selectAllBySourceOrder(true);
                    }

                    for (MediaBean m:allDataItems)
                    {
                        mAllMediaListAdapter.addItem(new AllMediaListBean(m));
                        Log.d(TAG,m.toString());
                    }
                    mAllMediaListAdapter.refresh();
                    updateMultiActionButtonState();
                    break;

                case MSG_USB_MEDIA_SCAN_DONE:
                    mMultiCopyToLocalBtn.setEnabled(mUsbMediaListAdapter.hasItemSelected());
                    break;

                case MSG_MEDIA_PLAY_COMPLETE:
                    mPlayer.mediaReplay();
                    break;

                case MSG_MEDIA_PLAY_STOP:
                    break;
            }
        }
    };

    private void setMediaInfoUI(final String name,final String mimeType, final int width, final int height, final long size, final int bps) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMediaInfoNameTextView.setText(name);
                mMediaInfoTypeTextView.setText(mimeType);
                mMediaInfoWidthHeightTextView.setText(width + "*" + height);
                mMediaInfoSizeTextView.setText(FileUtil.formatFileSize(size));
                if (bps>0) {
                    mMediaInfoBpsTextView.setText(FileUtil.formatFileSize(bps) + " bit/sec");
                } else {
                    mMediaInfoBpsTextView.setText("");
                }
            }
        });
    }

    private void updateUsbDeviceCapacityUI(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mUsbCapacityTextView.setText(text);
            }
        });
    }

    public static boolean localMassStorageMounted(Context context) {
        String[] mountList = FileUtil.getMountVolumePaths(context);
        if (Arrays.asList(mountList).contains(LOCAL_MASS_STORAGE_PATH)) {
            return true;
        } else {
            return false;
        }
    }

    private void updateLocalCapacityUI(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLocalCapacityTextView.setText(text);
            }
        });
    }

    private void updateCapacity(final boolean internal, final String path)
    {
        mCapacityUpdateService.execute(
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            String fsUsed = FileUtil.formatFileSize(FileUtil.getTotalCapacity(path) -
                                    FileUtil.getAvailableCapacity(path));
                            String fsCapacity = FileUtil.formatFileSize(FileUtil.getTotalCapacity(path));
                            if (internal){
                                updateLocalCapacityUI(fsUsed + "/" + fsCapacity);
                            } else {
                                updateUsbDeviceCapacityUI(fsUsed + "/" + fsCapacity);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "mCapacityUpdateService error!" + e);
                        }
                    }
                });
    }

    private void updateCopyItemToMediaList()
    {
        mCopyDoneUpdateService.execute(
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            String path;
                            while(!mCopyDoneDeque.isEmpty()) {
                                path = mCopyDoneDeque.pop();
                                Message msg = mHandler.obtainMessage();
                                msg.what = MSG_LOCAL_MEDIA_DATABASE_UPDATE;
                                Bundle b = new Bundle();
                                b.putString(BUNDLE_KEY_MEDIA_NAME, FileUtil.getFileName(path));
                                b.putInt(BUNDLE_KEY_MEDIA_TYPE, MediaScanUtil.getMediaTypeFromPath(path));
                                b.putInt(BUNDLE_KEY_MEDIA_DURATION, mLocalMediaScanner.getMediaDuration(path));
                                b.putString(BUNDLE_KEY_MEDIA_PATH, path);
                                b.putLong(BUNDLE_KEY_MEDIA_SIZE, FileUtil.getFileSize(path));
                                msg.setData(b);
                                mHandler.sendMessage(msg);

                            }

                            // 让UI与数据库 sync一次
                            Message msg = mHandler.obtainMessage();
                            msg.what = MSG_MEDIA_DATABASE_UI_SYNC;
                            mHandler.sendMessage(msg);

                        } catch (Exception e) {
                            Log.e(TAG, "error in mCopyDoneUpdateService!" + e);
                        }
                    }
                });
    }

    private void discoverMountDevice() {
        mUsbPartitionAdapter.clear();
        String[] mountList = FileUtil.getMountVolumePaths(this);
        for (String s : mountList) {
            if (!Arrays.asList(mMountExceptList).contains(s) && !s.contains(LOCAL_SATA_MOUNT_PATH)) {
                mUsbPartitionAdapter.add(s);
            }
        }
        mUsbPartitionAdapter.notifyDataSetChanged();
    }

    private class CopyTaskParam {
        static final int DIRECT_INTERNAL_TO_USB = 0;
        static final int DIRECT_USB_TO_INTERNAL = 1;
        Deque<String> fromList = new ArrayDeque<String>();
        int direct;
        int count;
        long totalSize;
    }

    private class CopyTask extends AsyncTask<CopyTaskParam, Integer, Void> {

        private ProgressDialog dialog;
        private CopyTaskParam param;

        public CopyTask() {
            dialog = new ProgressDialog(MediaActivity.this);
            dialog.setTitle(getResources().getString(R.string.str_media_file_copy_process_dialog_title));
            dialog.setMessage(getResources().getString(R.string.str_media_file_copy_process_dialog_prompt));
            dialog.setIndeterminate(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setCancelable(false);
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected Void doInBackground(CopyTaskParam... params) {
            long time = System.currentTimeMillis();
            param = params[0];
            try {
                long totalNow = 0;

                Log.i(TAG, "Copy file(s) total length: " + param.totalSize);

                mCopyDoneDeque.clear();
                byte[] bytes = new byte[USB_COPY_BUFFER_SIZE];

                while (!param.fromList.isEmpty()) {
                    String sourcePath = param.fromList.pop();
                    File fromFile = new File(sourcePath);

                    String toPath = copyTargetPathGenerate(param.direct,sourcePath);
                    if (toPath.isEmpty()) {
                        throw new IOException();
                    }
                    File toFile = FileUtil.createFile(toPath);

                    OutputStream out = new BufferedOutputStream(new FileOutputStream(toFile));
                    InputStream input = new BufferedInputStream(new FileInputStream(fromFile));

                    int count;
                    while ((count = input.read(bytes)) != -1) {
                        out.write(bytes, 0, count);
                        totalNow += count;
                        float progress = (float)totalNow/(float)param.totalSize*100;
                        publishProgress((int)progress);
                    }

                    // 不在此处查询文件的播放时间与文件大小，因为额外的emmc访问会影响到拷贝操作的速度，这边假如队列
                    mCopyDoneDeque.push(toPath);

                    out.close();
                    input.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "error in CopyTask!", e);
            }

            Log.i(TAG, "total copy time " + (System.currentTimeMillis() - time) + "ms");
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();
            ToastUtil.showToast(MediaActivity.this,getResources().getString(R.string.str_media_file_copy_toast) + param.count);
            if (param.direct == CopyTaskParam.DIRECT_INTERNAL_TO_USB) {
                // 向外复制完成后,刷新一次usb列表
                if (mUsbPartitionAdapter.getCount() > 0) {
                    Message newMsg = mHandler.obtainMessage();
                    newMsg.what = MSG_USB_PARTITION_SWITCH;
                    newMsg.arg1 = mUsbPartitionSpinner.getSelectedItemPosition();

                    if (mUsbPartitionSpinner.getSelectedItem()==null) {
                        newMsg.obj = "none";
                    } else {
                        newMsg.obj = mUsbPartitionSpinner.getSelectedItem().toString();
                    }

                    mHandler.sendMessage(newMsg);
                }
            } else {

                if(!mCopyDoneDeque.isEmpty())
                {
                    updateCopyItemToMediaList();
                }
                updateCapacity(true, LOCAL_MASS_STORAGE_PATH);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            dialog.setProgress(values[0]);
        }
    }

    private String copyTargetPathGenerate(int direct, String path) {
        String result = "";
        String basePath = "";
        if (direct==CopyTaskParam.DIRECT_USB_TO_INTERNAL) {
            basePath = LOCAL_MASS_STORAGE_PATH + File.separator + LOCAL_MEDIA_FOLDER;
        } else {
            if (mUsbPartitionSpinner.getSelectedItem()==null)
            {
                //没有找到usb设备，不用生成目录了
                return "";
            }

            // 类似于 /mnt/sda/sda1 + / + export
            basePath = mUsbPartitionSpinner.getSelectedItem().toString()
                    + File.separator + COPY_TO_USB_MEDIA_EXPORT_FOLDER;
        }

        result = basePath + File.separator + FileUtil.getFilePrefix(path) + "." + FileUtil.getFileExtension(path);
        Log.d(TAG, "target path Generate:" + result);
        return result;
    }

    private void copyUsbFileToInternal() {
        CopyTaskParam param = new CopyTaskParam();
        param.direct = CopyTaskParam.DIRECT_USB_TO_INTERNAL;
        param.count = 0;
        mMediaSameDeque.clear();
        while (!mUsbMediaCopyDeque.isEmpty()) {
            String path = mUsbMediaCopyDeque.pop();

            // 重复文件检测
            if (new MediaBeanDao(MediaActivity.this).queryById(copyTargetPathGenerate(CopyTaskParam.DIRECT_USB_TO_INTERNAL,path))!=null)
            {
                Log.i(TAG,"found same media file! " + path + ":" + copyTargetPathGenerate(CopyTaskParam.DIRECT_USB_TO_INTERNAL,path));
                mMediaSameDeque.push(path);
            }

            param.totalSize += FileUtil.getFileSize(path);
            param.fromList.push(path);
            param.count++;
        }

        if (!mMediaSameDeque.isEmpty())
        {
            CopyUserChoose(param);
        } else {
            if (param.count > 0) {
                new CopyTask().execute(param);
            }
        }
    }

    private void copyInternalFileToUsb() {
        CopyTaskParam param = new CopyTaskParam();
        param.direct = CopyTaskParam.DIRECT_INTERNAL_TO_USB;
        param.count = 0;
        mMediaSameDeque.clear();
        while (!mUsbMediaCopyDeque.isEmpty()) {
            String path = mUsbMediaCopyDeque.pop();

            // 重复文件检测
            for (UsbMediaListBean bean:mUsbMediaListAdapter.getListDatas())
            {
                if (bean.getPath().equals(copyTargetPathGenerate(CopyTaskParam.DIRECT_INTERNAL_TO_USB, path)))
                {
                    Log.i(TAG,"found same media file! " + path + ":" + copyTargetPathGenerate(CopyTaskParam.DIRECT_INTERNAL_TO_USB, path));
                    mMediaSameDeque.push(path);
                }
            }

            param.totalSize += FileUtil.getFileSize(path);
            param.fromList.push(path);
            param.count++;
        }

        // 此处检查判断是否存在usb设备
        if (mUsbPartitionSpinner.getSelectedItemPosition()==AdapterView.INVALID_POSITION) {
            Log.e(TAG,"no usb device to copy to!");
            return;
        }

        if (!mMediaSameDeque.isEmpty())
        {
            CopyUserChoose(param);
        } else {
            if (param.count > 0) {
                new CopyTask().execute(param);
            }
        }
    }

    private void CopyUserChoose(final CopyTaskParam param ) {
        new AlertDialog.Builder(this).setIcon(R.drawable.img_media_warning)
                .setTitle(R.string.str_media_file_copy_same_dialog_title)
                .setMessage(mMediaSameDeque.toString())
                .setNegativeButton(R.string.str_media_dialog_copy_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (param.count > 0) {
                            new CopyTask().execute(param);
                        }
                    }
                })
                .setPositiveButton(R.string.str_media_dialog_copy_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 什么也不做
                    }
                })
                .setNeutralButton(R.string.str_media_dialog_copy_skip, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Iterator itr = mMediaSameDeque.iterator();
                        String tmp;
                        while (itr.hasNext()) {
                            tmp = (String)itr.next();
                            param.fromList.remove(tmp);
                            param.totalSize -= FileUtil.getFileSize(tmp);
                            param.count--;
                        }

                        if (param.count > 0) {
                            new CopyTask().execute(param);
                        }
                    }
                })
                .show();
    }

    private void deleteInternalMediaFile() {
        new AlertDialog.Builder(this).setIcon(R.drawable.img_media_warning)
                .setTitle(R.string.str_media_file_delete_dialog_title)
                .setPositiveButton(R.string.str_media_dialog_button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int deleteCount = 0;
                        Deque<PlayListBean> needToDeleteFromPlaylistDeque = new ArrayDeque<PlayListBean>();
                        boolean removePlaying = false;
                        while (!mMediaDeleteDeque.isEmpty()) {
                            AllMediaListBean needDeleteDiskData = mMediaDeleteDeque.pop();

                            needToDeleteFromPlaylistDeque.clear();
                            removePlaying = false;

                            // 从播放列表删除
                            for (PlayListBean playItem:mPlayListAdapter.getListDatas())
                            {
                                // 删除的文件在播放列表中
                                if (playItem.getMediaData().getPath().equals(needDeleteDiskData.getMediaData().getPath()))
                                {
                                    needToDeleteFromPlaylistDeque.add(playItem);
                                }
                            }

                            while (!needToDeleteFromPlaylistDeque.isEmpty()) {
                                PlayListBean needDeletePlayData = needToDeleteFromPlaylistDeque.pop();
                                mPlayListAdapter.removeItem(needDeletePlayData);
                                // 播放列表处于正在播放的状态
                                if (needDeletePlayData.isPlaying())
                                {
                                    removePlaying = true;
                                    mPlayer.mediaStop();
                                }
                            }

                            // playlist还有剩下的元素
                            if (mPlayListAdapter.getCount()>=1 && removePlaying)
                            {
                                mPlayer.mediaPlayNext();
                                removePlaying = false;
                            }

                            // 从列表显示中删除
                            mAllMediaListAdapter.removeItem(needDeleteDiskData);
                            // 从数据库删除
                            new MediaBeanDao(MediaActivity.this).delete(needDeleteDiskData.getMediaData());

                            //从磁盘删除
                            FileUtil.deleteFile(needDeleteDiskData.getMediaData().getPath());

                            deleteCount++;
                        }

                        updateCapacity(true, LOCAL_MASS_STORAGE_PATH);
                        mAllMediaListAdapter.refresh();
                        ToastUtil.showToast(MediaActivity.this, getResources().getString(R.string.str_media_file_delete_toast) + deleteCount);
                    }
                })
                .setNegativeButton(R.string.str_media_dialog_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 什么也不做
                    }
                }).show();
    }
}

package com.whitesky.tv.projectorlauncher.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
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
import com.whitesky.tv.projectorlauncher.service.mqtt.bean.FileListPushBean;
import com.whitesky.tv.projectorlauncher.service.mqtt.bean.MediaListPushBean;
import com.whitesky.tv.projectorlauncher.service.mqtt.bean.PlayModePushBean;
import com.whitesky.tv.projectorlauncher.service.mqtt.DataListCovert;
import com.whitesky.tv.projectorlauncher.utils.FileUtil;
import com.whitesky.tv.projectorlauncher.utils.MediaScanUtil;
import com.whitesky.tv.projectorlauncher.utils.PathUtil;
import com.whitesky.tv.projectorlauncher.utils.SharedPreferencesUtil;
import com.whitesky.tv.projectorlauncher.utils.ToastUtil;
import com.whitesky.tv.projectorlauncher.utils.ViewUtil;

import java.io.File;
import java.io.IOException;
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
import static com.whitesky.tv.projectorlauncher.common.Contants.MASS_STORAGE_PATH;
import static com.whitesky.tv.projectorlauncher.common.Contants.LOCAL_SATA_MOUNT_PATH;
import static com.whitesky.tv.projectorlauncher.common.Contants.MEDIA_LIST_ORDER_DEFAULT;
import static com.whitesky.tv.projectorlauncher.common.Contants.MEDIA_LIST_ORDER_DURATION;
import static com.whitesky.tv.projectorlauncher.common.Contants.MEDIA_LIST_ORDER_NAME;
import static com.whitesky.tv.projectorlauncher.common.Contants.MEDIA_LIST_ORDER_SOURCE;
import static com.whitesky.tv.projectorlauncher.common.Contants.USB_DEVICE_DEFAULT_SEARCH_MEDIA_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.mMountExceptList;
import static com.whitesky.tv.projectorlauncher.common.HttpConstants.LOGIN_STATUS_NOT_YET;
import static com.whitesky.tv.projectorlauncher.common.HttpConstants.LOGIN_STATUS_SUCCESS;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.ERROR_FILE_NOT_EXIST;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.ERROR_FILE_PATH_NONE;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.ERROR_PLAYLIST_INVALIDED_POSITION;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.PLAYER_STATE_IDLE;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.PLAYER_STATE_PLAY_STOP;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.PLAYER_STATE_PLAY_COMPLETE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_UNKNOWN;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.SOURCE_CLOUD_FREE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_DOWNLOADED;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.ID_LOCAL;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_PICTURE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_VIDEO;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.SOURCE_LOCAL;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_NONE;
import static com.whitesky.tv.projectorlauncher.service.download.DownloadService.EXTRA_KEY_URL;

/**
 * Created by jeff on 18-1-16.
 */
public class MediaActivity extends Activity
        implements View.OnClickListener,
        PictureVideoPlayer.OnMediaEventListener,
        RadioGroup.OnCheckedChangeListener {

    private static final String TAG = MediaActivity.class.getSimpleName();

    private final int MSG_USB_PLUG_IN = 0;
    private final int MSG_USB_PLUG_OUT = 1;
    private final int MSG_USB_PARTITION_SWITCH = 2;

    private final int MSG_USB_MEDIA_LIST_UI_ADD = 10;
    private final int MSG_USB_MEDIA_LIST_UI_CLEAN = 11;
    private final int MSG_USB_MEDIA_SCAN_DONE = 12;

    private final int MSG_LOCAL_MEDIA_DATABASE_CREATE_OR_UPDATE = 20;
    private final int MSG_LOCAL_MEDIA_LIST_UI_CLEAN = 21;
    private final int MSG_MEDIA_LIST_UI_SYNC_WITH_DATABASE = 22;

    private final int MSG_MEDIA_LIST_ITEM_DELETE = 40;
    private final int MSG_MEDIA_LIST_ITEM_PREVIEW = 41;
    private final int MSG_MEDIA_LIST_ITEM_DOWNLOAD_OR_PAUSE = 42;

    private final int MSG_USB_LIST_SELECTED_CHANGE = 46;
    private final int MSG_MEDIA_LIST_SELECTED_CHANGE = 47;

    // 媒体播放事件
    private final int MSG_MEDIA_PLAY_COMPLETE = 100;
    private final int MSG_MEDIA_PLAY_STOP = 102;

    private static final String BUNDLE_KEY_STORAGE_PATH = "storagePath";
    private static final String BUNDLE_KEY_MEDIA_TYPE = "type";
    private static final String BUNDLE_KEY_MEDIA_SIZE = "size";
    private static final String BUNDLE_KEY_MEDIA_DURATION = "duration";
    private static final String BUNDLE_KEY_MEDIA_PATH = "path";
    private static final String BUNDLE_KEY_MEDIA_NAME = "name";

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

    private CheckBox mAllMediaListCheckBox;
    private CheckBox mUsbMediaListCheckBox;

    private int mOriginPlayerMarginTop = 0;
    private int mOriginPlayerMarginLeft = 0;

    private List<PlayListBean> mPlayListBeans = new ArrayList<PlayListBean>();
    private PlayListAdapter mPlayListAdapter;    // 除onCreate与onResume外,其他所有对于playlist数据的操作都通过adapter做,好触发onPlaylistItemEvent
    private DragListView mDragPlayListView;

    private List<AllMediaListBean> mAllMediaListBeans = new ArrayList<AllMediaListBean>();
    private AllMediaListAdapter mAllMediaListAdapter;
    private ListView mAllMediaListView;

    private List<UsbMediaListBean> mUsbMediaListBeans = new ArrayList<UsbMediaListBean>();
    private UsbMediaListAdapter mUsbMediaListAdapter;
    private ListView mUsbMediaListView;

    private TextView mUsbListTitle;

    private Deque<String> mCopyDeque = new ArrayDeque<>();                         // 需要复制的文件
    private Deque<MediaBean> mDeleteDeque = new ArrayDeque<>();                    // 需要删除的文件
    private ArrayList<FileListPushBean> mNeedToDownloadList = null;                // MQTT推送过来的需要下载的文件列表
    private ArrayList<MediaListPushBean> mNeedToPlayList = null;                   // MQTT推送过来的播放列表

    private TextView mUsbCapacityTextView;
    private TextView mLocalCapacityTextView;
    ExecutorService mCapacityUpdateService = Executors.newSingleThreadExecutor();;                  // 用于更新磁盘容量显示的线程
    ExecutorService mCopyToInternalDoneUpdateUiService = Executors.newSingleThreadExecutor();;      // 用于复制完成后更新UI列表的线程

    private Spinner mUsbPartitionSpinner;
    private ArrayAdapter<String> mUsbPartitionAdapter;

    private void changeWholePlayList(List<PlayListBean> target) {
        mPlayListAdapter.setListDatas(target);
        mPlayListAdapter.refresh();
        savePlaylistToConfig();

        for (PlayListBean bean : mPlayListAdapter.getListDatas()) {
            if (bean.getMediaData().getDownloadState()==STATE_DOWNLOAD_NONE) {
                addToDownload(bean.getMediaData());
            }
        }

        if (mPlayListBeans.size()>0) {
            mPlayer.fullScreenSwitch(true);
            mPlayer.mediaPlay(0);
        }
    }

    private final BroadcastReceiver serviceEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (action.equals(Contants.ACTION_PUSH_PLAYLIST)) {

                ArrayList<MediaListPushBean> cloudPushPlaylist = intent.getParcelableArrayListExtra(Contants.EXTRA_PUSH_CONTEXT);

                if (cloudPushPlaylist==null) {
                    Log.e(TAG,"mqtt receive a error format push play list!");
                    return;
                }

                mPlayer.mediaStop();

                List<PlayListBean> pList = new ArrayList<>();
                boolean needSync = DataListCovert.covertCloudPushToPlayList(getApplicationContext(),pList,cloudPushPlaylist);

                if (needSync) {
                    if (mNeedToPlayList!=null) {
                        Log.w(TAG,"mNeedToPlayList already has a cloud sync http request");
                        return;
                    }

                    mNeedToPlayList = cloudPushPlaylist;
                    loadMediaListFromCloud(getApplicationContext(), new cloudListGetCallback() {
                        @Override
                        public void cloudSyncDone(boolean result) {
                            if (result == true) {
                                Message msg = mHandler.obtainMessage();
                                msg.what = MSG_MEDIA_LIST_UI_SYNC_WITH_DATABASE;
                                mHandler.sendMessage(msg);
                            }

                            List<PlayListBean> pList = new ArrayList<>();
                            boolean stillNeedSync = DataListCovert.covertCloudPushToPlayList(getApplicationContext(),pList,mNeedToPlayList);
                            if (stillNeedSync) {
                                Log.w(TAG,"we have sync with cloud,but still missing some item!");
                            }
                            mNeedToPlayList = null;
                            changeWholePlayList(pList);
                        }
                    });
                } else {
                    changeWholePlayList(pList);
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

            } else if (action.equals(Contants.ACTION_PUSH_DELETE)) {

                ArrayList<FileListPushBean> pushList = intent.getParcelableArrayListExtra(Contants.EXTRA_PUSH_CONTEXT);

                if (pushList==null) {
                    Log.e(TAG,"mqtt receive a error format delete list!");
                    return;
                }

                synchronized (mDeleteDeque) {
                    DataListCovert.covertCloudFileListToMediaBeanList(getApplicationContext(),mDeleteDeque,pushList);
                    deleteMediaFile();
                }

            } else if (action.equals(Contants.ACTION_PUSH_DOWNLOAD_NEED_SYNC)) {

                ArrayList<FileListPushBean> downloadCloudList = intent.getParcelableArrayListExtra(Contants.EXTRA_PUSH_CONTEXT);

                if (downloadCloudList==null) {
                    Log.e(TAG,"mqtt receive a error format push sync list!");
                    return;
                }

                if (mNeedToDownloadList!=null) {
                    Log.w(TAG,"mNeedToDownloadList already has a cloud sync http request");
                    return;
                }

                mNeedToDownloadList = downloadCloudList;
                loadMediaListFromCloud(getApplicationContext(), new cloudListGetCallback() {
                    @Override
                    public void cloudSyncDone(boolean result) {
                        if (result == true) {
                            Message msg = mHandler.obtainMessage();
                            msg.what = MSG_MEDIA_LIST_UI_SYNC_WITH_DATABASE;
                            mHandler.sendMessage(msg);
                        }

                        Deque<MediaBean> dDeque = new ArrayDeque<>();
                        boolean stillNeedSync = DataListCovert.covertCloudFileListToMediaBeanList(getApplicationContext(), dDeque, mNeedToDownloadList);
                        if (stillNeedSync) {
                            Log.w(TAG, "we have sync with cloud,but still missing some item!");
                        }

                        mNeedToDownloadList = null;

                        while (!dDeque.isEmpty()) {
                            addToDownload(dDeque.pop());
                        }
                    }
                });

            } else if (action.equals(Contants.ACTION_DOWNLOAD_STATE_UPDATE)) {

                MediaBean bean = intent.getParcelableExtra(Contants.EXTRA_DOWNLOAD_STATE_CONTEXT);

                if (bean.getDownloadState()==STATE_DOWNLOAD_NONE) {
                    updateCapacityUi(MASS_STORAGE_PATH);
                } else if (bean.getDownloadState()==STATE_DOWNLOAD_DOWNLOADED) {

                    updateCapacityUi(MASS_STORAGE_PATH);

                    if (mPlayListAdapter.isAllItemsFromCloud() && mPlayer.isFullScreen() && mPlayer.getPlayState()== PLAYER_STATE_IDLE) {
                        Message msg = mHandler.obtainMessage();
                        msg.what = MSG_MEDIA_PLAY_COMPLETE;
                        mHandler.sendMessage(msg);
                    }
                }

                mPlayListAdapter.update(bean);
                mPlayListAdapter.refresh();

                mAllMediaListAdapter.update(bean);
                mAllMediaListAdapter.refresh();
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
        Log.d(TAG,"on Play Stop!");
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_MEDIA_PLAY_STOP;
        mHandler.sendMessage(msg);
    }

    @Override
    public void onMediaPlayCompletion() {
        // 播放完毕
        Log.d(TAG,"on Play Complete!");
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_MEDIA_PLAY_COMPLETE;

        // 正常播放完成的状态是COMPLETE
        // 如果这边获得状态为IDLE,则表示是文件没有下载直接当作播放完成
        // 防止在播放列表文件全部没有下载的情况下,过于频繁的的下一首
        if (mPlayer.getPlayState() == PLAYER_STATE_PLAY_STOP) {
            mHandler.sendMessageDelayed(msg, 1000);
        } else {
            mHandler.sendMessage(msg);
        }
    }

    @Override
    public void onMediaPlayInfoUpdate(String name, String mimeType, int width, int height, long size, int bps) {
        // 直接改变UI显示的播放信息
        Log.d(TAG,"on Play Info Update!");
        setMediaInfoUi(name, mimeType, width, height, size, bps);
        mPlayListAdapter.refresh();
    }

    @Override
    public void onMediaPlayError(int error, MediaBean errorBean) {
        // 播放错误
        Log.d(TAG,"media Play error! ERR_NO:" + error);

        // 需要使用toast提示用户的错误
        switch (error) {
            case ERROR_PLAYLIST_INVALIDED_POSITION:
                ToastUtil.showToast(MediaActivity.this, R.string.str_media_play_list_empty);
                break;
            case ERROR_FILE_PATH_NONE:
                ToastUtil.showToast(MediaActivity.this, R.string.str_media_play_path_error);
                break;
            case ERROR_FILE_NOT_EXIST:
                ToastUtil.showToast(MediaActivity.this, R.string.str_media_play_file_not_found_error);
                break;
            default:
                break;
        }

        Message msg = mHandler.obtainMessage();
        msg.what = MSG_MEDIA_PLAY_COMPLETE;
        mHandler.sendMessage(msg);
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
                msg.what = MSG_MEDIA_LIST_UI_SYNC_WITH_DATABASE;
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

    private void loadMediaListOrderMode() {
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

    private void loadMediaListFromDb() {
        // 初始化媒体列表
        if (new MediaBeanDao(MediaActivity.this).selectAll().isEmpty())
        {   // 如果数据库为空,则扫描一次本地媒体文件
            Log.i(TAG, "empty media database, so scan and download media list");
            String localMediaStorePath = PathUtil.localFileStoragePath();
            FileUtil.createDir(localMediaStorePath);
            int ret = mLocalMediaScanner.safeScanning(localMediaStorePath);
            if (ret==-2) {
                Log.e(TAG,PathUtil.localFileStoragePath() + " not found!");
                ToastUtil.showToast(getApplicationContext(),PathUtil.localFileStoragePath() + " not found!");
            }

            loadMediaListFromCloud(getApplicationContext(), new cloudListGetCallback() {
                @Override
                public void cloudSyncDone(boolean result) {
                    if (result == true) {
                        Message msg = mHandler.obtainMessage();
                        msg.what = MSG_MEDIA_LIST_UI_SYNC_WITH_DATABASE;
                        mHandler.sendMessage(msg);
                    }
                }
            });
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

    public interface cloudListGetCallback{
        void cloudSyncDone(boolean result);
    }

    // 从云端同步数据库，然后与本地端磁盘的数据进行比对，比对完成后调callback函数
    public static void loadMediaListFromCloud(final Context context, final cloudListGetCallback callback) {
        Log.i(TAG, "load Media List From Cloud in");
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
                .add("sn", DeviceInfoActivity.getSysSn(context))
                .build();

        Request request = new Request.Builder().url(HttpConstants.URL_GET_SHARE_LIST).post(body).build();
        Call call = mClient.newCall(request);

        call.enqueue(new okhttp3.Callback() {
            // 失败
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG,"onFailure" + e.toString());
                if (callback!=null) {
                    callback.cloudSyncDone(false);
                }
            }

            // 成功
            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {

                if (!response.isSuccessful()) {
                    Log.e(TAG,"response is not success!" + response.toString());
                    if (callback!=null) {
                        callback.cloudSyncDone(false);
                    }

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
                            new MediaBeanDao(context).deleteItemsFromCloud();

                            if (!cloudList.getResult().isEmpty()) {
                                List<MediaBean> listCloud = new ArrayList<>();
                                DataListCovert.covertCloudResultToMediaList(context, listCloud, cloudList.getResult());
                                new MediaBeanDao(context).createOrUpdate(listCloud);
                            }
                        } else if (cloudList.getStatus().equals(LOGIN_STATUS_NOT_YET)) {
                            Log.d(TAG,"onResponse device not login yet,need user login this device!");
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    ToastUtil.showToast(context,context.getResources().getString(R.string.str_media_cloud_file_need_login_toast));
                                }
                            });
                        } else {
                            Log.d(TAG,"onResponse unknown status " + cloudList.getStatus());
                        }

                    } else {
                        Log.e(TAG, "cloud list json parse error! " + htmlBody);
                    }

                    // 云端列表更新后必须做一次与本地已经下载文件的匹配
                    updateDbCloudItemWithLocalDisk(context,new MediaScanUtil());

                    if (callback!=null) {
                        callback.cloudSyncDone(true);
                    }

                } else {
                    Log.e(TAG, "onResponse http undefine code " + response.code());
                    if (callback!=null) {
                        callback.cloudSyncDone(false);
                    }
                }
            }
        });
    }

    // 对于每一个云文件夹中存在的文件,在bd中插入或更新一条
    // 以后删除文件的时候,对于没有url的文件需要提示云端可能已经删除,需要确认
    public static void updateDbCloudItemWithLocalDisk(Context context,MediaScanUtil scanner) {
        File cloudFreeFolder = new File(PathUtil.cloudFreeFileStoragePath());
        updateCloudDbItemByTraversal(cloudFreeFolder,context,scanner);

        File cloudPrivateFolder = new File(PathUtil.cloudPrivateFileStoragePath());
        updateCloudDbItemByTraversal(cloudPrivateFolder,context,scanner);
    }

    private static void updateCloudDbItemByTraversal(File cloudStorageFolder, Context context, MediaScanUtil scanner) {
        String[] filenames = cloudStorageFolder.list();
        int type = MEDIA_UNKNOWN;
        int duration = 0;
        long size = 0;

        if (filenames != null) {
            for (String name : filenames) {
                String path = cloudStorageFolder + File.separator + name;
                File file = new File(path);
                type = MediaScanUtil.getMediaTypeFromPath(name);
                if (type==MEDIA_PICTURE || type==MEDIA_VIDEO) {
                    duration = scanner.getMediaDuration(path);
                } else {
                    continue;
                }

                MediaBean bean = new MediaBeanDao(context).queryByPath(path);

                if (bean!=null) {
                    // 直接更新记录的播放时间
                    bean.setDuration(duration);
                } else {
                    // 可能是云端下载的,但是云端已经删除了
                    size = file.length();
                    bean = new MediaBean(name,ID_LOCAL,type,SOURCE_CLOUD_FREE,path,duration,size);
                    bean.setDownloadState(STATE_DOWNLOAD_DOWNLOADED);
                    bean.setUrl("");
                }

                new MediaBeanDao(context).createOrUpdate(bean);
            }
        }
    }

    private void savePlaylistToConfig() {
        savePlaylistToConfig(this,mPlayListAdapter.getListDatas());
    }

    public static synchronized void savePlaylistToConfig(Context context, List<PlayListBean> pList) {
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
        List<PlayListBean> data = loadPlaylistFromConfig(this);

        // 取数据库检查一遍，防止MediaActivity不在前台,而播放列表中的条目却有下载任务，那么下载完成playlist中的项目也永远是没下载状态
        for(Iterator it = data.iterator();it.hasNext();){
            PlayListBean needCheckItem = (PlayListBean) it.next();
            MediaBean dbItem = new MediaBeanDao(getApplicationContext()).queryByPath(needCheckItem.getMediaData().getPath());
            if (dbItem==null) {
                it.remove();
            } else {
                needCheckItem.getMediaData().setDownloadState(dbItem.getDownloadState());
                needCheckItem.getMediaData().setDownloadProgress(dbItem.getDownloadProgress());
            }
        }
        mPlayListAdapter.setListDatas(data);
        mPlayListAdapter.refresh();
    }

    public static List<PlayListBean> loadPlaylistFromConfig(Context context) {
        Gson gson = new Gson();
        SharedPreferencesUtil config = new SharedPreferencesUtil(context, Contants.PREF_CONFIG);
        String jsonStr = config.getString(CONFIG_PLAYLIST, "[]");
        Type type = new TypeToken<List<PlayListBean>>() {
        }.getType();
        List<PlayListBean> data = gson.fromJson(jsonStr, type);
        return data;
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
                mDeleteDeque.clear();
                mDeleteDeque.push(mAllMediaListAdapter.getItem(position).getMediaData());
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
                msg.what = MSG_MEDIA_LIST_ITEM_DOWNLOAD_OR_PAUSE;
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
                    addToPlayList(mediaItem);
                }
            }
        });

        // usb设备媒体列表
        mUsbMediaListAdapter = new UsbMediaListAdapter(getApplicationContext(), mUsbMediaListBeans);
        mUsbMediaListAdapter.setOnUsbItemEventListener(new UsbMediaListAdapter.OnUsbItemEventListener() {
            @Override
            public void doItemCopy(int position) {
                Message msg = mHandler.obtainMessage();
                mCopyDeque.clear();
                mCopyDeque.push(mUsbMediaListAdapter.getItem(position).getPath());
                copyUsbFileToInternal();
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
                if (mPlayer.getPlayState()!= PLAYER_STATE_IDLE
                        && mPlayer.getPlayState()!= PLAYER_STATE_PLAY_STOP
                        && mPlayer.getPlayState()!= PLAYER_STATE_PLAY_COMPLETE) {
                    PlayListBean curPlay = mPlayer.getCurPlaylistBean();
                    if (curPlay!=null && curPlay.equals(mPlayListAdapter.getItem(position))) {
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
        mLocalMediaScanner.setNeedSize(true);
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
                msg.what = MSG_LOCAL_MEDIA_LIST_UI_CLEAN;
                mHandler.sendMessage(msg);
            }

            @Override
            public void onFindMedia(int type, String name, String extension, String path, int duration, long size) {
                if (type == MEDIA_PICTURE || type == MEDIA_VIDEO) {
                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_LOCAL_MEDIA_DATABASE_CREATE_OR_UPDATE;
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
                msg.what = MSG_MEDIA_LIST_UI_SYNC_WITH_DATABASE;
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
                msg.what = MSG_USB_MEDIA_LIST_UI_CLEAN;
                mHandler.sendMessage(msg);
            }

            @Override
            public void onFindMedia(int type, String name, String extension, String path, int duration, long size) {
                if (type == MEDIA_PICTURE || type == MEDIA_VIDEO) {
                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_USB_MEDIA_LIST_UI_ADD;
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

        loadReplayMode();
        loadMediaListOrderMode();
        loadMediaListFromDb();
        updateMultiActionButtonUiState();
        loadPlaylistFromConfig();

        // 监听usb插拔事件
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        usbFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        usbFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        usbFilter.addDataScheme("file");
        registerReceiver(usbMountEventReceiver, usbFilter);

        // 监听mqtt控制命令
        IntentFilter serviceEventFilter = new IntentFilter();
        serviceEventFilter.addAction(Contants.ACTION_PUSH_PLAYLIST);
        serviceEventFilter.addAction(Contants.ACTION_PUSH_PLAYMODE);
        serviceEventFilter.addAction(Contants.ACTION_PUSH_DELETE);
        serviceEventFilter.addAction(Contants.ACTION_PUSH_DOWNLOAD_NEED_SYNC);
        serviceEventFilter.addAction(Contants.ACTION_DOWNLOAD_STATE_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceEventReceiver, serviceEventFilter);

        mPlayer.setStartRightNow(true);

        // 主动枚举一次usb设备,防止在此activity无法接受usbReceiver的时候有u盘设备插上
        discoverMountDevice();

        // 开线程去查询本地容量
        updateCapacityUi(MASS_STORAGE_PATH);

        ((MainApplication)getApplication()).isMediaActivityForeground = true;
        ((MainApplication)getApplication()).mFirstInitDone = true;
    }

    @Override
    protected void onPause() {
        if (mPlayer != null) {
            mPlayer.mediaStop();
        }

        unregisterReceiver(usbMountEventReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceEventReceiver);

        super.onPause();

        ((MainApplication)getApplication()).isBusyInCopy = false;
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

        if (mCopyToInternalDoneUpdateUiService != null) {
            mCopyToInternalDoneUpdateUiService.shutdownNow();
            mCopyToInternalDoneUpdateUiService = null;
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
                String localMediaStorePath = PathUtil.localFileStoragePath();
                FileUtil.createDir(localMediaStorePath);
                int ret = mLocalMediaScanner.safeScanning(localMediaStorePath);
                if (ret==-2) {
                    Log.e(TAG,PathUtil.localFileStoragePath() + " not found!");
                    ToastUtil.showToast(getApplicationContext(),PathUtil.localFileStoragePath() + " not found!");
                }
                break;

            case R.id.bt_media_cloud_list_refresh:
                loadMediaListFromCloud(getApplicationContext(), new cloudListGetCallback() {
                    @Override
                    public void cloudSyncDone(boolean result) {
                        if (result == true) {
                            Message msg = mHandler.obtainMessage();
                            msg.what = MSG_MEDIA_LIST_UI_SYNC_WITH_DATABASE;
                            mHandler.sendMessage(msg);
                        }
                    }
                });
                break;

            case R.id.cb_media_all_list_check:
                for (AllMediaListBean data : mAllMediaListBeans) {
                    data.setSelected(mAllMediaListCheckBox.isChecked());
                }
                mAllMediaListAdapter.refresh();
                updateMultiActionButtonUiState();
                break;

            case R.id.cb_media_usb_list_check:
                for (UsbMediaListBean data : mUsbMediaListBeans) {
                    data.setSelected(mUsbMediaListCheckBox.isChecked());
                }
                mMultiCopyToLocalBtn.setEnabled(mUsbMediaListAdapter.hasItemSelected());
                mUsbMediaListAdapter.refresh();
                break;


            case R.id.bt_media_multi_copy_to_left:
                mCopyDeque.clear();
                for (UsbMediaListBean data : mUsbMediaListAdapter.getListDatas()) {
                    if (data.isSelected()) {
                        mCopyDeque.add(data.getPath());
                    }
                }

                if (!mCopyDeque.isEmpty()) {
                    copyUsbFileToInternal();
                }
                break;

            case R.id.bt_media_multi_delete:
                mDeleteDeque.clear();
                for (AllMediaListBean data : mAllMediaListBeans) {
                    if (data.isSelected()) {
                        mDeleteDeque.push(data.getMediaData());
                        // 这边只加入等待删除的列表,从播放列表以及数据库删除记录的动作,在处理msg的地方做
                    }
                }

                Message msg = mHandler.obtainMessage();
                msg.what = MSG_MEDIA_LIST_ITEM_DELETE;
                mHandler.sendMessage(msg);
                break;

            case R.id.bt_media_multi_add:
                for (AllMediaListBean data : mAllMediaListAdapter.getListDatas()) {
                    if (data.isSelected()) {
                        addToPlayList(data.getMediaData());
                        data.setSelected(false);
                    }
                }
                mAllMediaListAdapter.refresh();
                break;

            case R.id.bt_media_multi_download:
                for (AllMediaListBean data : mAllMediaListAdapter.getListDatas()) {
                    if (data.isSelected()) {
                        addToDownload(data.getMediaData());
                        data.setSelected(false);
                    }
                }
                mAllMediaListAdapter.refresh();
                break;

            case R.id.bt_media_multi_copy_to_right:
                mCopyDeque.clear();
                for (AllMediaListBean data : mAllMediaListAdapter.getListDatas()) {
                    if (data.isSelected()) {
                        if (data.getMediaData().getSource()==MediaBean.SOURCE_LOCAL) {
                            mCopyDeque.add(data.getMediaData().getPath());
                        } else {
                            ToastUtil.showToast(MediaActivity.this, getResources().getString(R.string.str_media_cloud_file_could_not_export_toast));
                        }
                    }
                }
                copyInternalFileToUsb();
                break;

            default:
                break;
        }
    }

    private void addToDownload(MediaBean addItem) {
        Intent intent = new Intent().setAction(DownloadService.ACTION_MEDIA_DOWNLOAD_START);
        intent.putExtra(EXTRA_KEY_URL, addItem.getUrl());
        Log.i(TAG, "call download:" + addItem.toString());
        MediaActivity.this.startService(intent);
    }

    private void addToPlayList(MediaBean addItem) {
        if (addItem.getDownloadState()==STATE_DOWNLOAD_NONE) {
            addToDownload(addItem);
        }
        mPlayListAdapter.addItem(new PlayListBean(addItem));
    }

    private void updateMultiActionButtonUiState() {
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

                    if (MASS_STORAGE_PATH.equals(storagePath)) {
                        // 挂载了硬盘设备,很可能是开机,直接触发播放
                        // 播放类中在surfaceHolder建立的时候会尝试播放一次，但是可能会因为没有挂载SATA而失败，这边补一次
                        Log.i(TAG, "mount sata device means power on complete, start full screen play media");
                        if (mPlayer.getPlayState()== PLAYER_STATE_IDLE) {
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
                        updateCapacityUi(currentPath);

                        // 由扫描来更新usb media列表
                        int ret = mUsbMediaScanner.safeScanning(currentPath + File.separator + USB_DEVICE_DEFAULT_SEARCH_MEDIA_FOLDER);
                        if (ret==-2) {
                            // 根本没有这个目录
                            mUsbMediaListAdapter.clear();
                            mUsbMediaListAdapter.refresh();
                        }
                    }
                    updateMultiActionButtonUiState();
                    break;

                case MSG_LOCAL_MEDIA_DATABASE_CREATE_OR_UPDATE:
                    Bundle b = msg.getData();
                    int type = b.getInt(BUNDLE_KEY_MEDIA_TYPE);
                    String name = b.getString(BUNDLE_KEY_MEDIA_NAME);
                    String path = b.getString(BUNDLE_KEY_MEDIA_PATH);
                    int duration = b.getInt(BUNDLE_KEY_MEDIA_DURATION);
                    long size = b.getLong(BUNDLE_KEY_MEDIA_SIZE);

                    MediaBean data = new MediaBean(name, ID_LOCAL, type, MediaBean.SOURCE_LOCAL, path, duration, size);
                    data.setDownloadState(STATE_DOWNLOAD_DOWNLOADED);
                    new MediaBeanDao(MediaActivity.this).createOrUpdate(data);
                    break;

                case MSG_USB_MEDIA_LIST_UI_ADD:
                    b = msg.getData();
                    name = b.getString(BUNDLE_KEY_MEDIA_NAME);
                    type = b.getInt(BUNDLE_KEY_MEDIA_TYPE);
                    size = b.getLong(BUNDLE_KEY_MEDIA_SIZE);
                    path = b.getString(BUNDLE_KEY_MEDIA_PATH);

                    UsbMediaListBean UsbMedia = new UsbMediaListBean(name, type, path, size);
                    mUsbMediaListAdapter.addItem(UsbMedia);
                    mUsbMediaListAdapter.refresh();
                    break;

                case MSG_USB_MEDIA_LIST_UI_CLEAN:
                    mUsbMediaListAdapter.clear();
                    mUsbMediaListAdapter.refresh();
                    break;

                case MSG_LOCAL_MEDIA_LIST_UI_CLEAN:
                    mAllMediaListAdapter.clear();
                    mAllMediaListAdapter.refresh();
                    new MediaBeanDao(MediaActivity.this).deleteItemsLocalImport();
                    break;

                case MSG_USB_LIST_SELECTED_CHANGE:
                    if (msg.arg1==1) {
                        mMultiCopyToLocalBtn.setEnabled(true);
                    } else {
                        mMultiCopyToLocalBtn.setEnabled(false);
                    }
                    break;

                case MSG_MEDIA_LIST_SELECTED_CHANGE:
                    updateMultiActionButtonUiState();
                    break;

                case MSG_MEDIA_LIST_ITEM_DELETE:
                    alertUserToDeleteMediaFile();
                    break;

                case MSG_MEDIA_LIST_ITEM_DOWNLOAD_OR_PAUSE:
                    Intent intent = new Intent().setAction(DownloadService.ACTION_MEDIA_DOWNLOAD_START_PAUSE);
                    intent.putExtra(EXTRA_KEY_URL, ((MediaBean)msg.obj).getUrl());
                    Log.i(TAG,"call download:" + ((MediaBean)msg.obj).toString());
                    startService(intent);
                    break;

                case MSG_MEDIA_LIST_ITEM_PREVIEW:
                    MediaBean previewItem = (MediaBean)msg.obj;
                    mPlayer.mediaStop();
                    mPlayer.mediaPreview(previewItem);
                    break;

                case MSG_MEDIA_LIST_UI_SYNC_WITH_DATABASE:
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
                    }
                    mAllMediaListAdapter.refresh();
                    updateMultiActionButtonUiState();
                    updateCapacityUi(MASS_STORAGE_PATH);
                    break;

                case MSG_USB_MEDIA_SCAN_DONE:
                    mMultiCopyToLocalBtn.setEnabled(mUsbMediaListAdapter.hasItemSelected());
                    break;

                case MSG_MEDIA_PLAY_COMPLETE:
                    if (mPlayListAdapter.hasPlayableItem()) {
                        mPlayer.mediaAutoReplay();
                    }
                    break;

                case MSG_MEDIA_PLAY_STOP:
                    break;
            }
        }
    };

    public static boolean isLocalMassStorageMounted(Context context) {
        String[] mountList = FileUtil.getMountVolumePaths(context);
        if (Arrays.asList(mountList).contains(MASS_STORAGE_PATH)) {
            return true;
        } else {
            return false;
        }
    }

    private void setMediaInfoUi(final String name, final String mimeType, final int width, final int height, final long size, final int bps) {
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

    private void updateTextViewOnUiThread(final TextView textView, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        });
    }

    private void updateCapacityUi(final String path) {
        mCapacityUpdateService.execute(
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            String fsUsed = FileUtil.formatFileSize(FileUtil.getTotalCapacity(path) -
                                    FileUtil.getAvailableCapacity(path));
                            String fsCapacity = FileUtil.formatFileSize(FileUtil.getTotalCapacity(path));

                            if (path.contains(LOCAL_SATA_MOUNT_PATH)) {
                                updateTextViewOnUiThread(mLocalCapacityTextView, fsUsed + "/" + fsCapacity);
                            } else {
                                updateTextViewOnUiThread(mUsbCapacityTextView, fsUsed + "/" + fsCapacity);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Update Capacity error!" + e);
                        }
                    }
                });
    }

    private void updateMediaListUiAfterCopy(final Deque<String> items) {
        mCopyToInternalDoneUpdateUiService.execute(
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            String path;
                            while(!items.isEmpty()) {
                                path = items.pop();
                                Message msg = mHandler.obtainMessage();
                                msg.what = MSG_LOCAL_MEDIA_DATABASE_CREATE_OR_UPDATE;
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
                            msg.what = MSG_MEDIA_LIST_UI_SYNC_WITH_DATABASE;
                            mHandler.sendMessage(msg);

                        } catch (Exception e) {
                            Log.e(TAG, "Exception in Copy Done Update Ui Service!" + e);
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

    private class CopyToUsbCallback implements CopyTask.CopyDoneListener {
        @Override
        public void onCopyStartCallback() {
            ((MainApplication)getApplication()).isBusyInCopy = true;
        }

        @Override
        public void onAllCopyDoneCallback(Deque<String> copyDoneItem) {
            ToastUtil.showToast(MediaActivity.this, getResources().getString(R.string.str_media_file_copy_toast) + copyDoneItem.size());

            if (!copyDoneItem.isEmpty()) {
                // 复制完成后取消多选框的状态
                for (AllMediaListBean tmp : mAllMediaListAdapter.getListDatas()) {
                    tmp.setSelected(false);
                }
                mAllMediaListCheckBox.setChecked(false);

                mAllMediaListAdapter.refresh();
            }

            // 由于拷贝到usb的目录是export目录,不是media目录,不会被扫描出来,所以不用再刷新usb列表
//            if (mUsbPartitionAdapter.getCount() > 0) {
//                Message newMsg = mHandler.obtainMessage();
//                newMsg.what = MSG_USB_PARTITION_SWITCH;
//                newMsg.arg1 = mUsbPartitionSpinner.getSelectedItemPosition();
//
//                if (mUsbPartitionSpinner.getSelectedItem() == null) {
//                    newMsg.obj = "none";
//                } else {
//                    newMsg.obj = mUsbPartitionSpinner.getSelectedItem().toString();
//                }
//
//                mHandler.sendMessage(newMsg);
//            }

            ((MainApplication)getApplication()).isBusyInCopy = false;
        }
    }

    private class CopyToInternalCallback implements CopyTask.CopyDoneListener {
        @Override
        public void onCopyStartCallback() {
            ((MainApplication)getApplication()).isBusyInCopy = true;
        }

        @Override
        public void onAllCopyDoneCallback(Deque<String> copyDoneItem) {
            ToastUtil.showToast(MediaActivity.this,getResources().getString(R.string.str_media_file_copy_toast) + copyDoneItem.size());
            if(!copyDoneItem.isEmpty()) {

                for (UsbMediaListBean tmp : mUsbMediaListAdapter.getListDatas()) {
                    tmp.setSelected(false);
                }

                mUsbMediaListCheckBox.setChecked(false);

                mUsbMediaListAdapter.refresh();

                updateMediaListUiAfterCopy(copyDoneItem);
            }
        }
    }

    private void checkAndCopy(CopyTask.CopyTaskParam param, Deque<String> waitCopyDeque) {
        Deque<String> sameMediaDeque = new ArrayDeque<String>();
        sameMediaDeque.clear();
        synchronized (waitCopyDeque) {
            while (!waitCopyDeque.isEmpty()) {
                String path = waitCopyDeque.pop();

                // 重复文件检测
                String toPath = PathUtil.pathGenerate(path, param.desFolder);
                File desFolderFile = new File(param.desFolder);
                for (File file : desFolderFile.listFiles()) {
                    if (file.getAbsolutePath().equals(toPath)) {
                        Log.i(TAG, "found same media file! " + file.getAbsolutePath());
                        sameMediaDeque.push(path);
                        break;
                    }
                }

                param.totalSize += FileUtil.getFileSize(path);
                param.fromList.push(path);
            }
        }

        if (!sameMediaDeque.isEmpty())
        {
            askUserChooseCopyOrNot(param,sameMediaDeque);
            return;
        }

        if (param.fromList.size() > 0) {
            new CopyTask(this).execute(param);
        }
    }

    private void copyUsbFileToInternal() {
        if (mCopyDeque==null || mCopyDeque.isEmpty()) {
            Log.e(TAG,"CopyDeque is empty!");
            return;
        }

        if (!isLocalMassStorageMounted(this)) {
            Log.w(TAG,"local sata device not mount! copy just return");
            return;
        }

        CopyTask.CopyTaskParam param = new CopyTask.CopyTaskParam();
        param.desFolder = PathUtil.localFileStoragePath();
        param.callback = new CopyToInternalCallback();

        checkAndCopy(param,mCopyDeque);
    }

    private void copyInternalFileToUsb() {
        if (mCopyDeque==null || mCopyDeque.isEmpty()) {
            Log.e(TAG,"CopyDeque is empty!");
            return;
        }

        // 检查是否存在usb设备
        if (mUsbPartitionSpinner.getSelectedItemPosition()==AdapterView.INVALID_POSITION) {
            Log.w(TAG,"usb device not plug in! copy just return");
            return;
        }

        String usbDeviceMountPoint = mUsbPartitionSpinner.getSelectedItem().toString();
        if (usbDeviceMountPoint==null || usbDeviceMountPoint.isEmpty()) {
            Log.w(TAG,"usb device not plug in! copy just return");
            return;
        }

        CopyTask.CopyTaskParam param = new CopyTask.CopyTaskParam();
        param.desFolder = usbDeviceMountPoint + File.separator + COPY_TO_USB_MEDIA_EXPORT_FOLDER;
        param.callback = new CopyToUsbCallback();

        checkAndCopy(param,mCopyDeque);
    }

    private void askUserChooseCopyOrNot(final CopyTask.CopyTaskParam param, final Deque<String> sameItems) {
        String title = String.valueOf(sameItems.size()) + getResources().getString(R.string.str_media_file_copy_same_dialog_title);
        String msg = "";
        for (String tmp : sameItems) {
            msg += FileUtil.getFileName(tmp) + "\n";
        }

        new AlertDialog.Builder(this).setIcon(R.drawable.img_media_warning)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton(R.string.str_media_dialog_copy_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (param.fromList.size() > 0) {
                            new CopyTask(MediaActivity.this).execute(param);
                        }
                    }
                })
                .setPositiveButton(R.string.str_media_dialog_copy_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setNeutralButton(R.string.str_media_dialog_copy_skip, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Iterator itr = sameItems.iterator();
                        String tmp;
                        while (itr.hasNext()) {
                            tmp = (String)itr.next();
                            param.fromList.remove(tmp);
                            param.totalSize -= FileUtil.getFileSize(tmp);
                        }

                        if (param.fromList.size() > 0) {
                            new CopyTask(MediaActivity.this).execute(param);
                        }
                    }
                })
                .show();
    }

    private void alertUserToDeleteMediaFile() {
        while (mDeleteDeque.isEmpty()) {
            Log.w(TAG,"no item(s) selected to delete");
            return;
        }

        new AlertDialog.Builder(this).setIcon(R.drawable.img_media_warning)
                .setTitle(R.string.str_media_file_delete_dialog_title)
                .setPositiveButton(R.string.str_media_dialog_button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        synchronized (mDeleteDeque) {
                            deleteMediaFile();
                        }
                    }
                })
                .setNegativeButton(R.string.str_media_dialog_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                }).show();
    }

    private void deleteMediaFile() {
        int deleteCount = 0;
        boolean removePlaying = false;

        Deque<PlayListBean> needToDeleteFromPlaylistDeque = new ArrayDeque<PlayListBean>();

        while (!mDeleteDeque.isEmpty()) {
            MediaBean needDeleteDiskData = mDeleteDeque.pop();

            // 当前删除的项当前正在播放则先停止播放
            if (needDeleteDiskData.getPath().equals(mPlayer.getCurPlayPath())) {
                removePlaying = true;
                mPlayer.mediaStop();
            }

            // 从播放列表找受影响的条目,因为播放列表的条目可能是重复的
            // 所以使用队列记录下与当前的删除文件一致的条目
            needToDeleteFromPlaylistDeque.clear();
            for (PlayListBean playItem:mPlayListAdapter.getListDatas())
            {
                // 删除的文件在播放列表中
                if (playItem.getMediaData().getPath().equals(needDeleteDiskData.getPath()))
                {
                    needToDeleteFromPlaylistDeque.add(playItem);
                }
            }

            // 从播放列表删除这些条目
            while (!needToDeleteFromPlaylistDeque.isEmpty()) {
                PlayListBean needDeletePlayData = needToDeleteFromPlaylistDeque.pop();
                mPlayListAdapter.removeItem(needDeletePlayData);
            }

            if (needDeleteDiskData.getSource()==SOURCE_LOCAL) { // 本地文件

                // 从界面中删除
                mAllMediaListAdapter.removeItem(needDeleteDiskData);
                // 从数据库删除
                new MediaBeanDao(MediaActivity.this).delete(needDeleteDiskData);
                //从磁盘删除
                FileUtil.deleteFile(needDeleteDiskData.getPath());

            } else {                                                            // 云端文件

                int downloadState = needDeleteDiskData.getDownloadState();
                if (downloadState == STATE_DOWNLOAD_DOWNLOADED) {

                    needDeleteDiskData.setDownloadProgress(0);
                    needDeleteDiskData.setDownloadState(STATE_DOWNLOAD_NONE);
                    needDeleteDiskData.setDuration(0);

                    // 更新界面
                    mAllMediaListAdapter.update(needDeleteDiskData);
                    // 更新数据库
                    new MediaBeanDao(MediaActivity.this).update(needDeleteDiskData);
                    // 从磁盘删除
                    FileUtil.deleteFile(needDeleteDiskData.getPath());

                } else if (downloadState != STATE_DOWNLOAD_NONE) {

                    Intent intent = new Intent().setAction(DownloadService.ACTION_MEDIA_DOWNLOAD_CANCEL);
                    intent.putExtra(EXTRA_KEY_URL, needDeleteDiskData.getUrl());
                    Log.i(TAG, intent.getAction().toString());
                    startService(intent);
                }
            }

            deleteCount++;
        }

        // playlist还有剩下的元素,并且停止过播放
        if (mPlayListAdapter.getCount()>=1 && removePlaying)
        {
            mPlayer.mediaPlayNext();
        }

        mAllMediaListAdapter.refresh();
        updateCapacityUi(MASS_STORAGE_PATH);
        ToastUtil.showToast(MediaActivity.this, getResources().getString(R.string.str_media_file_delete_toast) + deleteCount);
    }
}

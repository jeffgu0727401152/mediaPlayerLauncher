package com.whitesky.tv.projectorlauncher.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
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
import com.j256.ormlite.dao.ForeignCollection;
import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.admin.DeviceInfoActivity;
import com.whitesky.tv.projectorlauncher.application.MainApplication;
import com.whitesky.tv.projectorlauncher.common.Contants;
import com.whitesky.tv.projectorlauncher.common.HttpConstants;
import com.whitesky.tv.projectorlauncher.home.HomeActivity;
import com.whitesky.tv.projectorlauncher.media.adapter.MediaLibraryListAdapter;
import com.whitesky.tv.projectorlauncher.media.adapter.PlayListAdapter;
import com.whitesky.tv.projectorlauncher.media.adapter.UsbMediaListAdapter;
import com.whitesky.tv.projectorlauncher.media.bean.MediaLibraryListBean;
import com.whitesky.tv.projectorlauncher.media.bean.CloudListBean;
import com.whitesky.tv.projectorlauncher.media.bean.UsbMediaListBean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBeanDao;
import com.whitesky.tv.projectorlauncher.media.db.PlayBean;
import com.whitesky.tv.projectorlauncher.media.db.PlayBeanDao;
import com.whitesky.tv.projectorlauncher.service.download.DownloadService;
import com.whitesky.tv.projectorlauncher.service.mqtt.bean.FileListPushBean;
import com.whitesky.tv.projectorlauncher.service.mqtt.bean.MediaListPushBean;
import com.whitesky.tv.projectorlauncher.service.mqtt.bean.PlayModePushBean;
import com.whitesky.tv.projectorlauncher.service.mqtt.DataListCovert;
import com.whitesky.tv.projectorlauncher.utils.FileUtil;
import com.whitesky.tv.projectorlauncher.utils.MediaScanUtil;
import com.whitesky.tv.projectorlauncher.utils.PathUtil;
import com.whitesky.tv.projectorlauncher.utils.SharedPreferencesUtil;
import com.whitesky.tv.projectorlauncher.utils.ShellUtil;
import com.whitesky.tv.projectorlauncher.utils.ToastUtil;
import com.whitesky.tv.projectorlauncher.utils.ViewUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.widget.AdapterView.INVALID_POSITION;
import static com.whitesky.tv.projectorlauncher.common.Contants.ACTION_CMD_QRCODE_CONTROL;
import static com.whitesky.tv.projectorlauncher.common.Contants.CLOUD_MEDIA_FREE_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.CLOUD_MEDIA_PRIVATE_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.CLOUD_MEDIA_PUBLIC_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.CONFIG_MEDIA_LIST_ORDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.CONFIG_PLAY_INDEX;
import static com.whitesky.tv.projectorlauncher.common.Contants.CONFIG_QRCODE_URL;
import static com.whitesky.tv.projectorlauncher.common.Contants.CONFIG_REPLAY_MODE;
import static com.whitesky.tv.projectorlauncher.common.Contants.CONFIG_SHOW_MASK;
import static com.whitesky.tv.projectorlauncher.common.Contants.CONFIG_SHOW_QRCODE;
import static com.whitesky.tv.projectorlauncher.common.Contants.COPY_TO_USB_MEDIA_EXPORT_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.MASS_STORAGE_PATH;
import static com.whitesky.tv.projectorlauncher.common.Contants.MASS_STORAGE_MOUNT_BROKER;
import static com.whitesky.tv.projectorlauncher.common.Contants.LOCAL_SATA_MOUNT_PATH;
import static com.whitesky.tv.projectorlauncher.common.Contants.MEDIA_LIST_ORDER_DEFAULT;
import static com.whitesky.tv.projectorlauncher.common.Contants.MEDIA_LIST_ORDER_DURATION;
import static com.whitesky.tv.projectorlauncher.common.Contants.MEDIA_LIST_ORDER_NAME;
import static com.whitesky.tv.projectorlauncher.common.Contants.MEDIA_LIST_ORDER_SOURCE;
import static com.whitesky.tv.projectorlauncher.common.Contants.USB_DEVICE_DEFAULT_SEARCH_MEDIA_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.mMountExceptList;
import static com.whitesky.tv.projectorlauncher.common.HttpConstants.LOGIN_STATUS_NOT_YET;
import static com.whitesky.tv.projectorlauncher.common.HttpConstants.LOGIN_STATUS_SUCCESS;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.ERROR_FILE_NEED_DOWNLOAD;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.ERROR_FILE_NOT_EXIST;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.ERROR_FILE_PATH_NONE;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.ERROR_IMAGE_PLAY_ERROR;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.ERROR_PLAYLIST_INVALIDED_POSITION;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.ERROR_PLAYLIST_MEDIA_NONE;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.ERROR_VIDEO_PLAY_FAILED;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.ERROR_VIDEO_PREPARE_FAILED;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.PLAYER_STATE_IDLE;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.PLAYER_STATE_PLAY_COMPLETE;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.PLAYER_STATE_PLAY_STOP;
import static com.whitesky.tv.projectorlauncher.media.adapter.PlayListAdapter.CHANGE_EVENT_ADD;
import static com.whitesky.tv.projectorlauncher.media.adapter.PlayListAdapter.CHANGE_EVENT_CLEAR;
import static com.whitesky.tv.projectorlauncher.media.adapter.PlayListAdapter.CHANGE_EVENT_EXCHANGE;
import static com.whitesky.tv.projectorlauncher.media.adapter.PlayListAdapter.CHANGE_EVENT_REMOVE;
import static com.whitesky.tv.projectorlauncher.media.adapter.PlayListAdapter.CHANGE_EVENT_SCALE;
import static com.whitesky.tv.projectorlauncher.media.adapter.PlayListAdapter.CHANGE_EVENT_TIME;
import static com.whitesky.tv.projectorlauncher.media.adapter.PlayListAdapter.CHANGE_EVENT_UPDATE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_UNKNOWN;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.SOURCE_CLOUD_FREE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.SOURCE_CLOUD_PRIVATE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.SOURCE_CLOUD_PUBLIC;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_DOWNLOADED;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.ID_LOCAL;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_PICTURE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_VIDEO;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.SOURCE_LOCAL;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_DOWNLOADING;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_NONE;
import static com.whitesky.tv.projectorlauncher.media.db.PlayBean.PLAY_INDEX_PREVIEW;
import static com.whitesky.tv.projectorlauncher.service.download.DownloadService.EXTRA_KEY_URL;
import static com.whitesky.tv.projectorlauncher.service.mqtt.MqttSslService.MSG_CMD_NEXT;
import static com.whitesky.tv.projectorlauncher.service.mqtt.MqttSslService.MSG_CMD_PREVIOUS;

/**
 * Created by jeff on 18-1-16.
 */
public class MediaActivity extends Activity
        implements View.OnClickListener,
        PictureVideoPlayer.OnMediaEventListener,
        RadioGroup.OnCheckedChangeListener {

    private static final String TAG = MediaActivity.class.getSimpleName();

    private static final long LOCAL_CAPACITY_WARNING_SIZE = 1024l*1024l*1024l*5;     //5G以下容量红色提示

    // 重放模式
    public static final int MEDIA_REPLAY_ONE = 0;
    public static final int MEDIA_REPLAY_ALL = 1;
    public static final int MEDIA_REPLAY_SHUFFLE = 2;
    public static final int MEDIA_REPLAY_MODE_DEFAULT = MEDIA_REPLAY_ALL;

    // 播放方向
    public static final int MEDIA_PLAY_DRIECT_FORWARD = 0;
    public static final int MEDIA_PLAY_DRIECT_BACKWORD = 1;
    public static final int MEDIA_PLAY_DRIECT_STAY = 2;

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

    private List<PlayBean> mPlayListBeans = new ArrayList<>();
    private PlayListAdapter mPlayListAdapter;    // 除onCreate与onResume外,其他所有对于playlist数据的操作都通过adapter做,好触发onPlaylistItemEvent
    private DragListView mDragPlayListView;

    private List<MediaLibraryListBean> mMediaLibraryListBeans = new ArrayList<MediaLibraryListBean>();
    private MediaLibraryListAdapter mMediaLibraryListAdapter;
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

    private long mLastDownloadUpdateUiTime = 0;

    private void changeWholePlayList(List<PlayBean> target) {
        // 替换整个列表的这个调用会回调CHANGE_EVENT_CLEAR到MediaActivity,将PlayIndex设置为-1
        mPlayListAdapter.setListDatasNotifyChange(target);
        mPlayListAdapter.refresh();

        for (PlayBean bean : mPlayListAdapter.getListDatas()) {
            if (bean.getMedia().getDownloadState()==STATE_DOWNLOAD_NONE) {
                addToDownload(bean.getMedia());
            }
        }

        if (mPlayListBeans.size()>0) {
            mPlayer.fullScreenSwitch(true);
            mPlayer.mediaPlay(MEDIA_PLAY_DRIECT_FORWARD,false);
        }
    }

    private final BroadcastReceiver serviceEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (action.equals(Contants.ACTION_CMD_PLAY_CONTROL)) {

                int playControlMsg = intent.getIntExtra(Contants.EXTRA_MQTT_ACTION_CONTEXT,-1);
                if (playControlMsg == -1) {
                    Log.e(TAG,"mqtt receive a error format play control!");
                    return;
                }
                if (mPlayListAdapter.hasPlayableItem()) {
                    if (playControlMsg==MSG_CMD_PREVIOUS) {
                        mPlayer.mediaPlayPrevious();
                    } else if (playControlMsg==MSG_CMD_NEXT)
                        mPlayer.mediaPlayNext();
                }

            }  else if (action.equals(ACTION_CMD_QRCODE_CONTROL)) {

                String QRCodeStr = intent.getStringExtra(Contants.EXTRA_MQTT_ACTION_CONTEXT);
                if (QRCodeStr == null || QRCodeStr.isEmpty()) {
                    mPlayer.getMaskController().hideQRcode();
                } else {
                    mPlayer.getMaskController().showQRcode(QRCodeStr,null);
                }

            } else if (action.equals(Contants.ACTION_PUSH_PLAYLIST)) {

                ArrayList<MediaListPushBean> cloudPushPlaylist = intent.getParcelableArrayListExtra(Contants.EXTRA_MQTT_ACTION_CONTEXT);

                if (cloudPushPlaylist==null) {
                    Log.e(TAG,"mqtt receive a error format push play list!");
                    return;
                }

                mPlayer.mediaStop();

                List<PlayBean> pList = new ArrayList<>();
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

                            List<PlayBean> pList = new ArrayList<>();
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

                PlayModePushBean pushReq = intent.getParcelableExtra(Contants.EXTRA_MQTT_ACTION_CONTEXT);

                if (pushReq==null) {
                    Log.e(TAG,"mqtt receive a error format push play mode!");
                    return;
                }

                if (pushReq.getPlayMode()==MEDIA_REPLAY_SHUFFLE
                        || pushReq.getPlayMode()==MEDIA_REPLAY_ONE
                        || pushReq.getPlayMode()==MEDIA_REPLAY_ALL) {
                    saveReplayModeToConfig(getApplicationContext(), pushReq.getPlayMode());
                    loadReplayModeToUi();
                }

                if (pushReq.getMask()==0 || pushReq.getMask()==1) {
                    saveShowMaskToConfig(getApplicationContext(), pushReq.getMask() == 0 ? false : true);
                    mPlayer.getMaskController().showDefaultMask();
                }

            } else if (action.equals(Contants.ACTION_PUSH_DELETE)) {

                ArrayList<FileListPushBean> pushList = intent.getParcelableArrayListExtra(Contants.EXTRA_MQTT_ACTION_CONTEXT);

                if (pushList==null) {
                    Log.e(TAG,"mqtt receive a error format delete list!");
                    return;
                }

                synchronized (mDeleteDeque) {
                    DataListCovert.covertCloudFileListToMediaBeanList(getApplicationContext(),mDeleteDeque,pushList);
                }
                deleteMediaFileInThread(mDeleteDeque);

            } else if (action.equals(Contants.ACTION_PUSH_DOWNLOAD_NEED_SYNC)) {

                ArrayList<FileListPushBean> downloadCloudList = intent.getParcelableArrayListExtra(Contants.EXTRA_MQTT_ACTION_CONTEXT);

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

                if (bean.getDownloadState()==STATE_DOWNLOAD_NONE || bean.getDownloadState()==STATE_DOWNLOAD_DOWNLOADED) {
                    // 删除和下载完成文件更新磁盘容量
                    updateCapacityUi(MASS_STORAGE_PATH);

                }

                mMediaLibraryListAdapter.update(bean);

                // 被播放加入播放列表的项目,才需要在下载的时候通知播放列表更新
                MediaBean updateMedia = new MediaBeanDao(getApplicationContext()).queryByPath(bean.getPath());
                if (updateMedia == null)
                {
                    Log.w(TAG, "update media has been removed : " + bean.toString());
                    return;
                }

                ForeignCollection<PlayBean> playListRefs = updateMedia.getPlayBeans();
                if (playListRefs!=null && !playListRefs.isEmpty() && bean.getDownloadState()!=STATE_DOWNLOAD_DOWNLOADING)
                {
                    // 正在下载中的进度不通知到播放列表,减轻加锁以后的更新压力
                    mPlayListAdapter.updateNotifyChange(bean);
                    mPlayListAdapter.refresh();
                }

                // 如果一个下载列表中不存在已经下载完成的项目, 在play complete的时候就不会在调用mediaPlay
                // 那么当这边有下载完的条目的时候,就需要开始播放
                if (bean.getDownloadState()==STATE_DOWNLOAD_DOWNLOADED) {
                    if (mPlayer.isFullScreen() && mPlayer.getPlayState()== PLAYER_STATE_PLAY_STOP) {
                        Message msg = mHandler.obtainMessage();
                        msg.what = MSG_MEDIA_PLAY_COMPLETE;
                        mHandler.sendMessage(msg);
                    }
                }

                // 调用refresh刷新list会导致UI无法,目前是每下载2M就会汇报一次进度,如果网速非常快,更新频繁可能导致ANR
                // 所以此处实时更新变量的内容,但是刷新限定一秒更新一次,除非需要更新的内容是 下载完毕/下载错误
                long timeNow = System.currentTimeMillis();
                if (timeNow - mLastDownloadUpdateUiTime > 1000
                        || bean.getDownloadState()!=STATE_DOWNLOAD_DOWNLOADING) {
                    Log.i(TAG,"download update Ui list");
                    mMediaLibraryListAdapter.refresh();
                    mLastDownloadUpdateUiTime = timeNow;
                }

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
                Log.i(TAG,"mount " + intent.getData().getPath());
                ToastUtil.showToast(context, getResources().getString(R.string.str_media_usb_device_plug_in_toast) + intent.getData().getPath());

            } else if (action.equals(Intent.ACTION_MEDIA_REMOVED) || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                mHandler.removeMessages(MSG_USB_PLUG_OUT);
                Message msg = new Message();
                msg.what = MSG_USB_PLUG_OUT;
                Bundle bundle = new Bundle();
                bundle.putString(BUNDLE_KEY_STORAGE_PATH, intent.getData().getPath());
                msg.setData(bundle);
                mHandler.sendMessageDelayed(msg, 500);
                Log.i(TAG,"unmount " + intent.getData().getPath());
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
    public void onMediaPlayInfoUpdate(String name, String mimeType, int width, int height, long size, int bps) {
        // 直接改变UI显示的播放信息
        Log.d(TAG,"on Play Info Update!");
        setMediaInfoUi(name, mimeType, width, height, size, bps);
    }

    // 利用 方向,播放模式,当前播放位置, 是否强制更新位置, 计算出下一个播放位置
    // 如果播放列表没有内容则返回 INVALID_POSITION
    private int updatePlayPosition(int direct, int replayMode, int playIndex, boolean force) {
        List<PlayBean> pList = new PlayBeanDao(this).selectAll();
        if (pList.isEmpty()) {
            Log.w(TAG,"update position, pList empty!");
            return INVALID_POSITION;
        }

        int count = pList.size();
        Log.i(TAG,"update position, direct = " + direct
                + ", replayMode = " + replayMode
                + ", playIndex = " + playIndex
                + ", force = " + force
                +", pList size = " + count);

        // 如果是要求继续播放当前项目,只检查是否越界, 如果越界就拉回
        if (MEDIA_PLAY_DRIECT_STAY == direct) {
            if (playIndex<=INVALID_POSITION || playIndex>=count) {
                playIndex = 0;
            }
            Log.d(TAG,"update position (STAY), return " + playIndex);
            return playIndex;
        }

        switch(replayMode)
        {
            case MEDIA_REPLAY_ONE:
                if (!force) {
                    // 如果不强制更新,那么检查是否当前的playIndex是否合法
                    // 调整为合法后break除去,否则会穿过这个case,根据方向调整
                    if (playIndex<=INVALID_POSITION || playIndex>=count) {
                        playIndex = 0;
                    }
                    break;
                }
            case MEDIA_REPLAY_ALL:
                if (direct==MEDIA_PLAY_DRIECT_FORWARD)
                {
                    playIndex++;
                    if (playIndex >= count) {
                        playIndex = 0;
                    }
                }
                else if (direct==MEDIA_PLAY_DRIECT_BACKWORD)
                {
                    playIndex--;
                    if (playIndex<0) {
                        playIndex = count-1;
                    }
                }
                break;

            case MEDIA_REPLAY_SHUFFLE:
                playIndex = getRandomNum(count);
                break;
        }

        Log.d(TAG,"update position, return " + playIndex);
        return playIndex;
    }

    private int getRandomNum(int endNum){
        if(endNum > 0){
            Random random = new Random();
            return random.nextInt(endNum);
        }
        return 0;
    }

    @Override
    public PlayBean onMediaPlayRequestPlayBean(int direct, boolean force) {
        Log.d(TAG,"on Media Play Request!");

        int playIndex = getPlayIndexFromConfig();
        int replayMode = loadReplayModeFromConfig(getApplicationContext());

        // 计算出应该播放的位置
        playIndex = updatePlayPosition(direct,replayMode, playIndex, force);

        // 该播放的位置的文件是否已经下载
        if (playIndex!=INVALID_POSITION && mPlayListAdapter.getItem(playIndex).getMedia().getDownloadState()!=STATE_DOWNLOAD_DOWNLOADED) {
            playIndex = mPlayListAdapter.firstPlayableItemIndex(playIndex);
        }

        mPlayListAdapter.setPlayIndex(playIndex);
        savePlayIndexToConfig(this,mPlayListAdapter.getPlayIndex());
        mPlayListAdapter.refresh();

        Log.d(TAG,"new playIndex = " + playIndex);

        if (playIndex==INVALID_POSITION) {
            Log.w(TAG,"playlist is nothing to play!");
            return null;
        } else {
            return new PlayBeanDao(this).queryByIdx(playIndex);
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
        mHandler.sendMessage(msg);
    }

    @Override
    public void onMediaPlayError(int error, MediaBean errorBean) {
        // 播放错误
        Log.d(TAG,"media Play ERR_NO:" + error);
        if (errorBean!=null) {
            Log.d(TAG, "error bean is " + errorBean.toString());
        }

        // 需要使用toast提示用户的错误
        switch (error) {
            case ERROR_FILE_PATH_NONE:
                ToastUtil.showToast(MediaActivity.this, R.string.str_media_play_path_error);
                break;
            case ERROR_FILE_NOT_EXIST:
                ToastUtil.showToast(MediaActivity.this, R.string.str_media_play_file_not_found_error);
                break;
            case ERROR_PLAYLIST_INVALIDED_POSITION:
                ToastUtil.showToast(MediaActivity.this, R.string.str_media_play_list_empty);
                break;
            case ERROR_PLAYLIST_MEDIA_NONE:
                // 播放列表数据中没有媒体数据
                break;
            case ERROR_VIDEO_PREPARE_FAILED:
                ToastUtil.showToast(MediaActivity.this, R.string.str_media_play_error_file_format);
                break;
            case ERROR_VIDEO_PLAY_FAILED:
                ToastUtil.showToast(MediaActivity.this, R.string.str_media_play_video_error);
                break;
            case ERROR_IMAGE_PLAY_ERROR:
                ToastUtil.showToast(MediaActivity.this, R.string.str_media_play_image_error);
                break;
            case ERROR_FILE_NEED_DOWNLOAD:
                // 文件需要下载
                break;
            default:
                break;
        }

        Message msg = mHandler.obtainMessage();
        msg.what = MSG_MEDIA_PLAY_COMPLETE;
        mHandler.sendMessageDelayed(msg,2000);
    }
    // 媒体播放的回调函数=============结束


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch(group.getId()) {
            case R.id.rg_media_replay_mode:

                int replayMode = MEDIA_REPLAY_MODE_DEFAULT;

                if (checkedId == mReplayOneRadioButton.getId()) {
                    replayMode = MEDIA_REPLAY_ONE;
                } else if (checkedId == mReplayAllRadioButton.getId()) {
                    replayMode = MEDIA_REPLAY_ALL;
                } else if (checkedId == mReplayShuffleRadioButton.getId()) {
                    replayMode = MEDIA_REPLAY_SHUFFLE;
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

    public static String loadQRcodeUrlFromConfig(Context context) {
        SharedPreferencesUtil config = new SharedPreferencesUtil(context, Contants.PREF_CONFIG);
        return config.getString(CONFIG_QRCODE_URL);
    }

    public static void saveQRcodeUrlToConfig(Context context, String qrCodeStr) {
        SharedPreferencesUtil config = new SharedPreferencesUtil(context, Contants.PREF_CONFIG);
        config.putString(CONFIG_QRCODE_URL,qrCodeStr);
    }

    public static boolean needShowQRcode(Context context) {
        SharedPreferencesUtil config = new SharedPreferencesUtil(context, Contants.PREF_CONFIG);
        return config.getBoolean(CONFIG_SHOW_QRCODE);
    }

    public static void saveShowQRcodeToConfig(Context context, boolean showQrcode) {
        SharedPreferencesUtil config = new SharedPreferencesUtil(context, Contants.PREF_CONFIG);
        config.putBoolean(CONFIG_SHOW_QRCODE,showQrcode);
    }

    public static void saveShowMaskToConfig(Context context, boolean showMask) {
        SharedPreferencesUtil config = new SharedPreferencesUtil(context, Contants.PREF_CONFIG);
        config.putBoolean(CONFIG_SHOW_MASK,showMask);
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
        return config.getInt(CONFIG_REPLAY_MODE, MEDIA_REPLAY_MODE_DEFAULT);
    }

    private void loadReplayModeToUi() {
        int playMode = loadReplayModeFromConfig(getApplicationContext());
        int checkId = mReplayAllRadioButton.getId();
        switch (playMode) {
            case MEDIA_REPLAY_ONE:
                checkId = mReplayOneRadioButton.getId();
                break;
            case MEDIA_REPLAY_ALL:
                checkId = mReplayAllRadioButton.getId();
                break;
            case MEDIA_REPLAY_SHUFFLE:
                checkId = mReplayShuffleRadioButton.getId();
                break;
            default:
                break;
        }
        mReplayModeRadioGroup.check(checkId);
    }

    private void loadMediaListFromDb() {
        String localMediaStorePath = PathUtil.localFileStoragePath();
        File localFolder = new File(localMediaStorePath);
        if (!localFolder.exists()) {
            localFolder.mkdir();
        } else if (!localFolder.isDirectory()) {
            localFolder.delete();
            localFolder.mkdir();
        }

        // 初始化媒体列表
        if (new MediaBeanDao(MediaActivity.this).selectAll().isEmpty())
        {   // 如果数据库为空,则扫描一次本地媒体文件
            Log.i(TAG, "empty media database, so scan and download media list");
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
            Log.i(TAG, "has media database, so get media list from media database");
            for (MediaBean m:new MediaBeanDao(MediaActivity.this).selectAll())
            {
                mMediaLibraryListBeans.add(new MediaLibraryListBean(m));
            }

            if (mMediaLibraryListAdapter !=null) {
                mMediaLibraryListAdapter.refresh();
            }
            Log.i(TAG, "sync media database done");
        }
    }

    public interface cloudListGetCallback{
        void cloudSyncDone(boolean result);
    }

    // 从云端同步数据库，然后与本地端磁盘的数据进行比对，比对完成后调callback函数
    public static void loadMediaListFromCloud(final Context context, final cloudListGetCallback callback) {
        Log.i(TAG, "call load Media List From Cloud in");
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

                    new MediaBeanDao(context).deleteItemsFromCloud();

                    if  (cloudList != null) {
                        if (cloudList.getStatus().equals(LOGIN_STATUS_SUCCESS)) {

                            // 获取用户oemID,用于系统底层播放读取,解密播放文件
                            String userAccount = cloudList.getAccount();
                            if (userAccount!=null && !userAccount.isEmpty()) {
                                Log.d(TAG,"user account = " + userAccount);
                                SystemProperties.set("oemdata.whitesky.account", userAccount);
                            }

                            Log.d(TAG,"onResponse get "+cloudList.getResult().size() + " media info(s) from cloud");

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

        File cloudPublicFolder = new File(PathUtil.cloudPublicFileStoragePath());
        updateCloudDbItemByTraversal(cloudPublicFolder,context,scanner);
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
                    int source = SOURCE_CLOUD_FREE;
                    if (cloudStorageFolder.getAbsolutePath().contains(CLOUD_MEDIA_FREE_FOLDER)) {
                        source = SOURCE_CLOUD_FREE;
                    } else if (cloudStorageFolder.getAbsolutePath().contains(CLOUD_MEDIA_PRIVATE_FOLDER)) {
                        source = SOURCE_CLOUD_PRIVATE;
                    } else if (cloudStorageFolder.getAbsolutePath().contains(CLOUD_MEDIA_PUBLIC_FOLDER)) {
                        source = SOURCE_CLOUD_PUBLIC;
                    }

                    bean = new MediaBean(name,ID_LOCAL,type,source, path,duration,size);
                    bean.setDownloadState(STATE_DOWNLOAD_DOWNLOADED);
                    bean.setUrl("");
                }

                new MediaBeanDao(context).createOrUpdate(bean);
            }
        }
    }

    private int getPlayIndexFromConfig() {
        SharedPreferencesUtil config = new SharedPreferencesUtil(this, Contants.PREF_CONFIG);
        return config.getInt(CONFIG_PLAY_INDEX);
    }

    public static void savePlayIndexToConfig(Context context,int playIndex) {
        Log.d(TAG,"save play index = " + playIndex);
        SharedPreferencesUtil config = new SharedPreferencesUtil(context, Contants.PREF_CONFIG);
        config.putInt(CONFIG_PLAY_INDEX, playIndex);
    }

    public static boolean havePlayList(Context context) {
        List<PlayBean> pList = new PlayBeanDao(context).selectAll();
        if(pList.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    private void loadPlaylistFromDb() {
        List<PlayBean> datas = new PlayBeanDao(this).selectAll();
        // 因为 media表 和 play表 在数据库中是关联的, 所以从数据库中查询出的PlayBean对象中的MediaBean一定是新的
        // 不必担心media表中的数据下载完成了, 而Play表查询出来的对象的MediaBean字段中还是正在下载
        for (PlayBean bean : datas) {
            mPlayListBeans.add(bean);
        }

        if (mPlayListAdapter !=null) {
            mPlayListAdapter.refresh();
        }
    }

    private void initView() {
        setContentView(R.layout.activity_media);
        mPlayer = findViewById(R.id.pictureVideoPlayer_playArea);

        mMultiCopyToLocalBtn = findViewById(R.id.bt_media_multi_copy_to_left);
        mMultiDeleteLocalBtn = findViewById(R.id.bt_media_multi_delete);
        mMultiAddToPlayListBtn = findViewById(R.id.bt_media_multi_add);
        mMultiDownloadBtn = findViewById(R.id.bt_media_multi_download);
        mMultiCopyToUsbBtn = findViewById(R.id.bt_media_multi_copy_to_right);

        mLocalMediaListRefreshBtn = findViewById(R.id.bt_media_local_list_refresh);
        mCloudMediaListRefreshBtn = findViewById(R.id.bt_media_cloud_list_refresh);

        mMediaInfoNameTextView = findViewById(R.id.tv_media_play_info_name);
        mMediaInfoTypeTextView = findViewById(R.id.tv_media_play_info_type);
        mMediaInfoWidthHeightTextView = findViewById(R.id.tv_media_play_info_wh);
        mMediaInfoSizeTextView = findViewById(R.id.tv_media_play_info_size);
        mMediaInfoBpsTextView = findViewById(R.id.tv_media_play_info_bps);

        mReplayModeRadioGroup = findViewById(R.id.rg_media_replay_mode);
        mReplayAllRadioButton = findViewById(R.id.rb_media_replay_all);
        mReplayOneRadioButton = findViewById(R.id.rb_media_replay_one);
        mReplayShuffleRadioButton = findViewById(R.id.rb_media_replay_shuffle);

        mMediaListOrderRadioGroup = findViewById(R.id.rg_media_list_order_mode);
        mMediaListOrderNameRadioButton = findViewById(R.id.rb_media_order_name);
        mMediaListOrderDurationRadioButton = findViewById(R.id.rb_media_order_duration);
        mMediaListOrderSourceRadioButton = findViewById(R.id.rb_media_order_source);

        mAllMediaListView = findViewById(R.id.lv_media_all_list);
        mUsbMediaListView = findViewById(R.id.lv_media_usb_list);
        mDragPlayListView = findViewById(R.id.lv_media_playList);

        mLocalCapacityTextView = findViewById(R.id.tv_media_all_list_capacity);
        mUsbCapacityTextView = findViewById(R.id.tv_media_usb_list_capacity);
        mUsbPartitionSpinner = findViewById(R.id.sp_media_usb_partition_spinner);

        mUsbListTitle = findViewById(R.id.tv_media_usb_list_name);

        mAllMediaListCheckBox = findViewById(R.id.cb_media_all_list_check);
        mUsbMediaListCheckBox = findViewById(R.id.cb_media_usb_list_check);
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
                msg.obj = mUsbPartitionAdapter.getItem(position);
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
        mMediaLibraryListAdapter = new MediaLibraryListAdapter(getApplicationContext(), mMediaLibraryListBeans);
        mMediaLibraryListAdapter.setOnALlMediaListItemListener(new MediaLibraryListAdapter.OnAllMediaListItemEventListener() {
            @Override
            public void doItemDelete(int position) {
                mDeleteDeque.clear();
                mDeleteDeque.push(mMediaLibraryListAdapter.getItem(position).getMediaData());
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_MEDIA_LIST_ITEM_DELETE;
                mHandler.sendMessage(msg);
            }

            @Override
            public void doItemPreview(int position) {
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_MEDIA_LIST_ITEM_PREVIEW;
                msg.obj = mMediaLibraryListAdapter.getItem(position).getMediaData();
                mHandler.sendMessage(msg);
            }

            @Override
            public void doItemDownLoad(int position) {
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_MEDIA_LIST_ITEM_DOWNLOAD_OR_PAUSE;
                msg.obj = mMediaLibraryListAdapter.getItem(position).getMediaData();
                mHandler.sendMessage(msg);
            }

            @Override
            public void itemSelectedChange() {
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_MEDIA_LIST_SELECTED_CHANGE;
                mHandler.sendMessage(msg);
            }
        });
        mAllMediaListView.setAdapter(mMediaLibraryListAdapter);
        mAllMediaListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 如果是双击
                if (ViewUtil.isFastDoubleClick()) {
                    MediaBean mediaItem = mMediaLibraryListBeans.get(position).getMediaData();
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
            public List<PlayBean> onPlayListChanged(int event, List<PlayBean> changeBeans) {
                if (event!=CHANGE_EVENT_CLEAR && (changeBeans==null || changeBeans.isEmpty())) {
                    Log.w(TAG, "change event" + event + ", but Beans is empty!");
                    return new PlayBeanDao(MediaActivity.this).selectAll();
                }

                int playIndex;

                switch (event) {
                    case CHANGE_EVENT_REMOVE:
                        // remove 比较特殊, 我们是先删除数据库, 然后此函数返回后再sync play list
                        new PlayBeanDao(MediaActivity.this).delete(changeBeans);

                        // 根据remove的条目的idx, 判断当前的playIndex需要前移几次
                        // 如果移除的条目含有正在播放的,则停止播放
                        // 全移除完毕后如果还有可以播放的条目,则播放该条目
                        boolean playNeedResume = false;
                        int removeHowManyItemsBeforePlayItem = 0;
                        playIndex = getPlayIndexFromConfig();

                        PlayBean curPlay = mPlayer.getCurPlayBean();
                        if (curPlay!=null) {
                            for (PlayBean willDelete:changeBeans) {
                                // 只删除播放列表,不用考虑正在预览的情况,因为媒体库中的该文件没有受到影响
                                if (playNeedResume==false && playIndex == willDelete.getIdx()) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mPlayer.mediaStop();
                                        }
                                    });
                                    playNeedResume = true;
                                }

                                if (playIndex > willDelete.getIdx()) {
                                    removeHowManyItemsBeforePlayItem++;
                                }
                            }
                        }

                        // 更新playIndex
                        if (removeHowManyItemsBeforePlayItem!=0) {
                            playIndex = playIndex - removeHowManyItemsBeforePlayItem;
                            mPlayListAdapter.setPlayIndex(playIndex);
                            savePlayIndexToConfig(MediaActivity.this, playIndex);
                        }

                        if (mPlayListAdapter.hasPlayableItem() && playNeedResume) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mPlayer.mediaPlay(MEDIA_PLAY_DRIECT_STAY,true);
                                }
                            });
                        }
                        break;

                    case CHANGE_EVENT_SCALE:

                        if (!mPlayer.isPreview()
                                && mPlayer.getPlayState()!= PLAYER_STATE_IDLE
                                && mPlayer.getPlayState()!= PLAYER_STATE_PLAY_STOP
                                && mPlayer.getPlayState()!= PLAYER_STATE_PLAY_COMPLETE) {

                            playIndex = mPlayListAdapter.getPlayIndex();
                            if (playIndex == changeBeans.get(0).getIdx()) {
                                mPlayer.changeScaleNow(changeBeans.get(0).getScale());
                            }
                        }

                        for (PlayBean bean:changeBeans) {
                            new PlayBeanDao(MediaActivity.this).createOrUpdate(bean);
                        }
                        break;

                    case CHANGE_EVENT_CLEAR:
                        new PlayBeanDao(MediaActivity.this).deleteAll();
                        mPlayListAdapter.setPlayIndex(INVALID_POSITION);
                        savePlayIndexToConfig(MediaActivity.this, INVALID_POSITION);
                        break;

                    case CHANGE_EVENT_EXCHANGE:
                        // play index可能改变,所以此处同步顺序改变后的PlayIndex
                        playIndex = getPlayIndexFromConfig();
                        if (playIndex == changeBeans.get(0).getIdx()) {
                            playIndex =  changeBeans.get(1).getIdx();
                            mPlayListAdapter.setPlayIndex(playIndex);
                            savePlayIndexToConfig(MediaActivity.this, playIndex);
                        } else if (playIndex == changeBeans.get(1).getIdx()) {
                            playIndex =  changeBeans.get(0).getIdx();
                            mPlayListAdapter.setPlayIndex(playIndex);
                            savePlayIndexToConfig(MediaActivity.this, playIndex);
                        }

                        for (PlayBean bean:changeBeans) {
                            new PlayBeanDao(MediaActivity.this).createOrUpdate(bean);
                        }
                        break;

                    case CHANGE_EVENT_ADD:
                    case CHANGE_EVENT_TIME:
                    case CHANGE_EVENT_UPDATE:
                    default:
                        for (PlayBean bean:changeBeans) {
                            new PlayBeanDao(MediaActivity.this).createOrUpdate(bean);
                        }
                        break;
                }

                return new PlayBeanDao(MediaActivity.this).selectAll();
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
                return mPlayListAdapter.exchangeNotifyChange(srcPosition, position);
            }
        });

        mDragPlayListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 如果是双击
                if (ViewUtil.isFastDoubleClick()) {
                    Log.i(TAG, "double click play list!");
                    mPlayer.mediaStop();
                    mPlayListAdapter.setPlayIndex(position);
                    savePlayIndexToConfig(MediaActivity.this, position);
                    mPlayListAdapter.refresh();
                    mPlayer.mediaPlay(mPlayListAdapter.getItem(position));
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate");

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
        Log.d(TAG,"onResume in");

        super.onResume();
        LinearLayout layout = findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.shape_background);

        loadReplayModeToUi();
        loadMediaListOrderMode();
        loadMediaListFromDb();
        loadPlaylistFromDb();
        updateMultiActionButtonUiState();

        if (!needShowQRcode(this) || loadQRcodeUrlFromConfig(this).isEmpty()) {
            mPlayer.getMaskController().hideQRcode();
        } else {
            mPlayer.getMaskController().showQRcode(loadQRcodeUrlFromConfig(this),null);
        }

        // 监听usb插拔事件
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        usbFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        usbFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        usbFilter.addDataScheme("file");
        registerReceiver(usbMountEventReceiver, usbFilter);

        // 监听mqtt控制命令
        IntentFilter serviceEventFilter = new IntentFilter();
        serviceEventFilter.addAction(Contants.ACTION_CMD_PLAY_CONTROL);
        serviceEventFilter.addAction(Contants.ACTION_PUSH_PLAYLIST);
        serviceEventFilter.addAction(Contants.ACTION_PUSH_PLAYMODE);
        serviceEventFilter.addAction(Contants.ACTION_PUSH_DELETE);
        serviceEventFilter.addAction(Contants.ACTION_CMD_QRCODE_CONTROL);
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

        Log.d(TAG,"onResume out");
    }

    @Override
    protected void onPause() {
        Log.d(TAG,"onPause");

        if (mPlayer != null) {
            mPlayer.mediaStop();
        }

        unregisterReceiver(usbMountEventReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceEventReceiver);

        mHandler.removeCallbacksAndMessages(null);

        super.onPause();

        ((MainApplication)getApplication()).isBusyInCopy = false;
        ((MainApplication)getApplication()).isMediaActivityForeground = false;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy");
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
                mCloudMediaListRefreshBtn.setEnabled(false);
                loadMediaListFromCloud(getApplicationContext(), new cloudListGetCallback() {
                    @Override
                    public void cloudSyncDone(boolean result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mCloudMediaListRefreshBtn.setEnabled(true);
                            }
                        });

                        if (result == true) {
                            Message msg = mHandler.obtainMessage();
                            msg.what = MSG_MEDIA_LIST_UI_SYNC_WITH_DATABASE;
                            mHandler.sendMessage(msg);
                        }
                    }
                });
                break;

            case R.id.cb_media_all_list_check:
                for (MediaLibraryListBean data : mMediaLibraryListBeans) {
                    data.setSelected(mAllMediaListCheckBox.isChecked());
                }
                mMediaLibraryListAdapter.refresh();
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
                for (MediaLibraryListBean data : mMediaLibraryListBeans) {
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
                for (MediaLibraryListBean data : mMediaLibraryListAdapter.getListDatas()) {
                    if (data.isSelected()) {
                        addToPlayList(data.getMediaData());
                        data.setSelected(false);
                    }
                }
                mMediaLibraryListAdapter.refresh();
                break;

            case R.id.bt_media_multi_download:
                for (MediaLibraryListBean data : mMediaLibraryListAdapter.getListDatas()) {
                    if (data.isSelected()) {
                        addToDownload(data.getMediaData());
                        data.setSelected(false);
                    }
                }
                mMediaLibraryListAdapter.refresh();
                break;

            case R.id.bt_media_multi_copy_to_right:
                mCopyDeque.clear();
                for (MediaLibraryListBean data : mMediaLibraryListAdapter.getListDatas()) {
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
        if (addItem.getSource()==SOURCE_LOCAL || addItem.getDownloadState()==STATE_DOWNLOAD_DOWNLOADED) {
            Log.i(TAG, "call download do nothing, item is local/downloaded:" + addItem.toString());
            return;
        }

        Intent intent = new Intent().setAction(DownloadService.ACTION_MEDIA_DOWNLOAD_START);
        intent.putExtra(EXTRA_KEY_URL, addItem.getUrl());
        Log.i(TAG, "call download:" + addItem.toString());
        MediaActivity.this.startService(intent);
    }

    private void addToPlayList(MediaBean addItem) {
        if (addItem.getDownloadState()==STATE_DOWNLOAD_NONE) {
            addToDownload(addItem);
        }
        mPlayListAdapter.addItemNotifyChange(new PlayBean(addItem));
        mPlayListAdapter.refresh();
    }

    private void updateMultiActionButtonUiState() {
        if (mMediaLibraryListAdapter.hasItemSelected()) {
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

                    if (MASS_STORAGE_MOUNT_BROKER.equals(storagePath)) {
                        // 挂载了硬盘设备,很可能是开机,直接触发播放
                        // 播放类中在surfaceHolder建立的时候会尝试播放一次，但是可能会因为没有挂载SATA而失败，这边补一次
                        Log.i(TAG, "mount sata device means power on complete, start full screen play media");
                        if (mPlayer.getPlayState()== PLAYER_STATE_IDLE) {
                            mPlayer.fullScreenSwitch(true);
                            mPlayer.mediaPlay(MEDIA_PLAY_DRIECT_STAY,true);
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
                    mMediaLibraryListAdapter.clear();
                    mMediaLibraryListAdapter.refresh();
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
                    Intent intent = new Intent().setAction(DownloadService.ACTION_MEDIA_DOWNLOAD_START_OR_PAUSE);
                    intent.putExtra(EXTRA_KEY_URL, ((MediaBean)msg.obj).getUrl());
                    Log.i(TAG,"call download:" + msg.obj.toString());
                    startService(intent);
                    break;

                case MSG_MEDIA_LIST_ITEM_PREVIEW:
                    MediaBean previewItem = (MediaBean)msg.obj;
                    mPlayer.mediaStop();
                    PlayBean previewPlayBean = new PlayBean(previewItem);
                    previewPlayBean.setIdx(PLAY_INDEX_PREVIEW);
                    // 使用INVALID_POSITION -1 来让play list ui上不显示播放图标
                    mPlayListAdapter.setPlayIndex(INVALID_POSITION);
                    mPlayListAdapter.refresh();
                    // preview的时候只是去掉UI上的播放标记
                    // savePlayIndexToConfig(MediaActivity.this, INVALID_POSITION);
                    mPlayer.mediaPlay(previewPlayBean);
                    break;

                case MSG_MEDIA_LIST_UI_SYNC_WITH_DATABASE:
                    mMediaLibraryListAdapter.clear();

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
                        mMediaLibraryListAdapter.addItem(new MediaLibraryListBean(m));
                    }
                    mMediaLibraryListAdapter.refresh();
                    updateMultiActionButtonUiState();
                    break;

                case MSG_USB_MEDIA_SCAN_DONE:
                    mMultiCopyToLocalBtn.setEnabled(mUsbMediaListAdapter.hasItemSelected());
                    break;

                case MSG_MEDIA_PLAY_COMPLETE:
                    if (mPlayListAdapter.hasPlayableItem()) {
                        mPlayer.mediaStop();
                        mPlayer.mediaPlay(MEDIA_PLAY_DRIECT_FORWARD,false);
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
            // 自己挂载的硬盘在java层mountService无法得知,所以此处再使用shell判断一次
            ShellUtil.CommandResult result = ShellUtil.execCommand("mount |busybox sed 's/ /\\n/g'| grep \"/mnt/sata/\"",false,true);
            if (result.successMsg.contains(MASS_STORAGE_PATH)) {
                return true;
            } else {
                return false;
            }
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

    private void changeTextViewColorOnUiThread(final TextView textView, final int color) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setTextColor(color);
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
                            long totalSize = FileUtil.getTotalCapacity(path);
                            long availableSize = FileUtil.getAvailableCapacity(path);
                            String fsUsed = FileUtil.formatFileSize(totalSize - availableSize);
                            String fsCapacity = FileUtil.formatFileSize(totalSize);

                            if (path.contains(LOCAL_SATA_MOUNT_PATH)) {
                                changeTextViewColorOnUiThread(mLocalCapacityTextView,availableSize> LOCAL_CAPACITY_WARNING_SIZE ? Color.WHITE:Color.RED);
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
        updateCapacityUi(MASS_STORAGE_PATH);
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

    private class CopyToUsbCallback implements CopyTask.CopyTaskListener {
        @Override
        public void onCopyStartCallback() {
            ((MainApplication)getApplication()).isBusyInCopy = true;
        }

        @Override
        public void onAllCopyDoneCallback(Deque<String> copyDoneItem) {
            ToastUtil.showToast(MediaActivity.this, getResources().getString(R.string.str_media_file_copy_toast) + copyDoneItem.size());

            if (!copyDoneItem.isEmpty()) {
                // 复制完成后取消多选框的状态
                for (MediaLibraryListBean tmp : mMediaLibraryListAdapter.getListDatas()) {
                    tmp.setSelected(false);
                }
                mAllMediaListCheckBox.setChecked(false);

                mMediaLibraryListAdapter.refresh();
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

    private class CopyToInternalCallback implements CopyTask.CopyTaskListener {
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

    private void checkCapacityAndCopy(CopyTask.CopyTaskParam param, Deque<String> waitCopyDeque) {
        Deque<String> sameMediaDeque = new ArrayDeque<String>();
        sameMediaDeque.clear();

        // for循环里面desFolderFile.listFiles()为null会导致crash
        // 而如果local目录不存在会导致listFiles为null,所以这边检查不存在则建立
        File desFolderFile = new File(param.desFolder);
        if (desFolderFile.listFiles()==null) {
            if (!desFolderFile.exists()) {
                desFolderFile.mkdir();
            } else {
                desFolderFile.delete();
                desFolderFile.mkdir();
            }
        }

        synchronized (waitCopyDeque) {
            while (!waitCopyDeque.isEmpty()) {
                String path = waitCopyDeque.pop();

                // 重复文件检测
                String toPath = PathUtil.pathGenerate(path, param.desFolder);
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

        if (param.totalSize > FileUtil.getAvailableCapacity(param.desFolder)) {
            ToastUtil.showToast(this,getResources().getString(R.string.str_media_file_copy_capacity_prompt));
            return;
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

        checkCapacityAndCopy(param,mCopyDeque);
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

        checkCapacityAndCopy(param,mCopyDeque);
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
                            deleteMediaFileInThread(mDeleteDeque);
                    }
                })
                .setNegativeButton(R.string.str_media_dialog_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                }).show();
    }

    private class DeleteMediaCallback implements DeleteTask.DeleteTaskListener {
        @Override
        public void onDeleteStartCallback(){
            ((MainApplication)getApplication()).isBusyInDelete = true;
        }


        @Override
        public void onDeleteOneCallback(MediaBean bean){
            // 如果有预览,则停止预览
            PlayBean curPlay = mPlayer.getCurPlayBean();
            if (curPlay!=null && mPlayer.isPreview()) {
                if (bean.getPath().equals(curPlay.getMedia().getPath())) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPlayer.mediaStop();
                        }
                    });
                }
            }

            mPlayListAdapter.removeItemNotifyChange(new ArrayList<>(bean.getPlayBeans()));

            if (bean.getSource()==SOURCE_LOCAL) { // 本地文件

                // 从界面中删除
                mMediaLibraryListAdapter.removeItem(bean);
                // 从数据库删除
                new MediaBeanDao(MediaActivity.this).delete(bean);
                // 从磁盘删除
                FileUtil.deleteFile(bean.getPath());

            } else {                              // 云端文件

                int downloadState = bean.getDownloadState();

                if (downloadState == STATE_DOWNLOAD_DOWNLOADED) {

                    bean.setDownloadProgress(0);
                    bean.setDownloadState(STATE_DOWNLOAD_NONE);
                    bean.setDuration(0);

                    // 更新界面
                    mMediaLibraryListAdapter.update(bean);
                    // 更新数据库
                    new MediaBeanDao(MediaActivity.this).update(bean);
                    // 从磁盘删除
                    FileUtil.deleteFile(bean.getPath());

                } else if (downloadState == STATE_DOWNLOAD_NONE) {

                    // 没有开始下载的云端条目,啥都不做

                } else {

                    Intent intent = new Intent().setAction(DownloadService.ACTION_MEDIA_DOWNLOAD_CANCEL);
                    intent.putExtra(EXTRA_KEY_URL, bean.getUrl());
                    Log.i(TAG, intent.getAction());
                    startService(intent);

                }
            }
        }

        @Override
        public void onAllDeleteDoneCallback(int deleteCount) {
            if (deleteCount>0) {
                updateCapacityUi(MASS_STORAGE_PATH);
                mMediaLibraryListAdapter.refresh();
                mPlayListAdapter.refresh();
            }

            ToastUtil.showToast(MediaActivity.this, getResources().getString(R.string.str_media_file_delete_toast) + deleteCount);
            ((MainApplication)getApplication()).isBusyInDelete = false;
        }
    }

    private void deleteMediaFileInThread(Deque<MediaBean> deleteDeque) {
        DeleteTask.DeleteTaskParam param = new DeleteTask.DeleteTaskParam();
        param.deleteQueue = new ArrayDeque<>(deleteDeque);
        param.callback = new DeleteMediaCallback();
        new DeleteTask(this,true).execute(param);
    }
}

package com.whiteskycn.tv.projectorlauncher.media;

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
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.common.Contants;
import com.whiteskycn.tv.projectorlauncher.home.HomeActivity;
import com.whiteskycn.tv.projectorlauncher.media.adapter.AllMediaListAdapter;
import com.whiteskycn.tv.projectorlauncher.media.adapter.PlayListAdapter;
import com.whiteskycn.tv.projectorlauncher.media.adapter.UsbMediaListAdapter;
import com.whiteskycn.tv.projectorlauncher.media.bean.AllMediaListBean;
import com.whiteskycn.tv.projectorlauncher.media.bean.PlayListBean;
import com.whiteskycn.tv.projectorlauncher.media.bean.UsbMediaListBean;
import com.whiteskycn.tv.projectorlauncher.media.db.MediaBean;
import com.whiteskycn.tv.projectorlauncher.media.db.MediaBeanDao;
import com.whiteskycn.tv.projectorlauncher.utils.FileUtil;
import com.whiteskycn.tv.projectorlauncher.utils.MediaScanUtil;
import com.whiteskycn.tv.projectorlauncher.utils.SharedPreferencesUtil;
import com.whiteskycn.tv.projectorlauncher.utils.ToastUtil;
import com.whiteskycn.tv.projectorlauncher.utils.ViewUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_IDLE;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PAUSE_PICTURE;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PAUSE_VIDEO;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PLAY_VIDEO;
import static com.whiteskycn.tv.projectorlauncher.media.db.MediaBean.MEDIA_PICTURE;
import static com.whiteskycn.tv.projectorlauncher.media.db.MediaBean.MEDIA_VIDEO;
import static com.whiteskycn.tv.projectorlauncher.utils.MediaScanUtil.getMediaTypeFromPath;

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
    private final int MSG_USB_DEVICE_CAPACITY_SET = 3;
    private final int MSG_LOCAL_CAPACITY_SET = 4;

    private final int MSG_LOCAL_MEDIA_DATABASE_UPDATE = 10;
    private final int MSG_LOCAL_MEDIA_LIST_CLEAN = 11;
    private final int MSG_USB_MEDIA_LIST_UPDATE = 12;
    private final int MSG_USB_MEDIA_LIST_CLEAN = 13;

    private final int MSG_LOCAL_MEDIA_DATABASE_UI_SYNC = 20;
    private final int MSG_USB_MEDIA_SCAN_DONE = 21;

    private final int MSG_USB_COPY_TO_INTERNAL = 40;
    private final int MSG_MEDIA_LIST_ITEM_DELETE = 41;
    private final int MSG_MEDIA_LIST_ITEM_PREVIEW = 42;
    private final int MSG_MEDIA_LIST_ITEM_DOWNLOAD = 43;

    private final int MSG_USB_LIST_SELECTED_CHANGE = 44;
    private final int MSG_MEDIA_LIST_SELECTED_CHANGE = 45;

    // 媒体播放事件
    private final int MSG_MEDIA_PLAY_COMPLETE = 100;
    private final int MSG_UPDATE_PLAYED_TIME = 101;
    private final int MSG_UPDATE_DURATION_TIME = 102;
    private final int MSG_UPDATE_MEDIA_INFO = 103;
    private final int MSG_MEDIA_PLAY_STOP = 104;

    private static final String BUNDLE_KEY_STORAGE_PATH = "storagePath";

    private static final String BUNDLE_KEY_MEDIA_TYPE = "type";
    private static final String BUNDLE_KEY_MEDIA_SIZE = "size";
    private static final String BUNDLE_KEY_MEDIA_DURATION = "duration";
    private static final String BUNDLE_KEY_MEDIA_PATH = "path";
    private static final String BUNDLE_KEY_MEDIA_NAME = "name";

    private static final String BUNDLE_KEY_INFO_NAME = "name";
    private static final String BUNDLE_KEY_INFO_TYPE = "type";
    private static final String BUNDLE_KEY_INFO_SIZE = "size";
    private static final String BUNDLE_KEY_INFO_WIDTH = "width";
    private static final String BUNDLE_KEY_INFO_HEIGHT = "height";
    private static final String BUNDLE_KEY_INFO_BPS = "bitRate";


    private static final String CONFIG_PLAYMODE = "playMode";

    private static final String LOCAL_MASS_STORAGE_PATH = "/mnt/sdcard";  // todo 修改为硬盘的路径
    private static final String CLOUD_MEDIA_FOLDER = "cloud";
    private static final String LOCAL_MEDIA_FOLDER = "local";

    private static final String USB_DEVICE_DEFAULT_SEARCH_MEDIA_FOLDER = "media";
    private static final String COPY_TO_USB_MEDIA_EXPORT_FOLDER = "export";

    private String[] mMountExceptList = new String[]{"/mnt/sdcard", "/storage/emulated/0"}; // 排除在外的挂载目录,非移动硬盘

    private final int DOUBLE_TAP_DELAY_MS = 600;            // 双击间隔时间,可调参数
    private final int USB_COPY_BUFFER_SIZE = 1024*1024;     // 拷贝文件缓冲区长度,可调参数

    private MediaScanUtil mLocalMediaScanner;
    private MediaScanUtil mUsbMediaScanner;

    private PictureVideoPlayer mPlayer;

    private SurfaceView mVideoPlaySurfaceView;
    private ImageView mPicturePlayView;
    private MaskControl mMaskArea;
    private ImageView mMaskView;
    private PaintableImageView mPaintView;

    private Button mNetViewBtn;
    private Button mGrayViewBtn;

    private Button mPreviousBtn;
    private Button mPlayBtn;
    private Button mNextBtn;
    private Button mVolumeBtn;

    private Button mMediaMultiCopyToLeftBtn;
    private Button mMediaMultiDeleteBtn;
    private Button mMediaMultiAddToPlayListBtn;
    private Button mMediaMultiDownloadBtn;
    private Button mMediaMultiCopyToRightBtn;

    private Button mMediaListRefreshBtn;

    private FrameLayout mPlayerPanel;

    private AudioManager mAudioManager;

    private SeekBar mPlayProgressSeekBar;
    private SeekBar mMediaVolumeLevelSeekBar;
    private TextView mMediaPlayedTimeTextView;
    private TextView mMediaDurationTimeTextView;

    private TextView mMediaInfoNameTextView;
    private TextView mMediaInfoTypeTextView;
    private TextView mMediaInfoWidthHeightTextView;
    private TextView mMediaInfoSizeTextView;
    private TextView mMediaInfoBpsTextView;

    private RadioGroup mReplayModeRadioGroup;
    private RadioButton mReplayAllRadioButton;
    private RadioButton mReplayOneRadioButton;
    private RadioButton mReplayShuffleRadioButton;

    private int mOriginSurfaceHeight = 0;
    private int mOriginSurfaceWidth = 0;
    private int mOriginPlayerMarginTop = 0;
    private int mOriginPlayerMarginLeft = 0;

    private boolean mDoNotUpdateSeekBar = false;        //用户在拖动seekbar期间,系统不去更改seekbar位置
    private boolean mIsFullScreen;                      //是否在全屏播放标志
    private long mLastFullScreenClickTime;              //双击最大化

    private List<PlayListBean> mPlayListBeans = new ArrayList<PlayListBean>();
    private PlayListAdapter mPlayListAdapter;
    private DragListView mDragPlayListView;
    private int mLastPlayListClickPosition = 0;       //双击直接播放
    private long mLastPlayListClickTime;

    private List<AllMediaListBean> mAllMediaListBeans = new ArrayList<AllMediaListBean>();
    private AllMediaListAdapter mAllMediaListAdapter;
    private ListView mAllMediaListView;
    private int mLastAllMediaListClickPosition = 0;     //双击添加到播放列表功能
    private long mLastAllMediaListClickTime;

    private List<UsbMediaListBean> mUsbMediaListBeans = new ArrayList<UsbMediaListBean>();
    private UsbMediaListAdapter mUsbMediaListAdapter;
    private ListView mUsbMediaListView;
    private int mLastUsbMediaListClickPosition = 0;     //usb media list item的双击功能尚未启用,但是预留code
    private long mLastUsbMediaListClickTime;

    private CheckBox mAllMediaListCheckBox;
    private CheckBox mUsbMediaListCheckBox;

    private TextView mUsbListTitle;
    private Deque<String> mUsbMediaCopyDeque = new ArrayDeque<String>();                        //需要复制的文件
    private Deque<AllMediaListBean> mMediaDeleteDeque = new ArrayDeque<AllMediaListBean>();     //需要删除的文件
    private Deque<String> mMediaSameDeque = new ArrayDeque<String>();                           //复制中重复冲突的文件
    private Deque<String> mCopyDoneDeque = new ArrayDeque<String>();                           //复制完成的文件

    private TextView mUsbCapacityTextView;
    private TextView mLocalCapacityTextView;
    ExecutorService mCapacityUpdateService;          //用于更新容量的线程

    ExecutorService mCopyDoneUpdateService;          //用于复制后的列表

    private Spinner mUsbPartitionSpinner;
    private ArrayAdapter<String> mUsbPartitionAdapter;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
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
    public void onMediaInfoUpdate(String name, String mimeType, int width, int height, long size, int bps) {
        // 在UI显示播放信息
        setMediaInfoUI(name, mimeType, width, height, size, bps);

        for (PlayListBean bean : mPlayListAdapter.getListDatas()) {
            bean.setPlaying(false);
        }

        if (!mPlayer.isPreview()) {
            mPlayListAdapter.getItem(mPlayer.getPlayPosition()).setPlaying(true);
        }

        mPlayListAdapter.refresh();
    }

    @Override
    public void onMediaDurationSet(int mesc) {
        // 设置播放控制条的时间与长度
        mPlayProgressSeekBar.setMax(mesc);
        setMediaDurationTimeUI(mesc);
    }

    @Override
    public void onMediaSeekComplete() {
        Log.i(TAG, "onMediaSeekComplete");
        //如果视频在暂停的时候拖了进度条,则继续播放
        if (mPlayer.getPlayState().equals(MEDIA_PLAY_VIDEO)) {
            mPlayBtn.setBackgroundResource(R.drawable.selector_media_pause_btn);
        }
    }

    @Override
    public void onMediaPlayError() {
        //todo error handler,在播放列表里标记出来
        mPlayBtn.setBackgroundResource(R.drawable.selector_media_play_btn);
    }
    // 媒体播放的回调函数=============结束

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == mReplayAllRadioButton.getId()) {
            if (mPlayer != null)
                mPlayer.setReplayMode(PictureVideoPlayer.MediaReplayMode.MEDIA_REPLAY_ALL);
        } else if (checkedId == mReplayOneRadioButton.getId()) {
            if (mPlayer != null)
                mPlayer.setReplayMode(PictureVideoPlayer.MediaReplayMode.MEDIA_REPLAY_ONE);
        } else if (checkedId == mReplayShuffleRadioButton.getId()) {
            if (mPlayer != null)
                mPlayer.setReplayMode(PictureVideoPlayer.MediaReplayMode.MEDIA_REPLAY_SHUFFLE);
        }
        SharedPreferencesUtil shared = new SharedPreferencesUtil(getApplicationContext(), Contants.CONFIG);
        shared.putInt(CONFIG_PLAYMODE, checkedId);
    }

    @Override
    public void onMediaUpdateSeekBar(int msec) {
        if (!mDoNotUpdateSeekBar) {
            setMediaPlayedTime(msec);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();

        mPicturePlayView.setOnClickListener(this);
        mVideoPlaySurfaceView.setOnClickListener(this);
        mPreviousBtn.setOnClickListener(this);
        mPlayBtn.setOnClickListener(this);
        mNextBtn.setOnClickListener(this);
        mVolumeBtn.setOnClickListener(this);

        mGrayViewBtn.setOnClickListener(this);
        mNetViewBtn.setOnClickListener(this);

        mAllMediaListCheckBox.setOnClickListener(this);
        mUsbMediaListCheckBox.setOnClickListener(this);

        mMediaMultiCopyToLeftBtn.setOnClickListener(this);
        mMediaMultiDeleteBtn.setOnClickListener(this);
        mMediaMultiAddToPlayListBtn.setOnClickListener(this);
        mMediaMultiDownloadBtn.setOnClickListener(this);
        mMediaMultiCopyToRightBtn.setOnClickListener(this);

        mMediaListRefreshBtn.setOnClickListener(this);

        mReplayModeRadioGroup.setOnCheckedChangeListener(this);

        mPicturePlayView.setOnLongClickListener(mLongPressHandle);
        mVideoPlaySurfaceView.setOnLongClickListener(mLongPressHandle);

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

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        mMaskArea = new MaskControl(this, mMaskView, mPaintView);

        mPlayer = new PictureVideoPlayer(this, mVideoPlaySurfaceView, mPicturePlayView, mPlayListBeans);
        mPlayer.setOnMediaEventListener(this);

        mCapacityUpdateService = Executors.newSingleThreadExecutor();
        mCopyDoneUpdateService = Executors.newSingleThreadExecutor();

        // 保存原始的布局位置,最大化后最小化的时候需要用
        mIsFullScreen = false;
        mVideoPlaySurfaceView.getHolder().addCallback(svCallback);
        FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) mVideoPlaySurfaceView.getLayoutParams();
        mOriginSurfaceHeight = flp.height;
        mOriginSurfaceWidth = flp.width;

        LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) mPlayerPanel.getLayoutParams();
        mOriginPlayerMarginTop = llp.topMargin;
        mOriginPlayerMarginLeft = llp.leftMargin;

        mPlayProgressSeekBar.setOnSeekBarChangeListener(mDurationSeekbarChange);
        mMediaVolumeLevelSeekBar.setOnSeekBarChangeListener(mVolumeSeekbarChange);

        //云端本地的全媒体列表设置
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
                if (position == mLastAllMediaListClickPosition
                        && (Math.abs(mLastAllMediaListClickTime - System.currentTimeMillis()) < DOUBLE_TAP_DELAY_MS)) {
                    mLastAllMediaListClickPosition = -1;
                    mLastAllMediaListClickTime = 0;
                    mPlayListAdapter.addItem(new PlayListBean(mAllMediaListBeans.get(position).getMediaData()));
                    mPlayListAdapter.saveToConfig();
                    mPlayListAdapter.refresh();
                } else {
                    Log.d(TAG, "position = " + position + "; id = " + id);
                    mLastAllMediaListClickPosition = position;
                    mLastAllMediaListClickTime = System.currentTimeMillis();
                }
            }
        });

        //播放列表设置
        mPlayListAdapter = new PlayListAdapter(getApplicationContext(), mPlayListBeans);
        mPlayListAdapter.loadFromConfig();
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
                if (position == mLastPlayListClickPosition
                        && (Math.abs(mLastPlayListClickTime - System.currentTimeMillis()) < DOUBLE_TAP_DELAY_MS)) {
                    mLastPlayListClickPosition = -1;
                    mLastPlayListClickTime = 0;
                    Log.i(TAG, "double click play list!");
                    mPlayer.mediaStop();
                    mPlayer.mediaPlay(position);
                } else {
                    mLastPlayListClickPosition = position;
                    mLastPlayListClickTime = System.currentTimeMillis();
                }
            }
        });


        mUsbPartitionAdapter = new ArrayAdapter<String>(this, R.layout.item_usb_partition_spinner, R.id.tv_partition_idx);
        mUsbPartitionSpinner.setAdapter(mUsbPartitionAdapter);

        //Usb设备媒体列表设置
        mUsbMediaListAdapter = new UsbMediaListAdapter(getApplicationContext(), mUsbMediaListBeans);
        mUsbMediaListAdapter.setOnUsbItemCopyListener(new UsbMediaListAdapter.OnUsbItemEventListener() {
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
                if (position == mLastUsbMediaListClickPosition
                        && (Math.abs(mLastUsbMediaListClickTime - System.currentTimeMillis()) < DOUBLE_TAP_DELAY_MS)) {
                    mLastUsbMediaListClickPosition = -1;
                    mLastUsbMediaListClickTime = 0;
                    Log.i(TAG, "double click usb media list!");
                    // 预留了双击的处理,但是目前没有做任何操作
                } else {
                    mLastUsbMediaListClickPosition = position;
                    mLastUsbMediaListClickTime = System.currentTimeMillis();
                }
            }
        });

        //递归扫描sd卡根目录
        mLocalMediaScanner = new MediaScanUtil();
        mLocalMediaScanner.setNeedDuration(true);
        mLocalMediaScanner.setMediaFileScanListener(new MediaScanUtil.MediaFileScanListener() {
            @Override
            public void onMediaScanBegin() {
                Log.i(TAG, "local media scan begin!");
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_LOCAL_MEDIA_LIST_CLEAN;
                mHandler.sendMessage(msg);
            }

            @Override
            public void onFindMedia(int type, String name, String extension, String path, int duration, long size) {
                if (type == MEDIA_PICTURE ||
                        type == MEDIA_VIDEO) {
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
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_LOCAL_MEDIA_DATABASE_UI_SYNC;
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
                if (type == MEDIA_PICTURE ||
                        type == MEDIA_VIDEO) {
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

        // 监听usb插拔事件
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        usbFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        usbFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        usbFilter.addDataScheme("file");
        registerReceiver(usbReceiver, usbFilter);

        // 主动枚举一次usb设备加入usb spinner中
        discoverUsbMountDevice();

        // 开线程去查询本地容量
        updateCapacity(true, LOCAL_MASS_STORAGE_PATH);
    }

    private void initView() {
        setContentView(R.layout.activity_media);

        mVideoPlaySurfaceView = (SurfaceView) findViewById(R.id.sv_media_playVideo);
        mPicturePlayView = (ImageView) findViewById(R.id.iv_media_playPicture);
        mMaskView = (ImageView) findViewById(R.id.iv_media_mask);
        mPaintView = (PaintableImageView) findViewById(R.id.iv_media_paint);

        mNetViewBtn = (Button) findViewById(R.id.btn_media_netView);
        mGrayViewBtn = (Button) findViewById(R.id.btn_media_grayView);

        mPlayBtn = (Button) findViewById(R.id.bt_media_play);
        mPreviousBtn = (Button) findViewById(R.id.bt_media_playPrevious);
        mNextBtn = (Button) findViewById(R.id.bt_media_playNext);
        mVolumeBtn = (Button) findViewById(R.id.bt_media_volume);

        mMediaMultiCopyToLeftBtn = (Button) findViewById(R.id.bt_media_multi_copy_to_left);
        mMediaMultiDeleteBtn = (Button) findViewById(R.id.bt_media_multi_delete);
        mMediaMultiAddToPlayListBtn = (Button) findViewById(R.id.bt_media_multi_add);
        mMediaMultiDownloadBtn = (Button) findViewById(R.id.bt_media_multi_download);
        mMediaMultiCopyToRightBtn = (Button) findViewById(R.id.bt_media_multi_copy_to_right);

        mMediaListRefreshBtn = (Button) findViewById(R.id.bt_media_all_list_refresh);

        mPlayerPanel = (FrameLayout) findViewById(R.id.fl_media_player);
        mPlayProgressSeekBar = (SeekBar) findViewById(R.id.sb_media_playProgress);
        mMediaVolumeLevelSeekBar = (SeekBar) findViewById(R.id.sb_media_volume_level);

        mMediaPlayedTimeTextView = (TextView) findViewById(R.id.tv_media_playedTime);
        mMediaDurationTimeTextView = (TextView) findViewById(R.id.tv_media_durationTime);

        mMediaInfoNameTextView = (TextView) findViewById(R.id.tv_media_play_info_name);
        mMediaInfoTypeTextView = (TextView) findViewById(R.id.tv_media_play_info_type);
        mMediaInfoWidthHeightTextView = (TextView) findViewById(R.id.tv_media_play_info_wh);
        mMediaInfoSizeTextView = (TextView) findViewById(R.id.tv_media_play_info_size);
        mMediaInfoBpsTextView = (TextView) findViewById(R.id.tv_media_play_info_bps);

        mReplayModeRadioGroup = (RadioGroup) findViewById(R.id.rg_media_replay_mode);
        mReplayAllRadioButton = (RadioButton) findViewById(R.id.rb_media_replay_all);
        mReplayOneRadioButton = (RadioButton) findViewById(R.id.rb_media_replay_one);
        mReplayShuffleRadioButton = (RadioButton) findViewById(R.id.rb_media_replay_shuffle);

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

    private void updateMultiActionButtonState()
    {
        if (mAllMediaListAdapter.hasItemSelected()) {
            // 额外需要检查是否存在usb设备
            if (mUsbPartitionSpinner.getSelectedItemPosition()!=AdapterView.INVALID_POSITION) {
                mMediaMultiCopyToRightBtn.setEnabled(true);
            } else {
                mMediaMultiCopyToRightBtn.setEnabled(false);
            }
            mMediaMultiAddToPlayListBtn.setEnabled(true);
            mMediaMultiDeleteBtn.setEnabled(true);
            mMediaMultiDownloadBtn.setEnabled(true);
        } else {
            mMediaMultiCopyToRightBtn.setEnabled(false);
            mMediaMultiAddToPlayListBtn.setEnabled(false);
            mMediaMultiDeleteBtn.setEnabled(false);
            mMediaMultiDownloadBtn.setEnabled(false);
        }
    }

    private void loadPersistData() {
        // 加载播放模式,如果没有配置,则默认为全部循环
        SharedPreferencesUtil config = new SharedPreferencesUtil(getApplicationContext(), Contants.CONFIG);
        int playMode = config.getInt(CONFIG_PLAYMODE, mReplayAllRadioButton.getId());
        mReplayModeRadioGroup.check(playMode);
        onCheckedChanged(null,playMode);

        // 设置播放时长为00:00:00
        setMediaPlayedTime(0);
        setMediaDurationTimeUI(0);

        // 获取系统最大音量
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // 获取设备当前音量
        int currentVolume =mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        // 设置seekbar的最大值
        mMediaVolumeLevelSeekBar.setMax(maxVolume);
        // 显示音量
        mMediaVolumeLevelSeekBar.setProgress(currentVolume);


        mMediaMultiCopyToLeftBtn.setEnabled(mUsbMediaListAdapter.hasItemSelected());

        updateMultiActionButtonState();

        // 初始化本地媒体列表
        if (new MediaBeanDao(MediaActivity.this).selectAll().isEmpty())
        {   // 如果数据库为空,则扫描一次本地媒体文件
            mLocalMediaScanner.safeScanning(LOCAL_MASS_STORAGE_PATH);
        } else {
            // 如果有数据库,则从数据库获取
            for (MediaBean m:new MediaBeanDao(MediaActivity.this).selectAll())
            {
                mAllMediaListBeans.add(new AllMediaListBean(m));
                Log.d(TAG,m.toString());
            }
            mAllMediaListAdapter.refresh();
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_USB_PLUG_IN:
                    String storagePath = msg.getData().getString(BUNDLE_KEY_STORAGE_PATH);
                    discoverUsbMountDevice();
                    break;

                case MSG_USB_PLUG_OUT:
                    storagePath = msg.getData().getString(BUNDLE_KEY_STORAGE_PATH);
                    String currentSelect = "";
                    if (mUsbPartitionSpinner.getSelectedItem()!=null) {
                        currentSelect = mUsbPartitionSpinner.getSelectedItem().toString();
                    }

                    discoverUsbMountDevice();

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
                    mUsbCapacityTextView.setText(getResources().getString(R.string.str_media_wait_capacity));

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
                        mUsbMediaScanner.safeScanning(currentPath + File.separator + USB_DEVICE_DEFAULT_SEARCH_MEDIA_FOLDER);
                    }
                    updateMultiActionButtonState();
                    break;

                case MSG_USB_DEVICE_CAPACITY_SET:
                    mUsbCapacityTextView.setText((String) msg.obj);
                    break;

                case MSG_LOCAL_CAPACITY_SET:
                    mLocalCapacityTextView.setText((String) msg.obj);
                    break;

                case MSG_UPDATE_DURATION_TIME:
                    setMediaDurationTimeUI(msg.arg1);
                    break;

                case MSG_UPDATE_PLAYED_TIME:
                    setMediaPlayedTime(msg.arg1);
                    break;

                case MSG_UPDATE_MEDIA_INFO:
                    Bundle b = msg.getData();
                    String nameInfo = b.getString(BUNDLE_KEY_INFO_NAME);
                    String typeInfo = b.getString(BUNDLE_KEY_INFO_TYPE);
                    int widthInfo = b.getInt(BUNDLE_KEY_INFO_WIDTH);
                    int heightInfo = b.getInt(BUNDLE_KEY_INFO_HEIGHT);
                    long sizeInfo = b.getLong(BUNDLE_KEY_INFO_SIZE);
                    int bpsInfo = b.getInt(BUNDLE_KEY_INFO_BPS);
                    setMediaInfoUI(nameInfo,typeInfo,widthInfo,heightInfo,sizeInfo,bpsInfo);
                    break;

                case MSG_LOCAL_MEDIA_DATABASE_UPDATE:
                    b = msg.getData();
                    int type = b.getInt(BUNDLE_KEY_MEDIA_TYPE);
                    String name = b.getString(BUNDLE_KEY_MEDIA_NAME);
                    String path = b.getString(BUNDLE_KEY_MEDIA_PATH);
                    int duration = b.getInt(BUNDLE_KEY_MEDIA_DURATION);
                    long size = b.getLong(BUNDLE_KEY_MEDIA_SIZE);

                    MediaBean data = new MediaBean(name, type, MediaBean.SOURCE_LOCAL, path, duration, size, true);
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

                case MSG_LOCAL_MEDIA_LIST_CLEAN:
                    mMediaListRefreshBtn.setEnabled(false);
                    mAllMediaListAdapter.clear();
                    mAllMediaListAdapter.refresh();
                    break;

                case MSG_USB_COPY_TO_INTERNAL:
                    if (!mUsbMediaCopyDeque.isEmpty()) {
                        copyUsbFileToInternal();
                    }
                    break;

                case MSG_USB_LIST_SELECTED_CHANGE:
                    if (msg.arg1==1) {
                        mMediaMultiCopyToLeftBtn.setEnabled(true);
                    } else {
                        mMediaMultiCopyToLeftBtn.setEnabled(false);
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
//todo download
                    break;

                case MSG_MEDIA_LIST_ITEM_PREVIEW:
                    MediaBean previewItem = (MediaBean)msg.obj;
                    mPlayer.mediaStop();
                    mPlayer.mediaPreview(previewItem);
                    break;

                case MSG_LOCAL_MEDIA_DATABASE_UI_SYNC:
                    mMediaListRefreshBtn.setEnabled(true);
                    mAllMediaListAdapter.clear();
                    for (MediaBean m:new MediaBeanDao(MediaActivity.this).selectAll())
                    {
                        mAllMediaListAdapter.addItem(new AllMediaListBean(m));
                        Log.d(TAG,m.toString());
                    }
                    mAllMediaListAdapter.refresh();
                    updateMultiActionButtonState();
                    break;

                case MSG_USB_MEDIA_SCAN_DONE:
                    mMediaMultiCopyToLeftBtn.setEnabled(mUsbMediaListAdapter.hasItemSelected());
                    break;

                case MSG_MEDIA_PLAY_COMPLETE:
                    mPlayer.mediaReplay();
                    break;

                case MSG_MEDIA_PLAY_STOP:
                    break;
            }
        }
    };


    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.sv_media_playVideo:
            case R.id.iv_media_playPicture:
                if ((Math.abs(mLastFullScreenClickTime - System.currentTimeMillis()) < DOUBLE_TAP_DELAY_MS)) {
                    mLastFullScreenClickTime = 0;
                    if (!mIsFullScreen) {
                        fullScreenDisplaySwitch();
                    }
                } else {
                    mLastFullScreenClickTime = System.currentTimeMillis();
                }
                break;

            case R.id.bt_media_play:
                if (mPlayer.getPlayState().equals(MEDIA_IDLE)) {
                    mPlayBtn.setBackgroundResource(R.drawable.selector_media_pause_btn);
                    mPlayer.mediaPlay(0);
                } else {
                    if (mPlayer.getPlayState().equals(MEDIA_PAUSE_PICTURE) ||
                            mPlayer.getPlayState().equals(MEDIA_PAUSE_VIDEO)) {
                        mPlayBtn.setBackgroundResource(R.drawable.selector_media_pause_btn);
                    } else {
                        mPlayBtn.setBackgroundResource(R.drawable.selector_media_play_btn);
                    }
                    mPlayer.mediaPauseResume();
                }
                break;

            case R.id.bt_media_playNext:
                mPlayer.mediaPlayNext();
                break;

            case R.id.bt_media_playPrevious:
                mPlayer.mediaPlayPrevious();
                break;

            case R.id.bt_media_volume:
                if (mMediaVolumeLevelSeekBar.getVisibility()==View.INVISIBLE) {
                    mMediaVolumeLevelSeekBar.setVisibility(View.VISIBLE);
                } else {
                    mMediaVolumeLevelSeekBar.setVisibility(View.INVISIBLE);
                }
                break;

            case R.id.btn_media_netView:
                if (mMaskArea.getMaskState()!= MaskControl.SCREEN_MASK_MODE_NET) {
                    mMaskArea.showNet();
                } else {
                    mMaskArea.hide();
                }
                break;

            case R.id.btn_media_grayView:
                if (mMaskArea.getMaskState()!= MaskControl.SCREEN_MASK_MODE_GRAY_1
                        && mMaskArea.getMaskState()!= MaskControl.SCREEN_MASK_MODE_GRAY_2
                        && mMaskArea.getMaskState()!= MaskControl.SCREEN_MASK_MODE_GRAY_3) {
                    mMaskArea.showGray1();
                } else {
                    switch(mMaskArea.getMaskState()) {
                        case MaskControl.SCREEN_MASK_MODE_GRAY_1:
                            mMaskArea.showGray2();
                            break;

                        case MaskControl.SCREEN_MASK_MODE_GRAY_2:
                            mMaskArea.showGray3();
                            break;

                        case MaskControl.SCREEN_MASK_MODE_GRAY_3:
                            mMaskArea.hide();
                            break;

                        default:
                            break;
                    }
                }
                break;

            case R.id.bt_media_all_list_refresh:
                mLocalMediaScanner.safeScanning(LOCAL_MASS_STORAGE_PATH);
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
                mMediaMultiCopyToLeftBtn.setEnabled(mUsbMediaListAdapter.hasItemSelected());
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
                        mPlayListAdapter.addItem(new PlayListBean(data.getMediaData()));
                    }
                }
                mPlayListAdapter.saveToConfig();
                mPlayListAdapter.refresh();
                break;

            case R.id.bt_media_multi_download:
                break;

            case R.id.bt_media_multi_copy_to_right:
                mUsbMediaCopyDeque.clear();
                for (AllMediaListBean data : mAllMediaListAdapter.getListDatas()) {
                    if (data.isSelected()) {
                        mUsbMediaCopyDeque.add(data.getMediaData().getPath());
                    }
                }

                if (!mUsbMediaCopyDeque.isEmpty()) {
                    // 目前不存在点击单个item复制到u盘的情况,所以内部复制到U盘就不使用handler message了
                    copyInternalFileToUsb();
                }
                break;

            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LinearLayout layout = (LinearLayout) findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.img_background);

        // 为保证播放模式可以被设置,加载配置必须在mPlayer被new出以后做
        loadPersistData();
    }

    @Override
    protected void onDestroy() {
        if (mPlayer != null) {
            mPlayer.mediaStop();
            mPlayer.release();
            mPlayer = null;
        }

        if (mCapacityUpdateService != null) {
            mCapacityUpdateService.shutdown();
            mCapacityUpdateService = null;
        }

        if (mCopyDoneUpdateService != null) {
            mCopyDoneUpdateService.shutdown();
            mCopyDoneUpdateService = null;
        }

        mLocalMediaScanner.release();
        mUsbMediaScanner.release();

        unregisterReceiver(usbReceiver);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mIsFullScreen) {
            new AlertDialog.Builder(this).setIcon(R.drawable.img_media_warning)
                    .setTitle(getResources().getString(R.string.str_media_full_screen_quite))
                    .setPositiveButton(getResources().getString(R.string.str_media_dialog_button_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            fullScreenDisplaySwitch();
                        }
                    }).setNegativeButton(getResources().getString(R.string.str_media_dialog_button_cancel), new DialogInterface.OnClickListener() {
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

    private SurfaceHolder.Callback svCallback = new SurfaceHolder.Callback() {
        // SurfaceHolder被修改的时候回调
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i(TAG, "SurfaceHolder 被销毁");
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, "SurfaceHolder 被创建");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            Log.i(TAG, "SurfaceHolder 大小被改变");
        }

    };

    private SeekBar.OnSeekBarChangeListener mDurationSeekbarChange = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // 当进度条停止修改的时候触发
            int progress = seekBar.getProgress();
            mPlayer.seekTo(progress);
            mDoNotUpdateSeekBar = false; //用户的拖动停止了,允许更新seekBar
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                setMediaPlayedTime(progress);
                mDoNotUpdateSeekBar = true; //在拖动的时候,需要防止播放开的线程来改变seekBar的位置
            }
        }
    };

    private View.OnLongClickListener mLongPressHandle = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (mIsFullScreen) {
                Toast.makeText(MediaActivity.this, "长按成功", Toast.LENGTH_SHORT).show();
                mMaskArea.showPaintWindow();
                LinearLayout maskControlBarLayout = (LinearLayout) findViewById(R.id.ll_media_mask_paint_controlBar);
                maskControlBarLayout.setVisibility(View.VISIBLE);
                maskControlBarLayout.bringToFront();
            }

            return true;
        }
    };

    private SeekBar.OnSeekBarChangeListener mVolumeSeekbarChange = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }
        }
    };

    private void setMediaInfoUI(String name,String mimeType, int width, int height, long size, int bps) {
        if (Looper.myLooper() == Looper.getMainLooper()) { // UI主线程
            mMediaInfoNameTextView.setText(name);
            mMediaInfoTypeTextView.setText(mimeType);
            mMediaInfoWidthHeightTextView.setText(width + "*" + height);
            mMediaInfoSizeTextView.setText(FileUtil.formatFileSize(size));
            if (bps>0) {
                mMediaInfoBpsTextView.setText(FileUtil.formatFileSize(bps) + " bit/sec");
            } else {
                mMediaInfoBpsTextView.setText("");
            }
        } else { // 非UI主线程
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_UPDATE_MEDIA_INFO;
            Bundle b = new Bundle();
            b.putString(BUNDLE_KEY_INFO_NAME, name);
            b.putString(BUNDLE_KEY_INFO_TYPE, mimeType);
            b.putInt(BUNDLE_KEY_INFO_WIDTH, width);
            b.putInt(BUNDLE_KEY_INFO_HEIGHT, height);
            b.putLong(BUNDLE_KEY_INFO_SIZE, size);
            b.putInt(BUNDLE_KEY_INFO_BPS, bps);
            msg.setData(b);
            mHandler.sendMessage(msg);
        }
    }

    //只能在主线程调用
    private void setTimeTextView(TextView tv, int milliseconds) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(milliseconds);
        tv.setText(hms);
    }

    private void setMediaDurationTimeUI(int milliseconds) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // UI主线程
            setTimeTextView(mMediaDurationTimeTextView, milliseconds);
            mPlayProgressSeekBar.setMax(milliseconds);
        } else { // 非UI主线程
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_UPDATE_DURATION_TIME;
            msg.arg1 = milliseconds;
            mHandler.sendMessage(msg);
        }
    }

    private void setMediaPlayedTime(int milliseconds) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // UI主线程
            mPlayProgressSeekBar.setProgress(milliseconds);
            setTimeTextView(mMediaPlayedTimeTextView, milliseconds);
        } else {
            // 非UI主线程
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_UPDATE_PLAYED_TIME;
            msg.arg1 = milliseconds;
            mHandler.sendMessage(msg);
        }
    }

    private void setUsbDeviceCapacity(String text) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // UI主线程
            mUsbCapacityTextView.setText(text);
        } else {
            // 非UI主线程
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_USB_DEVICE_CAPACITY_SET;
            msg.obj = text;
            mHandler.sendMessage(msg);
        }
    }

    private void setLocalCapacity(String text) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // UI主线程
            mLocalCapacityTextView.setText(text);
        } else {
            // 非UI主线程
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_LOCAL_CAPACITY_SET;
            msg.obj = text;
            mHandler.sendMessage(msg);
        }
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
                                setLocalCapacity(fsUsed + "/" + fsCapacity);
                            } else {
                                setUsbDeviceCapacity(fsUsed + "/" + fsCapacity);
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
                                b.putString(BUNDLE_KEY_MEDIA_NAME, FileUtil.getFilePrefix(path));
                                b.putInt(BUNDLE_KEY_MEDIA_TYPE, getMediaTypeFromPath(path));
                                b.putInt(BUNDLE_KEY_MEDIA_DURATION, mLocalMediaScanner.getMediaDuration(path));
                                b.putString(BUNDLE_KEY_MEDIA_PATH, path);
                                b.putLong(BUNDLE_KEY_MEDIA_SIZE, FileUtil.getFileSize(path));
                                msg.setData(b);
                                mHandler.sendMessage(msg);

                            }

                            // 让UI与数据库 sync一次
                            Message msg = mHandler.obtainMessage();
                            msg.what = MSG_LOCAL_MEDIA_DATABASE_UI_SYNC;
                            mHandler.sendMessage(msg);

                        } catch (Exception e) {
                            Log.e(TAG, "error in mCopyDoneUpdateService!" + e);
                        }
                    }
                });
    }

    private void fullScreenDisplaySwitch() {
        if (!mIsFullScreen) {
            LinearLayout controlBarLayout = (LinearLayout) findViewById(R.id.ll_media_playControlBar);
            controlBarLayout.setVisibility(View.INVISIBLE);
            mGrayViewBtn.setVisibility(View.INVISIBLE);
            mNetViewBtn.setVisibility(View.INVISIBLE);
            mMediaVolumeLevelSeekBar.setVisibility(View.INVISIBLE);
            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) mVideoPlaySurfaceView.getLayoutParams();
            flp.width = getResources().getDimensionPixelSize(R.dimen.x1920);
            flp.height = getResources().getDimensionPixelSize(R.dimen.x1080);
            mVideoPlaySurfaceView.setLayoutParams(flp);
            mPicturePlayView.setLayoutParams(flp);
            mMaskView.setLayoutParams(flp);
            mPaintView.setLayoutParams(flp);

            LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) mPlayerPanel.getLayoutParams();
            llp.topMargin = 0;
            llp.leftMargin = 0;
            mPlayerPanel.setLayoutParams(llp);

            mIsFullScreen = true;
        } else {
            LinearLayout controlBarLayout = (LinearLayout) findViewById(R.id.ll_media_playControlBar);
            controlBarLayout.setVisibility(View.VISIBLE);

            LinearLayout maskControlBarLayout = (LinearLayout) findViewById(R.id.ll_media_mask_paint_controlBar);
            maskControlBarLayout.setVisibility(View.INVISIBLE);

            mGrayViewBtn.setVisibility(View.VISIBLE);
            mNetViewBtn.setVisibility(View.VISIBLE);

            LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) mPlayerPanel.getLayoutParams();
            llp.topMargin = mOriginPlayerMarginTop;
            llp.leftMargin = mOriginPlayerMarginLeft;
            mPlayerPanel.setLayoutParams(llp);

            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) mVideoPlaySurfaceView.getLayoutParams();
            flp.width = mOriginSurfaceWidth;
            flp.height = mOriginSurfaceHeight;
            mVideoPlaySurfaceView.setLayoutParams(flp);
            mPicturePlayView.setLayoutParams(flp);
            mMaskView.setLayoutParams(flp);
            mPaintView.setLayoutParams(flp);

            if (mMaskArea.getMaskState()==MaskControl.SCREEN_MASK_MODE_WINDOW) {
                mMaskArea.hide();
            }

            mIsFullScreen = false;
        }
    }

    private void discoverUsbMountDevice() {
        mUsbPartitionAdapter.clear();
        String[] mountList = FileUtil.getMountVolumePaths(this);
        for (String s : mountList) {
            if (!FileUtil.contains(mMountExceptList, s)) {
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
                long total = 0;

                Log.i(TAG, "Copy file(s) total length: " + param.totalSize);

                mCopyDoneDeque.clear();
                byte[] bytes = new byte[USB_COPY_BUFFER_SIZE];

                while (!param.fromList.isEmpty()) {
                    String sourcePath = param.fromList.pop();
                    File fromFile = new File(sourcePath);

                    String toPath = pathGenerate(param.direct,sourcePath);
                    if (toPath.isEmpty()) {
                        throw new IOException();
                    }
                    File toFile = FileUtil.createFile(toPath);

                    OutputStream out = new BufferedOutputStream(new FileOutputStream(toFile));
                    InputStream input = new BufferedInputStream(new FileInputStream(fromFile));

                    int count;
                    while ((count = input.read(bytes)) != -1) {
                        out.write(bytes, 0, count);
                        total += count;
                        int progress = (int) total;
                        if (param.totalSize > Integer.MAX_VALUE) {
                            progress = (int) (total / 1024);
                        }
                        publishProgress(progress);
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
            int max = (int) param.totalSize;
            if (param.totalSize > Integer.MAX_VALUE) {
                max = (int) (param.totalSize) / 1024;
            }

            dialog.setMax(max);
            dialog.setProgress(values[0]);
        }
    }

    private String pathGenerate(int direct, String path) {
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

            // 类似于 /mnt/sda/sda1 + / + media + / + export
            basePath = mUsbPartitionSpinner.getSelectedItem().toString()
                    + File.separator + USB_DEVICE_DEFAULT_SEARCH_MEDIA_FOLDER
                    + File.separator + COPY_TO_USB_MEDIA_EXPORT_FOLDER;
        }

        result = basePath + File.separator + FileUtil.getFilePrefix(path) + "." + FileUtil.getFileExtension(path);
        Log.d(TAG, "pathGenerate:" + result);
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
            if (new MediaBeanDao(MediaActivity.this).queryById(pathGenerate(CopyTaskParam.DIRECT_USB_TO_INTERNAL,path))!=null)
            {
                Log.i(TAG,"found same media file! " + path + ":" + pathGenerate(CopyTaskParam.DIRECT_USB_TO_INTERNAL,path));
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
                if (bean.getPath().equals(pathGenerate(CopyTaskParam.DIRECT_INTERNAL_TO_USB, path)))
                {
                    Log.i(TAG,"found same media file! " + path + ":" + pathGenerate(CopyTaskParam.DIRECT_INTERNAL_TO_USB, path));
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

    private void CopyUserChoose(final CopyTaskParam param )
    {
        new AlertDialog.Builder(this).setIcon(R.drawable.img_media_warning)
                .setTitle(getResources().getString(R.string.str_media_file_copy_same_dialog_title))
                .setMessage(mMediaSameDeque.toString())
                .setNegativeButton(getResources().getString(R.string.str_media_dialog_copy_continue), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (param.count > 0) {
                            new CopyTask().execute(param);
                        }
                    }
                })
                .setPositiveButton(getResources().getString(R.string.str_media_dialog_copy_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 什么也不做
                    }
                })
                .setNeutralButton(getResources().getString(R.string.str_media_dialog_copy_skip), new DialogInterface.OnClickListener() {
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
                .setTitle(getResources().getString(R.string.str_media_file_delete_dialog_title))
                .setPositiveButton(getResources().getString(R.string.str_media_dialog_button_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int deleteCount = 0;
                        while (!mMediaDeleteDeque.isEmpty()) {
                            AllMediaListBean data = mMediaDeleteDeque.pop();

                            // 从播放列表删除
                            for (Iterator<PlayListBean> it = mPlayListAdapter.getListDatas().iterator();it.hasNext();)
                            {
                                PlayListBean playItem = it.next();
                                // 删除的文件在播放列表中
                                if (playItem.getMediaData().getPath().equals(data.getMediaData().getPath()))
                                {
                                    // 播放列表处于正在播放的状态
                                    if (playItem.isPlaying())
                                    {
                                        // 需要删除的是最后一个元素
                                        if (mPlayListAdapter.getCount()<=1)
                                        {
                                            mPlayer.mediaStop();
                                        } else {
                                            mPlayer.mediaPlayNext();
                                        }
                                    }
                                    it.remove();
                                }
                            }
                            mPlayListAdapter.saveToConfig();
                            mPlayListAdapter.refresh();

                            //从磁盘删除
                            FileUtil.deleteFile(data.getMediaData().getPath());
                            // 从列表显示中删除
                            mAllMediaListAdapter.removeItem(data);
                            // 从数据库删除
                            new MediaBeanDao(MediaActivity.this).delete(data.getMediaData());

                            deleteCount++;

                        }

                        updateCapacity(true, LOCAL_MASS_STORAGE_PATH);
                        mAllMediaListAdapter.refresh();
                        ToastUtil.showToast(MediaActivity.this, getResources().getString(R.string.str_media_file_delete_toast) + deleteCount);
                    }
                })
                .setNegativeButton(getResources().getString(R.string.str_media_dialog_button_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 什么也不做
                    }
                }).show();
    }
}

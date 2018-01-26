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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.home.HomeActivity;
import com.whiteskycn.tv.projectorlauncher.media.adapter.AllMediaListAdapter;
import com.whiteskycn.tv.projectorlauncher.media.adapter.PlayListAdapter;
import com.whiteskycn.tv.projectorlauncher.media.adapter.UsbMediaListAdapter;
import com.whiteskycn.tv.projectorlauncher.media.bean.MediaFileBean;
import com.whiteskycn.tv.projectorlauncher.media.bean.AllMediaListBean;
import com.whiteskycn.tv.projectorlauncher.media.bean.PlayListBean;
import com.whiteskycn.tv.projectorlauncher.media.bean.UsbMediaListBean;
import com.whiteskycn.tv.projectorlauncher.utils.FileUtil;
import com.whiteskycn.tv.projectorlauncher.utils.MediaScanUtil;
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
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.widget.AdapterView.INVALID_POSITION;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_IDLE;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PAUSE_PICTURE;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PAUSE_VIDEO;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PLAY_VIDEO;

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
    private final int MSG_UPDATE_USB_DEVICE_CAPACITY = 3;

    private final int MSG_UPDATE_LOCAL_MEDIA_LIST = 10;
    private final int MSG_CLEAN_LOCAL_MEDIA_LIST = 11;
    private final int MSG_UPDATE_USB_MEDIA_LIST = 12;
    private final int MSG_CLEAN_USB_MEDIA_LIST = 13;

    private final int MSG_LOCAL_MEDIA_SCAN_DONE = 20;
    private final int MSG_USB_MEDIA_SCAN_DONE = 21;

    private final int MSG_USB_COPY_TO_INTERNAL = 40;
    private final int MSG_DELETE_MEDIA_LIST_ITEM = 41;

    // 媒体播放事件
    private final int MSG_MEDIA_PLAY_COMPLETE = 100;
    private final int MSG_UPDATE_PLAYED_TIME = 101;
    private final int MSG_UPDATE_DURATION_TIME = 102;

    private static final String USB_DEFAULT_MEDIA_PATH = "media";
    private static final String USB_EXPORT_MEDIA_PATH = "exportMedia";
    private static final String INTERNAL_COPY_MEDIA_PATH = "importMedia";
    private String[] mMountExceptList = new String[]{"/mnt/sdcard","/storage/emulated/0"}; // 排除在外的挂载目录,非移动硬盘

    private final int DOUBLE_TAP_DELAY_MS = 800;
    private final int USB_COPY_BUFFER_SIZE = 1024*10;

    private MediaScanUtil mLocalMediaScanner;
    private MediaScanUtil mUsbMediaScanner;

    private PictureVideoPlayer mPlayer;

    private SurfaceView mVideoPlaySurfaceView;
    private ImageView mPicturePlayView;

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

    private FrameLayout mPlayerPanel;

    private SeekBar mPlayProgressSeekBar;
    private TextView mMediaPlayedTimeTextView;
    private TextView mMediaDurationTimeTextView;

    private RadioGroup mReplayModeRadioGroup;
    private RadioButton mReplayAllRadioButton;
    private RadioButton mReplayOneRadioButton;
    private RadioButton mReplayShuffleRadioButton;

    private int mOriginSurfaceHeight = 0;
    private int mOriginSurfaceWidth = 0;
    private boolean mDontUpdateSeekBar = false;
    private boolean mIsFullScreen;                      //是否在全屏播放标志
    private long mLastFullScreenClickTime;              //双击最大化功能

    private List<PlayListBean> mPlayListBeans = new ArrayList<PlayListBean>();
    private PlayListAdapter mPlayListAdapter;
    private DragListView mDragPlayListView;

    private List<AllMediaListBean> mAllMediaListBeans = new ArrayList<AllMediaListBean>();
    private AllMediaListAdapter mAllMediaListAdapter;
    private ListView mAllMediaListView;
    private int mLastAllMediaListClickPosition = 0;     //双击添加到播放列表功能
    private long mLastAllMediaListClickTime;

    private List<UsbMediaListBean> mUsbMediaListBeans = new ArrayList<UsbMediaListBean>();
    private UsbMediaListAdapter mUsbMediaListAdapter;
    private ListView mUsbMediaListView;
    private int mLastUsbMediaListClickPosition = 0;     //双击预留
    private long mLastUsbMediaListClickTime;

    private TextView mUsbListTitle;
    private Deque<String> mUsbMediaCopyDeque = new ArrayDeque<String>(); //将需要复制的文件的路径加入队列中
    private Deque<AllMediaListBean> mMediaDeleteDeque = new ArrayDeque<AllMediaListBean>();  //将需要删除的文件加入队列中
    ExecutorService mUsbCapacityUpdateService;          //专门用于更新U盘容量的线程

    private TextView mUsbCapacityTextView;
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
                bundle.putString("storagePath",intent.getData().getPath());
                msg.setData(bundle);
                mHandler.sendMessageDelayed(msg, 500);
                ToastUtil.showToast(context, "mount! path = "+intent.getData().getPath());

            } else if (action.equals(Intent.ACTION_MEDIA_REMOVED) || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                mHandler.removeMessages(MSG_USB_PLUG_OUT);
                Message msg = new Message();
                msg.what = MSG_USB_PLUG_OUT;
                Bundle bundle = new Bundle();
                bundle.putString("storagePath",intent.getData().getPath());
                msg.setData(bundle);
                mHandler.sendMessageDelayed(msg, 500);
                ToastUtil.showToast(context, "umount! path = "+intent.getData().getPath());
            }
        }
    };

    @Override
    public void onMediaCompletion() {
        // 播放完毕
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_MEDIA_PLAY_COMPLETE;
        mHandler.sendMessage(msg);
    }

    @Override
    public void onMediaDurationSet(int mesc) {
        // 设置播放控制条的时间与长度
        mPlayProgressSeekBar.setMax(mesc);
        setMediaDurationTime(mesc);
    }

    @Override
    public void onMediaSeekComplete() {
        Log.i(TAG, "onMediaSeekComplete");
        //如果视频在暂停的时候拖了进度条,则继续播放
        if (mPlayer.getPlayState().equals(MEDIA_PLAY_VIDEO)) {
            mPlayBtn.setBackgroundResource(R.drawable.img_media_pause);
        }
    }

    @Override
    public void onMediaPlayError() {
        //todo error handler
        showInfo();
    }

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
        //todo 储存状态到preference
    }

    @Override
    public void onMediaUpdateSeekBar(int msec) {
        if (!mDontUpdateSeekBar) {
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
        mMediaMultiCopyToLeftBtn.setOnClickListener(this);
        mMediaMultiDeleteBtn.setOnClickListener(this);
        mMediaMultiAddToPlayListBtn.setOnClickListener(this);
        mMediaMultiDownloadBtn.setOnClickListener(this);
        mMediaMultiCopyToRightBtn.setOnClickListener(this);
        mReplayModeRadioGroup.setOnCheckedChangeListener(this);
        //todo 从preference获取
        mReplayModeRadioGroup.check(mReplayAllRadioButton.getId());

        mUsbPartitionSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                // spinner由空到有第一个会跳过来选中一次
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_USB_PARTITION_SWITCH;
                msg.arg1 = position;
                mHandler.sendMessage(msg);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mPlayer = new PictureVideoPlayer(this, mVideoPlaySurfaceView, mPicturePlayView, mPlayListBeans);
        mPlayer.setOnMediaEventListener(this);

        mUsbCapacityUpdateService = Executors.newSingleThreadExecutor();

        mIsFullScreen = false;
        mVideoPlaySurfaceView.getHolder().addCallback(svCallback);
        ViewGroup.LayoutParams lp = mVideoPlaySurfaceView.getLayoutParams();
        mOriginSurfaceHeight = lp.height;
        mOriginSurfaceWidth = lp.width;

        mPlayProgressSeekBar.setOnSeekBarChangeListener(mSeekbarChange);

        //云端本地的全媒体列表设置
        mAllMediaListAdapter = new AllMediaListAdapter(getApplicationContext(), mAllMediaListBeans);
        mAllMediaListAdapter.setOnALlMediaListItemListener(new AllMediaListAdapter.OnAllMediaListItemEventListener() {
            @Override
            public void doItemDelete(int position) {
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_DELETE_MEDIA_LIST_ITEM;
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
                    mPlayListBeans.add(new PlayListBean(mAllMediaListBeans.get(position).getMediaData()));
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
        mDragPlayListView.setAdapter(mPlayListAdapter);
        mDragPlayListView.setDragItemListener(new DragListView.SimpleAnimationDragItemListener() {
            private Rect mFrame = new Rect();
            private boolean mIsSelected;

            @Override
            public boolean canDrag(View dragView, int x, int y) {
                // 获取可拖拽的图标
                View dragger = dragView.findViewById(R.id.iv_media_spinner);
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
                View drag = dragView.findViewById(R.id.iv_media_spinner);
                dragView.setSelected(true);
                if (drag != null) {
                    drag.setSelected(true);
                }
            }

            @Override
            public Bitmap afterDrawingCache(View dragView, Bitmap bitmap) {
                dragView.setSelected(mIsSelected);
                View drag = dragView.findViewById(R.id.iv_media_spinner);
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

        mUsbPartitionAdapter = new ArrayAdapter<String>(this, R.layout.item_usb_partition_spinner, R.id.tv_partition_idx);
        mUsbPartitionSpinner.setAdapter(mUsbPartitionAdapter);

        //Usb设备媒体列表设置
        mUsbMediaListAdapter = new UsbMediaListAdapter(getApplicationContext(), mUsbMediaListBeans);
        mUsbMediaListAdapter.setOnUsbItemCopyListener(new UsbMediaListAdapter.OnUsbItemCopyListener() {
            @Override
            public void doItemCopy(int position) {
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_USB_COPY_TO_INTERNAL;
                mUsbMediaCopyDeque.clear();
                mUsbMediaCopyDeque.push(mUsbMediaListAdapter.getItem(position).getPath());
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
                    // todo 预留了双击的处理,但是目前没有做任何操作
                } else {
                    Log.d(TAG, "position = " + position + "; id = " + id);
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
                msg.what = MSG_CLEAN_LOCAL_MEDIA_LIST;
                mHandler.sendMessage(msg);
            }

            @Override
            public void onFindMedia(MediaScanUtil.MediaTypeEnum type, String name, String extension, String path, int duration, long size) {
                if (type.equals(MediaScanUtil.MediaTypeEnum.PICTURE) ||
                        type.equals(MediaScanUtil.MediaTypeEnum.VIDEO)) {
                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_UPDATE_LOCAL_MEDIA_LIST;
                    Bundle b = new Bundle();
                    b.putInt("type", type.ordinal());
                    b.putInt("duration", duration);
                    b.putString("name", name);
                    b.putString("path", path);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                }
            }

            @Override
            public void onMediaScanDone() {
                Log.i(TAG, "local media scan done!");
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_LOCAL_MEDIA_SCAN_DONE;
                mHandler.sendMessage(msg);
            }
        });

        //递归扫描sd卡根目录
        mUsbMediaScanner = new MediaScanUtil();
        mUsbMediaScanner.setNeedSize(true);
        mUsbMediaScanner.setMediaFileScanListener(new MediaScanUtil.MediaFileScanListener() {
            @Override
            public void onMediaScanBegin() {
                Log.i(TAG, "usb media scan begin!");
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_CLEAN_USB_MEDIA_LIST;
                mHandler.sendMessage(msg);
            }

            @Override
            public void onFindMedia(MediaScanUtil.MediaTypeEnum type, String name, String extension, String path, int duration, long size) {
                if (type.equals(MediaScanUtil.MediaTypeEnum.PICTURE) ||
                        type.equals(MediaScanUtil.MediaTypeEnum.VIDEO)) {
                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_UPDATE_USB_MEDIA_LIST;
                    Bundle b = new Bundle();
                    b.putInt("type", type.ordinal());
                    b.putString("name", name);
                    b.putString("path", path);
                    b.putLong("size", size);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                }
            }

            @Override
            public void onMediaScanDone() {
                Log.i(TAG, "media scan done!");
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

        // 扫描一次本地媒体文件
        // todo 是否使用数据库储存
        mLocalMediaScanner.safeScanning("/mnt/sdcard");
    }

    private void initView() {
        setContentView(R.layout.activity_media);

        mVideoPlaySurfaceView = (SurfaceView) findViewById(R.id.sv_media_playVideo);
        mPicturePlayView = (ImageView) findViewById(R.id.iv_media_playPicture);

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

        mPlayerPanel = (FrameLayout) findViewById(R.id.fl_media_player);
        mPlayProgressSeekBar = (SeekBar) findViewById(R.id.sb_media_playProgress);

        mMediaPlayedTimeTextView = (TextView) findViewById(R.id.tv_media_playedTime);
        mMediaDurationTimeTextView = (TextView) findViewById(R.id.tv_media_durationTime);

        mReplayModeRadioGroup = (RadioGroup) findViewById(R.id.rg_media_replay_mode);
        mReplayAllRadioButton = (RadioButton) findViewById(R.id.rb_media_replay_all);
        mReplayOneRadioButton = (RadioButton) findViewById(R.id.rb_media_replay_one);
        mReplayShuffleRadioButton = (RadioButton) findViewById(R.id.rb_media_replay_shuffle);

        mAllMediaListView = (ListView) findViewById(R.id.lv_media_all_list);
        mUsbMediaListView = (ListView) findViewById(R.id.lv_media_usb_list);
        mDragPlayListView = (DragListView) findViewById(R.id.lv_media_playList);

        mUsbCapacityTextView = (TextView) findViewById(R.id.tv_media_usb_list_capacity);
        mUsbPartitionSpinner = (Spinner) findViewById(R.id.sp_media_usb_partition_spinner);

        mUsbListTitle = (TextView) findViewById(R.id.tv_media_usb_list_name);

        //设置播放时长为00:00:00
        setMediaPlayedTime(0);
        setMediaDurationTime(0);
    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_USB_PLUG_IN:
                    String storagePath = msg.getData().getString("storagePath");
                    //先remove后add,防止插入两个一样的
                    mUsbPartitionAdapter.remove(storagePath);
                    mUsbPartitionAdapter.add(storagePath);
                    mUsbPartitionAdapter.notifyDataSetChanged();
                    //如果spinner中的内容从无到有,spinner会自己调一次onItemSelect
                    // 回调中我们send MSG_USB_PARTITION_SWITCH
                    break;

                case MSG_USB_PLUG_OUT:
                    storagePath = msg.getData().getString("storagePath");
                    mUsbPartitionAdapter.remove(storagePath);
                    mUsbPartitionAdapter.notifyDataSetChanged();

                    if (mUsbPartitionAdapter.getCount()>0) {
                        Message newMsg = mHandler.obtainMessage();
                        newMsg.what = MSG_USB_PARTITION_SWITCH;
                        newMsg.arg1 = mUsbPartitionSpinner.getSelectedItemPosition();
                        mHandler.sendMessage(newMsg);
                    } else {
                        Message newMsg = mHandler.obtainMessage();
                        newMsg.what = MSG_USB_PARTITION_SWITCH;
                        newMsg.arg1 = -1;
                        mHandler.sendMessage(newMsg);
                    }
                    break;

                case MSG_USB_PARTITION_SWITCH:
                    // 首先将容量信息消除,等待线程查询成功后再发消息更新
                    mUsbCapacityTextView.setText("--GB" + "/" + "--GB");

                    if (msg.arg1 < mUsbPartitionAdapter.getCount() && msg.arg1 >= 0) {

                        final String currentPath = mUsbPartitionAdapter.getItem(msg.arg1);
                        mUsbListTitle.setText(currentPath);

                        // 开线程去查询容量
                        mUsbCapacityUpdateService.execute(
                            new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        String fsUsed = FileUtil.covertFormatFileSize(FileUtil.getTotalCapacity(currentPath) -
                                                FileUtil.getAvailableCapacity(currentPath));
                                        String fsCapacity = FileUtil.covertFormatFileSize(FileUtil.getTotalCapacity(currentPath));
                                        updateUsbDeviceCapacity(fsUsed + "/" + fsCapacity);
                                    } catch (Exception e)
                                    {
                                        Log.e(TAG,"mUsbCapacityUpdateService error!" + e);
                                    }
                                }
                            });

                        // 由扫描来更新usb media列表
                        mUsbMediaScanner.safeScanning(currentPath + File.separator + USB_DEFAULT_MEDIA_PATH);

                    } else if (msg.arg1==-1) {

                        mUsbListTitle.setText("无USB设备");
                        mUsbMediaListAdapter.clear();
                        mUsbMediaListAdapter.refresh();

                    }
                    break;

                case MSG_UPDATE_USB_DEVICE_CAPACITY:
                    mUsbCapacityTextView.setText((String)msg.obj);
                break;

                case MSG_UPDATE_DURATION_TIME:
                    mPlayProgressSeekBar.setMax(msg.arg1);
                    setTimeTextView(mMediaDurationTimeTextView, msg.arg1);
                    break;

                case MSG_UPDATE_PLAYED_TIME:
                    mPlayProgressSeekBar.setProgress(msg.arg1);
                    setTimeTextView(mMediaPlayedTimeTextView, msg.arg1);
                    break;

                case MSG_UPDATE_LOCAL_MEDIA_LIST:
                    Bundle b = msg.getData();
                    int type = b.getInt("type");
                    int duration = b.getInt("duration");
                    String name = b.getString("name");
                    String path = b.getString("path");

                    MediaFileBean.MediaTypeEnum mediaType = MediaFileBean.MediaTypeEnum.UNKNOWN;
                    if (type == MediaScanUtil.MediaTypeEnum.VIDEO.ordinal()) {
                        mediaType = MediaFileBean.MediaTypeEnum.VIDEO;
                    } else if (type == MediaScanUtil.MediaTypeEnum.PICTURE.ordinal()) {
                        mediaType = MediaFileBean.MediaTypeEnum.PICTURE;
                    }

                    mAllMediaListBeans.add(new AllMediaListBean(new MediaFileBean("12345", name, mediaType, MediaFileBean.MediaSourceEnum.LOCAL, true, path, duration)));
                    mAllMediaListAdapter.refresh();
                    break;

                case MSG_UPDATE_USB_MEDIA_LIST:
                    b = msg.getData();
                    type = b.getInt("type");
                    long size = b.getLong("size");
                    name = b.getString("name");
                    path = b.getString("path");

                    UsbMediaListBean UsbMedia;
                    if (type == MediaScanUtil.MediaTypeEnum.VIDEO.ordinal()) {
                        UsbMedia = new UsbMediaListBean(MediaScanUtil.MediaTypeEnum.VIDEO,name,path,size);
                        mUsbMediaListAdapter.addItem(UsbMedia);
                    } else if (type == MediaScanUtil.MediaTypeEnum.PICTURE.ordinal()) {
                        UsbMedia = new UsbMediaListBean(MediaScanUtil.MediaTypeEnum.PICTURE,name,path,size);
                        mUsbMediaListAdapter.addItem(UsbMedia);
                    }
                    mUsbMediaListAdapter.refresh();
                    break;

                case MSG_CLEAN_USB_MEDIA_LIST:
                    mUsbMediaListAdapter.clear();
                    mUsbMediaListAdapter.refresh();
                    break;

                case MSG_CLEAN_LOCAL_MEDIA_LIST:
                    mAllMediaListAdapter.clear();
                    mAllMediaListAdapter.refresh();
                    break;

                case MSG_USB_COPY_TO_INTERNAL:
                    copyUsbFileToInternal();
                    break;

                case MSG_DELETE_MEDIA_LIST_ITEM:
                    deleteInternalMediaFile();
                    break;

                case MSG_LOCAL_MEDIA_SCAN_DONE:
                    Log.i(TAG, "media scan done in handler!");
                    break;

                case MSG_USB_MEDIA_SCAN_DONE:
                    Log.i(TAG, "usb media scan done in handler!");
                    break;

                case MSG_MEDIA_PLAY_COMPLETE:
                    mPlayer.mediaReplay();
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
                    fullScreenDisplaySwitch();
                } else {
                    mLastFullScreenClickTime = System.currentTimeMillis();
                }
                break;

            case R.id.bt_media_play:
                if (mPlayer.getPlayState().equals(MEDIA_IDLE)) {
                    mPlayer.mediaPlay(0);
                    mPlayBtn.setBackgroundResource(R.drawable.img_media_pause);
                } else {
                    if (mPlayer.getPlayState().equals(MEDIA_PAUSE_PICTURE) ||
                            mPlayer.getPlayState().equals(MEDIA_PAUSE_VIDEO)) {
                        mPlayBtn.setBackgroundResource(R.drawable.img_media_pause);
                    } else {
                        mPlayBtn.setBackgroundResource(R.drawable.img_media_play);
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
                // todo
                break;

            case R.id.bt_media_multi_copy_to_left:
                mUsbMediaCopyDeque.clear();
                for (UsbMediaListBean data: mUsbMediaListAdapter.getListDatas())
                {
                    if (data.isSelected())
                    {
                        mUsbMediaCopyDeque.add(data.getPath());
                    }
                }

                ToastUtil.showToast(this,"need copy "+mUsbMediaCopyDeque.size() + " file(s)");

                if (mUsbMediaCopyDeque.size()>0)
                {
                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_USB_COPY_TO_INTERNAL;
                    mHandler.sendMessage(msg);
                }
                break;

            case R.id.bt_media_multi_delete:
                for(AllMediaListBean data:mAllMediaListBeans)
                {// todo select 有问题!!!!
                    mMediaDeleteDeque.clear();
                    if(data.isSelected()) {
                        mMediaDeleteDeque.push(data);
                        // todo 删除播放列表中的数据
                    }
                }
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_DELETE_MEDIA_LIST_ITEM;
                mHandler.sendMessage(msg);
                break;

            case R.id.bt_media_multi_add:
                for(AllMediaListBean data:mAllMediaListBeans)
                {
                    if(data.isSelected()) {
                        mPlayListAdapter.addItem(new PlayListBean(data.getMediaData()));
                    }
                }
                mPlayListAdapter.refresh();
                break;

            case R.id.bt_media_multi_download:
                break;

            case R.id.bt_media_multi_copy_to_right:
                mUsbMediaCopyDeque.clear();
                for (AllMediaListBean data: mAllMediaListAdapter.getListDatas())
                {
                    if (data.isSelected())
                    {
                        mUsbMediaCopyDeque.add(data.getMediaData().getFilePath());
                    }
                }

                ToastUtil.showToast(this,"need copy "+mUsbMediaCopyDeque.size() + " file(s)");

                if (mUsbMediaCopyDeque.size()>0)
                {
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
    }

    @Override
    protected void onDestroy() {
        if (mPlayer != null) {
            mPlayer.mediaStop();
            mPlayer.release();
            mPlayer = null;
        }

        if (mUsbCapacityUpdateService!=null) {
            mUsbCapacityUpdateService.shutdown();
            mUsbCapacityUpdateService = null;
        }

        unregisterReceiver(usbReceiver);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mIsFullScreen) {
            new AlertDialog.Builder(this).setIcon(R.drawable.pullup_icon_big)
                    .setTitle("您是否要退出全屏？")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            fullScreenDisplaySwitch();
                        }
                    }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
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
            //todo 切换图片后被销毁
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

    private SeekBar.OnSeekBarChangeListener mSeekbarChange = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // 当进度条停止修改的时候触发
            int progress = seekBar.getProgress();
            mPlayer.seekTo(progress);
            mDontUpdateSeekBar = false; //用户的拖动停止了,允许更新seekBar
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                setMediaPlayedTime(progress);
                mDontUpdateSeekBar = true; //在拖动的时候,需要防止播放开的线程来改变seekBar的位置
            }
        }
    };

    //只能在主线程调用
    private void setTimeTextView(TextView tv, int milliseconds) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(milliseconds);
        tv.setText(hms);
    }

    private void setMediaDurationTime(int milliseconds) {
        if (Looper.myLooper() == Looper.getMainLooper()) { // UI主线程
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

    private void updateUsbDeviceCapacity(String text)
    {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // UI主线程
            mUsbCapacityTextView.setText(text);
        } else {
            // 非UI主线程
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_UPDATE_USB_DEVICE_CAPACITY;
            msg.obj = text;
            mHandler.sendMessage(msg);
        }
    }

    private void fullScreenDisplaySwitch() {
        if (!mIsFullScreen) {
            LinearLayout controlBarLayout = (LinearLayout) findViewById(R.id.ll_media_playControlBar);
            controlBarLayout.setVisibility(View.INVISIBLE);
            ViewGroup.LayoutParams lp = mVideoPlaySurfaceView.getLayoutParams();
            lp.height = R.dimen.x1920;
            lp.width = R.dimen.x1080;
            mVideoPlaySurfaceView.setLayoutParams(lp);
            mPicturePlayView.setLayoutParams(lp);
            mIsFullScreen = true;
        } else {
            LinearLayout controlBarLayout = (LinearLayout) findViewById(R.id.ll_media_playControlBar);
            controlBarLayout.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams lp = mVideoPlaySurfaceView.getLayoutParams();
            lp.width = mOriginSurfaceWidth;
            lp.height = mOriginSurfaceHeight;
            mVideoPlaySurfaceView.setLayoutParams(lp);
            mPicturePlayView.setLayoutParams(lp);
            mIsFullScreen = false;
        }
    }

    private void discoverUsbMountDevice() {
        String[] mountList = FileUtil.getMountVolumePaths(this);
        for (String s:mountList)
        {
            if (!FileUtil.contains(mMountExceptList,s)) {
                mUsbPartitionAdapter.add(s);
            }
        }
    }

    private class CopyTaskParam {
        static final int DIRECT_INTERNAL_TO_USB = 0;
        static final int DIRECT_USB_TO_INTERNAL = 1;
        Deque<String> fromList = new ArrayDeque<String>();
        int direct;
        long totalSize;
    }

    private class CopyTask extends AsyncTask<CopyTaskParam, Integer, Void> {

        private ProgressDialog dialog;
        private CopyTaskParam param;

        public CopyTask() {
            dialog = new ProgressDialog(MediaActivity.this);
            dialog.setTitle("Copying file");
            dialog.setMessage("Copying a file to the internal storage, this can take some time!");
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

                String basePath;
                if (param.direct == CopyTaskParam.DIRECT_USB_TO_INTERNAL) {
                    basePath =Environment.getExternalStorageDirectory().getAbsolutePath()
                            + "/" + INTERNAL_COPY_MEDIA_PATH;
                } else {
                    basePath = mUsbPartitionSpinner.getSelectedItem().toString()
                            + "/" + USB_EXPORT_MEDIA_PATH;
                }

                while(!param.fromList.isEmpty()) {
                    String sourcePath = param.fromList.pop();
                    File fromFile = new File(sourcePath);

                    int index = fromFile.getName().lastIndexOf(".") > 0
                            ? fromFile.getName().lastIndexOf(".")
                            : fromFile.getName().length();
                    String prefix = fromFile.getName().substring(0, index);
                    String ext = fromFile.getName().substring(index);

                    File toFile = FileUtil.createFile(basePath,prefix+ext);

                    OutputStream out = new BufferedOutputStream(new FileOutputStream(toFile));
                    InputStream input = new BufferedInputStream(new FileInputStream(fromFile));
                    byte[] bytes = new byte[USB_COPY_BUFFER_SIZE];
                    int count;

                    Log.d(TAG, "Copy file with total length: " + param.totalSize);

                    while ((count = input.read(bytes)) != -1) {
                        out.write(bytes, 0, count);
                        total += count;
                        int progress = (int) total;
                        if (param.totalSize > Integer.MAX_VALUE) {
                            progress = (int) (total / 1024);
                        }
                        publishProgress(progress);
                    }
                    out.close();
                    input.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "error copying!", e);
            }
            Log.d(TAG, "copy time: " + (System.currentTimeMillis() - time));
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int max = (int) param.totalSize;
            if(param.totalSize > Integer.MAX_VALUE) {
                max = (int) (param.totalSize) / 1024;
            }
            dialog.setMax(max);
            dialog.setProgress(values[0]);
        }
    }

    private void copyUsbFileToInternal()
    {
        //todo 重复文件检测
        CopyTaskParam param = new CopyTaskParam();
        param.direct=CopyTaskParam.DIRECT_USB_TO_INTERNAL;
        while(!mUsbMediaCopyDeque.isEmpty())
        {
            String path = mUsbMediaCopyDeque.pop();
            param.totalSize += FileUtil.getFileSize(path);
            param.fromList.push(path);
        }
        new CopyTask().execute(param);
    }

    private void copyInternalFileToUsb()
    {
        //todo 重复文件检测
        CopyTaskParam param = new CopyTaskParam();
        param.direct=CopyTaskParam.DIRECT_INTERNAL_TO_USB;
        while(!mUsbMediaCopyDeque.isEmpty())
        {
            String path = mUsbMediaCopyDeque.pop();
            param.totalSize += FileUtil.getFileSize(path);
            param.fromList.push(path);
        }
        new CopyTask().execute(param);
    }

    private void deleteInternalMediaFile()
    {
        new AlertDialog.Builder(this).setIcon(R.drawable.pullup_icon_big)
                .setTitle("您是否要删除文件？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        while(!mMediaDeleteDeque.isEmpty())
                        {
                            AllMediaListBean data = mMediaDeleteDeque.pop();
                            FileUtil.deleteFile(data.getMediaData().getFilePath());
                            mAllMediaListAdapter.removeItem(data);
                        }
                        mAllMediaListAdapter.refresh();
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        }).show();
    }

    private void showInfo() {
        new AlertDialog.Builder(this)
                .setTitle("我的listview")
                .setMessage("介绍...")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }
}

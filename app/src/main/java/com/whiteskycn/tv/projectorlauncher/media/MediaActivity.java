package com.whiteskycn.tv.projectorlauncher.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
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
import com.whiteskycn.tv.projectorlauncher.utils.MediaFileScanUtil;
import com.whiteskycn.tv.projectorlauncher.utils.ViewUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import static android.widget.AdapterView.INVALID_POSITION;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_IDLE;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PAUSE_PICTURE;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PAUSE_VIDEO;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PLAY_VIDEO;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class MediaActivity extends Activity
        implements View.OnClickListener,
        PictureVideoPlayer.OnMediaEventListener,
        RadioGroup.OnCheckedChangeListener
{
    private final String TAG = this.getClass().getSimpleName();

    private final int MSG_UPDATE_PLAYEDTIME = 1;
    private final int MSG_UPDATE_DURATIONTIME = 2;
    private final int MSG_ADD_TO_MEDIA_LIST = 10;
    private final int MSG_MEDIA_SCAN_DONE = 11;
    private final int MSG_MEDIA_PLAY_COMPLETE = 100;

    private final int mDoubleTapDelay_ms = 800;

    private MediaFileScanUtil mMediaScanner;

    private PictureVideoPlayer mPlayer;

    private SurfaceView mVideoPlaySurfaceView;
    private ImageView mPicturePlayView;

    private Button mNetViewBtn;
    private Button mGrayViewBtn;

    private Button mPreviousBtn;
    private Button mPlayBtn;
    private Button mNextBtn;
    private Button mVolumeBtn;

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
    private ListView mLvAllMediaList;
    private int mLastAllMediaListClickPosition = 0;     //双击添加到播放列表功能
    private long mLastAllMediaListClickTime;

    private List<UsbMediaListBean> mUsbMediaListBeans = new ArrayList<UsbMediaListBean>();
    private UsbMediaListAdapter mUsbMediaListAdapter;
    private ListView mLvUsbMediaList;
    private int mLastUsbMediaListClickPosition = 0;     //双击添加到播放列表功能
    private long mLastUsbMediaListClickTime;

    @Override
    public void onMediaCompletion()
    {
        // 播放完毕
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_MEDIA_PLAY_COMPLETE;
        mHandler.sendMessage(msg);
    }

    @Override
    public void onMediaDurationSet(int mesc)
    {
        // 设置播放控制条的时间与长度
        mPlayProgressSeekBar.setMax(mesc);
        setMediaDurationTime(mesc);
    }

    @Override
    public void onMediaSeekComplete()
    {
        Log.i(TAG,"onMediaSeekComplete");
        //如果视频在暂停的时候拖了进度条,则继续播放
        if(mPlayer.getPlayState().equals(MEDIA_PLAY_VIDEO)) {
            mPlayBtn.setBackgroundResource(R.drawable.img_media_pause);
        }
    }

    @Override
    public void onMediaPlayError()
    {
        //todo error handler
        showInfo();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId)
    {
        if(checkedId == mReplayAllRadioButton.getId())
        {
            if (mPlayer!=null) mPlayer.setReplayMode(PictureVideoPlayer.MediaReplayMode.MEDIA_REPLAY_ALL);
        }
        else if(checkedId == mReplayOneRadioButton.getId())
        {
            if (mPlayer!=null) mPlayer.setReplayMode(PictureVideoPlayer.MediaReplayMode.MEDIA_REPLAY_ONE);
        }
        else if(checkedId == mReplayShuffleRadioButton.getId())
        {
            if (mPlayer!=null) mPlayer.setReplayMode(PictureVideoPlayer.MediaReplayMode.MEDIA_REPLAY_SHUFFLE);
        }
        //todo 储存状态到preference
    }

    @Override
    public void onMediaUpdateSeekBar(int msec)
    {
        if (!mDontUpdateSeekBar) {
            setMediaPlayedTime(msec);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        initView();

        mPlayer = new PictureVideoPlayer(this,mVideoPlaySurfaceView,mPicturePlayView,mPlayListBeans);
        mPlayer.setOnCompletionListener(this);

        mIsFullScreen = false;

        mVideoPlaySurfaceView.getHolder().addCallback(svCallback);
        ViewGroup.LayoutParams lp = mVideoPlaySurfaceView.getLayoutParams();
        mOriginSurfaceHeight = lp.height;
        mOriginSurfaceWidth = lp.width;

        mPlayProgressSeekBar.setOnSeekBarChangeListener(mSeekbarChange);

        //云端本地的全媒体列表设置
        mAllMediaListAdapter = new AllMediaListAdapter(getApplicationContext(), mAllMediaListBeans);
        mLvAllMediaList.setAdapter(mAllMediaListAdapter);
        mLvAllMediaList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 如果是双击
                if(position == mLastAllMediaListClickPosition
                        && (Math.abs(mLastAllMediaListClickTime-System.currentTimeMillis()) < mDoubleTapDelay_ms)){
                    mLastAllMediaListClickPosition = -1;
                    mLastAllMediaListClickTime = 0;
                    addToPlayList(position);
                }else {
                    Log.d(TAG,"position = " + position + "; id = " + id);
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

        //Usb设备媒体列表设置
        mUsbMediaListAdapter = new UsbMediaListAdapter(getApplicationContext(), mUsbMediaListBeans);
        mLvUsbMediaList.setAdapter(mUsbMediaListAdapter);
        mLvUsbMediaList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 如果是双击
                if(position == mLastUsbMediaListClickPosition
                        && (Math.abs(mLastUsbMediaListClickTime-System.currentTimeMillis()) < mDoubleTapDelay_ms)){
                    mLastUsbMediaListClickPosition = -1;
                    mLastUsbMediaListClickTime = 0;
                    Log.i(TAG,"copy to left!");
                    //todo copyToAllMediaList(position);
                }else {
                    Log.d(TAG,"position = " + position + "; id = " + id);
                    mLastUsbMediaListClickPosition = position;
                    mLastUsbMediaListClickTime = System.currentTimeMillis();
                }
            }
        });

        //递归扫描sd卡根目录
        mMediaScanner = new MediaFileScanUtil();
        mMediaScanner.setMediaFileScanListener(new MediaFileScanUtil.MediaFileScanListener() {
            @Override
            public void onFindMedia(MediaFileScanUtil.MediaTypeEnum type, String name, String extension, String path, int duration) {
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_ADD_TO_MEDIA_LIST;
                Bundle b = new Bundle();
                b.putInt("type", type.ordinal());
                b.putInt("duration", duration);
                b.putString("name", name);
                b.putString("path", path);
                msg.setData(b);
                mHandler.sendMessage(msg);
            }

            @Override
            public void onMediaScanDone()
            {
                Log.i(TAG,"media scan done!");
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_MEDIA_SCAN_DONE;
                mHandler.sendMessage(msg);
            }
        });

        mMediaScanner.safeScanning("/mnt/sdcard");
    }

    private void initView()
    {
        setContentView(R.layout.activity_media);

        mVideoPlaySurfaceView = (SurfaceView)findViewById(R.id.sv_media_playVideo);
        mPicturePlayView = (ImageView)findViewById(R.id.iv_media_playPicture);

        mNetViewBtn = (Button)findViewById(R.id.btn_media_netView);
        mGrayViewBtn = (Button)findViewById(R.id.btn_media_grayView);

        mPlayBtn = (Button)findViewById(R.id.bt_media_play);
        mPreviousBtn = (Button)findViewById(R.id.bt_media_playPrevious);
        mNextBtn = (Button)findViewById(R.id.bt_media_playNext);
        mVolumeBtn =  (Button)findViewById(R.id.bt_media_volume);

        mPlayerPanel = (FrameLayout)findViewById(R.id.fl_media_player);
        mPlayProgressSeekBar = (SeekBar)findViewById(R.id.sb_media_playProgress);

        mMediaPlayedTimeTextView = (TextView)findViewById(R.id.tv_media_playedTime);
        mMediaDurationTimeTextView = (TextView)findViewById(R.id.tv_media_durationTime);

        mReplayModeRadioGroup = (RadioGroup)findViewById(R.id.rg_media_replay_mode);
        mReplayAllRadioButton=(RadioButton)findViewById(R.id.rb_media_replay_all);
        mReplayOneRadioButton=(RadioButton)findViewById(R.id.rb_media_replay_one);
        mReplayShuffleRadioButton=(RadioButton)findViewById(R.id.rb_media_replay_shuffle);

        mLvAllMediaList = (ListView)findViewById(R.id.lv_media_all_list);
        mLvUsbMediaList = (ListView)findViewById(R.id.lv_media_usb_list);
        mDragPlayListView = (DragListView) findViewById(R.id.lv_media_playList);

        mPicturePlayView.setOnClickListener(this);
        mVideoPlaySurfaceView.setOnClickListener(this);
        mPreviousBtn.setOnClickListener(this);
        mPlayBtn.setOnClickListener(this);
        mNextBtn.setOnClickListener(this);
        mVolumeBtn.setOnClickListener(this);

        mReplayModeRadioGroup.setOnCheckedChangeListener(this);
        //todo 从preference获取
        mReplayModeRadioGroup.check(mReplayAllRadioButton.getId());

        //设置播放时长为00:00:00
        setMediaPlayedTime(0);
        setMediaDurationTime(0);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what)
            {
                case MSG_UPDATE_DURATIONTIME:
                    mPlayProgressSeekBar.setMax(msg.arg1);
                    setTimeTextView(mMediaDurationTimeTextView,msg.arg1);
                    break;
                case MSG_UPDATE_PLAYEDTIME:
                    mPlayProgressSeekBar.setProgress(msg.arg1);
                    setTimeTextView(mMediaPlayedTimeTextView,msg.arg1);
                    break;
                case MSG_ADD_TO_MEDIA_LIST:
                    Bundle b = msg.getData();
                    int type = b.getInt("type");
                    int duration = b.getInt("duration");
                    String name = b.getString("name");
                    String path = b.getString("path");

                    MediaFileBean.MediaTypeEnum mediaType;
                    if (type==MediaFileScanUtil.MediaTypeEnum.VIDEO.ordinal())
                    {
                        mediaType = MediaFileBean.MediaTypeEnum.VIDEO;
                    }
                    else if (type==MediaFileScanUtil.MediaTypeEnum.PICTURE.ordinal())
                    {
                        mediaType = MediaFileBean.MediaTypeEnum.PICTURE;
                    }
                    else
                    {
                        mediaType = MediaFileBean.MediaTypeEnum.UNKNOWN;
                    }

                    mAllMediaListBeans.add(new AllMediaListBean(new MediaFileBean("12345",name,mediaType,MediaFileBean.MediaSourceEnum.LOCAL,true,path,duration)));
                    mAllMediaListAdapter.refresh();
                    break;
                case MSG_MEDIA_SCAN_DONE:
                    Log.i(TAG,"media scan done in handler!");
                    break;
                case MSG_MEDIA_PLAY_COMPLETE:
                    mPlayer.mediaReplay();
                    break;
            }
        }
    };


    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.sv_media_playVideo:
            case R.id.iv_media_playPicture:
                if((Math.abs(mLastFullScreenClickTime-System.currentTimeMillis()) < mDoubleTapDelay_ms)){
                    mLastFullScreenClickTime = 0;
                    fullScreenDisplaySwitch();
                }else {
                    mLastFullScreenClickTime = System.currentTimeMillis();
                }
                break;
            case R.id.bt_media_play:
                if (mPlayer.getPlayState().equals(MEDIA_IDLE))
                {
                    mPlayer.mediaPlay(0);
                    mPlayBtn.setBackgroundResource(R.drawable.img_media_pause);
                }
                else
                {
                    if(mPlayer.getPlayState().equals(MEDIA_PAUSE_PICTURE) ||
                            mPlayer.getPlayState().equals(MEDIA_PAUSE_VIDEO)) {
                        mPlayBtn.setBackgroundResource(R.drawable.img_media_pause);
                    }
                    else
                    {
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
                showInfo();
                break;
            default:
                break;
        }
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        LinearLayout layout = (LinearLayout)findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.img_background);
    }

    @Override
    protected void onDestroy()
    {
        if (mPlayer!=null) {
            mPlayer.mediaStop();
            mPlayer.release();;
            mPlayer = null;
        }
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
                            // TODO Auto-generated method stub
                            fullScreenDisplaySwitch();
                        }
                    }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
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
    private void setTimeTextView(TextView tv,int milliseconds)
    {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(milliseconds);
        tv.setText(hms);
    }

    private void setMediaDurationTime(int milliseconds)
    {
        if (Looper.myLooper() == Looper.getMainLooper()) { // UI主线程
            setTimeTextView(mMediaDurationTimeTextView,milliseconds);
            mPlayProgressSeekBar.setMax(milliseconds);
        } else { // 非UI主线程
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_UPDATE_DURATIONTIME;
            msg.arg1 = milliseconds;
            mHandler.sendMessage(msg);
        }
    }

    private void setMediaPlayedTime(int milliseconds)
    {
        if (Looper.myLooper() == Looper.getMainLooper()) { // UI主线程
            mPlayProgressSeekBar.setProgress(milliseconds);
            setTimeTextView(mMediaPlayedTimeTextView,milliseconds);
        } else { // 非UI主线程
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_UPDATE_PLAYEDTIME;
            msg.arg1 = milliseconds;
            mHandler.sendMessage(msg);
        }
    }

    private void fullScreenDisplaySwitch()
    {
        if(!mIsFullScreen)
        {
            LinearLayout controlBarLayout = (LinearLayout)findViewById(R.id.ll_media_playControlBar);
            controlBarLayout.setVisibility(View.INVISIBLE);
            ViewGroup.LayoutParams lp = mVideoPlaySurfaceView.getLayoutParams();
            lp.height = R.dimen.x1920;
            lp.width = R.dimen.x1080;
            mVideoPlaySurfaceView.setLayoutParams(lp);
            mPicturePlayView.setLayoutParams(lp);
            mIsFullScreen = true;
        }
        else
        {
            LinearLayout controlBarLayout = (LinearLayout)findViewById(R.id.ll_media_playControlBar);
            controlBarLayout.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams lp = mVideoPlaySurfaceView.getLayoutParams();
            lp.width = mOriginSurfaceWidth;
            lp.height = mOriginSurfaceHeight;
            mVideoPlaySurfaceView.setLayoutParams(lp);
            mPicturePlayView.setLayoutParams(lp);
            mIsFullScreen = false;
        }
    }

    private void addToPlayList(int position)
    {
        if (position!=INVALID_POSITION && position<mAllMediaListBeans.size()) {
            mPlayListBeans.add(new PlayListBean(mAllMediaListBeans.get(position).getMediaData()));
            mPlayListAdapter.refresh();
        }
    }


    private void showInfo(){
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

package com.whitesky.tv.projectorlauncher.media;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.media.db.PlayBean;
import com.whitesky.tv.projectorlauncher.media.maskController.MaskController;
import com.whitesky.tv.projectorlauncher.service.download.DownloadService;
import com.whitesky.tv.projectorlauncher.utils.ToastUtil;
import com.whitesky.tv.projectorlauncher.utils.ViewUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.Context.AUDIO_SERVICE;
import static android.media.MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING;
import static android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT;
import static com.whitesky.tv.projectorlauncher.media.MediaActivity.MEDIA_PLAY_DRIECT_BACKWORD;
import static com.whitesky.tv.projectorlauncher.media.MediaActivity.MEDIA_PLAY_DRIECT_FORWARD;
import static com.whitesky.tv.projectorlauncher.media.MediaActivity.MEDIA_PLAY_DRIECT_STAY;
import static com.whitesky.tv.projectorlauncher.media.db.PlayBean.MEDIA_SCALE_FIT_CENTER;
import static com.whitesky.tv.projectorlauncher.media.db.PlayBean.MEDIA_SCALE_FIT_XY;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_PICTURE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_UNKNOWN;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_VIDEO;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_DOWNLOADED;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_NONE;
import static com.whitesky.tv.projectorlauncher.media.db.PlayBean.PLAY_INDEX_PREVIEW;
import static com.whitesky.tv.projectorlauncher.service.download.DownloadService.EXTRA_KEY_URL;

/**
 * Created by jeff on 18-1-22.
 */

public class PictureVideoPlayer extends FrameLayout implements View.OnClickListener{
    private final String TAG = this.getClass().getSimpleName();

    // 路径加上此头后setDataSource会调用底层插件播放
    public static final String PRIVATE_PROTOCOL_PREFIX = "privprotocol:";
    //public static final String PRIVATE_PROTOCOL_PREFIX = "";

    // 播放器的播放状态
    public static final int PLAYER_STATE_IDLE = -1;              //表示从来没有播放,只要开始播放过就不可能是IDLE
    public static final int PLAYER_STATE_PLAY_STOP = 0;
    public static final int PLAYER_STATE_PLAY_VIDEO = 1;
    public static final int PLAYER_STATE_PLAY_PICTURE = 2;
    public static final int PLAYER_STATE_PAUSE_VIDEO = 3;
    public static final int PLAYER_STATE_PAUSE_PICTURE = 4;
    public static final int PLAYER_STATE_PLAY_COMPLETE = 5;

    // 播放错误种类定义
    public static final int ERROR_FILE_PATH_NONE = -1;
    public static final int ERROR_FILE_NOT_EXIST = -2;
    public static final int ERROR_PLAYLIST_INVALIDED_POSITION = -3;
    public static final int ERROR_PLAYLIST_MEDIA_NONE = -4;
    public static final int ERROR_VIDEO_PREPARE_ERROR = -5;
    public static final int ERROR_VIDEO_PLAY_ERROR = -6;
    public static final int ERROR_IMAGE_PLAY_ERROR = -7;
    public static final int ERROR_FILE_NEED_DOWNLOAD = -8;

    public static final int PICTURE_DEFAULT_PLAY_DURATION_MS = 10000;
    private static final int UPDATE_SEEKBAR_THREAD_SLEEP_TIME_MS = 300;
    private static final int IMAGE_MAX_WIDTH = 5000;
    private static final int IMAGE_MAX_HEIGHT = 4000;
    private static final int IMAGE_MAX_SIZE = 32*1020*1024;

    private SurfaceView mSurfaceView;
    private ImageView mPictureView;

    public MaskController getMaskController() {
        return mMaskController;
    }

    private MaskController mMaskController;

    private Button mPreviousBtn;
    private Button mPlayBtn;
    private Button mNextBtn;
    private Button mVolumeBtn;

    private SeekBar mPlayProgressSeekBar;
    private SeekBar mMediaVolumeLevelSeekBar;
    private TextView mMediaPlayedTimeTextView;
    private TextView mMediaDurationTimeTextView;

    private Context mContext;
    private AudioManager mAudioManager;
    private MediaPlayer mMediaPlayer;

    public PlayBean getCurPlayBean() {
        return curPlayBean;
    }
    private PlayBean curPlayBean;

    public boolean isPreview() {
        return isPreview;
    }
    private boolean isPreview;

    ExecutorService mSeekBarUpdateService;

    private int mOriginSurfaceHeight = 0;
    private int mOriginSurfaceWidth = 0;
    private int mDisplayWidth = 1920;
    private int mDisplayHeight = 1080;

    private boolean mDoNotUpdateSeekBar = false;        // 用户在拖动seekbar期间,系统不去更改seekbar位置
    private boolean mMediaStop = false;                 // 用于结束mSeekBarUpdateService
    private boolean mIsFullScreen = false;              //是否全屏播放标志

    public void setStartRightNow(boolean startRightNow) {
        this.mStartRightNow = startRightNow;
    }

    private boolean mStartRightNow = false;                     //是否立刻播放

    private Point mLastLongPressPoint = new Point();

    private long mPicturePlayStartTime = 0;
    private long mPictureLastUpdateSeekBarTime = 0;
    private long mPicturePlayedTime = 0;

    public int getPlayState() {
        return mPlayState;
    }
    private int mPlayState = PLAYER_STATE_IDLE;

    //播放完成回调
    public interface OnMediaEventListener {
        void onMediaPlayFullScreenSwitch(boolean fullScreen);
        void onMediaPlayStop();
        void onMediaPlayCompletion();
        void onMediaPlayError(int error, MediaBean errorBean);
        void onMediaPlayInfoUpdate(String name, String mimeType, int width, int height, long size, int bps);
        PlayBean onMediaPlayRequestPlayBean(int direct, boolean force);
    }

    public void setOnMediaEventListener(OnMediaEventListener listener) {
        mOnMediaEventListener = listener;
    }

    private OnMediaEventListener mOnMediaEventListener;

    public PictureVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        LayoutInflater.from(context).inflate(R.layout.picture_video_player, this, true);
        mSurfaceView = findViewById(R.id.sv_playVideo);
        mPictureView = findViewById(R.id.iv_playPicture);
        mMaskController = findViewById(R.id.maskControl_maskArea);

        mPlayBtn = findViewById(R.id.bt_play);
        mPreviousBtn = findViewById(R.id.bt_playPrevious);
        mNextBtn = findViewById(R.id.bt_playNext);
        mVolumeBtn = findViewById(R.id.bt_volume);

        mPlayProgressSeekBar = findViewById(R.id.sb_playProgress);
        mMediaVolumeLevelSeekBar = findViewById(R.id.sb_volume_level);

        mMediaPlayedTimeTextView = findViewById(R.id.tv_playedTime);
        mMediaDurationTimeTextView = findViewById(R.id.tv_durationTime);

        mSurfaceView.setOnClickListener(this);
        mPictureView.setOnClickListener(this);
        mPreviousBtn.setOnClickListener(this);
        mPlayBtn.setOnClickListener(this);
        mNextBtn.setOnClickListener(this);
        mVolumeBtn.setOnClickListener(this);
        mPictureView.setOnLongClickListener(mLongPressHandle);
        mPictureView.setOnTouchListener(mTouchHandle);
        mSurfaceView.setOnLongClickListener(mLongPressHandle);
        mSurfaceView.setOnTouchListener(mTouchHandle);
        mPlayProgressSeekBar.setOnSeekBarChangeListener(mDurationSeekbarChange);
        mMediaVolumeLevelSeekBar.setOnSeekBarChangeListener(mVolumeSeekbarChange);

        mMediaPlayer = new MediaPlayer();
        mSeekBarUpdateService = Executors.newSingleThreadExecutor();

        mAudioManager = (AudioManager) mContext.getSystemService(AUDIO_SERVICE);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume =mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mMediaVolumeLevelSeekBar.setMax(maxVolume);
        mMediaVolumeLevelSeekBar.setProgress(currentVolume);

        WindowManager manager =  (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(metrics);
        mDisplayWidth = metrics.widthPixels;
        mDisplayHeight = metrics.heightPixels;

        setMediaPlayedTimeUI(0);
        setMediaDurationTimeUI(0);

        mSurfaceView.getHolder().addCallback(svCallback);

        FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) mSurfaceView.getLayoutParams();
        mOriginSurfaceHeight = flp.height;
        mOriginSurfaceWidth = flp.width;

        mMaskController.bringToFront();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.sv_playVideo:
            case R.id.iv_playPicture:
                if (ViewUtil.isFastDoubleClick()) {
                    if (!mIsFullScreen) {
                        fullScreenSwitch(true);
                    }
                }
                break;

            case R.id.bt_play:
                mediaPlayOrPauseOrResume();
                break;

            case R.id.bt_playNext:
                mediaPlayNext();
                break;

            case R.id.bt_playPrevious:
                mediaPlayPrevious();
                break;

            case R.id.bt_volume:
                if (mMediaVolumeLevelSeekBar.getVisibility() == View.INVISIBLE) {
                    mMediaVolumeLevelSeekBar.setVisibility(View.VISIBLE);
                    mMediaVolumeLevelSeekBar.bringToFront();
                } else {
                    mMediaVolumeLevelSeekBar.setVisibility(View.INVISIBLE);
                }
                break;
        }
    }

    public void changeScaleNow(int scaleType) {
        switch (scaleType) {
            case MEDIA_SCALE_FIT_XY:
                if (mPlayState == PLAYER_STATE_PLAY_PICTURE || mPlayState == PLAYER_STATE_PAUSE_PICTURE){
                    mPictureView.setScaleType(ImageView.ScaleType.FIT_XY);
                } else if (mPlayState == PLAYER_STATE_PLAY_VIDEO || mPlayState == PLAYER_STATE_PAUSE_VIDEO) {
                    mMediaPlayer.setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT);
                }
                break;
            case MEDIA_SCALE_FIT_CENTER:
                if (mPlayState == PLAYER_STATE_PLAY_PICTURE || mPlayState == PLAYER_STATE_PAUSE_PICTURE){
                    mPictureView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                } else  if (mPlayState == PLAYER_STATE_PLAY_VIDEO || mPlayState == PLAYER_STATE_PAUSE_VIDEO) {
                    mMediaPlayer.setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                }
                break;
            default:
                Log.e(TAG,"wrong scaleType!");
                break;
        }
    }

    public boolean isFullScreen() {
        return mIsFullScreen;
    }

    public void fullScreenSwitch(boolean setFullScreen) {
        if (mIsFullScreen == setFullScreen) {
            Log.d(TAG, "screenDisplaySwitch do nothing, mIsFullScreen == setFullScreen");
            return;
        }

        Log.d(TAG, "screenDisplaySwitch setFullScreen:" + setFullScreen);

        if (mOnMediaEventListener!=null) {
            mOnMediaEventListener.onMediaPlayFullScreenSwitch(setFullScreen);
        }

        if (setFullScreen) {
            LinearLayout controlBarLayout = findViewById(R.id.ll_playControlBar);
            controlBarLayout.setVisibility(View.INVISIBLE);
            mMediaVolumeLevelSeekBar.setVisibility(View.INVISIBLE);
            FrameLayout.LayoutParams flpSurface = (FrameLayout.LayoutParams) mSurfaceView.getLayoutParams();
            FrameLayout.LayoutParams flpPicture = (FrameLayout.LayoutParams) mPictureView.getLayoutParams();
            FrameLayout.LayoutParams flpMask = (FrameLayout.LayoutParams) mMaskController.getLayoutParams();
            flpSurface.width = mDisplayWidth;
            flpSurface.height = mDisplayHeight;
            flpPicture.width = mDisplayWidth;
            flpPicture.height = mDisplayHeight;
            flpMask.width = mDisplayWidth;
            flpMask.height = mDisplayHeight;
            mSurfaceView.setLayoutParams(flpSurface);
            mPictureView.setLayoutParams(flpPicture);
            mMaskController.setLayoutParams(flpMask);
            mMaskController.updateControlButtonVisible(false);

            mIsFullScreen = true;
        } else {
            LinearLayout controlBarLayout = findViewById(R.id.ll_playControlBar);
            controlBarLayout.setVisibility(View.VISIBLE);

            FrameLayout.LayoutParams flpSurface = (FrameLayout.LayoutParams) mSurfaceView.getLayoutParams();
            FrameLayout.LayoutParams flpPicture = (FrameLayout.LayoutParams) mPictureView.getLayoutParams();
            FrameLayout.LayoutParams flpMask = (FrameLayout.LayoutParams) mMaskController.getLayoutParams();
            flpSurface.width = mOriginSurfaceWidth;
            flpSurface.height = mOriginSurfaceHeight;
            flpPicture.width = mOriginSurfaceWidth;
            flpPicture.height = mOriginSurfaceHeight;
            flpMask.width = mOriginSurfaceWidth;
            flpMask.height = mOriginSurfaceHeight;
            mSurfaceView.setLayoutParams(flpSurface);
            mPictureView.setLayoutParams(flpPicture);
            mMaskController.setLayoutParams(flpMask);
            mMaskController.updateControlButtonVisible(true);

            mIsFullScreen = false;
        }
    }

    //只能在主线程调用
    private void setTimeTextView(TextView tv, int milliseconds) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(milliseconds);
        tv.setText(hms);
    }

    private void setMediaDurationTimeUI(final int milliseconds) {
        Activity activity = (Activity) mContext;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setTimeTextView(mMediaDurationTimeTextView, milliseconds);
                mPlayProgressSeekBar.setMax(milliseconds);
            }
        });
    }

    private void setMediaPlayedTimeUI(final int milliseconds) {
        Activity activity = (Activity) mContext;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setTimeTextView(mMediaPlayedTimeTextView, milliseconds);
                mPlayProgressSeekBar.setProgress(milliseconds);
            }
        });
    }

    private SurfaceHolder.Callback svCallback = new SurfaceHolder.Callback() {
        // SurfaceHolder被修改的时候回调
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i(TAG, "SurfaceHolder destroy");
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, "SurfaceHolder surfaceCreated");
            if (mStartRightNow==true && MediaActivity.havePlayList(mContext)
                    && MediaActivity.isLocalMassStorageMounted(mContext)) {
                Log.i(TAG, "SurfaceHolder auto play media");
                // 每次surface从隐藏到出现都是一次surfaceCreated,所以这里必须mStartRightNow来标志这次的surfaceCreated是由于MediaActivity onResume引起的
                fullScreenSwitch(true);
                mediaPlay(MEDIA_PLAY_DRIECT_STAY, true);
            }
            // 强制清空mStartRightNow标志,防止下次图片视频切换再自动播放
            mStartRightNow = false;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.i(TAG, "SurfaceHolder size change");
        }

    };

    private View.OnTouchListener mTouchHandle = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mLastLongPressPoint.x=(int) event.getRawX();
                mLastLongPressPoint.y=(int) event.getRawY();
            }
            return false;
        }
    };

    private View.OnLongClickListener mLongPressHandle = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (mIsFullScreen) {
                Toast.makeText(mContext, getResources().getString(R.string.str_media_mask_paint_begin), Toast.LENGTH_SHORT).show();
                mMaskController.showPaintWindow(mLastLongPressPoint);
            }
            return true;
        }
    };

    private SeekBar.OnSeekBarChangeListener mDurationSeekbarChange = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // 当进度条停止修改的时候触发
            int progress = seekBar.getProgress();
            seekTo(progress);
            mDoNotUpdateSeekBar = false; //用户的拖动停止了,允许更新seekBar
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                setMediaPlayedTimeUI(progress);
                mDoNotUpdateSeekBar = true; //在拖动的时候,需要防止播放开的线程来改变seekBar的位置
            }
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


    // 媒体播放控制
    public void mediaStop()
    {
        mPlayBtn.setBackgroundResource(R.drawable.selector_media_play_btn);

        pictureStop();
        videoStop();

        mMediaStop = true;    //保证上一个mSeekBarUpdateService一定可以被结束

        curPlayBean = null;

        if (mOnMediaEventListener!=null)
        {
            mOnMediaEventListener.onMediaPlayStop();
        }
    }

    // 告知方向和是否强制根据方向切换上一首下一首,mediaPlay自己去查询playIndex进行播放
    public void mediaPlay(int direct, boolean force) {
        mediaPlay(null,direct, force);
    }

    // 告知需要播放的条目, 根据条目的idx区分是preview还是播放列表双击
    public void mediaPlay(PlayBean bean) {
        mediaPlay(bean, MEDIA_PLAY_DRIECT_FORWARD, true);
    }

    // 如果传入的PlayBean不是空的,则播放这个bean,idx如果是-1,则代表是预览
    // 否则根据forward和force参数向MediaActivity请求
    private void mediaPlay(PlayBean bean, int direct, boolean force) {
        Log.d(TAG,"media Play bean:" + bean + " direct=" + direct + " force=" + force);

        curPlayBean = null;
        isPreview = false;
        if (bean==null) {
            // 沒有指定,所以我们主动去请求
            if (mOnMediaEventListener!=null) {
                // 下面的函数会设置play list ui上的正在播放标志，但是对于bean!=null的情况调不到这边,需要自己去设置ui
                curPlayBean = mOnMediaEventListener.onMediaPlayRequestPlayBean(direct,force);
            }

            if (curPlayBean==null) {
                // 没有请求到,则认为播放列表不存在
                mPlayState = PLAYER_STATE_PLAY_STOP;
                if (mOnMediaEventListener!=null)
                {
                    mOnMediaEventListener.onMediaPlayError(ERROR_PLAYLIST_INVALIDED_POSITION, null);
                }
                return;
            }
        } else {
            //双击播放列表/预览而来
            curPlayBean = bean;
            if(bean.getIdx()==PLAY_INDEX_PREVIEW) {
                // 由预览指定而来
                isPreview = true;
            }
        }


        String path="";
        int type = MEDIA_UNKNOWN;
        int time = PICTURE_DEFAULT_PLAY_DURATION_MS;
        int scale = MEDIA_SCALE_FIT_XY;

        MediaBean fileBean = curPlayBean.getMedia();
        if (fileBean!=null) {
            path = fileBean.getPath();
            type = fileBean.getType();
            scale =  curPlayBean.getScale();
        } else {
            mPlayState = PLAYER_STATE_PLAY_STOP;
            if (mOnMediaEventListener!=null)
            {
                mOnMediaEventListener.onMediaPlayError(ERROR_PLAYLIST_MEDIA_NONE, null);
            }
            return;
        }

        // 如果是没有下载的文件,则直接播放完成，并开服务下载他
        if (fileBean.getDownloadState()!=STATE_DOWNLOAD_DOWNLOADED) {

            if (fileBean.getDownloadState()==STATE_DOWNLOAD_NONE) {
                Intent intent = new Intent().setAction(DownloadService.ACTION_MEDIA_DOWNLOAD_START);
                intent.putExtra(EXTRA_KEY_URL, fileBean.getUrl());
                Log.i(TAG, "play call download:" + fileBean.toString());
                mContext.startService(intent);
            }

            Log.w(TAG,"could not start to play file which not downloaded, play next directly");

            // 这边特别设置STOP表示是因为文件没有下载而直接完成
            mPlayState = PLAYER_STATE_PLAY_STOP;
            if (mOnMediaEventListener!=null)
            {
                mOnMediaEventListener.onMediaPlayError(ERROR_FILE_NEED_DOWNLOAD, fileBean);
            }
            return;
        }

        if (path==null || path.isEmpty())
        {
            Log.e(TAG,"media Play FILE_PATH_ERROR!" + fileBean.toString());
            mPlayState = PLAYER_STATE_PLAY_STOP;
            if (mOnMediaEventListener!=null)
            {
                mOnMediaEventListener.onMediaPlayError(ERROR_FILE_PATH_NONE,fileBean);
            }
            return;
        }

        mPlayBtn.setBackgroundResource(R.drawable.selector_media_pause_btn);

        //ToastUtil.showToast(mContext, "play path=" + path);
        Log.d(TAG,"media Play path:" + path + ", type:" + type);

        switch (type)
        {
            case MEDIA_VIDEO:
                videoPlay(path, scale);
                break;
            case MEDIA_PICTURE:
                time = curPlayBean.getTime();
                picturePlay(path, time, scale);
                break;
            default:
                break;
        }

    }

    public void mediaPlayOrPauseOrResume()
    {
        switch(mPlayState)
        {
            case PLAYER_STATE_IDLE:
            case PLAYER_STATE_PLAY_STOP:
                mediaPlay(MEDIA_PLAY_DRIECT_STAY,false);
                break;
            case PLAYER_STATE_PLAY_PICTURE:
                mPlayBtn.setBackgroundResource(R.drawable.selector_media_play_btn);
                picturePause();
                break;
            case PLAYER_STATE_PLAY_VIDEO:
                mPlayBtn.setBackgroundResource(R.drawable.selector_media_play_btn);
                videoPause();
                break;
            case PLAYER_STATE_PAUSE_VIDEO:
                mPlayBtn.setBackgroundResource(R.drawable.selector_media_pause_btn);
                videoResume();
                break;
            case PLAYER_STATE_PAUSE_PICTURE:
                mPlayBtn.setBackgroundResource(R.drawable.selector_media_pause_btn);
                pictureResume();
                break;
            case PLAYER_STATE_PLAY_COMPLETE:
            default:
                // do nothing
                break;
        }
    }

    public void mediaPlayNext()
    {
        mediaStop();
        mediaPlay(MEDIA_PLAY_DRIECT_FORWARD, true);
    }

    public void mediaPlayPrevious()
    {
        mediaStop();
        mediaPlay(MEDIA_PLAY_DRIECT_BACKWORD, true);
    }

    private void seekTo(int msec)
    {
        switch (mPlayState)
        {
            case PLAYER_STATE_PLAY_VIDEO:
                mMediaPlayer.seekTo(msec);
                break;
            case PLAYER_STATE_PLAY_PICTURE:
            case PLAYER_STATE_PAUSE_PICTURE:
                //图片不需要seek功能
                break;
            case PLAYER_STATE_PAUSE_VIDEO:
                if (mMediaPlayer != null) {
                    // 设置当前播放的位置
                    if (!mMediaPlayer.isPlaying())
                    {
                        mPlayState = PLAYER_STATE_PLAY_VIDEO;
                        mMediaPlayer.start();
                    }
                    mMediaPlayer.seekTo(msec);
                }
                break;
            default:
                break;
        }
    }

    public void release()
    {
        mContext = null;
        mSurfaceView = null;
        mPictureView = null;
        mOnMediaEventListener = null;
        mPlayState = PLAYER_STATE_IDLE;
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mSeekBarUpdateService!=null) {
            mSeekBarUpdateService.shutdownNow();
            mSeekBarUpdateService = null;
        }

        mDoNotUpdateSeekBar = false;
        mMediaStop = false;
        mIsFullScreen = false;
        mStartRightNow = false;

        mPicturePlayedTime = 0;
        mPicturePlayStartTime = 0;
        mPictureLastUpdateSeekBarTime = 0;
    }

    private void videoStop() {
        Log.i(TAG, "video Stop");
        mPlayState = PLAYER_STATE_PLAY_STOP;
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
        }
    }

    private void videoPlay(String path, int scale) {
        File file = new File(path);
        if (!file.exists()) {
            Log.e(TAG,"video Play file not exists!");
            mPlayState = PLAYER_STATE_PLAY_STOP;
            if (mOnMediaEventListener!=null)
            {
                mOnMediaEventListener.onMediaPlayError(ERROR_FILE_NOT_EXIST,
                        curPlayBean ==null?null: curPlayBean.getMedia());
            }
            return;
        }

        mPictureView.setVisibility(View.INVISIBLE);
        mSurfaceView.setVisibility(View.VISIBLE);

        try {
            mMediaPlayer.reset();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDisplay(mSurfaceView.getHolder());
            mMediaPlayer.setDataSource(PRIVATE_PROTOCOL_PREFIX + file.getAbsolutePath());

            switch (scale) {
                case MEDIA_SCALE_FIT_XY:
                    mMediaPlayer.setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT);
                    break;
                case MEDIA_SCALE_FIT_CENTER:
                    mMediaPlayer.setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    break;
                default:
                    mMediaPlayer.setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT);
                    break;
            }

            Log.i(TAG,"media file prepare...");
            //准备文件信息
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(PRIVATE_PROTOCOL_PREFIX + file.getAbsolutePath(),new HashMap<String, String>());
            String width = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String bitRate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            mmr.release();

            String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
            String mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (TextUtils.isEmpty(mimeType)) {
                switch(extension.toUpperCase())
                {// 自己补充几种常见扩展名
                    case "MKV":
                        mimeType = "video/x-matroska";
                        break;

                    default:
                        mimeType = "video/unknown";
                        break;
                }
            }

            long size = file.length();

            if(mOnMediaEventListener !=null)
            {
                mOnMediaEventListener.onMediaPlayInfoUpdate(file.getName(),
                        mimeType, Integer.parseInt(width), Integer.parseInt(height), size,
                        Integer.parseInt(bitRate));
            }

            mMediaPlayer.prepareAsync();

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.i(TAG, "media file prepare done");
                    // 按照初始位置播放
                    mPlayState = PLAYER_STATE_PLAY_VIDEO;
                    mMediaPlayer.seekTo(0);
                    mMediaPlayer.start();

                    // 设置进度条的最大进度为视频流的最大播放时长
                    setMediaDurationTimeUI(mMediaPlayer.getDuration());

                    mSeekBarUpdateService.execute(
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Log.i(TAG, "mSeekBarUpdateService in");
                                    mMediaStop = false;
                                    while (mPlayState == PLAYER_STATE_PAUSE_VIDEO || mPlayState == PLAYER_STATE_PLAY_VIDEO) {
                                        int current = mMediaPlayer.getCurrentPosition();

                                        if (!mDoNotUpdateSeekBar) {
                                            setMediaPlayedTimeUI(current);
                                        }

                                        if (mMediaStop) {
                                            mMediaStop = false;
                                            Log.i(TAG, "mSeekBarUpdateService out");
                                            return;
                                        }

                                        sleep(UPDATE_SEEKBAR_THREAD_SLEEP_TIME_MS);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Exception in mSeekBarUpdateService execute!" + e);
                                }
                            }
                        });
                }
            });

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.i(TAG, "video Play onCompletion");
                    mPlayState = PLAYER_STATE_PLAY_COMPLETE;
                    if (mOnMediaEventListener!=null)
                    {
                        mOnMediaEventListener.onMediaPlayCompletion();
                    }
                }
            });

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.i(TAG, "video Play onError:" + what + " " +extra);
                    mMediaPlayer.reset();
                    mPlayState = PLAYER_STATE_PLAY_STOP;
                    if (mOnMediaEventListener!=null)
                    {
                        mOnMediaEventListener.onMediaPlayError(ERROR_VIDEO_PLAY_ERROR,
                                curPlayBean ==null?null: curPlayBean.getMedia());
                    }
                    return false;
                }
            });

            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    if (getPlayState() == PLAYER_STATE_PLAY_VIDEO) {
                        mPlayBtn.setBackgroundResource(R.drawable.selector_media_pause_btn);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in video prepare!");
            e.printStackTrace();

            mPlayState = PLAYER_STATE_PLAY_STOP;
            if (mOnMediaEventListener!=null)
            {
                mOnMediaEventListener.onMediaPlayError(ERROR_VIDEO_PREPARE_ERROR, curPlayBean ==null?null: curPlayBean.getMedia());
            }
        }
    }

    private void videoPause() {
        Log.i(TAG, "video Pause");
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mPlayState = PLAYER_STATE_PAUSE_VIDEO;
            ToastUtil.showToast(mContext, R.string.str_media_play_pause);
        }
    }

    private void videoResume() {
        Log.i(TAG, "video Resume");
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            mPlayState = PLAYER_STATE_PLAY_VIDEO;
            ToastUtil.showToast(mContext, R.string.str_media_play_resume);
        }
    }

    private void pictureStop()
    {
        Log.i(TAG, "picture Stop");
        mPlayState = PLAYER_STATE_PLAY_STOP;
        mPicturePlayedTime = 0;
    }

    private void picturePlay(String path, int duration, int scale)
    {
        File file = new File(path);
        if (!file.exists()) {
            Log.e(TAG,"picture Play file not exists!");
            mPlayState = PLAYER_STATE_PLAY_STOP;
            if (mOnMediaEventListener!=null)
            {
                mOnMediaEventListener.onMediaPlayError(ERROR_FILE_NOT_EXIST,
                        curPlayBean ==null?null: curPlayBean.getMedia());
            }
            return;
        }

        mPictureView.setVisibility(View.VISIBLE);
        mSurfaceView.setVisibility(View.INVISIBLE);

        //准备文件信息
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        String mimeType = options.outMimeType;
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "image/unknown";
        }
        long size = file.length();

        if (options.outWidth > IMAGE_MAX_WIDTH || options.outHeight > IMAGE_MAX_HEIGHT || size > IMAGE_MAX_SIZE) {
            Log.e(TAG,"image too large!" + options.outWidth + "*" + options.outHeight + " " + size);
            if(mOnMediaEventListener !=null)
            {
                mOnMediaEventListener.onMediaPlayError(ERROR_IMAGE_PLAY_ERROR,
                        curPlayBean ==null?null: curPlayBean.getMedia());
            }
            return;
        }

        if(mOnMediaEventListener !=null)
        {
            mOnMediaEventListener.onMediaPlayInfoUpdate(file.getName(),
                    mimeType, options.outWidth, options.outHeight, size, -1);
        }

        final int playDuration = duration;

        switch(scale)
        {
            case MEDIA_SCALE_FIT_XY:
                mPictureView.setScaleType(ImageView.ScaleType.FIT_XY);
                break;
            case MEDIA_SCALE_FIT_CENTER:
                mPictureView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                break;
            default:
                mPictureView.setScaleType(ImageView.ScaleType.FIT_XY);
                break;
        }

        mPictureView.setImageDrawable(Drawable.createFromPath(path));

        setMediaDurationTimeUI(playDuration);

        mPlayState = PLAYER_STATE_PLAY_PICTURE;
        mPicturePlayedTime = 0;
        mPicturePlayStartTime = System.currentTimeMillis();
        mPictureLastUpdateSeekBarTime = mPicturePlayStartTime;

        mSeekBarUpdateService.execute(new Thread() {
            @Override
            public void run() {
                try {
                    mMediaStop = false;
                    while (mPlayState == PLAYER_STATE_PLAY_PICTURE || mPlayState == PLAYER_STATE_PAUSE_PICTURE) {
                        long current = System.currentTimeMillis();
                        if (mPlayState == PLAYER_STATE_PLAY_PICTURE) {
                            mPicturePlayedTime += Math.abs(current - mPictureLastUpdateSeekBarTime);
                        }
                        mPictureLastUpdateSeekBarTime = current;

                        if (mPicturePlayedTime>playDuration)
                        {
                            mPicturePlayedTime = 0;

                            mPlayState = PLAYER_STATE_PLAY_COMPLETE;
                            if (mOnMediaEventListener!=null)
                            {
                                mOnMediaEventListener.onMediaPlayCompletion();
                            }
                        }

                        if (!mDoNotUpdateSeekBar) {
                            setMediaPlayedTimeUI((int)mPicturePlayedTime);
                        }

                        if (mMediaStop) {
                            mMediaStop = false;
                            return;
                        }

                        sleep(UPDATE_SEEKBAR_THREAD_SLEEP_TIME_MS);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception mSeekBarUpdateService execute!", e);
                }
            }
        });
    }

    private void picturePause() {
        Log.i(TAG, "picture Pause");
        mPlayState = PLAYER_STATE_PAUSE_PICTURE;
        ToastUtil.showToast(mContext, R.string.str_media_play_pause);
    }

    private void pictureResume() {
        Log.i(TAG, "picture Resume");
        mPlayState = PLAYER_STATE_PLAY_PICTURE;
        ToastUtil.showToast(mContext, R.string.str_media_play_resume);
    }

}

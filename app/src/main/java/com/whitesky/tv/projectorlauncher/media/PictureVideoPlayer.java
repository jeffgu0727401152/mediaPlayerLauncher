package com.whitesky.tv.projectorlauncher.media;

import android.app.Activity;
import android.content.Context;
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
import com.whitesky.tv.projectorlauncher.media.bean.PlayListBean;
import com.whitesky.tv.projectorlauncher.media.maskController.MaskController;
import com.whitesky.tv.projectorlauncher.utils.FileUtil;
import com.whitesky.tv.projectorlauncher.utils.ToastUtil;
import com.whitesky.tv.projectorlauncher.utils.ViewUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.Context.AUDIO_SERVICE;
import static android.media.MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING;
import static android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT;
import static android.widget.AdapterView.INVALID_POSITION;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_IDLE;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PAUSE_PICTURE;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PAUSE_VIDEO;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PLAY_COMPLETE;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PLAY_PICTURE;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PLAY_VIDEO;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.MEDIA_REPLAY_ALL;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.MEDIA_REPLAY_ONE;
import static com.whitesky.tv.projectorlauncher.media.bean.PlayListBean.MEDIA_SCALE_FIT_CENTER;
import static com.whitesky.tv.projectorlauncher.media.bean.PlayListBean.MEDIA_SCALE_FIT_XY;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_PICTURE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_UNKNOWN;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_VIDEO;

/**
 * Created by jeff on 18-1-22.
 */

public class PictureVideoPlayer extends FrameLayout implements View.OnClickListener{
    private final String TAG = this.getClass().getSimpleName();

    //重放模式
    public static final int  MEDIA_REPLAY_ONE = 0;
    public static final int  MEDIA_REPLAY_ALL = 1;
    public static final int  MEDIA_REPLAY_SHUFFLE = 2;
    public static final int  MEDIA_REPLAY_MODE_DEFAULT = MEDIA_REPLAY_ALL;

    public static final int PICTURE_DEFAULT_PLAY_DURATION_MS = 10000;
    private static final int UPDATE_SEEKBAR_THREAD_SLEEP_TIME_MS = 300;

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
    private MediaMetadataRetriever mMediaMetadataRetriever;

    private List<PlayListBean> mPlayList;

    public PlayListBean getCurPlaylistBean() {
        return curPlaylistBean;
    }

    private PlayListBean curPlaylistBean;

    ExecutorService mSeekBarUpdateService;

    private int mOriginSurfaceHeight = 0;
    private int mOriginSurfaceWidth = 0;
    private int mDisplayWidth = 1920;
    private int mDisplayHeight = 1080;

    private boolean mDoNotUpdateSeekBar = false;        // 用户在拖动seekbar期间,系统不去更改seekbar位置
    private boolean mMediaSwitch = false;               // 用于结束mSeekBarUpdateService
    private boolean mIsFullScreen = false;              //是否全屏播放标志

    public void setStartRightNow(boolean startRightNow) {
        this.mStartRightNow = startRightNow;
    }

    private boolean mStartRightNow = false;                     //是否立刻播放

    private Point longPressPoint = new Point();

    private long mPicturePlayStartTime = 0;
    private long mPictureLastUpdateSeekBarTime = 0;
    private long mPicturePlayedTime = 0;

    //播放器状态
    public enum MediaPlayState
    {
        MEDIA_IDLE,
        MEDIA_PLAY_VIDEO,
        MEDIA_PLAY_PICTURE,
        MEDIA_PAUSE_VIDEO,
        MEDIA_PAUSE_PICTURE,
        MEDIA_PLAY_COMPLETE
    };

    public MediaPlayState getPlayState() {
        return mPlayState;
    }

    private MediaPlayState mPlayState = MEDIA_IDLE;

    private int mPlayPosition;

    public boolean isPreview() {
        return mIsPreview;
    }

    private boolean mIsPreview = false;

    //重放模式

    public int getReplayMode() {
        return mReplayMode;
    }

    public void setReplayMode(int mode) {
        this.mReplayMode = mode;
    }
    private int mReplayMode = MEDIA_REPLAY_ALL;

    //播放完成回调
    public interface OnMediaEventListener
    {
        void onMediaPlayFullScreenSwitch(boolean fullScreen);
        void onMediaPlayStop();
        void onMediaPlayCompletion();
        void onMediaPlayError();
        void onMediaPlayInfoUpdate(String name, String mimeType, int width, int height, long size, int bps);
    }

    public void setOnMediaEventListener(OnMediaEventListener listener)
    {
        mOnMediaEventListener = listener;
    }

    private OnMediaEventListener mOnMediaEventListener;


    public PictureVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        LayoutInflater.from(context).inflate(R.layout.picture_video_player, this, true);
        mSurfaceView = (SurfaceView) findViewById(R.id.sv_playVideo);
        mPictureView = (ImageView) findViewById(R.id.iv_playPicture);
        mMaskController = (com.whitesky.tv.projectorlauncher.media.maskController.MaskController) findViewById(R.id.maskControl_maskArea);

        mPlayBtn = (Button) findViewById(R.id.bt_play);
        mPreviousBtn = (Button) findViewById(R.id.bt_playPrevious);
        mNextBtn = (Button) findViewById(R.id.bt_playNext);
        mVolumeBtn = (Button) findViewById(R.id.bt_volume);

        mPlayProgressSeekBar = (SeekBar) findViewById(R.id.sb_playProgress);
        mMediaVolumeLevelSeekBar = (SeekBar) findViewById(R.id.sb_volume_level);

        mMediaPlayedTimeTextView = (TextView) findViewById(R.id.tv_playedTime);
        mMediaDurationTimeTextView = (TextView) findViewById(R.id.tv_durationTime);

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
        mMediaMetadataRetriever = new MediaMetadataRetriever();
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

    public void setPlayList( List<PlayListBean> list) {
        mPlayList = list;
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
                if (mPlayState.equals(MEDIA_IDLE)) {
                    mPlayBtn.setBackgroundResource(R.drawable.selector_media_pause_btn);
                    mediaPlay(0);
                } else {
                    if (mPlayState.equals(MEDIA_PAUSE_PICTURE) ||
                            mPlayState.equals(MEDIA_PAUSE_VIDEO)) {
                        mPlayBtn.setBackgroundResource(R.drawable.selector_media_pause_btn);
                    } else {
                        mPlayBtn.setBackgroundResource(R.drawable.selector_media_play_btn);
                    }
                    mediaPauseResume();
                }
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
                if (mPlayState == MEDIA_PLAY_PICTURE || mPlayState == MEDIA_PAUSE_PICTURE){
                    mPictureView.setScaleType(ImageView.ScaleType.FIT_XY);
                } else {
                    mMediaPlayer.setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT);
                }
                break;
            case MEDIA_SCALE_FIT_CENTER:
                if (mPlayState == MEDIA_PLAY_PICTURE || mPlayState == MEDIA_PAUSE_PICTURE){
                    mPictureView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                } else {
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
            LinearLayout controlBarLayout = (LinearLayout) findViewById(R.id.ll_playControlBar);
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
            mMaskController.showControlButton(false);

            mIsFullScreen = true;
        } else {
            LinearLayout controlBarLayout = (LinearLayout) findViewById(R.id.ll_playControlBar);
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
            mMaskController.showControlButton(true);

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
            if (mPlayList != null && mPlayList.size() > 0
                    && mStartRightNow==true) {
                // 每次surface从隐藏到出现都是一次surfaceCreated,所以这里必须mStartRightNow来标志这次的surfaceCreated是由于MediaActivity onResume引起的
                mediaPlay(0);
                fullScreenSwitch(true);
                mStartRightNow = false;
                Log.i(TAG, "SurfaceHolder auto play media");
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            Log.i(TAG, "SurfaceHolder size change");
        }

    };

    private View.OnTouchListener mTouchHandle = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                longPressPoint.x=(int) event.getRawX();
                longPressPoint.y=(int) event.getRawY();
            }
            return false;
        }
    };

    private View.OnLongClickListener mLongPressHandle = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (mIsFullScreen) {
                Toast.makeText(mContext, getResources().getString(R.string.str_media_mask_paint_begin), Toast.LENGTH_SHORT).show();
                mMaskController.showPaintWindow(longPressPoint);
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
    public void mediaPreview(MediaBean mPreviewItem)
    {
        Log.d(TAG,"mediaPreview " + mPreviewItem.getPath());
        String path="";
        int type = MEDIA_UNKNOWN;
        int time = PICTURE_DEFAULT_PLAY_DURATION_MS;
        if (mPreviewItem!=null)
        {
            path = mPreviewItem.getPath();
            type = mPreviewItem.getType();
            time = mPreviewItem.getDuration();
        }

        if (path.isEmpty())
        {
            ToastUtil.showToast(mContext, R.string.str_media_play_path_error);
            mPlayState = MEDIA_IDLE;
            if (mOnMediaEventListener!=null)
            {
                mOnMediaEventListener.onMediaPlayError();
            }
            return;
        }

        mIsPreview = true;
        curPlaylistBean = null;

        switch (type)
        {
            case MEDIA_VIDEO:
                videoPlay(path);
                break;
            case MEDIA_PICTURE:
                picturePlay(path, time);
                break;
            default:
                break;
        }
    }


    public void mediaStop()
    {
        if (mPlayState.equals(MEDIA_PLAY_PICTURE)) {
            pictureStop();
        } else if (mPlayState.equals(MEDIA_PLAY_VIDEO))
        {
            videoStop();
        }
        curPlaylistBean = null;
        if (mOnMediaEventListener!=null)
        {
            mOnMediaEventListener.onMediaPlayStop();
        }

        mMediaSwitch = true;    //保证上一个mSeekBarUpdateService一定可以被结束
    }

    public void mediaPlay(int position)
    {
        Log.d(TAG,",mediaPlay position = " + position);
        String path="";
        int type = MEDIA_UNKNOWN;
        int time = PICTURE_DEFAULT_PLAY_DURATION_MS;
        int scale = MEDIA_SCALE_FIT_XY;
        if(position!=INVALID_POSITION && position<mPlayList.size())
        {
            MediaBean fileBean = mPlayList.get(position).getMediaData();
            if (fileBean!=null)
            {
                path = fileBean.getPath();
                type = fileBean.getType();
                time = fileBean.getDuration();
                scale =  mPlayList.get(position).getPlayScale();
            }
        } else {
            ToastUtil.showToast(mContext, R.string.str_media_play_list_empty);
            Log.e(TAG,"mediaPlay position(" + position +") INVALID_POSITION!");
            mPlayState = MEDIA_IDLE;
            if (mOnMediaEventListener!=null)
            {
                mOnMediaEventListener.onMediaPlayError();
            }
            return;
        }

        if (path.isEmpty())
        {
            ToastUtil.showToast(mContext, R.string.str_media_play_path_error);
            Log.e(TAG,"mediaPlay position(" + position +") PATH_ERROR!");
            mPlayState = MEDIA_IDLE;
            if (mOnMediaEventListener!=null)
            {
                mOnMediaEventListener.onMediaPlayError();
            }
            return;
        }

        mIsPreview = false;
        mPlayPosition = position;
        curPlaylistBean = mPlayList.get(position);

        //ToastUtil.showToast(mContext, "path=" + path + ", position=" + position);
        Log.d(TAG,"mediaPlay path:" + path + ", type:" + type);

        switch (type)
        {
            case MEDIA_VIDEO:
                videoPlay(path, scale);
                break;
            case MEDIA_PICTURE:
                picturePlay(path, time, scale);
                break;
            default:
                break;
        }
    }

    public void mediaPauseResume()
    {
        switch(mPlayState)
        {
            case MEDIA_PLAY_PICTURE:
                picturePause();
                break;
            case MEDIA_PLAY_VIDEO:
                videoPause();
                break;
            case MEDIA_PAUSE_VIDEO:
                videoResume();
                break;
            case MEDIA_PAUSE_PICTURE:
                pictureResume();
                break;
            default:
                Log.i(TAG,"pause when player IDLE!!!");
                break;
        }
    }

    public void mediaReplay()
    {
        if (mReplayMode!=MEDIA_REPLAY_ONE) {
            updatePlayPosition(true);
        }
        mediaStop();
        mediaPlay(mPlayPosition);
    }

    public void mediaPlayNext()
    {
        updatePlayPosition(true);
        mediaStop();
        mediaPlay(mPlayPosition);
    }

    public void mediaPlayPrevious()
    {
        updatePlayPosition(false);
        mediaStop();
        mediaPlay(mPlayPosition);
    }

    public void seekTo(int msec)
    {
        switch (mPlayState)
        {
            case MEDIA_PLAY_VIDEO:
                mMediaPlayer.seekTo(msec);
                break;
            case MEDIA_PLAY_PICTURE:
            case MEDIA_PAUSE_PICTURE:
                //图片不需要seek功能
                break;
            case MEDIA_PAUSE_VIDEO:
                if (mMediaPlayer != null) {
                    // 设置当前播放的位置
                    if (!mMediaPlayer.isPlaying())
                    {
                        mPlayState = MEDIA_PLAY_VIDEO;
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
        mPlayState = MEDIA_IDLE;
        mReplayMode = MEDIA_REPLAY_ALL;
        mPlayList = null;
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mMediaMetadataRetriever!=null) {
            mMediaMetadataRetriever.release();
            mMediaMetadataRetriever = null;
        }

        if (mSeekBarUpdateService!=null) {
            mSeekBarUpdateService.shutdownNow();
            mSeekBarUpdateService = null;
        }

        mDoNotUpdateSeekBar = false;
        mMediaSwitch = false;
        mIsFullScreen = false;
        mStartRightNow = false;

        mPicturePlayedTime = 0;
        mPicturePlayStartTime = 0;
        mPictureLastUpdateSeekBarTime = 0;
    }

    private void videoStop() {
        mPlayState = MEDIA_IDLE;
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
    }

    private void videoPlay(String path)
    {
        videoPlay(path, MEDIA_SCALE_FIT_XY);
    }

    private void videoPlay(String path, int scale) {
        mPictureView.setVisibility(View.INVISIBLE);
        mSurfaceView.setVisibility(View.VISIBLE);

        File file = new File(path);
        if (!file.exists()) {
            ToastUtil.showToast(mContext, R.string.str_media_play_path_error);
            Log.e(TAG,"videoPlay file not exists!");
            mPlayState = MEDIA_IDLE;
            if (mOnMediaEventListener!=null)
            {
                mOnMediaEventListener.onMediaPlayError();
            }
            return;
        }

        try {
            mMediaPlayer.reset();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDisplay(mSurfaceView.getHolder());
            mMediaPlayer.setDataSource(file.getAbsolutePath());

            //准备文件信息
            mMediaMetadataRetriever.setDataSource(file.getAbsolutePath());
            String width = mMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = mMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String bitRate = mMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);

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

            long size = FileUtil.getFileSize(file);

            if(mOnMediaEventListener !=null)
            {
                mOnMediaEventListener.onMediaPlayInfoUpdate(file.getName(),
                        mimeType, Integer.parseInt(width), Integer.parseInt(height), size,
                        Integer.parseInt(bitRate));
            }

            Log.i(TAG,"media file prepare...");
            mMediaPlayer.prepareAsync();

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.i(TAG, "media file prepare done");
                    // 按照初始位置播放
                    mPlayState = MEDIA_PLAY_VIDEO;
                    mMediaPlayer.seekTo(0);
                    mMediaPlayer.start();

                    // 设置进度条的最大进度为视频流的最大播放时长
                    setMediaDurationTimeUI(mMediaPlayer.getDuration());

                    mSeekBarUpdateService.execute(
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    mMediaSwitch = false;
                                    while ((mPlayState.equals(MEDIA_PAUSE_VIDEO) || mPlayState.equals(MEDIA_PLAY_VIDEO))) {
                                        int current = mMediaPlayer.getCurrentPosition();

                                        if (!mDoNotUpdateSeekBar) {
                                            setMediaPlayedTimeUI(current);
                                        }

                                        if (mMediaSwitch) {
                                            mMediaSwitch = false;
                                            return;
                                        }

                                        sleep(UPDATE_SEEKBAR_THREAD_SLEEP_TIME_MS);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "error in mSeekBarUpdateService execute!", e);
                                }
                            }
                        });
                }
            });

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    // 在播放完毕被回调
                    mPlayState = MEDIA_PLAY_COMPLETE;
                    if (mOnMediaEventListener!=null)
                    {
                        mOnMediaEventListener.onMediaPlayCompletion();
                    }
                }
            });

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // 发生错误停止播放
                    mMediaPlayer.reset();
                    mPlayState = MEDIA_IDLE;
                    if (mOnMediaEventListener!=null)
                    {
                        mOnMediaEventListener.onMediaPlayError();
                    }
                    return false;
                }
            });

            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    if (getPlayState().equals(MEDIA_PLAY_VIDEO)) {
                        mPlayBtn.setBackgroundResource(R.drawable.selector_media_pause_btn);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "error in video play!", e);
        }
    }

    private void videoPause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mPlayState = MEDIA_PAUSE_VIDEO;
            ToastUtil.showToast(mContext, R.string.str_media_play_pause);
        }
    }

    private void videoResume() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            mPlayState = MEDIA_PLAY_VIDEO;
            ToastUtil.showToast(mContext, R.string.str_media_play_resume);
        }
    }

    private void pictureStop()
    {
        mPlayState = MEDIA_IDLE;
        mPicturePlayedTime = 0;
    }

    private void picturePlay(String path, int duration)
    {
        picturePlay(path, duration, MEDIA_SCALE_FIT_XY);
    }

    private void picturePlay(String path, int duration, int scale)
    {
        File file = new File(path);
        if (!file.exists()) {
            ToastUtil.showToast(mContext, R.string.str_media_play_path_error);
            Log.e(TAG,"picturePlay file not exists!");
            mPlayState = MEDIA_IDLE;
            if (mOnMediaEventListener!=null)
            {
                mOnMediaEventListener.onMediaPlayError();
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
        long size = FileUtil.getFileSize(file);

        if(mOnMediaEventListener !=null)
        {
            mOnMediaEventListener.onMediaPlayInfoUpdate(file.getName(),
                    mimeType, options.outWidth, options.outHeight, size,
                    -1);
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

        mPlayState = MEDIA_PLAY_PICTURE;
        mPicturePlayedTime = 0;
        mPicturePlayStartTime = System.currentTimeMillis();
        mPictureLastUpdateSeekBarTime = mPicturePlayStartTime;

        mSeekBarUpdateService.execute(new Thread() {
            @Override
            public void run() {
                try {
                    mMediaSwitch = false;
                    while (mPlayState.equals(MEDIA_PLAY_PICTURE) || mPlayState.equals(MEDIA_PAUSE_PICTURE)) {
                        long current = System.currentTimeMillis();
                        if (mPlayState.equals(MEDIA_PLAY_PICTURE)) {
                            mPicturePlayedTime += Math.abs(current - mPictureLastUpdateSeekBarTime);
                        }
                        mPictureLastUpdateSeekBarTime = current;

                        if (mPicturePlayedTime>playDuration)
                        {
                            mPicturePlayedTime = 0;

                            mPlayState = MEDIA_PLAY_COMPLETE;
                            if (mOnMediaEventListener!=null)
                            {
                                mOnMediaEventListener.onMediaPlayCompletion();
                            }
                        }

                        if (!mDoNotUpdateSeekBar) {
                            setMediaPlayedTimeUI((int)mPicturePlayedTime);
                        }

                        if (mMediaSwitch) {
                            mMediaSwitch = false;
                            return;
                        }

                        sleep(UPDATE_SEEKBAR_THREAD_SLEEP_TIME_MS);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "error mSeekBarUpdateService execute!", e);
                }
            }
        });
    }

    private void picturePause() {
        mPlayState = MEDIA_PAUSE_PICTURE;
        ToastUtil.showToast(mContext, R.string.str_media_play_pause);
    }

    private void pictureResume() {
        mPlayState = MEDIA_PLAY_PICTURE;
        ToastUtil.showToast(mContext, R.string.str_media_play_resume);
    }

    private int updatePlayPosition(boolean forward) {
        if (mPlayList.size()==0)
        {
            mPlayPosition = INVALID_POSITION;
            return mPlayPosition;
        }

        // 借这个机会调整mPlayPosition的位置,播放列表是可以拖动编辑的,拖动以后播放列表的position会改变
        // 当前播放器的mPlayPosition与实际位置就不对了
        if (curPlaylistBean!=null) {
            mPlayPosition = mPlayList.indexOf(curPlaylistBean);
        }

        switch(mReplayMode)
        {
            case MEDIA_REPLAY_ALL:
            case MEDIA_REPLAY_ONE:
                if (forward)
                {
                    mPlayPosition++;
                    if (mPlayPosition >= mPlayList.size()) {
                        mPlayPosition = 0;
                    }
                }
                else
                {
                    mPlayPosition--;
                    if (mPlayPosition<0) {
                        mPlayPosition = mPlayList.size()-1;
                    }
                }
                break;

            case MEDIA_REPLAY_SHUFFLE:
                mPlayPosition = getRandomNum(mPlayList.size());
                break;
        }

        return mPlayPosition;
    }

    private int getRandomNum(int endNum){
        if(endNum > 0){
            Random random = new Random();
            return random.nextInt(endNum);
        }
        return 0;
    }

}

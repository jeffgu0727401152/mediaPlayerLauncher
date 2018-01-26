package com.whiteskycn.tv.projectorlauncher.media;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean;
import com.whiteskycn.tv.projectorlauncher.media.bean.PlayListBean;
import com.whiteskycn.tv.projectorlauncher.utils.ToastUtil;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.widget.AdapterView.INVALID_POSITION;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_IDLE;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PAUSE_PICTURE;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PAUSE_VIDEO;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PLAY_COMPLETE;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PLAY_PICTURE;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaPlayState.MEDIA_PLAY_VIDEO;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaReplayMode.MEDIA_REPLAY_ALL;
import static com.whiteskycn.tv.projectorlauncher.media.PictureVideoPlayer.MediaReplayMode.MEDIA_REPLAY_ONE;
import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.MEDIA_PICTURE;
import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.MEDIA_UNKNOWN;
import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.MEDIA_VIDEO;

/**
 * Created by jeff on 18-1-22.
 */

public class PictureVideoPlayer {
    private final String TAG = this.getClass().getSimpleName();
    private final int mUpdateSeekBarThreadSleep_ms = 300;
    public static final int PICTURE_DEFAULT_PLAY_DURATION_MS = 10000;

    private SurfaceView mSurfaceView;
    private ImageView mPictureView;
    private Activity mAttachActivity;

    private List<PlayListBean> mPlayList;

    private MediaPlayer mMediaPlayer;

    ExecutorService mSeekBarUpdateService;

    private int mPlayPosition;

    private boolean mIsPicturePause  = false;
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

    public void setPlayState(MediaPlayState playState) {
        this.mPlayState = playState;
    }

    private MediaPlayState mPlayState = MEDIA_IDLE;

//重放模式
    public enum MediaReplayMode
    {
        MEDIA_REPLAY_ALL,
        MEDIA_REPLAY_ONE,
        MEDIA_REPLAY_SHUFFLE,
    };

    public MediaReplayMode getReplayMode() {
        return mReplayMode;
    }

    public void setReplayMode(MediaReplayMode mode) {
        this.mReplayMode = mode;
    }
    private MediaReplayMode mReplayMode = MEDIA_REPLAY_ALL;


//播放完成回调
    public interface OnMediaEventListener
    {
        void onMediaCompletion();
        void onMediaSeekComplete();
        void onMediaPlayError();
        void onMediaDurationSet(int msec);
        void onMediaUpdateSeekBar(int msec);
    }

    public void setOnMediaEventListener(OnMediaEventListener listener)
    {
        mOnMediaEventListener = listener;
    }

    private OnMediaEventListener mOnMediaEventListener;

//构造函数
    public PictureVideoPlayer(Activity attach,
            SurfaceView videoView,
            ImageView pictureView,
            List<PlayListBean> list) {
        mAttachActivity = attach;
        mSurfaceView = videoView;
        mPictureView = pictureView;
        mPlayState = MEDIA_IDLE;
        mReplayMode = MEDIA_REPLAY_ALL;
        mPlayList = list;
        mMediaPlayer = new MediaPlayer();
        mSeekBarUpdateService = Executors.newSingleThreadExecutor();

        mIsPicturePause  = false;
        mPicturePlayStartTime = 0;
        mPictureLastUpdateSeekBarTime = 0;
        mPicturePlayedTime = 0;
    }

//媒体播放控制
    public void mediaStop()
    {
        if (mPlayState.equals(MEDIA_PLAY_PICTURE))
        {
            pictureStop();
        }
        else if (mPlayState.equals(MEDIA_PLAY_VIDEO))
        {
            videoStop();
        }

    }

    public void mediaPlay(int position)
    {
        // 获取视频文件地址
        String path="";
        int type = MEDIA_UNKNOWN;
        int time = PICTURE_DEFAULT_PLAY_DURATION_MS; //播放图片用
        if(position!=INVALID_POSITION && position<mPlayList.size())
        {
            RawMediaBean fileBean = mPlayList.get(position).getMediaData();
            if (fileBean!=null)
            {
                path = fileBean.getFilePath();
                type = fileBean.getType();
                time = fileBean.getDuration();
            }
        }

        if (path.isEmpty())
        {
            Log.i(TAG,"******videoPlay: path null");
            path = "/mnt/sdcard/Movies/LaLaLa.mkv";
            type = MEDIA_VIDEO;
            //todo error handle
        }

        ToastUtil.showToast(mAttachActivity, "play path="+path + ": pos="+position +  ": time="+time);

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
        if (!mReplayMode.equals(MEDIA_REPLAY_ONE)) {
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
        mAttachActivity = null;
        mSurfaceView = null;
        mPictureView = null;
        mOnMediaEventListener = null;
        mPlayState = MEDIA_IDLE;
        mReplayMode = MEDIA_REPLAY_ALL;
        mPlayList = null;
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }

        if (mSeekBarUpdateService!=null) {
            mSeekBarUpdateService.shutdown();
            mSeekBarUpdateService = null;
        }

        mIsPicturePause  = false;
        mPicturePlayedTime = 0;
        mPicturePlayStartTime = 0;
        mPictureLastUpdateSeekBarTime = 0;
    }

    private void videoStop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
    }

    private void videoPlay(String path) {
        mPictureView.setVisibility(View.INVISIBLE);
        mSurfaceView.setVisibility(View.VISIBLE);
        File file = new File(path);
        if (!file.exists()) {
            ToastUtil.showToast(mAttachActivity, "视频文件路径错误");
            return;
        }

        try {
            mMediaPlayer.reset();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDisplay(mSurfaceView.getHolder());
            mMediaPlayer.setDataSource(file.getAbsolutePath());
            Log.i(TAG,"开始装载");
            mMediaPlayer.prepareAsync();

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.i(TAG, "装载完成");
                    // 按照初始位置播放
                    mPlayState = MEDIA_PLAY_VIDEO;
                    mMediaPlayer.seekTo(0);
                    mMediaPlayer.start();
                    // 设置进度条的最大进度为视频流的最大播放时长
                    if(mOnMediaEventListener !=null)
                    {
                        mOnMediaEventListener.onMediaDurationSet(mMediaPlayer.getDuration());
                    }

                    mSeekBarUpdateService.execute(                    // 开线程更新进度条的刻度
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    while (mMediaPlayer!=null && mMediaPlayer.isPlaying()) {
                                        int current = mMediaPlayer.getCurrentPosition();
                                        if (mOnMediaEventListener!=null)
                                        {
                                            mOnMediaEventListener.onMediaUpdateSeekBar(current);
                                        }
                                        sleep(mUpdateSeekBarThreadSleep_ms);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "error mSeekBarUpdateService execute!", e);
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
                        mOnMediaEventListener.onMediaCompletion();
                    }
                }
            });

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // 发生错误停止播放
                    mMediaPlayer.reset();
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
                    if (mOnMediaEventListener!=null)
                    {
                        mOnMediaEventListener.onMediaSeekComplete();
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
            ToastUtil.showToast(mAttachActivity, "暂停播放");
        }
    }

    private void videoResume() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            mPlayState = MEDIA_PLAY_VIDEO;
            ToastUtil.showToast(mAttachActivity, "继续播放");
        }
    }

    private void pictureStop()
    {
        mPlayState = MEDIA_PLAY_COMPLETE;
        mIsPicturePause = false;
        mPicturePlayedTime = 0;
    }

    private void picturePlay(String path, int duration)
    {
        File file = new File(path);
        if (!file.exists()) {
            ToastUtil.showToast(mAttachActivity, "图片文件路径错误");
            return;
        }
        mPictureView.setVisibility(View.VISIBLE);
        mSurfaceView.setVisibility(View.INVISIBLE);

        final int playDuration = duration;
        mPlayState = MEDIA_PLAY_PICTURE;
        mPictureView.setBackground(Drawable.createFromPath(path));

        if(mOnMediaEventListener !=null)
        {//todo play picture set the real time
            mOnMediaEventListener.onMediaDurationSet(playDuration);
        }

        mPlayState = MEDIA_PLAY_PICTURE;
        mIsPicturePause = false;
        mPicturePlayedTime = 0;
        mPicturePlayStartTime = System.currentTimeMillis();
        mPictureLastUpdateSeekBarTime = mPicturePlayStartTime;

        mSeekBarUpdateService.execute(new Thread() {
            @Override
            public void run() {
                try {
                    while (mPlayState.equals(MEDIA_PLAY_PICTURE) || mPlayState.equals(MEDIA_PAUSE_PICTURE)) {
                        long current = System.currentTimeMillis();
                        if (!mIsPicturePause) {
                            mPicturePlayedTime += Math.abs(current - mPictureLastUpdateSeekBarTime);
                        }
                        mPictureLastUpdateSeekBarTime = current;

                        if (mPicturePlayedTime>playDuration)
                        {//todo play picture set the real time
                            mIsPicturePause = false;
                            mPicturePlayedTime = 0;

                            mPlayState = MEDIA_PLAY_COMPLETE;
                            if (mOnMediaEventListener!=null)
                            {
                                mOnMediaEventListener.onMediaCompletion();
                            }
                        }

                        if (mOnMediaEventListener!=null)
                        {
                            mOnMediaEventListener.onMediaUpdateSeekBar((int)mPicturePlayedTime);
                        }
                        sleep(mUpdateSeekBarThreadSleep_ms);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "error mSeekBarUpdateService execute!", e);
                }
            }
        });
    }

    private void picturePause()
    {
        if (!mIsPicturePause) {
            mIsPicturePause = true;
            mPlayState = MEDIA_PAUSE_PICTURE;
            ToastUtil.showToast(mAttachActivity, "暂停播放");
        }
    }

    private void pictureResume()
    {
        if (mIsPicturePause)
        {
            mPlayState = MEDIA_PLAY_PICTURE;
            mIsPicturePause = false;
            ToastUtil.showToast(mAttachActivity, "继续播放");
        }
    }

    private int updatePlayPosition(boolean forward)
    {
        if (mPlayList.size()==0)
        {
            mPlayPosition = INVALID_POSITION;
            return mPlayPosition;
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

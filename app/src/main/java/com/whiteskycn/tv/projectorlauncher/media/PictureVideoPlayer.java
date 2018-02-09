package com.whiteskycn.tv.projectorlauncher.media;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.media.db.MediaBean;
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
import static com.whiteskycn.tv.projectorlauncher.media.db.MediaBean.MEDIA_PICTURE;
import static com.whiteskycn.tv.projectorlauncher.media.db.MediaBean.MEDIA_UNKNOWN;
import static com.whiteskycn.tv.projectorlauncher.media.db.MediaBean.MEDIA_VIDEO;
import static com.whiteskycn.tv.projectorlauncher.utils.FileUtil.getFileSize;

/**
 * Created by jeff on 18-1-22.
 */

public class PictureVideoPlayer {
    private final String TAG = this.getClass().getSimpleName();

    public static final int MEDIA_SCALE_16_9 = 0;
    public static final int MEDIA_SCALE_4_3 = 1;
    public static final int MEDIA_SCALE_1_1 = 2;

    private final int mUpdateSeekBarThreadSleep_ms = 300;
    public static final int PICTURE_DEFAULT_PLAY_DURATION_MS = 10000;

    private SurfaceView mSurfaceView;
    private ImageView mPictureView;
    private Activity mAttachActivity;

    private List<PlayListBean> mPlayList;

    private MediaPlayer mMediaPlayer;
    private MediaMetadataRetriever mMediaMetadataRetriever;

    ExecutorService mSeekBarUpdateService;

    private int mPlayPosition;

    private boolean mIsPreview = false;
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

    private MediaPlayState mPlayState = MEDIA_IDLE;

    public int getPlayPosition() {
        return mPlayPosition;
    }

    public boolean isPreview() {
        return mIsPreview;
    }

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
        void onMediaPlayStop();
        void onMediaPlayCompletion();
        void onMediaSeekComplete();
        void onMediaPlayError();
        void onMediaInfoUpdate(String name, String mimeType, int width, int height, long size, int bps);
        void onMediaDurationSet(int msec);
        void onMediaUpdateSeekBar(int msec);
    }

    public void setOnMediaEventListener(OnMediaEventListener listener)
    {
        mOnMediaEventListener = listener;
    }

    private OnMediaEventListener mOnMediaEventListener;

    // 构造函数
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
        mMediaMetadataRetriever = new MediaMetadataRetriever();
        mSeekBarUpdateService = Executors.newSingleThreadExecutor();

        mIsPicturePause  = false;
        mPicturePlayStartTime = 0;
        mPictureLastUpdateSeekBarTime = 0;
        mPicturePlayedTime = 0;
    }

    // 媒体播放控制
    public void mediaPreview(MediaBean mPreviewItem)
    {
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
            ToastUtil.showToast(mAttachActivity, R.string.str_media_play_path_error);
            mPlayState = MEDIA_IDLE;
            if (mOnMediaEventListener!=null)
            {
                mOnMediaEventListener.onMediaPlayError();
            }
            return;
        }

        mIsPreview = true;

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
        if (mPlayState.equals(MEDIA_PLAY_PICTURE))
        {
            pictureStop();
        }
        else if (mPlayState.equals(MEDIA_PLAY_VIDEO))
        {
            videoStop();
        }

        if (mOnMediaEventListener!=null)
        {
            mOnMediaEventListener.onMediaPlayStop();
        }
    }

    public void mediaPlay(int position)
    {
        String path="";
        int type = MEDIA_UNKNOWN;
        int time = PICTURE_DEFAULT_PLAY_DURATION_MS;
        int scale = MEDIA_SCALE_16_9;
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
            ToastUtil.showToast(mAttachActivity, R.string.str_media_play_list_empty);
            mPlayState = MEDIA_IDLE;
            if (mOnMediaEventListener!=null)
            {
                mOnMediaEventListener.onMediaPlayError();
            }
            return;
        }

        if (path.isEmpty())
        {
            ToastUtil.showToast(mAttachActivity, R.string.str_media_play_path_error);
            mPlayState = MEDIA_IDLE;
            if (mOnMediaEventListener!=null)
            {
                mOnMediaEventListener.onMediaPlayError();
            }
            return;
        }

        mIsPreview = false;
        mPlayPosition = position;

        //ToastUtil.showToast(mAttachActivity, "play path=" + path + ", pos=" + position);

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

        if (mMediaMetadataRetriever!=null) {
            mMediaMetadataRetriever.release();
            mMediaMetadataRetriever = null;
        }

        if (mSeekBarUpdateService!=null) {
            mSeekBarUpdateService.shutdownNow();
            mSeekBarUpdateService = null;
        }

        mIsPicturePause  = false;
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
        videoPlay(path, MEDIA_SCALE_16_9);
    }

    private void videoPlay(String path, int scale) {
        mPictureView.setVisibility(View.INVISIBLE);
        mSurfaceView.setVisibility(View.VISIBLE);

        // todo 根据scale调整surfaceview尺寸

        File file = new File(path);
        if (!file.exists()) {
            ToastUtil.showToast(mAttachActivity, R.string.str_media_play_path_error);
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

            long size = getFileSize(file);

            if(mOnMediaEventListener !=null)
            {
                mOnMediaEventListener.onMediaInfoUpdate(file.getName(),
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
                    if(mOnMediaEventListener !=null)
                    {
                        mOnMediaEventListener.onMediaDurationSet(mMediaPlayer.getDuration());
                    }

                    mSeekBarUpdateService.execute(                    // 开线程更新进度条的刻度
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    while ((mPlayState.equals(MEDIA_PLAY_VIDEO) || mPlayState.equals(MEDIA_PLAY_VIDEO))
                                            && mMediaPlayer!=null && mMediaPlayer.isPlaying()) {
                                        int current = mMediaPlayer.getCurrentPosition();
                                        if (mOnMediaEventListener!=null)
                                        {
                                            mOnMediaEventListener.onMediaUpdateSeekBar(current);
                                        }
                                        sleep(mUpdateSeekBarThreadSleep_ms);
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
            ToastUtil.showToast(mAttachActivity, R.string.str_media_play_pause);
        }
    }

    private void videoResume() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            mPlayState = MEDIA_PLAY_VIDEO;
            ToastUtil.showToast(mAttachActivity, R.string.str_media_play_resume);
        }
    }

    private void pictureStop()
    {
        mPlayState = MEDIA_IDLE;
        mIsPicturePause = false;
        mPicturePlayedTime = 0;
    }

    private void picturePlay(String path, int duration)
    {
        picturePlay(path, duration, MEDIA_SCALE_16_9);
    }

    private void picturePlay(String path, int duration, int scale)
    {
        File file = new File(path);
        if (!file.exists()) {
            ToastUtil.showToast(mAttachActivity, R.string.str_media_play_path_error);
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
        long size = getFileSize(file);

        if(mOnMediaEventListener !=null)
        {
            mOnMediaEventListener.onMediaInfoUpdate(file.getName(),
                    mimeType, options.outWidth, options.outHeight, size,
                    -1);
        }

        final int playDuration = duration;
        mPlayState = MEDIA_PLAY_PICTURE;

        switch(scale)
        {
            case MEDIA_SCALE_16_9:
                mPictureView.setScaleType(ImageView.ScaleType.FIT_XY);
                break;
            case MEDIA_SCALE_4_3:
                mPictureView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                break;
            case MEDIA_SCALE_1_1:
                mPictureView.setScaleType(ImageView.ScaleType.CENTER);
                break;
            default:
                mPictureView.setScaleType(ImageView.ScaleType.FIT_XY);
                break;
        }

        mPictureView.setImageDrawable(Drawable.createFromPath(path));

        if(mOnMediaEventListener !=null)
        {
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
                        {
                            mIsPicturePause = false;
                            mPicturePlayedTime = 0;

                            mPlayState = MEDIA_PLAY_COMPLETE;
                            if (mOnMediaEventListener!=null)
                            {
                                mOnMediaEventListener.onMediaPlayCompletion();
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
            ToastUtil.showToast(mAttachActivity, R.string.str_media_play_pause);
        }
    }

    private void pictureResume()
    {
        if (mIsPicturePause)
        {
            mPlayState = MEDIA_PLAY_PICTURE;
            mIsPicturePause = false;
            ToastUtil.showToast(mAttachActivity, R.string.str_media_play_resume);
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
                int oldPos = mPlayPosition;
                if (mPlayList.size()>2) {
                    // 随机出来的值不能是正在播放的
                    while(mPlayPosition == oldPos) {
                        mPlayPosition = getRandomNum(mPlayList.size());
                    }
                } else {
                    mPlayPosition = getRandomNum(mPlayList.size());
                }
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

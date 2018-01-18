package com.whiteskycn.tv.projectorlauncher.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.home.HomeActivity;
import com.whiteskycn.tv.projectorlauncher.media.adapter.AllMediaListAdapter;
import com.whiteskycn.tv.projectorlauncher.media.adapter.PlayListAdapter;
import com.whiteskycn.tv.projectorlauncher.media.bean.MediaListBean;
import com.whiteskycn.tv.projectorlauncher.media.bean.PlayListBean;
import com.whiteskycn.tv.projectorlauncher.utils.MediaFileScanUtil;
import com.whiteskycn.tv.projectorlauncher.utils.ToastUtil;
import com.whiteskycn.tv.projectorlauncher.utils.ViewUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class MediaActivity extends Activity implements View.OnClickListener
{
    private final String TAG = this.getClass().getSimpleName();

    private final int MSG_UPDATE_PLAYEDTIME = 1;
    private final int MSG_UPDATE_DURATIONTIME = 2;

    private final int MSG_ADD_TO_MEDIA_LIST = 10;

    private MediaFileScanUtil mMediaScanner;
    private MediaPlayer mMediaPlayer;
    private SurfaceView mVideoPlaySurfaceView;
    private ImageView mPicturePlayView;

    private Button mNetViewBtn;
    private Button mGrayViewBtn;

    private Button mReplayOneBtn;
    private Button mReplayAllBtn;
    private Button mReplayListBtn;
    private Button mReplayShuffleBtn;

    private Button mPreviousBtn;
    private Button mPlayBtn;
    private Button mNextBtn;
    private Button mVolumeBtn;
    private SeekBar mPlayProgressSeekBar;
    private TextView mMediaPlayedTimeTextView;
    private TextView mMediaDurationTimeTextView;

    private int mOriginSurfaceHeight = 0;
    private int mOriginSurfaceWidth = 0;

    private boolean mIsProgressBarChangingByUser = false;

    private int mCurrentPosition = 0;
    private boolean mIsPlaying;
    private boolean mIsFullScreen;

    private List<PlayListBean> mPlayListBeans = new ArrayList<PlayListBean>();
    private PlayListAdapter mPlayListAdapter;
    private DragListView mDragPlayListView;

    private List<MediaListBean> mAllMediaListBeans = new ArrayList<MediaListBean>();
    private AllMediaListAdapter mAllMediaListAdapter;
    private ListView mLvAllMediaList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

        mIsPlaying = false;
        mIsFullScreen = false;

        mVideoPlaySurfaceView = (SurfaceView)findViewById(R.id.sv_media_playVideo);
        mPicturePlayView = (ImageView)findViewById(R.id.iv_media_playPicture);

        mNetViewBtn = (Button)findViewById(R.id.btn_media_netView);
        mGrayViewBtn = (Button)findViewById(R.id.btn_media_grayView);

        mReplayOneBtn = (Button)findViewById(R.id.bt_media_replayOne);
        mReplayAllBtn =  (Button)findViewById(R.id.bt_media_replayAll);
        mReplayListBtn = (Button)findViewById(R.id.bt_media_replayList);
        mReplayShuffleBtn =  (Button)findViewById(R.id.bt_media_replayShuffle);

        mPlayBtn = (Button)findViewById(R.id.bt_media_play);
        mPreviousBtn = (Button)findViewById(R.id.bt_media_playPrevious);
        mNextBtn = (Button)findViewById(R.id.bt_media_playNext);
        mVolumeBtn =  (Button)findViewById(R.id.bt_media_volume);
        mPlayProgressSeekBar = (SeekBar)findViewById(R.id.sb_media_playProgress);

        mMediaPlayedTimeTextView = (TextView)findViewById(R.id.tv_media_playedTime);
        mMediaDurationTimeTextView = (TextView)findViewById(R.id.tv_media_durationTime);

        mPreviousBtn.setOnClickListener(this);
        mPlayBtn.setOnClickListener(this);
        mNextBtn.setOnClickListener(this);
        mVolumeBtn.setOnClickListener(this);

        mVideoPlaySurfaceView.getHolder().addCallback(svCallback);
        mPlayProgressSeekBar.setOnSeekBarChangeListener(mSeekbarChange);

        ViewGroup.LayoutParams lp = mVideoPlaySurfaceView.getLayoutParams();
        mOriginSurfaceHeight = lp.height;
        mOriginSurfaceWidth = lp.width;

        //设置播放时长为0
        setMediaPlayedTimeTextView(0);
        setMediaDurationTimeTextView(0);

        //云端本地全媒体列表
        mLvAllMediaList = (ListView)findViewById(R.id.lv_media_all_list);
        mAllMediaListAdapter = new AllMediaListAdapter(getApplicationContext(), mAllMediaListBeans);
        mLvAllMediaList.setAdapter(mAllMediaListAdapter);
        mLvAllMediaList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Logger.d("position = " + position + "; id = " + id);
                showInfo();
            }
        });

        //播放列表设置
        mDragPlayListView = (DragListView) findViewById(R.id.lv_media_playList);
        mPlayListAdapter = new PlayListAdapter(getApplicationContext(), mPlayListBeans);
        mDragPlayListView.setAdapter(mPlayListAdapter);
        mDragPlayListView.setDragItemListener(new DragListView.SimpleAnimationDragItemListener() {

            private Rect mFrame = new Rect();
            private boolean mIsSelected;

            @Override
            public boolean canDrag(View dragView, int x, int y) {
                // 获取可拖拽的图标
                View dragger = dragView.findViewById(R.id.iv_media_tab);
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
                View drag = dragView.findViewById(R.id.iv_media_tab);
                dragView.setSelected(true);
                if (drag != null) {
                    drag.setSelected(true);
                }
            }

            @Override
            public Bitmap afterDrawingCache(View dragView, Bitmap bitmap) {
                dragView.setSelected(mIsSelected);
                View drag = dragView.findViewById(R.id.iv_media_tab);
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

        mMediaScanner = new MediaFileScanUtil();
        mMediaScanner.setmMediaFileScanListener(new MediaFileScanUtil.MediaFileScanListener() {
            @Override
            public void onFindMedia(MediaFileScanUtil.MediaType type, String name, String extension, String path) {
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_ADD_TO_MEDIA_LIST;
                msg.obj = name;
                mHandler.sendMessage(msg);
            }
        });
        mMediaScanner.safeScanning("/mnt/sdcard");
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what)
            {
                case MSG_UPDATE_DURATIONTIME:
                    setTimeTextView(mMediaDurationTimeTextView,msg.arg1);
                    break;
                case MSG_UPDATE_PLAYEDTIME:
                    setTimeTextView(mMediaPlayedTimeTextView,msg.arg1);
                    break;
                case MSG_ADD_TO_MEDIA_LIST:
                    String name = (String) msg.obj;
                    mAllMediaListBeans.add(new MediaListBean(name, "歌曲", 1, 0));
                    mPlayListAdapter.refresh();
                    break;
            }
        }
    };


    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.bt_media_play:
                if (!mIsPlaying) {
                    mediaPlay(0);
                } else {
                    mediaPause();
                }
                break;
            case R.id.bt_media_playNext:
                break;
            case R.id.bt_media_playPrevious:
                break;
            case R.id.bt_media_volume:
                LinearLayout controlBarLayout = (LinearLayout)findViewById(R.id.ll_media_playControlBar);
                controlBarLayout.setVisibility(View.INVISIBLE);
                ViewGroup.LayoutParams lp = mVideoPlaySurfaceView.getLayoutParams();
                lp.height = R.dimen.x1920;
                lp.width = R.dimen.x1080;
                mVideoPlaySurfaceView.setLayoutParams(lp);
                mIsFullScreen = true;
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

        if (mPlayListBeans.isEmpty()) {
            for (int i = 0; i < 10; i++) {
                mPlayListBeans.add(new PlayListBean(String.valueOf(i), "歌曲", i%2, 0));
            }
        }

        if (mAllMediaListBeans.isEmpty()) {
            for (int i = 0; i < 10; i++) {
                mAllMediaListBeans.add(new MediaListBean(String.valueOf(i), "歌曲", i%2, 0));
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        mediaStop();
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
                            LinearLayout controlBarLayout = (LinearLayout)findViewById(R.id.ll_media_playControlBar);
                            controlBarLayout.setVisibility(View.VISIBLE);
                            ViewGroup.LayoutParams lp = mVideoPlaySurfaceView.getLayoutParams();
                            lp.width = mOriginSurfaceWidth;
                            lp.height = mOriginSurfaceHeight;
                            mVideoPlaySurfaceView.setLayoutParams(lp);
                            mIsFullScreen = false;
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
            // 销毁SurfaceHolder的时候记录当前的播放位置并停止播放
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mCurrentPosition = mMediaPlayer.getCurrentPosition();
                mMediaPlayer.stop();
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, "SurfaceHolder 被创建");
            if (mCurrentPosition > 0) {
                // 创建SurfaceHolder的时候，如果存在上次播放的位置，则按照上次播放位置进行播放
                mediaPlay(mCurrentPosition);
                mCurrentPosition = 0;
            }
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
            if (mMediaPlayer != null) {
                // 设置当前播放的位置
                mMediaPlayer.seekTo(progress);
            }
            mIsProgressBarChangingByUser = false;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                setMediaPlayedTimeTextView(progress);
                mIsProgressBarChangingByUser = true;
            }
        }
    };

    /*
    * 停止播放
     */
    protected void mediaStop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mIsPlaying = false;
        }
    }

    /**
     * 开始播放
     *
     * @param msec 播放初始位置
     */
    protected void mediaPlay(final int msec) {
        // 获取视频文件地址
        String path = "/mnt/sdcard/Movies/LaLaLa.mkv";
        File file = new File(path);
        if (!file.exists()) {
            ToastUtil.showToast(this, "视频文件路径错误");
            return;
        }
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // 设置播放的视频源
            mMediaPlayer.setDataSource(file.getAbsolutePath());
            // 设置显示视频的SurfaceHolder
            mMediaPlayer.setDisplay(mVideoPlaySurfaceView.getHolder());
            Log.i(TAG, "开始装载");
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.i(TAG, "装载完成");
                    mMediaPlayer.start();
                    // 按照初始位置播放
                    mMediaPlayer.seekTo(msec);

                    // 设置进度条的最大进度为视频流的最大播放时长
                    mPlayProgressSeekBar.setMax(mMediaPlayer.getDuration());
                    setMediaDurationTimeTextView(mMediaPlayer.getDuration());

                    mIsPlaying = true;
                    mPlayBtn.setBackgroundResource(R.drawable.img_media_pause);

                    // 开始线程，更新进度条的刻度
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                while (mIsPlaying) {
                                    int current = mMediaPlayer
                                            .getCurrentPosition();
                                    if (!mIsProgressBarChangingByUser) {
                                        mPlayProgressSeekBar.setProgress(current);
                                        setMediaPlayedTimeTextView(current);
                                    }
                                    sleep(500);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
            });
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    // 在播放完毕被回调
                    mPlayBtn.setBackgroundResource(R.drawable.img_media_play);
                }
            });

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // 发生错误停止播放
                    mediaStop();
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 重新开始播放
     */
    protected void mediaReplay() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.seekTo(0);
            ToastUtil.showToast(this, "重新播放");
            mPlayBtn.setText("暂停");
            return;
        }
        mIsPlaying = false;
        mediaPlay(0);
    }

    /**
     * 暂停或继续
     */
    protected void mediaPause() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mPlayBtn.setBackgroundResource(R.drawable.img_media_pause);
            mMediaPlayer.start();
            ToastUtil.showToast(this, "继续播放");
        } else if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mPlayBtn.setBackgroundResource(R.drawable.img_media_play);
            mMediaPlayer.pause();
            ToastUtil.showToast(this, "暂停播放");
        }
    }

    //只能在主线程调用
    private void setTimeTextView(TextView tv,int milliseconds)
    {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(milliseconds);
        tv.setText(hms);
    }

    private void setMediaDurationTimeTextView(int milliseconds)
    {
        if (Looper.myLooper() == Looper.getMainLooper()) { // UI主线程
            setTimeTextView(mMediaDurationTimeTextView,milliseconds);
        } else { // 非UI主线程
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_UPDATE_DURATIONTIME;
            msg.arg1 = milliseconds;
            mHandler.sendMessage(msg);
        }
    }

    private void setMediaPlayedTimeTextView(int milliseconds)
    {
        if (Looper.myLooper() == Looper.getMainLooper()) { // UI主线程
            setTimeTextView(mMediaPlayedTimeTextView,milliseconds);
        } else { // 非UI主线程
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_UPDATE_PLAYEDTIME;
            msg.arg1 = milliseconds;
            mHandler.sendMessage(msg);
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

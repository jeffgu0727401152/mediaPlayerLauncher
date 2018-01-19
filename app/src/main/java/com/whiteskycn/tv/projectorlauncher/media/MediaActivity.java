package com.whiteskycn.tv.projectorlauncher.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
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
import com.whiteskycn.tv.projectorlauncher.media.bean.MediaFileBean;
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

import static android.widget.AdapterView.INVALID_POSITION;

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

    private final int MSG_MEDIA_PLAY_COMPLETE = 100;

    private MediaFileScanUtil mMediaScanner;
    private MediaPlayer mMediaPlayer;
    private SurfaceView mVideoPlaySurfaceView;
    private ImageView mPicturePlayView;

    private Button mNetViewBtn;
    private Button mGrayViewBtn;

    private Button mPreviousBtn;
    private Button mPlayBtn;
    private Button mNextBtn;
    private Button mVolumeBtn;
    private SeekBar mPlayProgressSeekBar;
    private TextView mMediaPlayedTimeTextView;
    private TextView mMediaDurationTimeTextView;

    private int mOriginSurfaceHeight = 0;
    private int mOriginSurfaceWidth = 0;

    private boolean mDontUpdateSeekBar = false;

    private int mPlayPosition = INVALID_POSITION;
    private boolean mIsFullScreen;

    private boolean mNeedPause = false;
    private boolean mIsPlaying = false;

    private boolean mIsPicturePlaying;
    private boolean mIsPicturePause  = false;
    private long mPicturePlayStartTime = 0;
    private long mPictureLastUpdateSeekBarTime = 0;
    private long mPicturePlayedTime = 0;

    private int mLastAllMediaListClickPosition = 0;
    private long mLastAllMediaListClickTime;

    private long mLastFullScreenClickTime;

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

        mIsPicturePlaying = false;
        mIsFullScreen = false;

        mVideoPlaySurfaceView = (SurfaceView)findViewById(R.id.sv_media_playVideo);
        mPicturePlayView = (ImageView)findViewById(R.id.iv_media_playPicture);

        mNetViewBtn = (Button)findViewById(R.id.btn_media_netView);
        mGrayViewBtn = (Button)findViewById(R.id.btn_media_grayView);

        mPlayBtn = (Button)findViewById(R.id.bt_media_play);
        mPreviousBtn = (Button)findViewById(R.id.bt_media_playPrevious);
        mNextBtn = (Button)findViewById(R.id.bt_media_playNext);
        mVolumeBtn =  (Button)findViewById(R.id.bt_media_volume);
        mPlayProgressSeekBar = (SeekBar)findViewById(R.id.sb_media_playProgress);

        mMediaPlayedTimeTextView = (TextView)findViewById(R.id.tv_media_playedTime);
        mMediaDurationTimeTextView = (TextView)findViewById(R.id.tv_media_durationTime);

        mPicturePlayView.setOnClickListener(this);
        mVideoPlaySurfaceView.setOnClickListener(this);
        mPreviousBtn.setOnClickListener(this);
        mPlayBtn.setOnClickListener(this);
        mNextBtn.setOnClickListener(this);
        mVolumeBtn.setOnClickListener(this);

        mVideoPlaySurfaceView.getHolder().addCallback(svCallback);
        mPlayProgressSeekBar.setOnSeekBarChangeListener(mSeekbarChange);

        ViewGroup.LayoutParams lp = mVideoPlaySurfaceView.getLayoutParams();
        mOriginSurfaceHeight = lp.height;
        mOriginSurfaceWidth = lp.width;

        //设置播放时长为00:00:00
        setMediaPlayedTimeTextView(0);
        setMediaDurationTimeTextView(0);

        //云端本地的全媒体列表
        mLvAllMediaList = (ListView)findViewById(R.id.lv_media_all_list);
        mAllMediaListAdapter = new AllMediaListAdapter(getApplicationContext(), mAllMediaListBeans);
        mLvAllMediaList.setAdapter(mAllMediaListAdapter);
        mLvAllMediaList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 如果是双击,1秒内连续点击判断为双击
                if(position == mLastAllMediaListClickPosition
                        && (Math.abs(mLastAllMediaListClickTime-System.currentTimeMillis()) < 800)){
                    mLastAllMediaListClickPosition = -1;
                    mLastAllMediaListClickTime = 0;
                    addToPlayList(position);
                }else {
                    Logger.d("position = " + position + "; id = " + id);
                    mLastAllMediaListClickPosition = position;
                    mLastAllMediaListClickTime = System.currentTimeMillis();
                }
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

        //递归扫描sd卡根目录
        mMediaScanner = new MediaFileScanUtil();
        mMediaScanner.setmMediaFileScanListener(new MediaFileScanUtil.MediaFileScanListener() {
            @Override
            public void onFindMedia(MediaFileScanUtil.MediaTypeEnum type, String name, String extension, String path) {
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_ADD_TO_MEDIA_LIST;
                Bundle b = new Bundle();
                b.putInt("type", type.ordinal());
                b.putString("name", name);
                b.putString("path", path);
                msg.setData(b);
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
                    Bundle b = msg.getData();
                    int type = b.getInt("type");
                    String name = b.getString("name");
                    String path = b.getString("path");

                    MediaFileBean.MediaTypeEnum mediaType;
                    if (type== MediaFileScanUtil.MediaTypeEnum.VIDEO.ordinal())
                    {
                        mediaType = MediaFileBean.MediaTypeEnum.VIDEO;
                    }
                    else if (type== MediaFileScanUtil.MediaTypeEnum.PICTURE.ordinal())
                    {
                        mediaType = MediaFileBean.MediaTypeEnum.PICTURE;
                    }
                    else
                    {
                        mediaType = MediaFileBean.MediaTypeEnum.UNKNOWN;
                    }
                    mAllMediaListBeans.add(new MediaListBean(new MediaFileBean("12345",name,mediaType,MediaFileBean.MediaSourceEnum.LOCAL,true,path)));
                    mAllMediaListAdapter.refresh();
                    break;
                case MSG_MEDIA_PLAY_COMPLETE:
                    mPlayBtn.setBackgroundResource(R.drawable.img_media_play);
                    mediaStop();
                    mediaPlay(updatePlayPosition(true));
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
                if((Math.abs(mLastFullScreenClickTime-System.currentTimeMillis()) < 800)){
                    mLastFullScreenClickTime = 0;
                    fullScreenDisplaySwitch();
                }else {
                    mLastFullScreenClickTime = System.currentTimeMillis();
                }
                break;
            case R.id.bt_media_play:
                if (!mIsPlaying)
                {
                    mPlayPosition = 0;
                    mediaPlay(mPlayPosition);
                }
                else
                {
                    mediaPause();
                }
                break;
            case R.id.bt_media_playNext:
                updatePlayPosition(true);
                mediaStop();
                mediaPlay(mPlayPosition);
                break;
            case R.id.bt_media_playPrevious:
                updatePlayPosition(false);
                mediaStop();
                mediaPlay(mPlayPosition);
                break;
            case R.id.bt_media_volume:
                mediaPause();
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
        mIsPlaying = false;
        videoDestroy();
        pictureStop();
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
            //videoDestroy();
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

            if (mIsPicturePlaying)
            {
                //todo seek picture
                return;
            }

            if (mMediaPlayer != null) {
                // 设置当前播放的位置
                if (!mMediaPlayer.isPlaying())
                {//如果没有在播放,默认seekTo调用后不更新surface上显示的图,这边取巧用播放暂停来更新
                    //todo 改进暂停时候的拖动,UI更新不及时
                    mMediaPlayer.start();
                    mNeedPause = true;
                }
                mMediaPlayer.seekTo(progress);
            }
            mDontUpdateSeekBar = false; //用户的拖动停止了,允许更新seekBar
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                setMediaPlayedTimeTextView(progress);
                mDontUpdateSeekBar = true; //在拖动的时候,需要防止播放开的线程来改变seekBar的位置
            }
        }
    };

    private void mediaStop()
    {
        if (mIsPicturePlaying)
        {
            pictureStop();
        }
        else
        {
            videoStop();
        }
    }

    private void mediaPlay(int position)
    {
        mIsPlaying = true;
        // 获取视频文件地址
        String path="";
        MediaFileBean.MediaTypeEnum type = MediaFileBean.MediaTypeEnum.UNKNOWN;
        if(position!=INVALID_POSITION && position<mPlayListBeans.size())
        {
            MediaFileBean fileBean = mPlayListBeans.get(position).getMediaData();
            if (fileBean!=null)
            {
                path = fileBean.getFilePath();
                type = fileBean.getType();
            }
        }

        Log.i(TAG, "!!!!!!!!~~~~~~~Play position" + position);

        if (path.isEmpty())
        {
            Log.i(TAG, "videoPlay: path null");
            path = "/mnt/sdcard/Movies/LaLaLa.mkv";
            type = MediaFileBean.MediaTypeEnum.VIDEO;
        }

        ToastUtil.showToast(this, "play path="+path + ": pos="+position);

        switch (type)
        {
            case VIDEO:
                pictureStop();
                videoPlay(path);
                break;
            case PICTURE:
                videoStop();
                picturePlay(path);
                break;
            default:
                break;
        }
    }

    private void mediaPause()
    {
        if (mIsPicturePlaying)
        {
            picturePause();
        }
        else
        {
            videoPause();
        }
    }

    private void videoStop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void videoDestroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void videoPlay(String path) {
        mPicturePlayView.setVisibility(View.INVISIBLE);
        mVideoPlaySurfaceView.setVisibility(View.VISIBLE);
        mIsPicturePlaying = false;
        File file = new File(path);
        if (!file.exists()) {
            ToastUtil.showToast(this, "视频文件路径错误");
            return;
        }

        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDisplay(mVideoPlaySurfaceView.getHolder());

            // 设置播放的视频源
            mMediaPlayer.setDataSource(file.getAbsolutePath());
            Log.i(TAG, "开始装载");
            mMediaPlayer.prepareAsync();

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.i(TAG, "装载完成");
                    mMediaPlayer.start();
                    // 按照初始位置播放
                    //mMediaPlayer.seekTo(0);

                    // 设置进度条的最大进度为视频流的最大播放时长
                    mPlayProgressSeekBar.setMax(mMediaPlayer.getDuration());
                    setMediaDurationTimeTextView(mMediaPlayer.getDuration());

                    mPlayBtn.setBackgroundResource(R.drawable.img_media_pause);

                    // 开线程更新进度条的刻度
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                while (mMediaPlayer!=null && mMediaPlayer.isPlaying()) {
                                    int current = mMediaPlayer.getCurrentPosition();
                                    if (!mDontUpdateSeekBar) {
                                        mPlayProgressSeekBar.setProgress(current);
                                        setMediaPlayedTimeTextView(current);
                                    }
                                    sleep(300);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                showInfo();
                            }
                        }
                    }.start();
                }
            });

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    // 在播放完毕被回调
                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_MEDIA_PLAY_COMPLETE;
                    mHandler.sendMessage(msg);
                }
            });

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // 发生错误停止播放
                    videoStop();
                    return false;
                }
            });

            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    if (mNeedPause == true) {
                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                mMediaPlayer.pause();
                            }
                        }, 250);
                        mMediaPlayer.pause();
                    }
                    mNeedPause = false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showInfo();
        }
    }

    private void videoPause() {
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

    private void pictureComplete()
    {
        mIsPicturePlaying = false;
        mIsPicturePause = false;
        mPicturePlayedTime = 0;

        Message msg = mHandler.obtainMessage();
        msg.what = MSG_MEDIA_PLAY_COMPLETE;
        mHandler.sendMessage(msg);
    }

    private void pictureStop()
    {
        mPlayBtn.setBackgroundResource(R.drawable.img_media_play);
        mIsPicturePlaying = false;
        mIsPicturePause = false;
        mPicturePlayedTime = 0;
    }

    private void picturePlay(String path)
    {
        File file = new File(path);
        if (!file.exists()) {
            ToastUtil.showToast(this, "图片文件路径错误");
            return;
        }
        mPicturePlayView.setVisibility(View.VISIBLE);
        mVideoPlaySurfaceView.setVisibility(View.INVISIBLE);
        mPlayBtn.setBackgroundResource(R.drawable.img_media_pause);

        mPicturePlayView.setBackground(Drawable.createFromPath(path));
        mPlayProgressSeekBar.setMax(4000);
        setMediaDurationTimeTextView(4000);//todo play time

        mIsPicturePlaying = true;
        mIsPicturePause = false;
        mPicturePlayedTime = 0;
        mPicturePlayStartTime = System.currentTimeMillis();
        mPictureLastUpdateSeekBarTime = mPicturePlayStartTime;
        // 开线程更新进度条的刻度
        new Thread() {
            @Override
            public void run() {
                try {
                    while (mIsPicturePlaying) {
                        long current = System.currentTimeMillis();
                        if (!mIsPicturePause) {
                            mPicturePlayedTime += Math.abs(current - mPictureLastUpdateSeekBarTime);
                        }
                        mPictureLastUpdateSeekBarTime = current;

                        if (mPicturePlayedTime>4000)
                        {//todo time change
                            pictureComplete();
                        }

                        if (!mDontUpdateSeekBar && !mIsPicturePause) {
                            mPlayProgressSeekBar.setProgress((int)mPicturePlayedTime);
                            setMediaPlayedTimeTextView((int)mPicturePlayedTime);
                        }
                        sleep(300);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showInfo();
                }
            }
        }.start();
    }

    private void picturePause()
    {
        if(mIsPicturePlaying) {
            if (!mIsPicturePause) {
                mIsPicturePause = true;
                mPlayBtn.setBackgroundResource(R.drawable.img_media_play);
                ToastUtil.showToast(this, "暂停播放");
            }
            else
            {
                mIsPicturePause = false;
                mPlayBtn.setBackgroundResource(R.drawable.img_media_pause);
                ToastUtil.showToast(this, "继续播放");
            }
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

    private int updatePlayPosition(boolean forward)
    {
        if (mPlayListBeans.size()==0)
        {
            mPlayPosition = INVALID_POSITION;
            return mPlayPosition;
        }

        if (forward)
        {
            mPlayPosition++;
            if (mPlayPosition >= mPlayListBeans.size()) {
                mPlayPosition = 0;
            }
        }
        else
        {
            mPlayPosition--;
            if (mPlayPosition<0) {
                mPlayPosition = mPlayListBeans.size()-1;
            }
        }
        return mPlayPosition;
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

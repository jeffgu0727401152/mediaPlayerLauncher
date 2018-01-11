package com.whiteskycn.tv.projectorlauncher.settings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import com.orhanobut.logger.Logger;
import com.whitesky.sdk.widget.WaveLoadingView;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.settings.model.TaskInfoProvider;
import com.whiteskycn.tv.projectorlauncher.settings.presenter.QuickenActivityImpl;
import com.whiteskycn.tv.projectorlauncher.utils.ToastUtil;

import static com.whiteskycn.tv.projectorlauncher.settings.model.SettingsModel.INTENT_DATA_QUICKEN_CLEAN_STATUS;
import static com.whiteskycn.tv.projectorlauncher.settings.model.SettingsModel.QUICKEN_CLEAN_UPDATE_URI;


/**
 * Created by xiaoxuan on 2017/10/13.
 */
public class QuickenActivity extends Activity implements UIQuickenMessage, View.OnClickListener
{
    private WaveLoadingView mWlvLoading;
    
    private Button mBtClean;
    
    // 是否停止线程
    private boolean isStopThread = false;
    
    private HandlerThread mWorkThread;
    private Handler mWorkThreadHandler;
    
    private QuickenActivityImpl mQuickenActivityImpl;
    
    private WorkResultReceiver mWorkResultReceiver;
    
    // 加速完成
    public final int QUICKEN_CLEAN_STATUS_FINISH = 1;
    
    // 不需要加速
    public final int QUICKEN_CLEAN_STATUS_NEEDENT = 2;
    
    public final int MSG_CLEAN = -1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quicken);
        mWlvLoading = (WaveLoadingView)findViewById(R.id.wlv_quicken_loading);
        mBtClean = (Button)findViewById(R.id.bt_quicken_clean);
        mBtClean.setOnClickListener(this);
    }
    
    public void setProgressValue(float value)
    {
        mWlvLoading.setProgressValue((int)value);
    }
    
    private final Handler mMsgHandler = new Handler(new Handler.Callback()
    {
        @Override
        public boolean handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MSG_CLEAN:
                    Logger.d("MSG_CLEAN");
                    mWlvLoading.setProgressValue(0);
                    mWlvLoading.setTopTitle("");
                    mWorkThreadHandler.postDelayed(new QuickenTask(), 1000);
                    break;
            }
            return false;
        }
    });
    
    @Override
    protected void onResume()
    {
        // 注册Ui更新接收器
        mWorkResultReceiver = new WorkResultReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(QUICKEN_CLEAN_UPDATE_URI);
        registerReceiver(mWorkResultReceiver, intentFilter);
        mWorkResultReceiver.setUiUpdateHandler(this);

        mWorkThread = new HandlerThread("handlerThread");
        mWorkThread.start();
        mWorkThreadHandler = new Handler(mWorkThread.getLooper());

        mQuickenActivityImpl = new QuickenActivityImpl(getApplicationContext());
        mQuickenActivityImpl.initData();
        float memorySurplus = mQuickenActivityImpl.getMemorySurplus();
        float totalMemory = mQuickenActivityImpl.getTotalMemory();
        float temp = memorySurplus / totalMemory;
        mQuickenActivityImpl.setCurrentMemoryPercent((int)(temp * 100));
        mWlvLoading.setProgressValue(mQuickenActivityImpl.getCurrentMemoryPercent());
        mWlvLoading.setTopTitle(getString(R.string.title_memory_footprint) + (int)(temp * 100) + "%");

        super.onResume();
    }
    
    @Override
    protected void onPause()
    {
        unregisterReceiver(mWorkResultReceiver);
        if (mQuickenActivityImpl != null)
        {
            mQuickenActivityImpl = null;
        }
        isStopThread = true;
        mMsgHandler.removeCallbacksAndMessages(null);
        mWorkThreadHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }
    
    @Override
    public void UpdateCleanStatus(int status)
    {
        switch (status)
        {
            case QUICKEN_CLEAN_STATUS_NEEDENT:
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Logger.d("CLEAR_NEEDENT：" + mQuickenActivityImpl.getCurrentMemoryPercent());
                        mWlvLoading.setProgressValue(mQuickenActivityImpl.getCurrentMemoryPercent());
                        mWlvLoading.setTopTitle(getString(R.string.title_memory_footprint)
                            + mQuickenActivityImpl.getCurrentMemoryPercent() + "%");
                    }
                });
                break;
            case QUICKEN_CLEAN_STATUS_FINISH:
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Logger.d("CLEAR_FINISH：" + mQuickenActivityImpl.getCurrentMemoryPercent());
                        mWlvLoading.setProgressValue(mQuickenActivityImpl.getCurrentMemoryPercent());
                        ToastUtil.showToast(getApplication(),
                            getString(R.string.title_already_cleanup)
                                + (int)Float.parseFloat(mQuickenActivityImpl.getClearMemory()) + "M 空间");
                        mWlvLoading.setTopTitle(getString(R.string.title_memory_footprint)
                            + mQuickenActivityImpl.getCurrentMemoryPercent() + "%");
                    }
                });
                break;
        }
    }
    
    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.bt_quicken_clean:
                mMsgHandler.removeMessages(MSG_CLEAN);
                mMsgHandler.sendEmptyMessageDelayed(MSG_CLEAN, 500);
                break;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER)
        {
            mMsgHandler.removeMessages(MSG_CLEAN);
            mMsgHandler.sendEmptyMessageDelayed(MSG_CLEAN, 500);
        }
        return super.dispatchKeyEvent(event);
    }

    
    // 通过handlerThread的handler的post方法可以把QuickenTask放在handlerThread中执行
    class QuickenTask implements Runnable
    {
        public QuickenTask()
        {
            Logger.d("添加任务");
        }
        
        @Override
        public void run()
        {
            Logger.d("开始任务");
            if (!isStopThread)
            {
                mQuickenActivityImpl.getRunningApp();
                TaskInfoProvider taskInfoProvider = new TaskInfoProvider(getApplicationContext());
                mQuickenActivityImpl.setUserTaskInfoBean(taskInfoProvider.GetAllTask(mQuickenActivityImpl.getAppProcessInfo()));
                mQuickenActivityImpl.KillTask();
            }
        }
    }

    @Override
    public void finish()
    {
        super.finish();
        overridePendingTransition(R.anim.activity_up_in, R.anim.activity_up_out);
    }
    
    private class WorkResultReceiver extends BroadcastReceiver
    {
        private UIQuickenMessage uiUpdateHandler;
        
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // 默认不需要加速
            int msg = intent.getIntExtra(INTENT_DATA_QUICKEN_CLEAN_STATUS, QUICKEN_CLEAN_STATUS_NEEDENT);
            uiUpdateHandler.UpdateCleanStatus(msg);
        }
        
        public void setUiUpdateHandler(UIQuickenMessage handler)
        {
            this.uiUpdateHandler = handler;
        }
    }
}

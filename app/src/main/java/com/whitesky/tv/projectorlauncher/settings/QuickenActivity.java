package com.whitesky.tv.projectorlauncher.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import com.whitesky.sdk.widget.WaveLoadingView;
import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.settings.bean.TaskInfoBean;
import com.whitesky.tv.projectorlauncher.settings.model.TaskInfoProvider;
import com.whitesky.tv.projectorlauncher.utils.ToastUtil;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by xiaoxuan on 2017/10/13.
 */
public class QuickenActivity extends Activity implements View.OnClickListener
{
    // 加速完成
    public final int QUICKEN_CLEAN_STATUS_SUCCESS = 1;
    // 不需要加速
    public final int QUICKEN_CLEAN_STATUS_FAIL = 2;
    
    public final int MSG_CLEAN = 3;

    private ActivityManager mActivityManager;

    // 设备剩余内存
    private float mMemorySurplus;
    // 设备总内存
    private float mMemoryTotal;

    // 清理了多大的内存 */
    private Float mMemoryClean;

    // 使用内存百分比
    private int mUsedMemoryPercent;
    
    private WaveLoadingView mWlvLoading;
    
    private Button mBtnClean;
    
    private HandlerThread mQuickenTaskHandlerThread;
    
    private Handler mQuickenTaskHandler;
    
    private final Handler mHandler = new Handler(new Handler.Callback()
    {
        @Override
        public boolean handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MSG_CLEAN:
                    mWlvLoading.setProgressValue(0);
                    mWlvLoading.setTopTitle("");
                    mQuickenTaskHandler.postDelayed(new QuickenTask(), 500);
                    break;

                case QUICKEN_CLEAN_STATUS_FAIL:
                    mWlvLoading.setProgressValue(mUsedMemoryPercent);
                    mWlvLoading.setTopTitle(getString(R.string.title_memory_footprint) + mUsedMemoryPercent + "%");
                    break;

                case QUICKEN_CLEAN_STATUS_SUCCESS:
                    mWlvLoading.setProgressValue(mUsedMemoryPercent);
                    mWlvLoading.setTopTitle(getString(R.string.title_memory_footprint) + mUsedMemoryPercent + "%");
                    ToastUtil.showToast(getApplication(),
                            getString(R.string.title_already_cleanup) + mMemoryClean.intValue() + "M 空间");
                    break;

                default:
                    break;
            }
            return false;
        }
    });
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quicken);
        mWlvLoading = findViewById(R.id.wlv_quicken_loading);
        mBtnClean = findViewById(R.id.bt_quicken_clean);
        mBtnClean.setOnClickListener(this);
    }
    
    public void setProgressValue(float value)
    {
        mWlvLoading.setProgressValue((int)value);
    }
    
    @Override
    protected void onResume()
    {
        mQuickenTaskHandlerThread = new HandlerThread("QuickenTaskHandler");
        mQuickenTaskHandlerThread.start();
        mQuickenTaskHandler = new Handler(mQuickenTaskHandlerThread.getLooper());

        mActivityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);

        // 在显示UI前获取运行的进程和设备的内存情况
        updateMemoryInfo();
        mWlvLoading.setProgressValue(mUsedMemoryPercent);
        mWlvLoading.setTopTitle(getString(R.string.title_memory_footprint) + mUsedMemoryPercent + "%");

        super.onResume();
    }
    
    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.bt_quicken_clean:
                mHandler.removeMessages(MSG_CLEAN);
                mHandler.sendEmptyMessageDelayed(MSG_CLEAN, 500);
                break;
        }
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER)
        {
            mHandler.removeMessages(MSG_CLEAN);
            mHandler.sendEmptyMessageDelayed(MSG_CLEAN, 500);
        }
        return super.dispatchKeyEvent(event);
    }
    
    @Override
    public void finish()
    {
        super.finish();
        overridePendingTransition(R.anim.activity_up_in, R.anim.activity_up_out);
        mHandler.removeCallbacksAndMessages(null);
        mQuickenTaskHandler.removeCallbacksAndMessages(null);
    }

    private long updateMemoryInfo()
    {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        mActivityManager.getMemoryInfo(memoryInfo);
        long availMemorySize = memoryInfo.availMem;
        long totalMemorySize = memoryInfo.totalMem;

        mMemorySurplus = (float)availMemorySize / 1024 / 1024;
        mMemoryTotal = (float)totalMemorySize / 1024 / 1024;

        mUsedMemoryPercent = (int)(100*((mMemoryTotal - mMemorySurplus)/mMemoryTotal));

        return availMemorySize;
    }
    
    private void killTask(List<TaskInfoBean> userTaskInfoBean)
    {
        // 清理进程
        for (TaskInfoBean info : userTaskInfoBean)
        {
            if (!info.getIsSystemProcess())
            {
                mActivityManager.killBackgroundProcesses(info.getPackageName());
                // 高级清理
                try
                {
                    Method method =
                        Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String.class);
                    method.invoke(mActivityManager, info.getPackageName());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        // 更新内存信息
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        mActivityManager.getMemoryInfo(memoryInfo);

        float currentAvailMemory = (float)memoryInfo.availMem / 1024 / 1024;
        float cleanSize = currentAvailMemory - mMemorySurplus;
        updateMemoryInfo();

        if (cleanSize > 0)
        {
            mMemoryClean = cleanSize;
            mHandler.sendEmptyMessage(QUICKEN_CLEAN_STATUS_SUCCESS);
        }
        else
        {
            mHandler.sendEmptyMessage(QUICKEN_CLEAN_STATUS_FAIL);
        }
    }

    // post QuickenTask在QuickenTaskHandlerThread中执行
    class QuickenTask implements Runnable
    {
        public QuickenTask() {}
        
        @Override
        public void run()
        {
            TaskInfoProvider taskInfoProvider = new TaskInfoProvider(getApplicationContext());
            killTask(taskInfoProvider.GetAllTask(mActivityManager.getRunningAppProcesses()));
        }
    }
}

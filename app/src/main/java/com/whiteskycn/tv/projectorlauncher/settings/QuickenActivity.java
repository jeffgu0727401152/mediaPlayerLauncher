package com.whiteskycn.tv.projectorlauncher.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import com.whitesky.sdk.widget.WaveLoadingView;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.settings.bean.TaskInfoBean;
import com.whiteskycn.tv.projectorlauncher.settings.model.TaskInfoProvider;
import com.whiteskycn.tv.projectorlauncher.utils.ToastUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by xiaoxuan on 2017/10/13.
 */
public class QuickenActivity extends Activity implements View.OnClickListener
{
    // 加速完成
    public final int QUICKEN_CLEAN_STATUS_FINISH = 1;
    
    // 不需要加速
    public final int QUICKEN_CLEAN_STATUS_NEEDENT = 2;
    
    public final int MSG_CLEAN = -1;
    
    private ActivityManager mActivityManager;
    
    private float mTotalMemory;
    
    // 内存信息
    private ActivityManager.MemoryInfo mMemoryInfo;
    
    // 正在运行的进程/
    private List<ActivityManager.RunningAppProcessInfo> mAppProcessInfo;
    
    // 加速前设备剩余内存
    private float mMemorySurPlus;
    
    // 进程信息
    private List<TaskInfoBean> mUserTaskInfoBean;
    
    // 清理了多大的内存 */
    private String mClearmemoryStr;
    
    private String mPercentnum;
    
    // 实时内存重用百分比
    private int mCurrentMemoryPercentInt;
    
    private WaveLoadingView mWlvLoading;
    
    private Button mBtClean;
    
    // 是否停止线程
    private boolean isStopThread = false;
    
    private HandlerThread mHandlerThread;
    
    private HandlerThreadHandler mHandlerThreadHandler;
    
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
                    mHandlerThreadHandler.postDelayed(new QuickenTask(), 1000);
                    break;
                case QUICKEN_CLEAN_STATUS_NEEDENT:
                    mWlvLoading.setProgressValue(getCurrentMemoryPercent());
                    mWlvLoading.setTopTitle(getString(R.string.title_memory_footprint) + getCurrentMemoryPercent()
                        + "%");
                    break;
                case QUICKEN_CLEAN_STATUS_FINISH:
                    mWlvLoading.setProgressValue(getCurrentMemoryPercent());
                    ToastUtil.showToast(getApplication(),
                        getString(R.string.title_already_cleanup) + (int)Float.parseFloat(getClearmemory()) + "M 空间");
                    mWlvLoading.setTopTitle(getString(R.string.title_memory_footprint) + getCurrentMemoryPercent()
                        + "%");
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
        mWlvLoading = (WaveLoadingView)findViewById(R.id.wlv_quicken_loading);
        mBtClean = (Button)findViewById(R.id.bt_quicken_clean);
        mBtClean.setOnClickListener(this);
    }
    
    public void setProgressValue(float value)
    {
        mWlvLoading.setProgressValue((int)value);
    }
    
    @Override
    protected void onResume()
    {
        mHandlerThread = new HandlerThread("handlerThread");
        mHandlerThread.start();
        mHandlerThreadHandler = new HandlerThreadHandler(mHandlerThread.getLooper());
        initData();
        float memorySurPlus = getMemorySurPlus();
        float totalMemory = getTotalMemory();
        float temp = memorySurPlus / totalMemory;
        setCurrentMemoryPercent((int)(temp * 100));
        mWlvLoading.setProgressValue(getCurrentMemoryPercent());
        mWlvLoading.setTopTitle(getString(R.string.title_memory_footprint) + (int)(temp * 100) + "%");
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
        isStopThread = true;
        mHandler.removeCallbacksAndMessages(null);
        mHandlerThreadHandler.removeCallbacksAndMessages(null);
    }
    
    private void initData()
    {
        // 在显示UI前获取运行的进程和设备的内存情况
        mActivityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        GetSurplusMemory();
        mTotalMemory = GetTotalMemory();
    }
    
    private List<ActivityManager.RunningAppProcessInfo> getRunningApp()
    {
        // 得到当前运行的进程数目
        mAppProcessInfo = mActivityManager.getRunningAppProcesses();
        return mAppProcessInfo;
    }
    
    private long GetSurplusMemory()
    {
        // 得到清理前剩余的内存
        mMemoryInfo = new ActivityManager.MemoryInfo();
        mActivityManager.getMemoryInfo(mMemoryInfo);
        long MemorySize = mMemoryInfo.availMem;
        mMemorySurPlus = (float)MemorySize / 1024 / 1024;
        return MemorySize;
    }
    
    private float GetTotalMemory()
    {
        String str1 = "/proc/meminfo";// 系统内存信息文件
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;
        try
        {
            FileReader fileReader = new FileReader(str1);
            BufferedReader bufferedReader = new BufferedReader(fileReader, 8192);
            str2 = bufferedReader.readLine();
            arrayOfString = str2.split("\\s+");
            initial_memory = Integer.valueOf(arrayOfString[1]);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (float)(initial_memory / 1024);
    }
    
    private void KillTask()
    {
        // 清理进程
        for (TaskInfoBean info : mUserTaskInfoBean)
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
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        mActivityManager.getMemoryInfo(memoryInfo);
        float MemorySize = (float)memoryInfo.availMem / 1024 / 1024;
        float size = MemorySize - mMemorySurPlus;
        if (size > 0)
        {
            DecimalFormat decimalFormat = new DecimalFormat("0.00");
            mClearmemoryStr = decimalFormat.format(size);
            mPercentnum = decimalFormat.format((size / mTotalMemory) * 100);
            float temp = Float.parseFloat(mClearmemoryStr);
            mCurrentMemoryPercentInt = (int)((mMemorySurPlus - temp) / mTotalMemory * 100);
            mHandler.sendEmptyMessage(QUICKEN_CLEAN_STATUS_FINISH);
        }
        else
        {
            mHandler.sendEmptyMessage(QUICKEN_CLEAN_STATUS_NEEDENT);
        }
    }
    
    private int getCurrentMemoryPercent()
    {
        return mCurrentMemoryPercentInt;
    }
    
    private void setCurrentMemoryPercent(int currentMemoryPercent)
    {
        this.mCurrentMemoryPercentInt = currentMemoryPercent;
    }
    
    private String getClearmemory()
    {
        return mClearmemoryStr;
    }
    
    private List<ActivityManager.RunningAppProcessInfo> getAppProcessInfo()
    {
        return mAppProcessInfo;
    }
    
    private List<TaskInfoBean> getUserTaskInfoBean()
    {
        return mUserTaskInfoBean;
    }
    
    private void setUserTaskInfoBean(List<TaskInfoBean> userTaskInfoBean)
    {
        this.mUserTaskInfoBean = userTaskInfoBean;
    }
    
    private float getMemorySurPlus()
    {
        return mMemorySurPlus;
    }
    
    private float getTotalMemory()
    {
        return mTotalMemory;
    }
    
    // 为加速线程自定义handler
    class HandlerThreadHandler extends Handler
    {
        public HandlerThreadHandler(Looper looper)
        {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
        }
    }
    
    // 通过handlerThread的handler的post方法可以把QuickenTask放在handlerThread中执行
    class QuickenTask implements Runnable
    {
        public QuickenTask()
        {
        }
        
        @Override
        public void run()
        {
            if (!isStopThread)
            {
                getRunningApp();
                TaskInfoProvider taskInfoProvider = new TaskInfoProvider(getApplicationContext());
                setUserTaskInfoBean(taskInfoProvider.GetAllTask(getAppProcessInfo()));
                KillTask();
            }
        }
    }
}

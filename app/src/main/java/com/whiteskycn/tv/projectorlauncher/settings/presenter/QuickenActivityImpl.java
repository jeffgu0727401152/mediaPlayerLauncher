package com.whiteskycn.tv.projectorlauncher.settings.presenter;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import com.orhanobut.logger.Logger;
import com.whiteskycn.tv.projectorlauncher.settings.bean.TaskInfoBean;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.List;

import static com.whiteskycn.tv.projectorlauncher.settings.model.SettingsModel.INTENT_DATA_QUICKEN_CLEAN_STATUS;
import static com.whiteskycn.tv.projectorlauncher.settings.model.SettingsModel.QUICKEN_CLEAN_UPDATE_URI;

/**
 * Created by xiaoxuan on 2017/12/29.
 */
public class QuickenActivityImpl implements IQuickenActivity
{
    // 加速完成
    public final int QUICKEN_CLEAN_STATUS_FINISH = 1;
    
    // 不需要加速
    public final int QUICKEN_CLEAN_STATUS_NEEDENT = 2;
    
    private Context mContext;
    
    private ActivityManager mActivityManager;

    //设备的总内存
    private float mTotalMemory;

    // 加速前设备剩余内存
    private float mMemorySurplus;

    // 正在运行的进程
    private List<ActivityManager.RunningAppProcessInfo> mAppProcessInfo;

    // 进程信息
    private List<TaskInfoBean> mUserTaskInfoBean;
    
    // 清理了多大的内存
    private String mClearMemoryStr;
    
    // 实时内存重用百分比
    private int mCurrentMemoryPercentInt;
    
    public QuickenActivityImpl(Context context)
    {
        this.mContext = context;
    }
    
    @Override
    public void initData()
    {
        // 在显示UI前获取运行的进程和设备的内存情况
        mActivityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mMemorySurplus = (float)GetSurplusMemory() / 1024 / 1024;
        mTotalMemory = GetTotalMemory();
    }
    
    @Override
    public List<ActivityManager.RunningAppProcessInfo> getRunningApp()
    {
        // 得到当前运行的进程数目
        mAppProcessInfo = mActivityManager.getRunningAppProcesses();
        return mAppProcessInfo;
    }
    
    @Override
    public long GetSurplusMemory()
    {
        // 得到清理前剩余的内存
        ActivityManager.MemoryInfo memoryInfo;
        memoryInfo = new ActivityManager.MemoryInfo();
        mActivityManager.getMemoryInfo(memoryInfo);
        long MemorySize = memoryInfo.availMem;
        return MemorySize;
    }
    
    @Override
    public float GetTotalMemory()
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
    
    @Override
    public void KillTask()
    {
        // 清理进程
        for (TaskInfoBean info : mUserTaskInfoBean)
        {
            if (!info.getIsSystemProcess())
            {
                Logger.d("info：" + info.getPackageName());
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
        float size = MemorySize - mMemorySurplus;
        if (size > 0)
        {
            DecimalFormat decimalFormat = new DecimalFormat("0.00");
            mClearMemoryStr = decimalFormat.format(size);
            float temp = Float.parseFloat(mClearMemoryStr);
            mCurrentMemoryPercentInt = (int)((mMemorySurplus - temp) / mTotalMemory * 100);
            Intent intentFinish = new Intent(QUICKEN_CLEAN_UPDATE_URI);
            intentFinish.putExtra(INTENT_DATA_QUICKEN_CLEAN_STATUS, QUICKEN_CLEAN_STATUS_FINISH);
            mContext.sendBroadcast(intentFinish);
        }
        else
        {
            Intent intentNeedEnt = new Intent(QUICKEN_CLEAN_UPDATE_URI);
            intentNeedEnt.putExtra(INTENT_DATA_QUICKEN_CLEAN_STATUS, QUICKEN_CLEAN_STATUS_NEEDENT);
            mContext.sendBroadcast(intentNeedEnt);
        }
    }
    
    public int getCurrentMemoryPercent()
    {
        return mCurrentMemoryPercentInt;
    }
    
    public void setCurrentMemoryPercent(int currentMemoryPercent)
    {
        this.mCurrentMemoryPercentInt = currentMemoryPercent;
    }
    
    public String getClearMemory()
    {
        return mClearMemoryStr;
    }
    
    public List<ActivityManager.RunningAppProcessInfo> getAppProcessInfo()
    {
        return mAppProcessInfo;
    }
    
    public List<TaskInfoBean> getUserTaskInfoBean()
    {
        return mUserTaskInfoBean;
    }
    
    public void setUserTaskInfoBean(List<TaskInfoBean> userTaskInfoBean)
    {
        this.mUserTaskInfoBean = userTaskInfoBean;
    }
    
    public float getMemorySurplus()
    {
        return mMemorySurplus;
    }
    
    public float getTotalMemory()
    {
        return mTotalMemory;
    }

}

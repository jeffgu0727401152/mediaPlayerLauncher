package com.whiteskycn.tv.projectorlauncher.settings.presenter;

import android.app.ActivityManager;

import java.util.List;

/**
 * Created by xiaoxuan on 2017/12/29.
 */

public interface IQuickenActivity
{
    public void initData();
    
    public List<ActivityManager.RunningAppProcessInfo> getRunningApp();
    
    public long GetSurplusMemory();
    
    public float GetTotalMemory();
    
    public void KillTask();
}

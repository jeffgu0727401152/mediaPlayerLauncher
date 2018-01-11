package com.whiteskycn.tv.projectorlauncher.settings.model;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.whiteskycn.tv.projectorlauncher.settings.bean.TaskInfoBean;

import java.util.ArrayList;
import java.util.List;

public class TaskInfoProvider
{
    private PackageManager mPackageManager;
    private ActivityManager mActivityManager;
    
    public TaskInfoProvider(Context context)
    {
        mPackageManager = context.getPackageManager();
        mActivityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
    }
    
    // 遍历传入的列表,将所有应用的信息传入taskinfo中
    public List<TaskInfoBean> GetAllTask(List<RunningAppProcessInfo> list)
    {
        List<TaskInfoBean> taskInfoBeans = new ArrayList<TaskInfoBean>();
        for (RunningAppProcessInfo appProcessInfo : list)
        {
            TaskInfoBean info = new TaskInfoBean();
            int id = appProcessInfo.pid;
            info.setId(id);
            String packageName = appProcessInfo.processName;
            info.setPackageName(packageName);
            try
            {
                // ApplicationInfo是AndroidMainfest文件里面整个Application节点的封装
                ApplicationInfo applicationInfo = mPackageManager.getPackageInfo(packageName, 0).applicationInfo;
                Drawable icon = applicationInfo.loadIcon(mPackageManager);
                info.setIcon(icon);
                String name = applicationInfo.loadLabel(mPackageManager).toString();
                info.setName(name);
                info.setIsSystemProcess(!IsSystemApp(applicationInfo));
                android.os.Debug.MemoryInfo[] memoryInfo = mActivityManager.getProcessMemoryInfo(new int[] {id});
                int memory = memoryInfo[0].getTotalPrivateDirty();
                info.setMemory(memory);
                // 不清除自己的进程
                if (!info.getPackageName().equalsIgnoreCase("com.whiteskycn.tv.projectorlauncher"))
                {
                    taskInfoBeans.add(info);
                }
                info = null;
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                info.setName(packageName);
                info.setIsSystemProcess(true);
            }
        }
        return taskInfoBeans;
    }
    
    public Boolean IsSystemApp(ApplicationInfo info)
    {
        // 有些系统应用是可以更新的，如果用户自己下载了一个系统的应用来更新了原来的，
        // 它就不是系统应用啦，这个就是判断这种情况的
        if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
        {
            return true;
        }
        else if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
        {
            return true;
        }
        return false;
    }
}

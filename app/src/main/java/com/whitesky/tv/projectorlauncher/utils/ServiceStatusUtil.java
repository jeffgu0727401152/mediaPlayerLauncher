package com.whitesky.tv.projectorlauncher.utils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;

import java.util.List;

/**
 * Created by jeff on 18-1-15.
 */

public class ServiceStatusUtil {
    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (serviceList == null || serviceList.size() == 0) {
            return false;
        }

        for (RunningServiceInfo info : serviceList) {
            if (info.service.getClassName().equals(serviceClass.getName()))
            {
                return true;
            }
        }
        return false;
    }
}
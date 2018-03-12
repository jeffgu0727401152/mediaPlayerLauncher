package com.whitesky.tv.projectorlauncher.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;


import com.whitesky.tv.projectorlauncher.app.bean.AppBean;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by mac on 17-6-2. 读取设备所有app
 * 
 * @author xiaoxuan
 */
public class AppUtils
{
    private Context mContext;
    public static final String[] mApkExceptList = new String[]{""};
    
    public AppUtils(Context context)
    {
        mContext = context;
    }
    
    public static void launchApp(Context context, String packageName)
    {
        // 判断是否安装过App，否则去市场下载
        if (isAppInstalled(context, packageName))
        {
            context.startActivity(context.getPackageManager().getLaunchIntentForPackage(packageName));
        }
        else
        {
            ToastUtil.showToast(context, "未曾安装此应用");
        }
    }
    
    /**
     * 检测某个应用是否安装
     *
     * @param context
     * @param packageName
     * @return
     */
    public static boolean isAppInstalled(Context context, String packageName)
    {
        try
        {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        }
        catch (NameNotFoundException e)
        {
            return false;
        }
    }
    
    public ArrayList<AppBean> getLaunchAppList()
    {
        PackageManager localPackageManager = mContext.getPackageManager();
        Intent localIntent = new Intent("android.intent.action.MAIN");
        localIntent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> localList = localPackageManager.queryIntentActivities(localIntent, 0);
        ArrayList<AppBean> localArrayList = null;
        Iterator<ResolveInfo> localIterator = null;
        if (localList != null)
        {
            localArrayList = new ArrayList<AppBean>();
            localIterator = localList.iterator();
        }
        while (true)
        {
            if (!localIterator.hasNext())
                break;
            ResolveInfo localResolveInfo = (ResolveInfo)localIterator.next();
            AppBean localAppBean = new AppBean();
            localAppBean.setIcon(localResolveInfo.activityInfo.loadIcon(localPackageManager));
            localAppBean.setName(localResolveInfo.activityInfo.loadLabel(localPackageManager).toString());
            localAppBean.setPackageName(localResolveInfo.activityInfo.packageName);
            localAppBean.setDataDir(localResolveInfo.activityInfo.applicationInfo.publicSourceDir);
            localAppBean.setLauncherName(localResolveInfo.activityInfo.name);
            String pkgName = localResolveInfo.activityInfo.packageName;
            PackageInfo mPackageInfo;
            try
            {
                mPackageInfo = mContext.getPackageManager().getPackageInfo(pkgName, 0);
                if ((mPackageInfo.applicationInfo.flags & mPackageInfo.applicationInfo.FLAG_SYSTEM) > 0)
                {// 系统预装
                    localAppBean.setSysApp(true);
                }
            }
            catch (NameNotFoundException e)
            {
                e.printStackTrace();
            }
            
            String apkPackageName = localAppBean.getPackageName();
            
            /**
             * 屏蔽例外app
             */
            if(!Arrays.asList(mApkExceptList).contains(apkPackageName) && !apkPackageName.equals(mContext.getPackageName())) {
                localArrayList.add(localAppBean);
            }
        }
        return localArrayList;
    }
    
    public List getAllInstallApp()
    {
        List<PackageInfo> packageInfoList = mContext.getPackageManager().getInstalledPackages(0);
        List<AppBean> result = new ArrayList<AppBean>();
        for (PackageInfo packageInfo : packageInfoList)
        {
            /*
             * 判断是否为非系统应用
             */
            AppBean app = new AppBean();
            app.setIcon(packageInfo.applicationInfo.loadIcon(mContext.getPackageManager()));
            app.setName(packageInfo.applicationInfo.loadLabel(mContext.getPackageManager()).toString());
            app.setPackageName(packageInfo.packageName);
            app.setVersionName(packageInfo.versionName);
            if ((packageInfo.applicationInfo.flags & packageInfo.applicationInfo.FLAG_SYSTEM) > 0) {
                // 系统预装
                app.setSysApp(true);
            } else {
                /**
                 * 屏蔽自己
                 */
                String apkPackageName = app.getPackageName();
                if(!Arrays.asList(mApkExceptList).contains(apkPackageName) && !apkPackageName.equals(mContext.getPackageName())) {
                    result.add(app);
                }
            }
        }
        return result;
    }
    
    public List getSysApp()
    {
        List<PackageInfo> packageInfoList = mContext.getPackageManager().getInstalledPackages(0);
        List<AppBean> result = new ArrayList<AppBean>();
        for (PackageInfo packageInfo : packageInfoList)
        {
            /*
             * 判断是否为非系统应用
             */
            AppBean app = new AppBean();
            app.setIcon(packageInfo.applicationInfo.loadIcon(mContext.getPackageManager()));
            app.setName(packageInfo.applicationInfo.loadLabel(mContext.getPackageManager()).toString());
            app.setPackageName(packageInfo.packageName);
            app.setVersionName(packageInfo.versionName);
            if ((packageInfo.applicationInfo.flags & packageInfo.applicationInfo.FLAG_SYSTEM) > 0) {
                // 系统预装
                app.setSysApp(true);
                String apkPackageName = app.getPackageName();
                if(!Arrays.asList(mApkExceptList).contains(apkPackageName) && !apkPackageName.equals(mContext.getPackageName())) {
                    result.add(app);
                }
            }
        }
        return result;
    }
    
    public ArrayList<AppBean> getUninstallAppList()
    {
        PackageManager localPackageManager = mContext.getPackageManager();
        Intent localIntent = new Intent("android.intent.action.MAIN");
        localIntent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> localList = localPackageManager.queryIntentActivities(localIntent, 0);
        ArrayList<AppBean> localArrayList = null;
        Iterator<ResolveInfo> localIterator = null;
        if (localList != null)
        {
            localArrayList = new ArrayList<AppBean>();
            localIterator = localList.iterator();
        }
        while (true)
        {
            if (!localIterator.hasNext())
                break;
            ResolveInfo localResolveInfo = (ResolveInfo)localIterator.next();
            AppBean localAppBean = new AppBean();
            localAppBean.setIcon(localResolveInfo.activityInfo.loadIcon(localPackageManager));
            localAppBean.setName(localResolveInfo.activityInfo.loadLabel(localPackageManager).toString());
            localAppBean.setPackageName(localResolveInfo.activityInfo.packageName);
            localAppBean.setDataDir(localResolveInfo.activityInfo.applicationInfo.publicSourceDir);
            String pkgName = localResolveInfo.activityInfo.packageName;
            PackageInfo mPackageInfo;
            try
            {
                mPackageInfo = mContext.getPackageManager().getPackageInfo(pkgName, 0);
                if ((mPackageInfo.applicationInfo.flags & mPackageInfo.applicationInfo.FLAG_SYSTEM) > 0)
                {// 系统预装
                    localAppBean.setSysApp(true);
                }
                else
                {
                    localArrayList.add(localAppBean);
                }
            }
            catch (NameNotFoundException e)
            {
                e.printStackTrace();
            }
        }
        return localArrayList;
    }
    
    public ArrayList<AppBean> getAutoRunAppList()
    {
        PackageManager localPackageManager = mContext.getPackageManager();
        Intent localIntent = new Intent("android.intent.action.MAIN");
        localIntent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> localList = localPackageManager.queryIntentActivities(localIntent, 0);
        ArrayList<AppBean> localArrayList = null;
        Iterator<ResolveInfo> localIterator = null;
        if (localList != null)
        {
            localArrayList = new ArrayList<AppBean>();
            localIterator = localList.iterator();
        }
        
        while (true)
        {
            if (!localIterator.hasNext())
                break;
            ResolveInfo localResolveInfo = localIterator.next();
            AppBean localAppBean = new AppBean();
            localAppBean.setIcon(localResolveInfo.activityInfo.loadIcon(localPackageManager));
            localAppBean.setName(localResolveInfo.activityInfo.loadLabel(localPackageManager).toString());
            localAppBean.setPackageName(localResolveInfo.activityInfo.packageName);
            localAppBean.setDataDir(localResolveInfo.activityInfo.applicationInfo.publicSourceDir);
            String pkgName = localResolveInfo.activityInfo.packageName;
            String permission = "android.permission.RECEIVE_BOOT_COMPLETED";
            try
            {
                PackageInfo mPackageInfo = mContext.getPackageManager().getPackageInfo(pkgName, 0);
                if ((PackageManager.PERMISSION_GRANTED == localPackageManager.checkPermission(permission, pkgName))
                    && !((mPackageInfo.applicationInfo.flags & mPackageInfo.applicationInfo.FLAG_SYSTEM) > 0))
                {
                    localArrayList.add(localAppBean);
                }
            }
            catch (NameNotFoundException e)
            {
                e.printStackTrace();
            }
        }
        return localArrayList;
    }

    public static void uninstallApp(Context context, String packageName){
        Intent uninstall_intent = new Intent();
        uninstall_intent.setAction(Intent.ACTION_DELETE);
        uninstall_intent.setData(Uri.parse("package:" + packageName));
        context.startActivity(uninstall_intent);
    }

    public static void installApk(Context context, File file) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file),
                "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
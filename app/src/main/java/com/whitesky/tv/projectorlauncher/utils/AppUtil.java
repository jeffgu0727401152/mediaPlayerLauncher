package com.whitesky.tv.projectorlauncher.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.util.DisplayMetrics;


import com.whitesky.tv.projectorlauncher.app.bean.AppBean;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by mac on 17-6-2. 读取设备所有app
 * 
 * @author xiaoxuan
 */
public class AppUtil
{
    private Context mContext;
    public static final String[] mApkExceptList = new String[]{""};
    
    public AppUtil(Context context)
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
            ResolveInfo localResolveInfo = localIterator.next();
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
            ResolveInfo localResolveInfo = localIterator.next();
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

    public static String getSignature(Context context) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> apps = pm.getInstalledPackages(PackageManager.GET_SIGNATURES);
        Iterator<PackageInfo> iter = apps.iterator();
        while(iter.hasNext()) {
            PackageInfo packageinfo = iter.next();
            String packageName = packageinfo.packageName;
            if (packageName.equals(context.getPackageName())) {
                return packageinfo.signatures[0].toCharsString();
            }
        }
        return null;
    }

    public static String getAPKSignature(String apkPath) {
        String PATH_PackageParser = "android.content.pm.PackageParser";
        try {
            // apk包的文件路径
            // 这是一个Package 解释器, 是隐藏的
            // 构造函数的参数只有一个, apk文件的路径
            // PackageParser packageParser = new PackageParser(apkPath);
            Class pkgParserCls = Class.forName(PATH_PackageParser);
            Class[] typeArgs = new Class[1];
            typeArgs[0] = String.class;
            Constructor pkgParserCt = pkgParserCls.getConstructor(typeArgs);
            Object[] valueArgs = new Object[1];
            valueArgs[0] = apkPath;
            Object pkgParser = pkgParserCt.newInstance(valueArgs);
            // 这个是与显示有关的, 里面涉及到一些像素显示等等, 我们使用默认的情况
            DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();
            // PackageParser.Package mPkgInfo = packageParser.parsePackage(new
            // File(apkPath), apkPath,
            // metrics, 0);
            typeArgs = new Class[4];
            typeArgs[0] = File.class;
            typeArgs[1] = String.class;
            typeArgs[2] = DisplayMetrics.class;
            typeArgs[3] = Integer.TYPE;
            Method pkgParser_parsePackageMtd = pkgParserCls.getDeclaredMethod("parsePackage", typeArgs);
            valueArgs = new Object[4];
            valueArgs[0] = new File(apkPath);
            valueArgs[1] = apkPath;
            valueArgs[2] = metrics;
            valueArgs[3] = PackageManager.GET_SIGNATURES;
            Object pkgParserPkg = pkgParser_parsePackageMtd.invoke(pkgParser, valueArgs);

            typeArgs = new Class[2];
            typeArgs[0] = pkgParserPkg.getClass();
            typeArgs[1] = Integer.TYPE;
            Method pkgParser_collectCertificatesMtd = pkgParserCls.getDeclaredMethod("collectCertificates", typeArgs);
            valueArgs = new Object[2];
            valueArgs[0] = pkgParserPkg;
            valueArgs[1] = PackageManager.GET_SIGNATURES;
            pkgParser_collectCertificatesMtd.invoke(pkgParser, valueArgs);
            // 应用程序信息包, 这个公开的, 不过有些函数, 变量没公开
            Field packageInfoFld = pkgParserPkg.getClass().getDeclaredField("mSignatures");
            Signature[] info = (Signature[]) packageInfoFld.get(pkgParserPkg);
            return info[0].toCharsString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getApkPackageName(Context context,String apkPath){
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
            if (info != null) {
                return info.applicationInfo.packageName;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static String getApkVersionName(Context context,String apkPath){
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
            if (info != null) {
                return info.versionName;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static int getApkVersionCode(Context context,String apkPath){
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
            if (info != null) {
                return info.versionCode;
            } else {
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }
}
package com.whitesky.tv.projectorlauncher.application;

import android.app.Application;

import com.wsd.android.NativeCertification;

//import com.squareup.leakcanary.LeakCanary;


/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class MainApplication extends Application
{
    // 启动后自动播放功能用
    // app被启动后,HomeActivity会检查这个标志与是否有播放列表决定是否跳转去MediaActivity
    // MediaActivity的onResume会将这个标志设置为true,下次HomeActivity就不再自动跳转过去了
    public boolean mFirstInitDone = false;
    public boolean isMediaActivityFullScreenPlaying = false;
    public boolean isMediaActivityForeground = false;
    public boolean isBusyInFormat = false;
    public boolean isBusyInCopy = false;

    @Override
    public void onCreate()
    {
        super.onCreate();
//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            // This process is dedicated to LeakCanary for heap analysis.
//            // You should not init your app in this process.
//            return;
//        }
//        LeakCanary.install(this);
    }
}

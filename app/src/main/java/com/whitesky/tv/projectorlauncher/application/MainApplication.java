package com.whitesky.tv.projectorlauncher.application;

import android.app.Application;

//import com.squareup.leakcanary.LeakCanary;


/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class MainApplication extends Application
{
    public boolean mInitDone = false;
    public boolean isFullScreenPlaying = false;

    @Override
    public void onCreate()
    {
        mInitDone = false;
        isFullScreenPlaying = false;
        super.onCreate();
//        if (LeakCanary.isInAnalyzerProcess(this)) {//1
//            // This process is dedicated to LeakCanary for heap analysis.
//            // You should not init your app in this process.
//            return;
//        }
//        LeakCanary.install(this);
    }
}

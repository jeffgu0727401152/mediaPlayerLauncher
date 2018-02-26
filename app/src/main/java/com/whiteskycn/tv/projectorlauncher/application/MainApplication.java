package com.whiteskycn.tv.projectorlauncher.application;

import android.app.Application;

//import com.squareup.leakcanary.LeakCanary;


/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class MainApplication extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
//        if (LeakCanary.isInAnalyzerProcess(this)) {//1
//            // This process is dedicated to LeakCanary for heap analysis.
//            // You should not init your app in this process.
//            return;
//        }
//        LeakCanary.install(this);
    }
}

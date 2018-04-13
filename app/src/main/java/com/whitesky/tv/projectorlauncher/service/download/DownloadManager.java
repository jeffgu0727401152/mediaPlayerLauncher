package com.whitesky.tv.projectorlauncher.service.download;

/**
 * Created by jeff on 18-3-14.
 */

import android.util.Log;

import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.utils.PathUtil;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;

import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_NONE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_PAUSED;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_WAITING;

/**
 * 下载管理器
 */
public class DownloadManager {

    private static final String TAG = DownloadManager.class.getSimpleName();

    //当线程池中的线程小于mCorePoolSize，直接创建新的线程加入线程池执行任务
    private static final int mCorePoolSize = 5;
    //最大线程数
    private static final int mMaximumPoolSize = 5;
    //线程执行完任务后，且队列中没有可以执行的任务，存活的时间，后面的参数是时间单位
    private static final long mKeepAliveTime = 100L;

    OkHttpClient mClient = new OkHttpClient();
    private ThreadPoolExecutor downloadExecutor = new ThreadPoolExecutor(mCorePoolSize, mMaximumPoolSize, mKeepAliveTime,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
            new ThreadFactory() {
                private AtomicInteger mInteger = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable runnable) {
                    return new Thread(runnable, "download thread #" + mInteger.getAndIncrement());
                }}, new ThreadPoolExecutor.AbortPolicy());



    private Map<String, DownloadRunnable> mTaskMap = new ConcurrentHashMap<>();
    private static DownloadManager instance;

    public DownloadManager() {}

    public static DownloadManager getInstance() {

        if (instance == null) {
            synchronized (DownloadManager.class) {
                if (instance == null) {
                    instance = new DownloadManager();
                }
            }
        }
        return instance;
    }

    /**
     * 开启下载，需要传入一个MediaBean对象
     */
    public void download(MediaBean downloadBean, final DownloadCallback callback) {
        if (downloadBean == null || downloadBean.getUrl().isEmpty()) {
            Log.w(TAG, "download bean is not valid!" + downloadBean==null?"null":downloadBean.toString());
            return;
        }

        Log.i(TAG, "prepare a download task:" + downloadBean.toString());

        DownloadRunnable task;
        if (!mTaskMap.containsKey(downloadBean.getUrl())) {

            // 创建一个下载任务，放入线程池
            task = new DownloadRunnable(downloadBean, mClient, new DownloadRunnable.RunNotify(){
                @Override
                public void notifyReturn(String url){
                    if (mTaskMap.containsKey(url)) {
                        mTaskMap.remove(url);
                    }
                }
            });

            task.setCallback(callback);

            // 线程放入map里面方便管理
            mTaskMap.put(downloadBean.getUrl(), task);
        } else {
            task = mTaskMap.get(downloadBean.getUrl());
        }

        // 防止runnable下载完成或发生错误，但是还没来得通知manager从mTaskMap中移出，这个时候又处理download这个url的请求
        // 所以这边需要强制只有已经在Task中为none/pause的状态给下载
        if (task.getEntity().getDownloadState()!= MediaBean.STATE_DOWNLOAD_NONE
                    && task.getEntity().getDownloadState() != MediaBean.STATE_DOWNLOAD_PAUSED
                    && task.getEntity().getDownloadState() != MediaBean.STATE_DOWNLOAD_ERROR)
        {
            Log.w(TAG,"do nothing because bean is already in downloading");
            return;
        }

        // 下载之前，把状态设置为STATE_WAITING
        // 此时并没有真的开始下载，只是把任务放入了线程池中
        // 当线程池的任务真正开始执行时，才会改为STATE_DOWNLOADING
        task.getEntity().setDownloadState(STATE_DOWNLOAD_WAITING);
        downloadBean.setDownloadState(STATE_DOWNLOAD_WAITING);
        if (callback != null) {
            callback.onStateChange(task.getEntity());
        }

        if (task!=null) {
            downloadExecutor.execute(task);
        }
    }

    /** 暂停下载，需要传入一个 MediaBean 对象 */
    public void pause(MediaBean pauseBean) {

        if (pauseBean == null || pauseBean.getUrl().isEmpty()) {
            Log.w(TAG, "pause bean is not valid!" + pauseBean==null?"null":pauseBean.toString());
            return;
        }

        Log.i(TAG, "pause a download task:" + pauseBean.toString());

        if (mTaskMap.containsKey(pauseBean.getUrl())) {
            // 拿到当前任务
            DownloadRunnable task = mTaskMap.get(pauseBean.getUrl());

            if (task.getEntity().getDownloadState() != MediaBean.STATE_DOWNLOAD_WAITING
                    && task.getEntity().getDownloadState() != MediaBean.STATE_DOWNLOAD_START
                    && task.getEntity().getDownloadState() != MediaBean.STATE_DOWNLOAD_DOWNLOADING)
            {
                Log.w(TAG,"do nothing because bean is already in pause");
                return;
            }

            // 将任务状态设置为暂停,run循环耗时代码自动结束
            task.getEntity().setDownloadState(STATE_DOWNLOAD_PAUSED);
            pauseBean.setDownloadState(STATE_DOWNLOAD_PAUSED);

            if (task.getCallback() != null) {
                task.getCallback().onStateChange(task.getEntity());
            }

            // 在下载队列里取消排队线程
            if (downloadExecutor.getQueue().contains(task)) {
                downloadExecutor.getQueue().remove(task);
            }

        } else {
            Log.w(TAG, "can not pause a download task not in mTaskMap!");
        }
    }

    /** 删除当前正在下载的任务 */
    public void cancel(MediaBean cancelBean) {

        if (cancelBean == null || cancelBean.getUrl().isEmpty()) {
            Log.w(TAG, "pause bean is not valid!" + cancelBean==null?"null":cancelBean.toString());
            return;
        }

        if (mTaskMap.containsKey(cancelBean.getUrl())) {
            // 拿到当前任务
            DownloadRunnable task = mTaskMap.get(cancelBean.getUrl());
            // 暂停下载任务(等于取消了该线程)
            task.getEntity().setDownloadState(STATE_DOWNLOAD_NONE);
            cancelBean.setDownloadState(STATE_DOWNLOAD_NONE);
            if (task.getCallback() != null) {
                task.getCallback().onStateChange(task.getEntity());
            }

            //删除下载的临时文件
            File file = PathUtil.getDownloadTempFileByUrl(new File(cancelBean.getPath()).getParent(),cancelBean.getUrl());
            if (file.exists()) {
                file.delete();
            }

            // 在下载队列里取消排队线程
            if (downloadExecutor.getQueue().contains(task)) {
                downloadExecutor.getQueue().remove(task);
            }

            mTaskMap.remove(cancelBean.getUrl());

        }  else {
            Log.w(TAG, "can not cancel a download task not in mTaskMap!");
        }
    }

    public boolean contains(String url) {
        if (mTaskMap.isEmpty() || url==null) {
            return false;
        }
        return mTaskMap.containsKey(url);
    }

    public void pauseAll() {
        for (Iterator<Map.Entry<String, DownloadRunnable>> it = mTaskMap.entrySet().iterator(); it.hasNext();){
            Map.Entry<String, DownloadRunnable> item = it.next();
            DownloadRunnable val = item.getValue();
            pause(val.getEntity());
        }
    }

    public void cancelAll() {
        for (Iterator<Map.Entry<String, DownloadRunnable>> it = mTaskMap.entrySet().iterator(); it.hasNext();){
            Map.Entry<String, DownloadRunnable> item = it.next();
            DownloadRunnable val = item.getValue();
            cancel(val.getEntity());
        }
    }
}
package com.whitesky.tv.projectorlauncher.service.download;

/**
 * Created by jeff on 18-3-14.
 */

import android.util.Log;

import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.utils.PathUtil;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.OkHttpClient;

import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_NONE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_PAUSED;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_WAITING;

/**
 * 下载管理器
 */
public class DownloadManager {

    private static final String TAG = DownloadManager.class.getSimpleName();

    private Map<String, DownloadRunnable> mTaskMap = new ConcurrentHashMap<>();

    OkHttpClient mClient = new OkHttpClient();

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
            task = new DownloadRunnable(downloadBean, mClient, callback);
            // 线程放入map里面方便管理
            mTaskMap.put(downloadBean.getUrl(), task);
        } else {
            task = mTaskMap.get(downloadBean.getUrl());
        }

        // 下载之前，把状态设置为STATE_WAITING
        // 此时并没有真的开始下载，只是把任务放入了线程池中
        // 当线程池的任务真正开始执行时，才会改为STATE_DOWNLOADING
        task.getEntity().setDownloadState(STATE_DOWNLOAD_WAITING);
        downloadBean.setDownloadState(STATE_DOWNLOAD_WAITING);
        if (callback != null) {
            callback.onStateChange(task.getEntity());
        }

        DownloadExecutor.getInstance().execute(task);
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

            // 将任务状态设置为暂停,run循环耗时代码自动结束
            task.getEntity().setDownloadState(STATE_DOWNLOAD_PAUSED);
            pauseBean.setDownloadState(STATE_DOWNLOAD_PAUSED);

            if (task.getCallback() != null) {
                task.getCallback().onStateChange(task.getEntity());
            }

            // 在下载队列里取消排队线程
            DownloadExecutor.getInstance().cancel(task);

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
            File file = PathUtil.getDownloadTempFileByUrl(cancelBean.getUrl());
            if (file.exists()) {
                file.delete();
            }

            // 在下载队列里取消排队线程
            if (DownloadExecutor.getInstance().contains(task)) {
                DownloadExecutor.getInstance().cancel(task);
            }

        }  else {
            Log.w(TAG, "can not cancel a download task not in mTaskMap!");
        }
    }

    /** 销毁的时候关闭线程池以及当前执行的线程，并清空所有数据和把当前下载状态存进数据库 */
    public void close() {
        DownloadExecutor.getInstance().stop();
        mClient = null;
        mTaskMap.clear();
    }
}
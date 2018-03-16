package com.whitesky.tv.projectorlauncher.service.download;

/**
 * Created by jeff on 18-3-14.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBeanDao;
import com.whitesky.tv.projectorlauncher.utils.Md5Util;
import com.whitesky.tv.projectorlauncher.utils.PathUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DELETE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOADED;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOADING;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_ERROR;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_NONE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_PAUSED;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_START;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_WAITING;
import static com.whitesky.tv.projectorlauncher.utils.PathUtil.PATH_FILE_DOWNLOAD_TEMP;

/**
 * 下载管理器
 */
public class DownloadManager {

    private static final String TAG = DownloadManager.class.getSimpleName();
    private static final int DOWNLOAD_BUFFER_SIZE = 1024*1024;

    /** 用于记录观察者，当信息发送了改变，需要通知他们 */
    private Map<String, DownloadObserver> mObservers = new ConcurrentHashMap<String, DownloadObserver>();
    /** 用于记录所有下载的任务，方便在取消下载时，通过url能找到该任务进行删除 */
    private Map<String, DownLoadTask> mTaskMap = new ConcurrentHashMap<String, DownLoadTask>();
    /** 全局记录当前正在下载的bean */
    private MediaBean mCacheBean;

    private Context mContext;

    OkHttpClient mClient = new OkHttpClient();

    private static DownloadManager instance;

    public DownloadManager(Context context) {
        this.mContext = context;
    }

    public static DownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new DownloadManager(context);
        }
        return instance;
    }

    /** 注册观察者 */
    public void registerObserver(String id, DownloadObserver observer) {
        if (!mObservers.containsKey(id)) {
            mObservers.put(id, observer);
        }

    }

    /** 移除所有观察者 */
    public void removeAllObserver() {
        mObservers.clear();
    }

    /** 移除对应url的观察者 */
    public void removeObserver(String url) {
        mObservers.remove(url);
    }

    /** 开启下载，需要传入一个MediaBean对象 */
    public void download(MediaBean loadBean) {

        if (mTaskMap.containsKey(loadBean.getUrl())) {
            Log.w(TAG,"download task has in queue!");
            return;
        }

        // 先判断是否有这个app的下载信息
        MediaBean bean = new MediaBeanDao(mContext).queryById(loadBean.getPath());
        // 如果没有，则根据loadBean创建一个新的下载信息
        if (bean == null) {
            Log.w(TAG,"download bean is not in database!" + loadBean.toString());
            return;
        }

        // 判断状态是否为STATE_NONE、STATE_PAUSED、STATE_ERROR、STATE_DELETE。只有这4种状态才能进行下载，其他状态不予处理
        if (bean.getDownloadState() == STATE_NONE
                || bean.getDownloadState() == STATE_PAUSED
                || bean.getDownloadState() == STATE_DELETE
                || bean.getDownloadState() == STATE_ERROR) {
            Log.i(TAG, "download start:" + bean.toString());

            // 下载之前，把状态设置为STATE_WAITING，此时并没有产开始下载，只是把任务放入了线程池中
            //，当任务真正开始执行时，才会改为STATE_DOWNLOADING
            bean.setDownloadState(STATE_WAITING);
            // 每次状态发生改变，都需要回调该方法通知所有观察者
            notifyDownloadStateChanged(bean);

            // 创建一个下载任务，放入线程池
            DownLoadTask task = new DownLoadTask(bean);
            // 线程放入map里面方便管理
            mTaskMap.put(bean.getUrl(), task);

            DownloadExecutor.execute(task);
        } else if (bean.getDownloadState() == STATE_DOWNLOADING // 如果正在下载则暂停
                || bean.getDownloadState() == STATE_WAITING) {
            Log.i(TAG, "download pause:" + bean.toString());
            if (mTaskMap.containsKey(bean.getUrl())) {
                DownLoadTask task = mTaskMap.get(bean.getUrl());
                task.bean.setDownloadState(STATE_PAUSED);
                new MediaBeanDao(mContext).update(task.bean);
                // 在下载队列里取消排队线程
                if (DownloadExecutor.cancel(task)) {
                    mObservers.get(bean.getUrl()).onStop(task.bean);
                }
            }
        }else if(bean.getDownloadState() == STATE_START){
            Log.i(TAG, "ERROR code 0!");
        }
    }

    /** 暂停下载，需要传入一个MediaBean对象 */
    public void pause(MediaBean bean) {
        Log.i(TAG, "pause a download task:" + bean.toString());
        if (mTaskMap.containsKey(bean.getUrl())) {
            // 拿到当前任务
            DownLoadTask task = mTaskMap.get(bean.getUrl());
            // 将任务状态设置为暂停
            task.bean.setDownloadState(STATE_PAUSED);
            bean.setDownloadState(STATE_PAUSED);

            notifyDownloadStateChanged(bean);

            // 在下载队列里取消排队线程
            if (DownloadExecutor.cancel(task)) {
                mObservers.get(bean.getUrl()).onStop(task.bean);
            }

            //不用删除文件
        } else {
            Log.e(TAG, "can not pause a download task not in mTaskMap!");
        }
    }

    /** 删除当前正在下载的任务 */
    public void cancel(MediaBean bean) {
        if (mTaskMap.containsKey(bean.getUrl())) {
            // 拿到当前任务
            DownLoadTask task = mTaskMap.get(bean.getUrl());
            // 暂停下载任务(等于取消了该线程)
            task.bean.setDownloadState(STATE_DELETE);

            // 再更改删除界面状态(这是也调一次是怕在没下载的时候删除)
            bean.setDownloadState(STATE_DELETE);
            notifyDownloadStateChanged(bean);

            // 删除文件
            File file = new File(bean.getPath());
            if (file.exists()) {
                file.delete();
            }
            file = null;
        }
    }

    /** 销毁的时候关闭线程池以及当前执行的线程，并清空所有数据和把当前下载状态存进数据库 */
    public void destroy() {
        if (mCacheBean != null) {
            mCacheBean.setDownloadState(STATE_PAUSED);
            new MediaBeanDao(mContext).update(mCacheBean);
        }
        DownloadExecutor.stop();
        for (Map.Entry<String, DownLoadTask> element : mTaskMap.entrySet()) {
            new MediaBeanDao(mContext).update(element.getValue().bean);
        }
        mObservers.clear();
        mTaskMap.clear();

    }

    public File getFileByUrl(String url) {
        String desFilePath = PathUtil.pathGenerate(PATH_FILE_DOWNLOAD_TEMP, url);
        File file = new File(desFilePath);

        if (!file.exists()) {
            try {
                File fileParent = file.getParentFile();
                if (!fileParent.exists()) {
                    fileParent.mkdirs();
                }
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    /** 当下载状态发送改变的时候回调 */
    private ExecuteHandler mHandler = new ExecuteHandler();

    /** 拿到主线程Looper */
    @SuppressLint("HandlerLeak")
    private class ExecuteHandler extends Handler {
        private ExecuteHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            MediaBean bean = (MediaBean) msg.obj;
            if (mObservers.containsKey(bean.getUrl())) {
                DownloadObserver observer = mObservers.get(bean.getUrl());
                switch (bean.getDownloadState()) {
                    case STATE_START:// 开始下载
                        observer.onStart(bean);
                        break;
                    case STATE_WAITING:// 准备下载
                        observer.onPrepare(bean);
                        break;
                    case STATE_DOWNLOADING:// 下载中
                        observer.onProgress(bean);
                        break;
                    case STATE_PAUSED:// 暂停
                        observer.onStop(bean);
                        break;
                    case STATE_DOWNLOADED:// 下载完毕
                        observer.onFinish(bean);
                        break;
                    case STATE_ERROR:// 下载失败
                        observer.onError(bean);
                        break;
                    case STATE_DELETE:// 删除成功
                        observer.onDelete(bean);
                        break;
                    case STATE_NONE:
                        observer.onEnqueue(bean);
                        break;
                }
            }
        }
    }

    /** 当下载状态发送改变的时候调用 */
    private void notifyDownloadStateChanged(MediaBean bean) {

        if (bean.getDownloadState()==STATE_DOWNLOADING) {
            long duringProgress = bean.getDownloadProgress() - mTaskMap.get(bean.getUrl()).getLastUpdateProgress();
            if (duringProgress<1024*1024*3) {
                // 防止频繁更新UI状态与数据库
                // 下载了1M以上才通知进度条与数据库更新
                return;
            }
            mTaskMap.get(bean.getUrl()).setLastUpdateProgress(bean.getDownloadProgress());
        }

        new MediaBeanDao(mContext).update(bean);

        Message message = mHandler.obtainMessage();
        message.obj = bean;
        mHandler.sendMessage(message);
    }

    public class DownLoadTask implements Runnable {

        private MediaBean bean;

        private long lastUpdateProgress;

        public void setLastUpdateProgress(long lastUpdateProgress) {
            this.lastUpdateProgress = lastUpdateProgress;
        }

        public long getLastUpdateProgress() {
            return lastUpdateProgress;
        }

        public DownLoadTask(MediaBean bean) {
            this.bean = bean;
            lastUpdateProgress = 0;
        }

        @Override
        public void run() {
            if (bean.getDownloadState() == STATE_PAUSED) {          // 等待中就暂停了
                return;
            } else if (bean.getDownloadState() == STATE_DELETE) {   // 等待中就被取消了
                mTaskMap.remove(bean.getUrl());
                return;
            }

            // 获取实际下载长度
            Response fileLengthRes = null;
            Request request = new Request.Builder().url(bean.getUrl()).build();
            try {
                fileLengthRes = mClient.newCall(request).execute();
            } catch (IOException e) {
                Log.e(TAG,"IOException in get file length" + e.toString());
            }

            if (fileLengthRes != null && fileLengthRes.isSuccessful()) {
                bean.setSize(fileLengthRes.body().contentLength());
                // todo 是否写数据库
            } else {
                Log.e(TAG, "get file length  Response failed!");
                bean.setDownloadState(STATE_ERROR);
                notifyDownloadStateChanged(bean);
                mTaskMap.remove(bean.getUrl());
                return;
            }

            bean.setDownloadProgress(STATE_START);// 开始下载
            notifyDownloadStateChanged(bean);

            // 当前下载的进度
            long completeSize = 0;
            File file = new File(bean.getPath());// 获取下载文件
            if (!file.exists()) {
                // 如果文件不存在
                completeSize = 0;

                try {
                    File fileParent = file.getParentFile();
                    if (!fileParent.exists()) {
                        fileParent.mkdirs();
                    }
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                // 如果存在就拿当前文件的长度，设置当前下载长度
                // (这样的好处就是不用每次在下载文件的时候都需要写入数据库才能记录当前下载的长度，一直操作数据库是很费资源的)
                completeSize = file.length();
            }

            bean.setDownloadProgress(completeSize);
            notifyDownloadStateChanged(bean);

            try {
                request = new Request.Builder().url(bean.getUrl())
                        .addHeader("Range", "bytes=" + completeSize + "-" + bean.getSize())
                        .build();
                Response response = mClient.newCall(request).execute();
                if (response == null || !response.isSuccessful()) {
                    Log.e(TAG, "download file response fail! ret code = " + response.code());
                    bean.setDownloadState(STATE_ERROR);
                    notifyDownloadStateChanged(bean);
                    mTaskMap.remove(bean.getUrl());
                    return;
                }

                Log.i(TAG, "~~debug~~ contentType:" + response.headers().get("Content-Type"));
                Log.i(TAG, "~~debug~~ contentSize:" + response.headers().get("Content-Length"));


                OutputStream os = new FileOutputStream(file, true);
                // 将要下载的文件写到保存在保存路径下的文件中
                InputStream is = response.body().byteStream();
                byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
                int length = -1;

                // 进入下载中状态
                bean.setDownloadState(STATE_DOWNLOADING);
                notifyDownloadStateChanged(bean);

                while ((length = is.read(buffer)) != -1) {
                    if (bean.getDownloadState() == STATE_PAUSED) {
                        os.close();
                        is.close();
                        return;
                    } else if (bean.getDownloadState() == STATE_DELETE) {
                        mTaskMap.remove(bean.getUrl());
                        os.close();
                        is.close();
                        file.delete();
                        return;
                    }

                    // 把当前下载的bean给全局记录的bean
                    mCacheBean = bean;
                    os.write(buffer, 0, length);
                    completeSize += length;

                    // 用消息将下载信息传给进度条，对进度条进行更新
                    bean.setDownloadProgress(completeSize);
                    notifyDownloadStateChanged(bean);
                }

                // while循环结束表明全部下载完成,那么通过文件长度检查下载结果
                if (bean.getSize() == bean.getDownloadProgress()) {
                    bean.setDownloadState(STATE_DOWNLOADED);
                    Log.d(TAG, "download success!" + bean.toString());
                    os.close();
                    is.close();
                    // todo rename move
                } else {
                    Log.d(TAG, "download fail!" + bean.toString());
                    bean.setDownloadState(STATE_ERROR);
                    bean.setDownloadProgress(0);
                    os.close();
                    is.close();
                    file.delete();
                }
            } catch (IOException e) {
                // 错误状态需要删除文件
                Log.d(TAG, "IOException in download " + e.toString());
                bean.setDownloadState(STATE_ERROR);
                bean.setDownloadProgress(0);
                file.delete();
            }
            notifyDownloadStateChanged(bean);
            mTaskMap.remove(bean.getUrl());
        }
    }

}
package com.whitesky.tv.projectorlauncher.service.download;

import android.util.Log;

import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.utils.FileUtil;
import com.whitesky.tv.projectorlauncher.utils.PathUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileLock;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_DOWNLOADED;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_DOWNLOADING;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_ERROR;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_NONE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_PAUSED;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_START;
import static com.whitesky.tv.projectorlauncher.utils.PathUtil.getDownloadTempFileByUrl;

/**
 * Created by jeff on 18-3-19.
 */

public class DownloadRunnable implements Runnable {
    private static final String TAG = DownloadRunnable.class.getSimpleName();

    private static final int DOWNLOAD_BUFFER_SIZE = 1024*512;
    private static final int PROGRESS_UPDATE_SIZE = 1024*1024*2;

    private DownloadCallback mCallback;
    private RunNotify mManagerNotify;    //和mCallback的调用顺序，一定保证先Notify manager 再 Callback外部
    private MediaBean mEntity;
    private OkHttpClient mClient;
    private long lastUpdateProgress;

    private FileLock fileLock = null;

    public interface RunNotify {
        void notifyReturn(String url);
    }

    public DownloadRunnable(MediaBean mEntity, OkHttpClient client, RunNotify mNotify) {
        this.mEntity = mEntity;
        this.mManagerNotify = mNotify;
        this.mClient = client;
        lastUpdateProgress = 0;
    }

    public DownloadCallback getCallback() {
        return mCallback;
    }

    public void setCallback(DownloadCallback callback) {
        this.mCallback = callback;
    }

    public MediaBean getEntity() {
        return mEntity;
    }

    private void runnableErrorReturn() {
        mEntity.setDownloadState(STATE_DOWNLOAD_ERROR);
        if (mManagerNotify!=null) mManagerNotify.notifyReturn(mEntity.getUrl());
        if(mCallback!=null) mCallback.onError(mEntity);
        try {
            if (fileLock != null) fileLock.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

    @Override
    public void run() {
        Log.i(TAG, "download runnable start running:" + mEntity.getPath());
        if (mEntity.getDownloadState() == STATE_DOWNLOAD_PAUSED
                || mEntity.getDownloadState() == STATE_DOWNLOAD_NONE) {  // 等待中就暂停/取消了
            //因为有断点续传,所以即使任务没开始,也需要删除下载文件,防止上次关机之前下载的文件在
            if ( mEntity.getDownloadState() == STATE_DOWNLOAD_NONE ) {
                File downloadTempFile = getDownloadTempFileByUrl(new File(mEntity.getPath()).getParent(),mEntity.getUrl());
                if (downloadTempFile.exists()) {
                    downloadTempFile.delete();
                }
            }

            return;
        }

        // 加文件锁,防止两个进程同时写一个文件
        FileUtil.createFile(PathUtil.getDownloadTempFileLockPathByUrl(mEntity.getUrl()));
        try {
            fileLock = new FileOutputStream(PathUtil.getDownloadTempFileLockPathByUrl(mEntity.getUrl())).getChannel().tryLock();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (fileLock == null ) {
            Log.e(TAG, " already has a thread downloading,just return");
            return;
        }

        // 获取实际下载长度
        Request request = new Request.Builder().url(mEntity.getUrl()).build();
        try {
            Response fileLengthRes = mClient.newCall(request).execute();
            if (fileLengthRes != null && fileLengthRes.isSuccessful()) {
                mEntity.setSize(fileLengthRes.body().contentLength());
                // 因为每次下载都是从网络获取长度,所以这边就不记到数据库了,下载完成后刷新列表,这个时候由磁盘内容更新数据库
            } else {
                Log.e(TAG, "get file length  Response failed!");
                runnableErrorReturn();
            }
        } catch (IOException e) {
            Log.e(TAG,"IOException in get file length" + e.toString());
            runnableErrorReturn();
        }

        mEntity.setDownloadState(STATE_DOWNLOAD_START);
        if(mCallback!=null) mCallback.onStateChange(mEntity);

        // 当前下载的进度
        long completeSize = 0;
        // 磁盘检查下载文件是否存在
        File finalFile = new File(mEntity.getPath());
        File tempFile = getDownloadTempFileByUrl(new File(mEntity.getPath()).getParent(),mEntity.getUrl());

        if (!tempFile.exists()) {
            // 如果文件不存在
            completeSize = 0;

            try {
                File fileParent = tempFile.getParentFile();
                if (!fileParent.exists()) {
                    fileParent.mkdirs();
                }
                tempFile.createNewFile();
            } catch (IOException e) {
                Log.e(TAG,"IOException in create file" + e.toString());
                runnableErrorReturn();
            }

        } else {
            // 如果存在就拿当前文件的长度，设置为当前下载进度
            completeSize = tempFile.length();
        }

        if (completeSize >= mEntity.getSize()) {
            Log.e(TAG, "temp file size is larger than total file!" + mEntity.toString());

            mEntity.setDownloadState(STATE_DOWNLOAD_ERROR);
            mEntity.setDownloadProgress(0);
            tempFile.delete();
            runnableErrorReturn();
        }

        lastUpdateProgress = completeSize;
        mEntity.setDownloadProgress(completeSize);
        if(mCallback!=null) mCallback.onProgress(mEntity);

        try {
            request = new Request.Builder().url(mEntity.getUrl())
                    .addHeader("Range", "bytes=" + completeSize + "-" + (mEntity.getSize() - 1))
                    .build();
            Response response = mClient.newCall(request).execute();
            if (response == null || !response.isSuccessful()) {
                Log.e(TAG, "response fail in downloading! ret code = " + response.code());
                runnableErrorReturn();
            }

            OutputStream os = new FileOutputStream(tempFile, true);
            InputStream is = response.body().byteStream();
            byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];

            // 进入下载中状态
            mEntity.setDownloadState(STATE_DOWNLOAD_DOWNLOADING);
            if(mCallback!=null) mCallback.onStateChange(mEntity);

            int length;
            while ((length = is.read(buffer)) != -1) {
                if (mEntity.getDownloadState() == STATE_DOWNLOAD_PAUSED
                        ||mEntity.getDownloadState() == STATE_DOWNLOAD_NONE) {
                    os.close();
                    is.close();

                    if (mEntity.getDownloadState() == STATE_DOWNLOAD_NONE) {
                        tempFile.delete();
                    }

                    try {
                        if (fileLock != null) fileLock.release();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }

                os.write(buffer, 0, length);
                completeSize += length;

                // 用消息将下载信息传给进度条，对进度条进行更新
                mEntity.setDownloadProgress(completeSize);

                if (completeSize - lastUpdateProgress > PROGRESS_UPDATE_SIZE) {
                    if(mCallback!=null) mCallback.onProgress(mEntity);
                    lastUpdateProgress = completeSize;
                }
            }

            // while循环结束表明全部下载完成,那么通过文件长度检查下载结果
            if (mEntity.getSize() == mEntity.getDownloadProgress()) {

                Log.d(TAG, "download check success!" + mEntity.toString());

                mEntity.setDownloadState(STATE_DOWNLOAD_DOWNLOADED);
                os.close();
                is.close();
                tempFile.renameTo(finalFile);
                if (mManagerNotify!=null) mManagerNotify.notifyReturn(mEntity.getUrl());
                if (mCallback!=null) mCallback.onFinish(mEntity);

            } else {

                Log.d(TAG, "download check fail!" + mEntity.toString());

                mEntity.setDownloadState(STATE_DOWNLOAD_ERROR);
                mEntity.setDownloadProgress(0);
                os.close();
                is.close();
                tempFile.delete();
                if (mManagerNotify!=null) mManagerNotify.notifyReturn(mEntity.getUrl());
                if(mCallback!=null) mCallback.onError(mEntity);
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException in downloading " + e.toString());
            runnableErrorReturn();
        } finally {
            try {
                if (fileLock != null) fileLock.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
    }
}
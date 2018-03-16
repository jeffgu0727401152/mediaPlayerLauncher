package com.whitesky.tv.projectorlauncher.service.download;

/**
 * Created by jeff on 18-3-14.
 */

import com.whitesky.tv.projectorlauncher.media.db.MediaBean;

/**
 *  观察者
 */
public interface DownloadObserver {
    /**准备下载*/
    void onPrepare(MediaBean bean);
    /** 开始下载 */
    void onStart(MediaBean bean);
    /** 下载中 */
    void onProgress(MediaBean bean);
    /** 暂停 */
    void onStop(MediaBean bean);
    /** 下载完成 */
    void onFinish(MediaBean bean);
    /** 下载失败 */
    void onError(MediaBean bean);
    /** 删除成功 */
    void onDelete(MediaBean bean);
    /** 新增下载*/
    void onEnqueue(MediaBean bean);
}
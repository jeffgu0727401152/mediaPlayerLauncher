package com.whitesky.tv.projectorlauncher.service.download;

import com.whitesky.tv.projectorlauncher.media.db.MediaBean;

/**
 * Created by jeff on 18-3-19.
 */

public interface DownloadCallback {
    // 所有由于DownloadManager导致的状态变化,要向上回报
    // 使用部分调用DownloadManager提供的接口,不直接修改bean状态

    /** 其他状态改变 */
    void onStateChange(MediaBean bean);
    /** 更新进度 */
    void onProgress(MediaBean bean);
    /** 完成 */
    void onFinish(MediaBean bean);
    /** 失败 */
    void onError(MediaBean bean);
}

package com.whitesky.tv.projectorlauncher.service.mqtt;

import android.content.Context;

import com.whitesky.tv.projectorlauncher.media.bean.PlayListBean;
import com.whitesky.tv.projectorlauncher.media.bean.Result;
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBeanDao;
import com.whitesky.tv.projectorlauncher.service.mqtt.bean.MediaListPushBean;
import com.whitesky.tv.projectorlauncher.utils.MediaScanUtil;
import com.whitesky.tv.projectorlauncher.utils.PathUtil;

import java.io.File;
import java.util.List;

import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.PICTURE_DEFAULT_PLAY_DURATION_MS;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOADED;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_NONE;
import static com.whitesky.tv.projectorlauncher.utils.PathUtil.PATH_FILE_FROM_CLOUD_FREE;

/**
 * Created by jeff on 18-3-9.
 */

public class DataListCovert {
    private static final String TAG = DataListCovert.class.getSimpleName();

    // 将云端推送过来的播放列表,转换为本地播放列表格式
    public static void covertCloudPushToPlayList(Context context, List<PlayListBean> desList, List<MediaListPushBean> srcList) {
        if (desList==null || srcList==null) {
            return;
        }

        desList.clear();
        for (int i = 0; i < srcList.size(); i++) {
            MediaBean media =  new MediaBeanDao(context).queryById(srcList.get(i).getName());
            if (media != null) {
                if (media.getType() == MediaBean.MEDIA_PICTURE) {
                    // duration单位,网页操作是s,本机是ms,网络传输使用ms,限定图片最小播放时间是5秒
                    media.setDuration(srcList.get(i).getDuration()>=5000 ? srcList.get(i).getDuration() : PICTURE_DEFAULT_PLAY_DURATION_MS);
                }

                PlayListBean pListItem = new PlayListBean(media);
                pListItem.setPlayScale(srcList.get(i).getScale());
                desList.add(pListItem);
            }
        }
    }

    // 将云端获取的文件列表,转换为本地数据库列表格式
    public static void covertCloudResultToMediaList(Context context, List<MediaBean> desList, List<Result> srcList) {
        if (desList==null || srcList==null) {
            return;
        }

        desList.clear();
        for (int i = 0; i < srcList.size(); i++) {
            Result srcItem = srcList.get(i);
            String localStoreLocation = PathUtil.pathGenerate(PATH_FILE_FROM_CLOUD_FREE, srcItem.getName());

            MediaBean desItem = new MediaBean(srcItem.getName(),
                    srcItem.getId(),
                    MediaScanUtil.getMediaTypeFromPath(srcItem.getName()),
                    srcItem.getSource(), localStoreLocation,0,0);
            desItem.setUrl(srcItem.getUrl());

            // 判断本地是否存在
            File localStoreFile = new File(localStoreLocation);
            if (localStoreFile.exists() && !localStoreFile.isDirectory()) {
                desItem.setDownloadState(STATE_DOWNLOADED);
                desItem.setSize(localStoreFile.length());
                // 媒体时间的获取在后续的云文件夹遍历中做
            } else {
                desItem.setDownloadState(STATE_NONE);
            }

            desList.add(desItem);
        }
    }
}

package com.whitesky.tv.projectorlauncher.utils;

import android.content.Context;
import android.util.Log;

import com.whitesky.tv.projectorlauncher.media.bean.PlayListBean;
import com.whitesky.tv.projectorlauncher.media.bean.Result;
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBeanDao;
import com.whitesky.tv.projectorlauncher.service.MediaListPushBean;

import java.io.File;
import java.util.List;

import static com.whitesky.tv.projectorlauncher.common.Contants.CLOUD_MEDIA_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.LOCAL_MASS_STORAGE_PATH;
import static com.whitesky.tv.projectorlauncher.common.Contants.LOCAL_MEDIA_FOLDER;
import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.PICTURE_DEFAULT_PLAY_DURATION_MS;

/**
 * Created by jeff on 18-3-9.
 */

public class CovertUtil {
    private static final String TAG = CovertUtil.class.getSimpleName();

    public static final int PATH_FILE_EXPORT_TO_USB = 0;
    public static final int PATH_FILE_IMPORT_TO_LOCAL = 1;
    public static final int PATH_FILE_FROM_CLOUD = 2;

    public static String pathCovert(int pathType, String path) {
        String result = "";
        String basePath = "";

        switch (pathType) {
            case PATH_FILE_EXPORT_TO_USB:
                // todo
                break;
            case PATH_FILE_IMPORT_TO_LOCAL:
                basePath = LOCAL_MASS_STORAGE_PATH + File.separator + LOCAL_MEDIA_FOLDER;
                break;
            case PATH_FILE_FROM_CLOUD:
                basePath = LOCAL_MASS_STORAGE_PATH + File.separator + CLOUD_MEDIA_FOLDER;
                break;
            default:
                break;
        }

        if (basePath.isEmpty()) {
            return "";
        }

        result = basePath + File.separator + FileUtil.getFilePrefix(path) + "." + FileUtil.getFileExtension(path);
        Log.d(TAG, "pathCovert:" + result);
        return result;
    }

    // 将云端推送过来的播放列表,转换为本地播放列表格式
    public static void covertPlayList(Context context, List<PlayListBean> desList, List<MediaListPushBean> srcList) {
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
    public static void covertMediaList(Context context, List<MediaBean> desList, List<Result> srcList) {
        if (desList==null || srcList==null) {
            return;
        }

        desList.clear();
        for (int i = 0; i < srcList.size(); i++) {
            MediaBean desItem = new MediaBean();
            Result srcItem = srcList.get(i);
            desItem.setId(srcItem.getId());
            desItem.setTitle(srcItem.getName());
            desItem.setSource(srcItem.getSource());
            desItem.setUrl(srcItem.getUrl());
            desItem.setDownload(false);
            desItem.setType(MediaScanUtil.getMediaTypeFromPath(srcItem.getName()));
            desItem.setPath(pathCovert(PATH_FILE_FROM_CLOUD,srcItem.getName()));
            desItem.setDuration(0);
            desItem.setSize(0);

            desList.add(desItem);
        }
    }
}

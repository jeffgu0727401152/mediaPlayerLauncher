package com.whiteskycn.tv.projectorlauncher.utils;

/**
 * Created by jeff on 18-1-18.
 */

import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.MEDIA_MUSIC;
import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.MEDIA_PICTURE;
import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.MEDIA_VIDEO;


public class MediaScanUtil {
    private final String TAG = this.getClass().getSimpleName();

    private MediaPlayer mp;
    private MediaFileScanListener mMediaFileScanListener;
    private boolean isNeedDuration = false;
    private boolean isNeedSize = false;

    public MediaScanUtil() {
        mMediaFileScanListener = null;
        mp = new MediaPlayer();
    }

    public MediaScanUtil(MediaFileScanListener mediaFileScanListener) {
        this.mMediaFileScanListener = mediaFileScanListener;
        mp = new MediaPlayer();
    }

    public void safeScanning(String path) {
        final File folder = new File(path);

        if (mMediaFileScanListener!=null)
        {
            mMediaFileScanListener.onMediaScanBegin();
        }

        if (!folder.exists()) {
            Log.i(TAG,"path not exist!");
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                scanning(folder,true);
            }
        }).start();
    }

    /**
     * 遍历指定文件夹下的资源文件
     *
     * @param folder 文件
     * @param needReport scan完需要回报
     */
    private void scanning(File folder, boolean needReport) {
        //指定正则表达式
        Pattern mPattern = Pattern.compile("([^\\.]*)\\.([^\\.]*)");
        // 当前目录下的所有文件
        final String[] filenames = folder.list();
        // 当前目录的名称
        //final String folderName = folder.getName();
        // 当前目录的绝对路径
        //final String folderPath = folder.getAbsolutePath();

        if (filenames != null) {
            // 遍历当前目录下的所有文件
            for (String name : filenames) {
                File file = new File(folder, name);
                // 如果是文件夹则继续递归当前方法
                if (file.isDirectory()) {
                    scanning(file,false);
                } else {
                // 如果是文件则对文件进行相关操作
                    Matcher matcher = mPattern.matcher(name);
                    if (matcher.matches()) {
                        // 文件名称
                        String fileName = matcher.group(1);
                        // 文件后缀
                        String fileExtension = matcher.group(2);
                        // 文件路径
                        String filePath = file.getAbsolutePath();
                        if (isMusic(fileExtension)) {
                            // 初始化音乐文件......................
                            Log.e(TAG,"This file is Music File,fileName=" + fileName + "."
                                    + fileExtension + ",filePath=" + filePath);
                            if (mMediaFileScanListener!=null)
                            {
                                int duration = 0;
                                if (mp!=null && isNeedDuration)
                                {
                                    duration = getMediaDuration(filePath);
                                }

                                long size = 0;
                                if (isNeedSize)
                                {
                                    try {
                                        size = FileUtil.getFileSize(filePath);
                                    } catch (Exception e) {
                                        Log.e(TAG,"get file size error!" + e);
                                    }
                                }
                                mMediaFileScanListener.onFindMedia(MEDIA_MUSIC,fileName, fileExtension, filePath, duration, size);
                            }
                        }

                        if (isPicture(fileExtension)) {
                            // 初始化图片文件......................
                            Log.e(TAG,"This file is Photo File,fileName=" + fileName + "."
                                    + fileExtension + ",filePath=" + filePath);
                            if (mMediaFileScanListener!=null)
                            {
                                long size = 0;
                                if (isNeedSize)
                                {
                                    try {
                                        size = FileUtil.getFileSize(filePath);
                                    } catch (Exception e) {
                                        Log.e(TAG,"get file size error!" + e);
                                    }
                                }
                                mMediaFileScanListener.onFindMedia(MEDIA_PICTURE, fileName, fileExtension, filePath,0, size);
                            }
                        }

                        if (isVideo(fileExtension)) {
                            // 初始化视频文件......................
                            Log.e(TAG,"This file is Video File,fileName=" + fileName + "."
                                    + fileExtension + ",filePath=" + filePath);
                            if (mMediaFileScanListener!=null)
                            {
                                int duration = 0;
                                if (mp!=null && isNeedDuration)
                                {
                                    duration = getMediaDuration(filePath);
                                }

                                long size = 0;
                                if (isNeedSize)
                                {
                                    try {
                                        size = FileUtil.getFileSize(filePath);
                                    } catch (Exception e) {
                                        Log.e(TAG,"get file size error!" + e);
                                    }
                                }
                                mMediaFileScanListener.onFindMedia(MEDIA_VIDEO, fileName, fileExtension, filePath, duration, size);
                            }
                        }
                    }
                }
            }
        }
        if (needReport && mMediaFileScanListener!=null)
        {
            mMediaFileScanListener.onMediaScanDone();
        }
    }

    /**
     * 判断是否是音乐文件
     *
     * @param extension 后缀名
     * @return
     */
    public static boolean isMusic(String extension) {

        if (extension == null) {
            return false;
        }

        extension=extension.replace(".","");

        final String ext = extension.toLowerCase();
        if (ext.equals("mp3") || ext.equals("m4a") || ext.equals("wav") || ext.equals("amr") || ext.equals("awb") ||
                ext.equals("aac") || ext.equals("flac") || ext.equals("mid") || ext.equals("midi") ||
                ext.equals("xmf") || ext.equals("rtttl") || ext.equals("rtx") || ext.equals("ota") ||
                ext.equals("wma") || ext.equals("ra") || ext.equals("mka") || ext.equals("m3u") || ext.equals("pls")) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否是图像文件
     *
     * @param extension 后缀名
     * @return
     */
    public static boolean isPicture(String extension) {

        if (extension == null) {
            return false;
        }

        extension=extension.replace(".","");

        final String ext = extension.toLowerCase();
        if (ext.endsWith("jpg") || ext.endsWith("jpeg") || ext.endsWith("gif") || ext.endsWith("png") ||
                ext.endsWith("bmp") || ext.endsWith("wbmp")) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否是视频文件
     *
     * @param extension 后缀名
     * @return
     */
    public static boolean isVideo(String extension) {

        if (extension == null) {
            return false;
        }

        extension=extension.replace(".","");

        final String ext = extension.toLowerCase();
        if (ext.endsWith("mpeg") || ext.endsWith("mp4") || ext.endsWith("mov") || ext.endsWith("m4v") ||
                ext.endsWith("3gp") || ext.endsWith("3gpp") || ext.endsWith("3g2") ||
                ext.endsWith("3gpp2") || ext.endsWith("avi") || ext.endsWith("px") ||
                ext.endsWith("wmv") || ext.endsWith("asf") || ext.endsWith("flv") ||
                ext.endsWith("mkv") || ext.endsWith("mpg") || ext.endsWith("rmvb") ||
                ext.endsWith("rm") || ext.endsWith("vob") || ext.endsWith("f4v")) {
            return true;
        }
        return false;
    }

    public int getMediaDuration(String path)
    {
        File file = new File(path);
        if (!file.exists()) {
            Log.e(TAG, "视频文件路径错误!!!");
            return -1;
        }

        try {
            mp.reset();
            mp.setDataSource(file.getAbsolutePath());
            mp.prepare();
        } catch (IOException e) {
            Log.e(TAG, "getMediaDuration error!" + e);
        }

        return mp.getDuration();
    }

    public boolean isNeedDuration() {
        return isNeedDuration;
    }

    public void setNeedDuration(boolean needDuration) {
        isNeedDuration = needDuration;
    }

    public boolean isNeedSize() {
        return isNeedSize;
    }

    public void setNeedSize(boolean needSize) {
        isNeedSize = needSize;
    }

    //内置存储设备用
    public interface MediaFileScanListener {
        /**
         * 查找到一个媒体文件的时候所需要进行的操作
         *
         * @param type          当前查找到媒体文件的类型
         * @param name          当前查找到媒体文件的名字
         * @param extension     当前查找到媒体文件的扩展名
         * @param path          当前查找到媒体文件的路径
         * @param duration      当前查找到媒体文件的时间长度
         */
        void onFindMedia(int type, String name, String extension, String path, int duration, long size);
        void onMediaScanBegin();
        void onMediaScanDone();
    }

    public void setMediaFileScanListener(MediaFileScanListener mediaFileScanListener) {
        this.mMediaFileScanListener = mediaFileScanListener;
    }
}
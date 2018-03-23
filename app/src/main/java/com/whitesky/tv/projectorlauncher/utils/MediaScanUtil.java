package com.whitesky.tv.projectorlauncher.utils;

/**
 * Created by jeff on 18-1-18.
 */

import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.whitesky.tv.projectorlauncher.media.db.MediaBean;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.whitesky.tv.projectorlauncher.media.PictureVideoPlayer.PICTURE_DEFAULT_PLAY_DURATION_MS;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_MUSIC;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_PICTURE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_VIDEO;


public class MediaScanUtil {
    private final String TAG = this.getClass().getSimpleName();

    private MediaFileScanListener mMediaFileScanListener = null;
    ExecutorService mBackgroundService = Executors.newSingleThreadExecutor();;    // 保证单线程
    private boolean isNeedDuration = false;
    private boolean isNeedSize = false;
    private boolean isRunning = false;

    public MediaScanUtil() {
        mMediaFileScanListener = null;
    }

    public MediaScanUtil(MediaFileScanListener mediaFileScanListener) {
        this.mMediaFileScanListener = mediaFileScanListener;
    }

    public void release()
    {
        if (mBackgroundService != null) {
            mBackgroundService.shutdownNow();
            mBackgroundService = null;
        }
    }

    public int safeScanning(String path) {
        final File folder = new File(path);

        if (!folder.exists() || !folder.isDirectory()) {
            Log.e(TAG,"path not exist!");
            return -2;
        }

        if (isRunning) {
            Log.d(TAG,"already running a scan task!");
            return -1;
        }

        mBackgroundService.execute(new Thread(new Runnable() {
            public void run() {
                isRunning = true;
                if (mMediaFileScanListener!=null)
                {
                    mMediaFileScanListener.onMediaScanBegin();
                }
                scanning(folder,true);
                isRunning = false;
            }
        }));

        return 0;
    }

    /**
     * 遍历指定文件夹下的资源文件
     *
     * @param folder 文件
     * @param needReport scan完需要回报
     */
    private void scanning(File folder, boolean needReport) {

        // 当前目录下的所有文件
        final String[] filenames = folder.list();

        if (filenames != null) {
            // 遍历当前目录下的所有文件
            for (String name : filenames) {
                File file = new File(folder, name);

                if (file.isDirectory()) { // 如果是文件夹则继续递归当前方法

                    scanning(file, false);

                } else { // 如果是文件则对文件进行相关操作

                    String fileName = file.getName();
                    String filePath = file.getAbsolutePath();
                    String fileExtension = FileUtil.getFileExtension(fileName);

                    if (isMusic(fileExtension)) {

                        Log.d(TAG, "Music File,fileName=" + fileName + "."
                                + fileExtension + ",filePath=" + filePath);
                        if (mMediaFileScanListener != null) {
                            int duration = 0;
                            if (isNeedDuration) {
                                duration = getMediaDuration(filePath);
                            }

                            long size = 0;
                            if (isNeedSize) {
                                try {
                                    size = FileUtil.getFileSize(filePath);
                                } catch (Exception e) {
                                    Log.e(TAG, "get file size error!" + e);
                                }
                            }
                            mMediaFileScanListener.onFindMedia(MEDIA_MUSIC, fileName, fileExtension, filePath, duration, size);
                        }
                    } else if (isPicture(fileExtension)) {
                        // 初始化图片文件......................
                        Log.d(TAG, "Picture File,fileName=" + fileName + "."
                                + fileExtension + ",filePath=" + filePath);
                        if (mMediaFileScanListener != null) {
                            long size = 0;
                            if (isNeedSize) {
                                try {
                                    size = FileUtil.getFileSize(filePath);
                                } catch (Exception e) {
                                    Log.e(TAG, "get file size error!" + e);
                                }
                            }
                            mMediaFileScanListener.onFindMedia(MEDIA_PICTURE, fileName, fileExtension, filePath, PICTURE_DEFAULT_PLAY_DURATION_MS, size);
                        }
                    } else if (isVideo(fileExtension)) {
                        // 初始化视频文件......................
                        Log.d(TAG, "Video File,fileName=" + fileName + "."
                                + fileExtension + ",filePath=" + filePath);
                        if (mMediaFileScanListener != null) {
                            int duration = 0;
                            if (isNeedDuration) {
                                duration = getMediaDuration(filePath);
                            }

                            long size = 0;
                            if (isNeedSize) {
                                try {
                                    size = FileUtil.getFileSize(filePath);
                                } catch (Exception e) {
                                    Log.e(TAG, "get file size error!" + e);
                                }
                            }
                            mMediaFileScanListener.onFindMedia(MEDIA_VIDEO, fileName, fileExtension, filePath, duration, size);
                        }
                    } else {
                        Log.d(TAG, "Unknown File,fileName=" + fileName + "."
                                + fileExtension + ",filePath=" + filePath);
                    }
                }
            }
        }

        if (needReport && mMediaFileScanListener != null) {
            mMediaFileScanListener.onMediaScanDone();
        }
    }

    public static int getMediaTypeFromPath(String filePath)
    {
        String ext = FileUtil.getFileExtension(filePath);

        if (isVideo(ext))
        {
            return MediaBean.MEDIA_VIDEO;
        } else if (isPicture(ext)) {
            return MediaBean.MEDIA_PICTURE;
        } else if (isMusic(ext)){
            return MediaBean.MEDIA_MUSIC;
        } else {
            return MediaBean.MEDIA_UNKNOWN;
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
                ext.endsWith("rm") || ext.endsWith("vob") || ext.endsWith("f4v") || ext.endsWith("ts")) {
            return true;
        }
        return false;
    }

    public int getMediaDuration(String path)
    {
        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            Log.e(TAG, "file path not exists!!!");
            return -1;
        }

        switch (getMediaTypeFromPath(path)) {
            case MediaBean.MEDIA_PICTURE:
            case MediaBean.MEDIA_UNKNOWN:
                return PICTURE_DEFAULT_PLAY_DURATION_MS;

            case MediaBean.MEDIA_VIDEO:
            case MediaBean.MEDIA_MUSIC:
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(path);
                String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                mmr.release();
                return Integer.parseInt(duration);

            default:
                break;
        }

        return 0;
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
package com.whitesky.tv.projectorlauncher.utils;

import android.util.Log;

import java.io.File;

import static com.whitesky.tv.projectorlauncher.common.Contants.CLOUD_MEDIA_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.CLOUD_MEDIA_FREE_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.CLOUD_MEDIA_PRIVATE_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.CLOUD_MEDIA_TEMP_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.COPY_TO_USB_MEDIA_EXPORT_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.LOCAL_MASS_STORAGE_PATH;
import static com.whitesky.tv.projectorlauncher.common.Contants.LOCAL_MEDIA_FOLDER;

/**
 * Created by jeff on 18-3-15.
 */

public class PathUtil {
    private static final String TAG = PathUtil.class.getSimpleName();

    public static final int PATH_FILE_EXPORT_TO_USB = 0;
    public static final int PATH_FILE_IMPORT_TO_LOCAL = 1;
    public static final int PATH_FILE_FROM_CLOUD_FREE = 2;
    public static final int PATH_FILE_FROM_CLOUD_PRIVATE = 3;
    public static final int PATH_FILE_DOWNLOAD_TEMP = 8;

    public static String cloudFreeFileStoragePath() {
        return LOCAL_MASS_STORAGE_PATH + File.separator + CLOUD_MEDIA_FOLDER + File.separator + PATH_FILE_FROM_CLOUD_FREE;
    }

    public static String cloudPrivateFileStoragePath() {
        return LOCAL_MASS_STORAGE_PATH + File.separator + CLOUD_MEDIA_FOLDER + File.separator + PATH_FILE_FROM_CLOUD_PRIVATE;
    }

    public static String cloudDownloadTempStoragePath() {
        return LOCAL_MASS_STORAGE_PATH + File.separator + CLOUD_MEDIA_FOLDER + File.separator + PATH_FILE_DOWNLOAD_TEMP;
    }


    public static String localFileStoragePath() {
        return LOCAL_MASS_STORAGE_PATH + File.separator + LOCAL_MEDIA_FOLDER;
    }

    public static String pathGenerate(String originalPath, String destination) {
        String result = "";
        String basePath = "";
        String fileName = "";

        basePath = destination;
        fileName = FileUtil.getFileName(originalPath);

        if (basePath.isEmpty() || fileName.isEmpty()) {
            Log.e(TAG, "pathGenerate fail!");
            return "";
        }

        result = basePath + File.separator + fileName;
        Log.d(TAG, "pathGenerate success:" + result);

        return result;
    }

    public static String pathGenerate(int pathType, String originalPath) {
        return pathGenerate(pathType,originalPath,null);
    }

    public static String pathGenerate(int pathType, String originalPath, String usbBase) {
        String result = "";
        String basePath = "";
        String fileName = "";

        switch (pathType) {
            case PATH_FILE_EXPORT_TO_USB:
                if (usbBase!=null || !usbBase.isEmpty()) {
                    basePath = usbBase + File.separator + COPY_TO_USB_MEDIA_EXPORT_FOLDER;
                }
                fileName = FileUtil.getFilePrefix(originalPath) + "." + FileUtil.getFileExtension(originalPath);
                break;
            case PATH_FILE_IMPORT_TO_LOCAL:
                basePath = LOCAL_MASS_STORAGE_PATH + File.separator + LOCAL_MEDIA_FOLDER;
                fileName = FileUtil.getFilePrefix(originalPath) + "." + FileUtil.getFileExtension(originalPath);
                break;
            case PATH_FILE_FROM_CLOUD_FREE:
                basePath = LOCAL_MASS_STORAGE_PATH + File.separator + CLOUD_MEDIA_FOLDER + File.separator + CLOUD_MEDIA_FREE_FOLDER;
                fileName = FileUtil.getFilePrefix(originalPath) + "." + FileUtil.getFileExtension(originalPath);
                break;
            case PATH_FILE_FROM_CLOUD_PRIVATE:
                basePath = LOCAL_MASS_STORAGE_PATH + File.separator + CLOUD_MEDIA_FOLDER + File.separator + CLOUD_MEDIA_PRIVATE_FOLDER;
                fileName = FileUtil.getFilePrefix(originalPath) + "." + FileUtil.getFileExtension(originalPath);
                break;
            case PATH_FILE_DOWNLOAD_TEMP:
                basePath = LOCAL_MASS_STORAGE_PATH + File.separator + CLOUD_MEDIA_FOLDER + File.separator + CLOUD_MEDIA_TEMP_FOLDER;
                fileName = Md5Util.generateCode(originalPath);
                break;
            default:
                Log.e(TAG,"pathGenerate error pathType " + pathType);
                break;
        }

        if (basePath.isEmpty() || fileName.isEmpty()) {
            Log.e(TAG, "pathGenerate fail!");
            return "";
        }

        result = basePath + File.separator + fileName;
        Log.d(TAG, "pathGenerate success:" + result);
        return result;
    }
}

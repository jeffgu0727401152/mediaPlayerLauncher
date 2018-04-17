package com.whitesky.tv.projectorlauncher.utils;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import static com.whitesky.tv.projectorlauncher.common.Contants.CLOUD_MEDIA_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.CLOUD_MEDIA_FREE_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.CLOUD_MEDIA_PRIVATE_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.CLOUD_MEDIA_PUBLIC_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.COPY_TO_USB_MEDIA_EXPORT_FOLDER;
import static com.whitesky.tv.projectorlauncher.common.Contants.MASS_STORAGE_PATH;
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
    public static final int PATH_FILE_FROM_CLOUD_PUBLIC = 4;

    public static String cloudFreeFileStoragePath() {
        return MASS_STORAGE_PATH + File.separator + CLOUD_MEDIA_FOLDER + File.separator + CLOUD_MEDIA_FREE_FOLDER;
    }

    public static String cloudPrivateFileStoragePath() {
        return MASS_STORAGE_PATH + File.separator + CLOUD_MEDIA_FOLDER + File.separator + CLOUD_MEDIA_PRIVATE_FOLDER;
    }

    public static String cloudPublicFileStoragePath() {
        return MASS_STORAGE_PATH + File.separator + CLOUD_MEDIA_FOLDER + File.separator + CLOUD_MEDIA_PUBLIC_FOLDER;
    }

    public static String getDownloadTempFileLockPathByUrl(String url) {
        String path = "/tmp";
        String fileName = Md5Util.generateCode(url);
        return path + File.separator + fileName + ".lck";
    }

    public static File getDownloadTempFileByUrl(String parentFolder, String url) {
        String fileName = Md5Util.generateCode(url);
        String desFilePath = parentFolder + File.separator + fileName;
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

    public static String localFileStoragePath() {
        return MASS_STORAGE_PATH + File.separator + LOCAL_MEDIA_FOLDER;
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
                basePath = MASS_STORAGE_PATH + File.separator + LOCAL_MEDIA_FOLDER;
                fileName = FileUtil.getFilePrefix(originalPath) + "." + FileUtil.getFileExtension(originalPath);
                break;
            case PATH_FILE_FROM_CLOUD_FREE:
                basePath = MASS_STORAGE_PATH + File.separator + CLOUD_MEDIA_FOLDER + File.separator + CLOUD_MEDIA_FREE_FOLDER;
                fileName = FileUtil.getFilePrefix(originalPath) + "." + FileUtil.getFileExtension(originalPath);
                break;
            case PATH_FILE_FROM_CLOUD_PRIVATE:
                basePath = MASS_STORAGE_PATH + File.separator + CLOUD_MEDIA_FOLDER + File.separator + CLOUD_MEDIA_PRIVATE_FOLDER;
                fileName = FileUtil.getFilePrefix(originalPath) + "." + FileUtil.getFileExtension(originalPath);
                break;
            case PATH_FILE_FROM_CLOUD_PUBLIC:
                basePath = MASS_STORAGE_PATH + File.separator + CLOUD_MEDIA_FOLDER + File.separator + CLOUD_MEDIA_PUBLIC_FOLDER;
                fileName = FileUtil.getFilePrefix(originalPath) + "." + FileUtil.getFileExtension(originalPath);
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

package com.whitesky.tv.projectorlauncher.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeff on 18-1-16.
 */

public class FileUtil
{
    private static final String TAG = FileUtil.class.getSimpleName();
    private static final int COPY_STEP_BYTE = 1024*10;

    public class StorageInfo {
        public String path;
        public String state;
        public boolean isRemovable;

        public StorageInfo(String path) {
            this.path = path;
        }

        public boolean isMounted() {
            return "mounted".equals(state);
        }
    }

    public List listAvailableStorage(Context context) {
        ArrayList storage = new ArrayList();
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        try {
            Class<?>[] paramClasses = {};
            Method getVolumeList = StorageManager.class.getMethod("getVolumeList", paramClasses);
            getVolumeList.setAccessible(true);
            Object[] params = {};
            Object[] invokes = (Object[]) getVolumeList.invoke(storageManager, params);
            if (invokes != null) {
                StorageInfo info = null;
                for (int i = 0; i < invokes.length; i++) {
                    Object obj = invokes[i];
                    Method getPath = obj.getClass().getMethod("getPath");
                    String path = (String) getPath.invoke(obj, new Object[0]);
                    info = new StorageInfo(path);
                    File file = new File(info.path);
                    if ((file.exists()) && (file.isDirectory()) && (file.canWrite())) {
                        Method isRemovable = obj.getClass().getMethod("isRemovable");
                        String state = null;
                        try {
                            Method getVolumeState = StorageManager.class.getMethod("getVolumeState", String.class);
                            state = (String) getVolumeState.invoke(storageManager, info.path);
                            info.state = state;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (info.isMounted()) {
                            info.isRemovable = ((Boolean) isRemovable.invoke(obj, new Object[0])).booleanValue();
                            storage.add(info);
                        }
                    }
                }
            }
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        storage.trimToSize();

        return storage;
    }

    public static String[] getMountVolumePaths(Context context) {
        String[] paths = null;
        StorageManager mStorageManager;
        Method mMethodGetPaths = null;
        try {
            mStorageManager = (StorageManager) context
                    .getSystemService(Activity.STORAGE_SERVICE);
            mMethodGetPaths = mStorageManager.getClass().getMethod(
                    "getVolumePaths");
            paths = (String[]) mMethodGetPaths.invoke(mStorageManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return paths;
    }

    /**
     * 检查是否挂载
     */
    public static boolean checkMounted(Context context, String mountPoint) {
        if (mountPoint == null) {
            return false;
        }
        StorageManager storageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        try {
            Method getVolumeState = storageManager.getClass().getMethod(
                    "getVolumeState", String.class);
            String state = (String) getVolumeState.invoke(storageManager,
                    mountPoint);
            return Environment.MEDIA_MOUNTED.equals(state);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @function 获得该路径下的剩余容量
     * @param path
     * @return
     */
    public static long getAvailableCapacity(String path) {
        StatFs stat = new StatFs(path);
        return stat.getAvailableBytes();
    }

    /**
     * @function 获得该路径下的总容量
     * @param path
     * @return
     */
    public static long getTotalCapacity(String path) {
        StatFs stat = new StatFs(path);
        return stat.getTotalBytes();
    }

    /**
     * @function 获得该路径下的磁盘的block大小
     * @param path
     * @return
     */
    public static long getBlockSize(String path) {
        StatFs stat = new StatFs(path);
        return stat.getBlockSizeLong();
    }

    /**
     *
     * @param filePath
     * @function 抽取文件路径中的文件名
     * @return
     */
    public static String getFileName(String filePath) {
        String name = "";

        if ((filePath != null) && (filePath.length() > 0)) {
            int start = filePath.lastIndexOf(File.separator);
            int end = filePath.length();
            name =  filePath.substring(start == -1 ? 0 : (start + 1), end);
        }

        return name;
    }


    /**
     * @function 抽取文件路径中的文件前缀(不带有扩展名)
     * @param filePath
     * @return
     */
    public static String getFilePrefix(String filePath){
        String prefix = "";

        if ((filePath != null) && (filePath.length() > 0)) {
            int start=filePath.lastIndexOf(File.separator);
            int end=filePath.lastIndexOf(".");
            if(end!=-1){
                prefix = filePath.substring(start == -1 ? 0 : (start+1), end);
            }
        }

        return prefix;
    }

    /**
     * @function 抽取文件路径中的后缀名(没有点)
     * @param fileName
     * @return
     */
    public static String getFileExtension(String fileName){
        String extension = "";
        if ((fileName != null) && (fileName.length() > 0)) {
            int dot = fileName.lastIndexOf(".");
            if (dot > -1 && (dot < (fileName.length() - 1))) {
                extension = fileName.substring(dot + 1);
            }
        }

        return extension;
    }

    /**
     * 获取指定文件大小(单位：字节)
     *
     * @param path
     * @return
     */
    public static long getFileSize(String path) {

        if (path == null || path.isEmpty()) {
            Log.e(TAG,"getFileSize path error!");
            return 0;
        }

        File file = new File(path);
        return file.length();
    }

    public static String formatFileSize(long sizeByte) {

        DecimalFormat df = new DecimalFormat("#.0");
        String fileSizeString = "";
        if (sizeByte < 1024) {
            fileSizeString = df.format((double) sizeByte) + "B";
        } else if (sizeByte < 1048576) {
            fileSizeString = df.format((double) sizeByte / 1024) + "K";
        } else if (sizeByte < 1073741824) {
            fileSizeString = df.format((double) sizeByte / 1048576) + "M";
        } else if (sizeByte < 1099511627776L){
            fileSizeString = df.format((double) sizeByte / 1073741824) + "G";
        } else {
            fileSizeString = df.format((double) sizeByte / 1099511627776L) + "T";
        }
        return fileSizeString;
    }

    /**
     * 根据URL获取文件名
     *
     * @param url URL
     *
     * @return 文件名
     */
    public static String getFileNameFromUrl(String url) {
        if (url.indexOf("/") != -1)
            return url.substring(url.lastIndexOf("/")).replace("/", "");
        else
            return url;
    }


    /**
     * 获取指定目录下文件的个数
     *
     * @return int
     */
    public static int getFileCount(String dirPath) {
        int count = 0;

        // 如果dirPath不以文件分隔符结尾，自动添加文件分隔符
        if (!dirPath.endsWith(File.separator)) {
            dirPath = dirPath + File.separator;
        }
        File dirFile = new File(dirPath);

        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return count;
        }


        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                count += 1;
            }
        }

        return count;
    }

    /**
     * 创建文件
     * @param pathname 希望创建文件的路径
     * @return File
     */
    public static File createFile(String pathname) {
        File file = new File(pathname);
        File fileParent = file.getParentFile();

        if(!fileParent.exists()){
            fileParent.mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "创建文件出错：" + e.toString());
                e.printStackTrace();
            }
        }
        return file;
    }

    /**
     * 创建文件
     * @param path 希望创建文件的路径
     * @param fileName 文件名
     * @return File
     */
    public static File createFile(String path, String fileName) {
        File file = new File(createDir(path), fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "创建文件出错：" + e.toString());
                e.printStackTrace();
            }
        }
        return file;
    }


    /**
     * 创建一个文件夹,如果该名字存在则直接返回
     *
     * @return File
     */
    public static File createDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 删除一个文件,如果是目录 或 不存在 则返回失败
     *
     * @return void
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file!=null && file.isFile() && file.exists())
            return file.delete();
        return false;
    }

    /**
     * 递归删除文件夹及文件夹下的文件,如果不是文件夹则返回失败
     *
     * @return boolean
     */
    public static boolean deleteDir(String dirPath) {
        boolean flag = false;
        // 如果dirPath不以文件分隔符结尾，自动添加文件分隔符
        if (!dirPath.endsWith(File.separator)) {
            dirPath = dirPath + File.separator;
        }
        File dirFile = new File(dirPath);

        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }

        flag = true;
        File[] files = dirFile.listFiles();
        // 遍历删除文件夹下的所有文件(包括子目录)
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                // 删除子文件
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag)
                    break;
            } else {
                // 删除子目录
                flag = deleteDir(files[i].getAbsolutePath());
                if (!flag)
                    break;
            }
        }
        if (!flag)
            return false;
        // 删除当前空目录
        return dirFile.delete();
    }

    /**
     * 根据路径删除指定的目录或文件，无论存在与否
     *
     * @return boolean
     */
    public static boolean deleteForce(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        } else {
            if (file.isFile()) {
                // 为文件时调用删除文件方法
                return deleteFile(filePath);
            } else {
                // 为目录时调用删除目录方法
                return deleteDir(filePath);
            }
        }
    }


    /**
     * 复制单个文件
     *
     * @param srcPath  原文件路径
     *
     * @param desPath  目标路径
     *
     */
    public static void copyFile(String srcPath, String desPath) {
        int byteSum = 0;
        int byteRead;
        File sourceFile = new File(srcPath);

        if (sourceFile.exists() && sourceFile.isFile()) {
            try {
                long time = System.currentTimeMillis();
                FileInputStream inStream = new FileInputStream(srcPath);
                FileOutputStream outStream = new FileOutputStream(desPath);
                byte[] buffer = new byte[COPY_STEP_BYTE];
                while ((byteRead = inStream.read(buffer)) != -1) {
                    byteSum += byteRead;
                    outStream.write(buffer, 0, byteRead);
                }
                outStream.close();
                inStream.close();
                Log.d(TAG, "拷贝文件成功,文件总大小为：" + byteSum + "字节");
                Log.d(TAG, "拷贝所花费时间为: " + (System.currentTimeMillis() - time) + "毫秒");
            } catch (IOException e) {
                Log.e(TAG, "拷贝文件出错：" + e.toString());
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "拷贝文件出错：源文件有问题！");
        }
    }

    /**
     * 复制整个文件夹内容
     *
     * @param srcPath 原文件路径
     *
     * @param desPath 复制后路径
     *
     */
    public static void copyFolder(String srcPath, String desPath) {

        try {
            (new File(desPath)).mkdirs();
            File from = new File(srcPath);
            String[] file = from.list();
            File temp = null;
            for (int i = 0; i < file.length; i++) {
                if (srcPath.endsWith(File.separator)) {
                    temp = new File(srcPath + file[i]);
                } else {
                    temp = new File(srcPath + File.separator + file[i]);
                }

                if (temp.isFile()) {
                    FileInputStream input = new FileInputStream(temp);
                    FileOutputStream output = new FileOutputStream(desPath
                            + "/" + (temp.getName()));
                    byte[] b = new byte[COPY_STEP_BYTE];
                    int len;
                    while ((len = input.read(b)) != -1) {
                        output.write(b, 0, len);
                    }
                    output.flush();
                    output.close();
                    input.close();
                }
                if (temp.isDirectory()) {
                    copyFolder(srcPath + "/" + file[i], desPath + "/" + file[i]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        }

    }

    /* 将图像保存到Data目录 */
    public static boolean saveBitmapToData(Activity act, Bitmap bmpToSave, String FileName, int Quality)
    {//参数依次为：调用的 Activity，需要写入 data 的位图，文件名（不含扩展名），扩展名，图像质量
        try
        {
            if (Quality > 100)
                Quality = 100;
            else if (Quality < 1)
                Quality = 1;

            FileOutputStream fos = act.openFileOutput(FileName, Context.MODE_PRIVATE);
            bmpToSave.compress(Bitmap.CompressFormat.PNG, Quality, fos);

            //写入文件
            fos.flush();
            fos.close();
            return true;
        }
        catch (Exception e)
        {
            if (e.getMessage() != null)
                Log.w(TAG, e.getMessage());
            else
                e.printStackTrace();

            return false;
        }
    }

    /* 从Data目录读取图像 */
    public static Bitmap getBitmapFromData(Activity act, String FileName)
    {
        FileInputStream fis = null;
        try
        {
            fis = act.openFileInput(FileName);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return null;
        }
        BufferedInputStream bis = new BufferedInputStream(fis);
        Bitmap bmpRet = BitmapFactory.decodeStream(bis);

        try
        {
            bis.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        try
        {
            fis.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return bmpRet;
    }

    /* 从Data目录删除文件 */
    public static void deleteFileFromData(Activity act, String FileName)
    {
        String path = act.getFilesDir() + File.separator + FileName;
        File file = new File(path);

        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            return;
        }

        file.delete();

        return;
    }

    /* 在Data目录下是否存在 */
    public static boolean fileExistInData(Activity act, String FileName)
    {
        String path = act.getFilesDir() + File.separator + FileName;
        File file = new File(path);

        if (file.exists()) {
            return true;
        }

        return false;
    }

}

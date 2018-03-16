package com.whitesky.tv.projectorlauncher.media;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.utils.FileUtil;
import com.whitesky.tv.projectorlauncher.utils.PathUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by jeff on 18-3-16.
 */

public class CopyTask extends AsyncTask<CopyTask.CopyTaskParam, Integer, Void> {

    private static final String TAG = CopyTask.class.getSimpleName();

    private final int USB_COPY_BUFFER_SIZE = 1024*1024;     // 拷贝文件缓冲区长度,可调参数

    private ProgressDialog dialog;
    private CopyTaskParam param;
    private Deque<String> copyDoneDeque = new ArrayDeque<String>();

    public interface CopyDoneListener {
        /**
         *
         * @param copyDoneItem          拷贝完成后返回实际拷贝的项目列表
         */
        void onAllCopyDoneCallback(Deque<String> copyDoneItem);
    }

    public static class CopyTaskParam {
        Deque<String> fromList = new ArrayDeque<String>();
        String desFolder;
        long totalSize;
        CopyDoneListener callback;
    }

    public CopyTask(Context context) {
        dialog = new ProgressDialog(context);
        dialog.setTitle(context.getResources().getString(R.string.str_media_file_copy_process_dialog_title));
        dialog.setMessage(context.getString(R.string.str_media_file_copy_process_dialog_prompt));
        dialog.setIndeterminate(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
        copyDoneDeque.clear();
    }

    @Override
    protected Void doInBackground(CopyTaskParam... params) {
        long time = System.currentTimeMillis();
        param = params[0];
        try {
            long totalNow = 0;

            Log.i(TAG, "Start Copy file(s) total length: " + param.totalSize);

            copyDoneDeque.clear();
            byte[] bytes = new byte[USB_COPY_BUFFER_SIZE];

            while (!param.fromList.isEmpty()) {
                String sourcePath = param.fromList.pop();
                File fromFile = new File(sourcePath);

                String toPath = PathUtil.pathGenerate(sourcePath,param.desFolder);
                if (toPath.isEmpty()) {
                    throw new IOException();
                }
                File toFile = FileUtil.createFile(toPath);

                OutputStream out = new BufferedOutputStream(new FileOutputStream(toFile));
                InputStream input = new BufferedInputStream(new FileInputStream(fromFile));

                int count;
                while ((count = input.read(bytes)) != -1) {
                    out.write(bytes, 0, count);
                    totalNow += count;
                    float progress = (float)totalNow/(float)param.totalSize*100;
                    publishProgress((int)progress);
                }

                copyDoneDeque.push(toPath);

                out.close();
                input.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "error in CopyTask!", e);
        }

        Log.i(TAG, "total copy time " + (System.currentTimeMillis() - time) + "ms");
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        dialog.dismiss();
        if (param.callback!=null) {
            param.callback.onAllCopyDoneCallback(copyDoneDeque);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        dialog.setProgress(values[0]);
    }
}
package com.whitesky.tv.projectorlauncher.media;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import java.util.Deque;


/**
 * Created by jeff on 18-5-3.
 */

public class DeleteTask extends AsyncTask<DeleteTask.DeleteTaskParam, Integer, Integer> {

    private static final String TAG = DeleteTask.class.getSimpleName();

    private ProgressDialog dialog;
    private DeleteTaskParam param;

    public interface DeleteTaskListener {
        void onDeleteStartCallback();
        void onDeleteOneCallback(MediaBean bean);
        void onAllDeleteDoneCallback(int deleteCount);
    }

    public static class DeleteTaskParam {
        public Deque<MediaBean> deleteQueue;
        public DeleteTaskListener callback;
        private int totalDeleteCount;
    }

    public DeleteTask(Context context, boolean needUi) {
        if (needUi) {
            dialog = new ProgressDialog(context);
            dialog.setTitle(context.getResources().getString(R.string.str_media_file_delete_process_dialog_title));
            dialog.setMessage(context.getString(R.string.str_media_file_delete_process_dialog_prompt));
            dialog.setIndeterminate(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setCancelable(false);
        } else {
            dialog = null;
        }
    }

    @Override
    protected void onPreExecute() {
        if (dialog!=null) {
            dialog.show();
        }
    }

    @Override
    protected Integer doInBackground(DeleteTaskParam... params) {
        long time = System.currentTimeMillis();
        param = params[0];

        if (param.deleteQueue==null || param.deleteQueue.isEmpty()) {
            return 0;
        }

        if (param.callback!=null) {
            param.callback.onDeleteStartCallback();
        }

        int deleteIndex = 0;
        param.totalDeleteCount = param.deleteQueue.size();
        Log.i(TAG, "Start Delete file(s), total count: " + param.totalDeleteCount);

        while (!param.deleteQueue.isEmpty()) {
            if (param.callback != null) {
                param.callback.onDeleteOneCallback(param.deleteQueue.pop());
            }
            deleteIndex ++;

            if (dialog!=null) {
                publishProgress(deleteIndex);
            }
        }


        Log.i(TAG, "total delete cost time " + (System.currentTimeMillis() - time) + "ms");
        return deleteIndex;
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (dialog!=null) {
            dialog.dismiss();
        }

        if (param.callback!=null) {
            param.callback.onAllDeleteDoneCallback(result);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (dialog!=null) {
            dialog.setMax(param.totalDeleteCount);
            dialog.setProgress(values[0]);
        }
    }
}

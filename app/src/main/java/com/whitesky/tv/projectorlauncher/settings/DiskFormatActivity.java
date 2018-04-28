package com.whitesky.tv.projectorlauncher.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.application.MainApplication;
import com.whitesky.tv.projectorlauncher.media.MediaActivity;
import com.whitesky.tv.projectorlauncher.media.db.MediaBeanDao;
import com.whitesky.tv.projectorlauncher.media.db.PlayBean;
import com.whitesky.tv.projectorlauncher.media.db.PlayBeanDao;
import com.whitesky.tv.projectorlauncher.service.download.DownloadService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import scut.carson_ho.kawaii_loadingview.Kawaii_LoadingView;

/**
 * Created by xiaoxuan on 2017/10/13.
 */

public class DiskFormatActivity extends Activity implements View.OnClickListener
{
    private final String TAG = this.getClass().getSimpleName();
    private final String INTENT_FORMAT_FAILED = "android.intent.action.OEM_WSD_MOUNT_FAILED";
    private final String INTENT_FORMAT_UMOUNT = "android.intent.action.OEM_WSD_UMOUNT_SATA";
    private final String INTENT_FORMAT_MOUNT = "android.intent.action.OEM_WSD_MOUNT_SATA";


    private static final int MSG_FORMAT_START = 0;
    private static final int MSG_FORMAT_DONE = 1;
    private static final int MSG_FORMAT_NONE = 2;

    private final MyHandler mHandler = new MyHandler(this);

    private Button mBtnFormat;
    
    private TextView mTvFormatInfo;
    
    private Kawaii_LoadingView mLvFormatPaddingView;
    
    private ImageView mIvLogo;

    private final BroadcastReceiver formatMountEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(INTENT_FORMAT_FAILED)) {

                mHandler.sendEmptyMessage(MSG_FORMAT_NONE);
                Log.e(TAG,"no sata disk found!");

            } else if (action.equals(INTENT_FORMAT_MOUNT)) {

                mHandler.sendEmptyMessage(MSG_FORMAT_DONE);
                Log.i(TAG,"sata disk format done!");

            } else if (action.equals(INTENT_FORMAT_UMOUNT)) {

                mHandler.sendEmptyMessage(MSG_FORMAT_START);
                Log.i(TAG,"sata disk format begin!");
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_format_disk);
        mBtnFormat = findViewById(R.id.bt_format_disk);
        mBtnFormat.setVisibility(View.VISIBLE);
        mTvFormatInfo = findViewById(R.id.tv_disk_format_info);
        mLvFormatPaddingView = findViewById(R.id.lv_disk_format_padding);
        mIvLogo = findViewById(R.id.iv_disk_format_logo_big);
        mBtnFormat.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(formatMountEventReceiver);
        ((MainApplication)getApplication()).isBusyInFormat = false;
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        RelativeLayout layout = findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.shape_background);

        // 监听format事件
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_FORMAT_FAILED);
        filter.addAction(INTENT_FORMAT_UMOUNT);
        filter.addAction(INTENT_FORMAT_MOUNT);
        registerReceiver(formatMountEventReceiver, filter);

        showDefaultView();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (((MainApplication)getApplication()).isBusyInFormat)
        {
            switch (keyCode)
            {
                case KeyEvent.KEYCODE_HOME:
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    return true;
            }
        }
        
        return super.onKeyDown(keyCode, event);
    }

    private void showDefaultView() {
        ((MainApplication)getApplication()).isBusyInFormat = false;
        mIvLogo.setVisibility(View.VISIBLE);
        mTvFormatInfo.setVisibility(View.VISIBLE);
        mTvFormatInfo.setText(R.string.str_format_description_default);
        mLvFormatPaddingView.setVisibility(View.INVISIBLE);
        mBtnFormat.setVisibility(View.VISIBLE);
    }

    private void showSataFormatDoneView() {
        ((MainApplication)getApplication()).isBusyInFormat = false;
        mIvLogo.setVisibility(View.VISIBLE);
        mTvFormatInfo.setVisibility(View.VISIBLE);
        mTvFormatInfo.setText(R.string.str_format_description_done);
        mLvFormatPaddingView.setVisibility(View.INVISIBLE);
        mBtnFormat.setVisibility(View.INVISIBLE);
    }

    private void showSataNotFoundView() {
        ((MainApplication)getApplication()).isBusyInFormat = false;
        mIvLogo.setVisibility(View.VISIBLE);
        mTvFormatInfo.setVisibility(View.VISIBLE);
        mTvFormatInfo.setText(R.string.str_format_not_found_sata);
        mLvFormatPaddingView.setVisibility(View.INVISIBLE);
        mBtnFormat.setVisibility(View.VISIBLE);
    }

    private void showFormatPadding()
    {
        ((MainApplication)getApplication()).isBusyInFormat = true;
        mIvLogo.setVisibility(View.INVISIBLE);
        mTvFormatInfo.setVisibility(View.VISIBLE);
        mTvFormatInfo.setText(R.string.str_format_padding);
        mLvFormatPaddingView.setVisibility(View.VISIBLE);
        mLvFormatPaddingView.startMoving();
        mBtnFormat.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.bt_format_disk:
                if (!ActivityManager.isUserAMonkey()) {
                    formatSataDisk();
                }
                break;
        }
    }

    private void formatSataDisk() {
        new AlertDialog.Builder(this).setIcon(R.drawable.img_media_warning)
                .setTitle(R.string.str_format_warning)
                .setPositiveButton(R.string.str_media_dialog_button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mTvFormatInfo.setText(R.string.str_format_search_sata_disk);

                        ((MainApplication)getApplication()).isBusyInFormat = true;
                        Intent intent = new Intent().setAction(DownloadService.ACTION_MEDIA_DOWNLOAD_CANCEL_ALL);
                        Log.i(TAG, "call download cancel all");
                        getApplicationContext().startService(intent);

                        SystemProperties.set("dev.wsd.formatsata.start", "1");

                        Log.i(TAG,"call sata disk format!");
                    }
                })
                .setNegativeButton(R.string.str_media_dialog_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 什么也不做
                    }
                }).show();
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(getApplicationContext(), SysSettingActivity.class);
        startActivity(intent);
        DiskFormatActivity.this.finish();
        super.onBackPressed();
    }
    
    private static class MyHandler extends Handler
    {
        private final WeakReference<DiskFormatActivity> mActivity;
        
        public MyHandler(DiskFormatActivity activity)
        {
            mActivity = new WeakReference<DiskFormatActivity>(activity);
        }
        
        @Override
        public void handleMessage(Message msg)
        {
            DiskFormatActivity activity = mActivity.get();
            if (activity != null)
            {
                switch (msg.what)
                {
                    case MSG_FORMAT_START:
                        activity.showFormatPadding();
                        break;

                    case MSG_FORMAT_DONE:
                        activity.showSataFormatDoneView();
                        // 格式化磁盘后清空数据库与播放列表
                        new MediaBeanDao(mActivity.get()).deleteAll();
                        new PlayBeanDao(mActivity.get()).deleteAll();
                        break;

                    case MSG_FORMAT_NONE:
                        activity.showSataNotFoundView();
                        break;

                    default:
                        break;
                }
            }
        }
    }
}

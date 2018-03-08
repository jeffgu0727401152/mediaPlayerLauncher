package com.whitesky.tv.projectorlauncher.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.utils.ToastUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;

import scut.carson_ho.kawaii_loadingview.Kawaii_LoadingView;

import static com.whitesky.tv.projectorlauncher.common.Contants.LOCAL_MASS_STORAGE_PATH;

/**
 * Created by xiaoxuan on 2017/10/13.
 */

public class DiskFormatActivity extends Activity implements View.OnClickListener
{
    private final String TAG = this.getClass().getSimpleName();

    private static final int MSG_FORMAT_START = 0;
    private static final int MSG_FORMAT_DONE = 1;
    private static final int MSG_FORMAT_NONE = 2;

    private final MyHandler mHandler = new MyHandler(this);

    private boolean isFormatPadding = false;
    
    private Button mBtnFormat;
    
    private TextView mTvFormatInfo;
    
    private Kawaii_LoadingView mLvFormatPaddingView;
    
    private ImageView mIvLogo;

    private final BroadcastReceiver formatEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String file = intent.getData().getPath();
            if (file!=null && file.equals(LOCAL_MASS_STORAGE_PATH)) {
                if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                    mHandler.sendEmptyMessage(MSG_FORMAT_DONE);
                    Log.i(TAG,"sata disk format done!");

                } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                    mHandler.sendEmptyMessage(MSG_FORMAT_START);
                    Log.i(TAG,"sata disk format begin!");
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_format_disk);
        mBtnFormat = (Button)findViewById(R.id.bt_format_disk);
        mBtnFormat.setVisibility(View.VISIBLE);
        mTvFormatInfo = (TextView)findViewById(R.id.tv_disk_format_info);
        mLvFormatPaddingView = (Kawaii_LoadingView)findViewById(R.id.lv_disk_format_padding);
        mIvLogo = (ImageView)findViewById(R.id.iv_disk_format_logo_big);
        mBtnFormat.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(formatEventReceiver);
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        LinearLayout layout = (LinearLayout)findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.shape_background);

        // 监听usb插拔事件
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        usbFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        usbFilter.addDataScheme("file");
        registerReceiver(formatEventReceiver, usbFilter);

        showDefaultView();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (isFormatPadding)
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
        isFormatPadding = false;
        mIvLogo.setVisibility(View.VISIBLE);
        mTvFormatInfo.setVisibility(View.VISIBLE);
        mTvFormatInfo.setText(R.string.str_format_description);
        mLvFormatPaddingView.setVisibility(View.INVISIBLE);
        mBtnFormat.setVisibility(View.VISIBLE);
    }

    private void showSataNotFoundView() {
        isFormatPadding = false;
        mIvLogo.setVisibility(View.VISIBLE);
        mTvFormatInfo.setVisibility(View.VISIBLE);
        mTvFormatInfo.setText(R.string.str_format_not_found_sata);
        mLvFormatPaddingView.setVisibility(View.INVISIBLE);
        mBtnFormat.setVisibility(View.VISIBLE);
    }

    private void showFormatPadding()
    {
        isFormatPadding = true;
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
                formatSataDisk();
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
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String ret = do_exec("sh /etc/autoformat.sh");
                                Log.d(TAG, ret);
                                if (!ret.contains("format is finished!")) {
                                    mHandler.sendEmptyMessage(MSG_FORMAT_NONE);
                                    Log.e(TAG,"no sata disk found!");
                                }
                            }
                        }).start();
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

    String do_exec(String cmd) {
        String s = "";
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                s += line + "\n";
            }
        } catch (Exception e) {
            Log.e(TAG,"Exception in do_exec,"+ e.toString());
        }
        return s;
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
                        activity.showDefaultView();
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

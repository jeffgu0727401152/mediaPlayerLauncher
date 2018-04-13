package com.whitesky.tv.projectorlauncher.settings;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import scut.carson_ho.kawaii_loadingview.Kawaii_LoadingView;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.utils.AppUtil;
import com.whitesky.tv.projectorlauncher.utils.FileUtil;
import com.whitesky.tv.projectorlauncher.utils.ToastUtil;

import static com.whitesky.tv.projectorlauncher.common.Contants.LOCAL_SATA_MOUNT_PATH;
import static com.whitesky.tv.projectorlauncher.common.Contants.mMountExceptList;
import static com.whitesky.tv.projectorlauncher.utils.AppUtil.getApkPackageName;
import static com.whitesky.tv.projectorlauncher.utils.AppUtil.getApkVersionName;

/**
 * Created by xiaoxuan on 2017/10/13.
 */

public class OTALocalActivity extends Activity implements View.OnClickListener
{
    private final String TAG = this.getClass().getSimpleName();

    private static final String DEFAULT_UPDATE_APK_NAME = "update.apk";
    private static final String DEFAULT_INSTALL_APK_NAME = "install.apk";

    private static final int MSG_NO_UPDATE = 0;
    private static final int MSG_HAS_UPDATE = 1;
    private static final int MSG_HAS_INSTALL = 2;
    private static final int MSG_CHECK_UPDATE = 3;

    private final MyHandler mHandler = new MyHandler(this);

    private boolean isUpdatePadding = false;
    
    private Button mBtUpdate;
    
    private TextView mTvUpdateInfo;
    
    private Kawaii_LoadingView mLvLoadingView;
    
    private ImageView mIvLogo;

    private String updateApkPath = "";
    private String updateApkPackageName;

    private String installApkPath = "";
    private String installApkPackageName;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            mHandler.sendEmptyMessage(MSG_CHECK_UPDATE);
            ToastUtil.showToast(context, intent.getData().getPath());
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota_local);
        mBtUpdate = findViewById(R.id.bt_local_update);
        mBtUpdate.setVisibility(View.INVISIBLE);
        mTvUpdateInfo = findViewById(R.id.tv_ota_update_info);
        mLvLoadingView = findViewById(R.id.lv_ota_update_padding);
        mIvLogo = findViewById(R.id.iv_ota_local_logo_big);
        mBtUpdate.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(usbReceiver);
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        RelativeLayout layout = findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.shape_background);

        // 监听usb插拔事件
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        usbFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        usbFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        usbFilter.addDataScheme("file");
        registerReceiver(usbReceiver, usbFilter);

        showDefaultView();
        mHandler.sendEmptyMessage(MSG_CHECK_UPDATE);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (isUpdatePadding)
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
        isUpdatePadding = false;
        mIvLogo.setVisibility(View.VISIBLE);
        mTvUpdateInfo.setVisibility(View.VISIBLE);
        mTvUpdateInfo.setText(R.string.str_update_local_title);
        mLvLoadingView.setVisibility(View.INVISIBLE);
        mBtUpdate.setVisibility(View.INVISIBLE);
    }

    private void showUpdatePadding()
    {
        isUpdatePadding = true;
        mIvLogo.setVisibility(View.INVISIBLE);
        mTvUpdateInfo.setVisibility(View.INVISIBLE);
        mLvLoadingView.setVisibility(View.VISIBLE);
        mLvLoadingView.startMoving();
        mBtUpdate.setVisibility(View.INVISIBLE);
    }

    private void showHasUpdate()
    {
        isUpdatePadding = false;
        mIvLogo.setVisibility(View.VISIBLE);
        mTvUpdateInfo.setText(R.string.str_update_found);
        mLvLoadingView.stopMoving();
        mLvLoadingView.setVisibility(View.INVISIBLE);
        mBtUpdate.setVisibility(View.VISIBLE);
        mBtUpdate.setEnabled(true);
        mBtUpdate.requestFocus();
        //ToastUtil.showToast(this,"name:"+updateApkInfo.appName + ", Ver:" + updateApkInfo.version);
    }

    private void showHasInstall()
    {
        isUpdatePadding = false;
        mIvLogo.setVisibility(View.VISIBLE);
        mTvUpdateInfo.setText(R.string.str_install_found);
        mLvLoadingView.stopMoving();
        mLvLoadingView.setVisibility(View.INVISIBLE);
        mBtUpdate.setVisibility(View.VISIBLE);
        mBtUpdate.setEnabled(true);
        mBtUpdate.requestFocus();
        //ToastUtil.showToast(this,"name:"+updateApkInfo.appName + ", Ver:" + updateApkInfo.version);
    }

    
    private void showNoUpdate()
    {
        isUpdatePadding = false;
        mIvLogo.setVisibility(View.VISIBLE);
        mTvUpdateInfo.setText(R.string.str_update_local_not_found);
        mLvLoadingView.stopMoving();
        mLvLoadingView.setVisibility(View.INVISIBLE);
        mBtUpdate.setVisibility(View.INVISIBLE);
    }
    
    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.bt_local_update:
                String apkPath = installApkPath.isEmpty()?updateApkPath:installApkPath;

                File file = new File(apkPath);
                if (file.exists() && !file.isDirectory()) {
                    AppUtil.installApk(this,file);
                }
                break;
        }
    }
    
    private void checkLocalUpdateOrInstall()
    {
        updateApkPath = "";
        updateApkPackageName = null;
        installApkPath = "";
        installApkPackageName = null;
        String[] mountList = FileUtil.getMountVolumePaths(this);
        for (String s : mountList) {
            if (!Arrays.asList(mMountExceptList).contains(s) && !s.contains(LOCAL_SATA_MOUNT_PATH)) {
                File fileUpdate = new File(s + File.separator + DEFAULT_UPDATE_APK_NAME);
                if (fileUpdate.exists() && !fileUpdate.isDirectory()) {
                    updateApkPackageName = getApkPackageName(this,s + File.separator + DEFAULT_UPDATE_APK_NAME);

                    if (updateApkPackageName!=null && updateApkPackageName.equals(this.getPackageName())) {
                        updateApkPath = s + File.separator + DEFAULT_UPDATE_APK_NAME;
                        mHandler.sendEmptyMessage(MSG_HAS_UPDATE);
                        Log.i(TAG,"found update apk ver:" + getApkVersionName(this,s + File.separator + DEFAULT_UPDATE_APK_NAME));
                        return;
                    }
                    Log.e(TAG,"update.apk wrong format!!!");
                }

                File fileInstall = new File(s + File.separator + DEFAULT_INSTALL_APK_NAME);
                if (fileInstall.exists() && !fileInstall.isDirectory()) {
                    installApkPackageName = getApkPackageName(this,s + File.separator + DEFAULT_INSTALL_APK_NAME);

                    if (installApkPackageName!=null) {
                        installApkPath = s + File.separator + DEFAULT_INSTALL_APK_NAME;
                        mHandler.sendEmptyMessage(MSG_HAS_INSTALL);
                        Log.i(TAG,"found install apk:" + installApkPackageName);
                        return;
                    }
                    Log.e(TAG,"apk  file wrong format!!!");
                }
            }
        }

        Log.i(TAG,"found no apk we required in usb device!");

        mHandler.sendEmptyMessage(MSG_NO_UPDATE);
    }


    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(getApplicationContext(), OTAActivity.class);
        startActivity(intent);
        OTALocalActivity.this.finish();
        super.onBackPressed();
    }
    
    private static class MyHandler extends Handler
    {
        private final WeakReference<OTALocalActivity> mActivity;
        
        public MyHandler(OTALocalActivity activity)
        {
            mActivity = new WeakReference<OTALocalActivity>(activity);
        }
        
        @Override
        public void handleMessage(Message msg)
        {
            OTALocalActivity activity = mActivity.get();
            if (activity != null)
            {
                switch (msg.what)
                {
                    case MSG_NO_UPDATE:
                        activity.showNoUpdate();
                        break;
                    case MSG_HAS_UPDATE:
                        activity.showHasUpdate();
                        break;
                    case MSG_HAS_INSTALL:
                        activity.showHasInstall();
                        break;
                    case MSG_CHECK_UPDATE:
                        activity.checkLocalUpdateOrInstall();
                        break;
                    default:
                        break;
                }
            }
        }
    }
}

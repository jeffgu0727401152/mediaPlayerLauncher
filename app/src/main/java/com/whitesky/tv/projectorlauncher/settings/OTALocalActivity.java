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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import com.whitesky.tv.projectorlauncher.utils.FileUtil;
import com.whitesky.tv.projectorlauncher.utils.ToastUtil;

import static com.whitesky.tv.projectorlauncher.common.Contants.mMountExceptList;

/**
 * Created by xiaoxuan on 2017/10/13.
 */

public class OTALocalActivity extends Activity implements View.OnClickListener
{
    private final String TAG = this.getClass().getSimpleName();

    private static final String DEFAULT_UPDATE_APK_NAME = "update.apk";
    private static final int MSG_NO_UPDATE = 0;
    private static final int MSG_HAS_UPDATE = 1;
    private static final int MSG_CHECK_UPDATE = 3;

    private final MyHandler mHandler = new MyHandler(this);

    private boolean isUpdatePadding = false;
    
    private Button mBtUpdate;
    
    private TextView mTvUpdateInfo;
    
    private Kawaii_LoadingView mLvLoadingView;
    
    private ImageView mIvLogo;

    private String updateApkPath = "";
    private ApkInfo updateApkInfo;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            mHandler.sendEmptyMessageDelayed(MSG_CHECK_UPDATE, 500);
            ToastUtil.showToast(context, intent.getData().getPath());
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota_local);
        mBtUpdate = (Button)findViewById(R.id.bt_local_update);
        mBtUpdate.setVisibility(View.INVISIBLE);
        mTvUpdateInfo = (TextView)findViewById(R.id.tv_ota_update_info);
        mLvLoadingView = (Kawaii_LoadingView)findViewById(R.id.lv_ota_update_padding);
        mIvLogo = (ImageView)findViewById(R.id.iv_ota_local_logo_big);
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
        LinearLayout layout = (LinearLayout)findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.shape_background);

        // 监听usb插拔事件
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        usbFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        usbFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        usbFilter.addDataScheme("file");
        registerReceiver(usbReceiver, usbFilter);

        showDefaultView();
        mHandler.sendEmptyMessageDelayed(MSG_CHECK_UPDATE, 2000);
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
                File file = new File(updateApkPath);
                if (file.exists() && !file.isDirectory()) {
                    installApk(file);
                }
                break;
        }
    }
    
    private void checkUpdate()
    {
        updateApkPath = "";
        updateApkInfo = null;
        String[] mountList = FileUtil.getMountVolumePaths(this);
        for (String s : mountList) {
            if (!Arrays.asList(mMountExceptList).contains(s)) {
                File file = new File(s + File.separator + DEFAULT_UPDATE_APK_NAME);
                if (file.exists() && !file.isDirectory()) {
                    updateApkInfo = getApkInfo(s + File.separator + DEFAULT_UPDATE_APK_NAME);

                    if (updateApkInfo!=null && updateApkInfo.packageName.equals(this.getPackageName())) {
                        updateApkPath = s + File.separator + DEFAULT_UPDATE_APK_NAME;
                        mHandler.sendEmptyMessage(MSG_HAS_UPDATE);
                        Log.d(TAG,"found update apk ver:" + updateApkInfo.version);
                        return;
                    }

                    Log.e(TAG,"packageName of the update.apk is not as same as this one!!!");
                }
            }
        }

        mHandler.sendEmptyMessage(MSG_NO_UPDATE);
    }
    
    private void UpdateSys()
    {
    }

    private class ApkInfo
    {
        String appName = "";
        String packageName = "";
        String version = "";
    }

    private ApkInfo getApkInfo(String filePath){
        PackageManager pm = getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(filePath, PackageManager.GET_ACTIVITIES);
        if (info != null) {
            ApplicationInfo appInfo = info.applicationInfo;
            String appName = pm.getApplicationLabel(appInfo).toString();
            String packName = appInfo.packageName;
            String version = info.versionName;
            ApkInfo apkInfo = new ApkInfo();
            apkInfo.appName = appName;
            apkInfo.packageName = packName;
            apkInfo.version = version;
            return apkInfo;
        } else {
            return null;
        }
    }

    public void installApk(File file) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file),
                "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
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
                    case MSG_CHECK_UPDATE:
                        activity.checkUpdate();
                        break;
                    default:
                        break;
                }
            }
        }
    }
}

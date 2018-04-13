package com.whitesky.tv.projectorlauncher.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.admin.DeviceInfoActivity;
import com.whitesky.tv.projectorlauncher.common.Contants;
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.service.download.DownloadService;
import com.whitesky.tv.projectorlauncher.service.mqtt.MqttSslService;
import com.whitesky.tv.projectorlauncher.service.mqtt.bean.VersionCheckResultBean;

import java.lang.ref.WeakReference;

import static com.whitesky.tv.projectorlauncher.common.Contants.ACTION_DOWNLOAD_OTA_INSTALL_FAILED;
import static com.whitesky.tv.projectorlauncher.service.download.DownloadService.APK_SIZE_MAX;
import static com.whitesky.tv.projectorlauncher.service.download.DownloadService.EXTRA_KEY_URL;

/**
 * Created by xiaoxuan on 2017/10/13.
 */

public class OTANetActivity extends Activity implements View.OnClickListener
{
    private final static String TAG = OTANetActivity.class.getSimpleName();

    private static final int MSG_NO_UPDATE = 0;
    private static final int MSG_HAS_UPDATE = 1;
    private static final int MSG_UPDATE_TOO_LARGE = 2;
    private static final int MSG_COULD_NOT_CONNECT_UPDATE_SERVER = 3;
    private static final int MSG_INSTALL_UPDATE = 4;
    private static final int MSG_CHECK_UPDATE_APK_FAIL = 5;

    private final MyHandler mHandler = new MyHandler(this);
    private Button mBtnUpdate;
    private TextView mTvUpdateInfo;
    private ImageView mIvLogo;
    private ProgressBar mProcessBar;

    private VersionCheckResultBean.Result mOta = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota_net);

        mBtnUpdate = (Button)findViewById(R.id.bt_ota_net_update);
        mBtnUpdate.setVisibility(View.VISIBLE);
        mBtnUpdate.setOnClickListener(this);

        mIvLogo = (ImageView)findViewById(R.id.iv_ota_net_logo);
        mTvUpdateInfo = (TextView)findViewById(R.id.tv_ota_net_info);
        mProcessBar = (ProgressBar)findViewById(R.id.pb_ota_net_update);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(otaDownloadReceiver);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        LinearLayout layout = (LinearLayout)findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.shape_background);

        showDefaultView();
        mOta = null;

        IntentFilter otaDownloadFilter = new IntentFilter();
        otaDownloadFilter.addAction(Contants.ACTION_DOWNLOAD_OTA_PROGRESS);
        otaDownloadFilter.addAction(Contants.ACTION_DOWNLOAD_OTA_INSTALL_FAILED);
        LocalBroadcastManager.getInstance(this).registerReceiver(otaDownloadReceiver, otaDownloadFilter);
    }

    private final BroadcastReceiver otaDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Contants.ACTION_DOWNLOAD_OTA_PROGRESS)) {
                final MediaBean bean = intent.getParcelableExtra(Contants.EXTRA_DOWNLOAD_STATE_CONTEXT);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showUpdatingView();
                        if (bean.getSize()!=0) {
                            mProcessBar.setProgress((int) (bean.getDownloadProgress() * 100 / bean.getSize()));
                        }
                    }
                });
            } else if (action.equals(ACTION_DOWNLOAD_OTA_INSTALL_FAILED)) {
                mHandler.sendEmptyMessage(MSG_CHECK_UPDATE_APK_FAIL);
            }
        }
    };

    private void showDefaultView() {
        mTvUpdateInfo.setVisibility(View.VISIBLE);
        mTvUpdateInfo.setText(R.string.title_check_update);
        mProcessBar.setVisibility(View.INVISIBLE);
        mBtnUpdate.setVisibility(View.VISIBLE);
    }

    private void showHaveUpdateView() {
        mTvUpdateInfo.setVisibility(View.VISIBLE);
        mTvUpdateInfo.setText(getResources().getString(R.string.str_update_found) + " Ver." + mOta.getAppVer());
        mProcessBar.setVisibility(View.INVISIBLE);
        mBtnUpdate.setText(R.string.str_ota_download_btn);
        mBtnUpdate.setVisibility(View.VISIBLE);
    }

    private void showCouldNotConnectView() {
        mTvUpdateInfo.setVisibility(View.VISIBLE);
        mTvUpdateInfo.setText(R.string.title_server_connect_error);
        mProcessBar.setVisibility(View.INVISIBLE);
        mBtnUpdate.setVisibility(View.VISIBLE);
    }

    private void showUpdateTooLargeView() {
        mTvUpdateInfo.setVisibility(View.VISIBLE);
        mTvUpdateInfo.setText(R.string.title_net_update_too_large);
        mProcessBar.setVisibility(View.INVISIBLE);
        mBtnUpdate.setVisibility(View.VISIBLE);
    }

    private void showNoUpdateView() {
        mTvUpdateInfo.setVisibility(View.VISIBLE);
        mTvUpdateInfo.setText(R.string.title_net_update_not_found);
        mProcessBar.setVisibility(View.INVISIBLE);
        mBtnUpdate.setVisibility(View.VISIBLE);
    }

    private void showUpdatingView() {
        mTvUpdateInfo.setVisibility(View.VISIBLE);
        mTvUpdateInfo.setText(R.string.title_net_update);
        mProcessBar.setVisibility(View.VISIBLE);
        mBtnUpdate.setVisibility(View.INVISIBLE);
    }

    private void showUpdateFailView() {
        mTvUpdateInfo.setVisibility(View.VISIBLE);
        mTvUpdateInfo.setText(R.string.str_update_file_check_failed);
        mProcessBar.setVisibility(View.INVISIBLE);
        mBtnUpdate.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.bt_ota_net_update:
                if (ActivityManager.isUserAMonkey()) {
                    return;
                }

                if (mOta == null) {

                    MqttSslService.reportVersionAndGetUpdateInfo(getApplicationContext(), new MqttSslService.requestVersionCallback() {
                        @Override
                        public void versionRequestDone(boolean ret, VersionCheckResultBean.Result result) {
                            if (ret==true) {
                                if (result!=null) {
                                    Log.i(TAG, "found ota version = " + result.getAppVer());
                                    Log.i(TAG, "found ota size = " + result.getSize());

                                    //boolean needDownload = serverVersionGreaterThanDeviceVersion(result.getAppVer(),DeviceInfoActivity.getVersionName(getApplicationContext()));
                                    // device端不判断版本,只要是服务器端说有新版本,一律允许安装,这样赋予服务器更大的自由权
                                    // 而服务端根据你上传的version取，去查询服务器上这个version是不是最后上传的apk（所有出货apk在服务器上都上传了）
                                    // 如果这个version找不到/这个version上传的日期后面又上传了新的apk，则允许服务器会告诉设备最新版的apk更新存在
                                    boolean needDownload = true;

                                    if (result.getSize() < APK_SIZE_MAX && needDownload) {
                                        mOta = result;
                                        mHandler.sendEmptyMessage(MSG_HAS_UPDATE);

                                    } else {
                                        Log.e(TAG,"server update apk not as our required!");
                                        mHandler.sendEmptyMessage(MSG_UPDATE_TOO_LARGE);
                                    }
                                } else {
                                    mHandler.sendEmptyMessage(MSG_NO_UPDATE);
                                }
                            } else {
                                Log.e(TAG,"server connect error!");
                                mHandler.sendEmptyMessage(MSG_COULD_NOT_CONNECT_UPDATE_SERVER);
                            }
                        }
                    });

                } else {
                    mHandler.sendEmptyMessage(MSG_INSTALL_UPDATE);
                }
                break;
        }
    }

    public static boolean serverVersionGreaterThanDeviceVersion(String ServerVer,String DeviceVer) {
        int versionOnServer = 0;
        int versionOnDevice = 0;
        String versionOnServerStr = ServerVer.replaceAll("[a-zA-Z]", "").replace(".","");
        String versionOnDeviceStr = DeviceVer.replaceAll("[a-zA-Z]", "").replace(".","");
        try {
            versionOnServer = Integer.parseInt(versionOnServerStr);
            versionOnDevice = Integer.parseInt(versionOnDeviceStr);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (versionOnServer>versionOnDevice) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(getApplicationContext(), OTAActivity.class);
        startActivity(intent);
        OTANetActivity.this.finish();
        super.onBackPressed();
    }
    
    private static class MyHandler extends Handler
    {
        private final WeakReference<OTANetActivity> mActivity;
        
        public MyHandler(OTANetActivity activity)
        {
            mActivity = new WeakReference<OTANetActivity>(activity);
        }
        
        @Override
        public void handleMessage(Message msg)
        {
            OTANetActivity activity = mActivity.get();
            if (activity != null)
            {
                switch (msg.what)
                {
                    case MSG_NO_UPDATE:
                        activity.showNoUpdateView();
                        break;

                    case MSG_HAS_UPDATE:
                        activity.showHaveUpdateView();
                        break;

                    case MSG_UPDATE_TOO_LARGE:
                        activity.showUpdateTooLargeView();
                        break;

                    case MSG_COULD_NOT_CONNECT_UPDATE_SERVER:
                        activity.showCouldNotConnectView();
                        break;

                    case MSG_INSTALL_UPDATE:
                        activity.showUpdatingView();
                        Intent intent = new Intent().setAction(DownloadService.ACTION_APK_DOWNLOAD_START);
                        intent.putExtra(EXTRA_KEY_URL, activity.mOta.getUrl());
                        activity.startService(intent);
                        break;

                    case MSG_CHECK_UPDATE_APK_FAIL:
                        activity.showUpdateFailView();
                        break;

                    default:
                        break;
                }
            }
        }
    }
}

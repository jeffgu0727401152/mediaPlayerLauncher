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
    private static final int MSG_INSTALL_UPDATE = 2;
    private static final int MSG_CHECK_UPDATE_APK_FAIL = 3;

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
                            if (ret) {
                                Log.i(TAG, "ota cloud found download version = " + result.getAppVer());
                                Log.i(TAG, "ota cloud found download size = " + result.getSize());

                                if (result.getSize() < APK_SIZE_MAX) {
                                    mOta = result;
                                    mHandler.sendEmptyMessage(MSG_HAS_UPDATE);
                                } else {
                                    Log.e(TAG, "ota server update apk to large!");
                                }
                            } else {
                                mHandler.sendEmptyMessage(MSG_NO_UPDATE);
                            }
                        }
                    });

                } else {
                    mHandler.sendEmptyMessage(MSG_INSTALL_UPDATE);
                }
                break;
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

package com.whiteskycn.tv.projectorlauncher.admin;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.admin.presenter.DeviceInfoImpl;
import com.whiteskycn.tv.projectorlauncher.settings.common.SkinSettingManager;


/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class DeviceInfoActivity extends Activity
{
    private final String TAG = this.getClass().getSimpleName();
    
    private TextView mTvSysVersion;
    
    private TextView mTvSysVersionDate;
    
    private TextView mEthMac;
    private TextView mWifiMac;
    
    private DeviceInfoImpl mDeviceInfoPresenter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        mTvSysVersion = (TextView)findViewById(R.id.tv_sys_version);
        mTvSysVersionDate = (TextView)findViewById(R.id.tv_admin_device_sys_version_date);
        mEthMac = (TextView)findViewById(R.id.tv_admin_device_mac);
        mWifiMac = (TextView)findViewById(R.id.tv_admin_device_wifi_mac);
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        mDeviceInfoPresenter = new DeviceInfoImpl();
        LinearLayout layout = (LinearLayout)findViewById(R.id.ll_skin);
        SkinSettingManager mSettingManager = new SkinSettingManager(this, layout);
        mSettingManager.initSkins();
        if (!TextUtils.isEmpty(mDeviceInfoPresenter.getSysVersion()))
        {
            mTvSysVersion.setText(mDeviceInfoPresenter.getSysVersion());
        }
        if (!TextUtils.isEmpty(mDeviceInfoPresenter.getSysVersionDate()))
        {
            mTvSysVersionDate.setText(mDeviceInfoPresenter.getSysVersionDate());
        }
        if (!TextUtils.isEmpty(mDeviceInfoPresenter.getEthMacAddr()))
        {
            mEthMac.setText(mDeviceInfoPresenter.getEthMacAddr());
        }
        if (!TextUtils.isEmpty(mDeviceInfoPresenter.getWifiMacAddr()))
        {
            mWifiMac.setText(mDeviceInfoPresenter.getWifiMacAddr());
        }
    }
    
    @Override
    protected void onPause()
    {
        if (mDeviceInfoPresenter != null)
        {
            mDeviceInfoPresenter = null;
        }
        super.onPause();
    }
}

package com.whiteskycn.tv.projectorlauncher.admin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.whiteskycn.tv.projectorlauncher.R;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;


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
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        mTvSysVersion = (TextView)findViewById(R.id.tv_sys_version);
        mTvSysVersionDate = (TextView)findViewById(R.id.tv_admin_device_sys_version_date);
        mEthMac = (TextView)findViewById(R.id.tv_admin_device_mac);
        mWifiMac = (TextView)findViewById(R.id.tv_admin_device_wifi_mac);
        if (!TextUtils.isEmpty(getSysVersion()))
        {
            mTvSysVersion.setText(getSysVersion());
        }
        if (!TextUtils.isEmpty(getSysVersionDate()))
        {
            mTvSysVersionDate.setText(getSysVersionDate());
        }
        if (!TextUtils.isEmpty(getEthMacAddr()))
        {
            mEthMac.setText(getEthMacAddr());
        }
        if (!TextUtils.isEmpty(getWifiMacAddr()))
        {
            mWifiMac.setText(getWifiMacAddr());
        }
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        LinearLayout layout = (LinearLayout)findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.img_background);
    }
    
    private String getSysVersion()
    {
        return SystemProperties.get("ro.sw.ver", "1.0.0");
    }
    
    private String getTerminalModel()
    {
        return null;
    }
    
    private String getSysVersionDate()
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyy-MM-dd HH:mm");
        long lt = new Long(SystemProperties.get("ro.build.date.utc", "1513328136"));
        Date date = new Date(lt * 1000);
        return simpleDateFormat.format(date);
    }
    
    private String getSysSN()
    {
        return null;
    }
    
    private String getUIVersion()
    {
        return null;
    }
    
    private String getDlpInfo()
    {
        return null;
    }
    
    private String getRamInfo()
    {
        return null;
    }
    
    private String getRomInfo()
    {
        return null;
    }
    
    private String getEthMacAddr()
    {
        String macAddress = null;
        StringBuffer buf = new StringBuffer();
        NetworkInterface networkInterface = null;
        try {
            networkInterface = NetworkInterface.getByName("eth0");
            if (networkInterface == null) {
                return "00:00:00:00:00:00";
            }
            byte[] addr = networkInterface.getHardwareAddress();
            for (byte b : addr) {
                buf.append(String.format("%02X:", b));
            }
            if (buf.length() > 0) {
                buf.deleteCharAt(buf.length() - 1);
            }
            macAddress = buf.toString();
        } catch (SocketException e) {
            e.printStackTrace();
            return "00:00:00:00:00:00";
        }
        return macAddress;
    }
    
    private String getWifiMacAddr()
    {
        String macAddress = null;
        StringBuffer buf = new StringBuffer();
        NetworkInterface networkInterface = null;
        try {
            networkInterface = NetworkInterface.getByName("wlan0");
            if (networkInterface == null) {
                return "00:00:00:00:00:00";
            }
            byte[] addr = networkInterface.getHardwareAddress();
            for (byte b : addr) {
                buf.append(String.format("%02X:", b));
            }
            if (buf.length() > 0) {
                buf.deleteCharAt(buf.length() - 1);
            }
            macAddress = buf.toString();
        } catch (SocketException e) {
            e.printStackTrace();
            return "00:00:00:00:00:00";
        }
        return macAddress;
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(getApplicationContext(), AdminActivity.class);
        startActivity(intent);
        DeviceInfoActivity.this.finish();
        super.onBackPressed();
    }
}

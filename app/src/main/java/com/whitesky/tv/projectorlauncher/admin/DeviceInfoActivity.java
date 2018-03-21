package com.whitesky.tv.projectorlauncher.admin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.whitesky.tv.projectorlauncher.R;

import java.io.File;
import java.io.FileInputStream;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.whitesky.tv.projectorlauncher.common.Contants.PROJECT_NAME;


/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class DeviceInfoActivity extends Activity
{
    private final String TAG = this.getClass().getSimpleName();
    private static final String  EMMC_SERIAL_NODE = "/sys/devices/platform/soc/f9830000.himciv200.MMC/mmc_host/mmc0/mmc0:0001/serial";
    private static final String  EMMC_MAN_NODE = "/sys/devices/platform/soc/f9830000.himciv200.MMC/mmc_host/mmc0/mmc0:0001/serial";

    private TextView mDeviceModel;
    private TextView mTvSysVersion;
    private TextView mTvSysVersionDate;
    private TextView mUIVersion;
    private TextView mEthMac;
    private TextView mRAMInfo;
    private TextView mROMInfo;
    private TextView mSysSN;

    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        mDeviceModel = (TextView)findViewById(R.id.tv_admin_device_device_model);
        mTvSysVersion = (TextView)findViewById(R.id.tv_sys_version);
        mTvSysVersionDate = (TextView)findViewById(R.id.tv_admin_device_sys_version_date);
        mUIVersion = (TextView)findViewById(R.id.tv_admin_device_ui_version);
        mRAMInfo = (TextView)findViewById(R.id.tv_admin_device_raminfo);
        mROMInfo = (TextView)findViewById(R.id.tv_admin_device_rominfo);
        mEthMac = (TextView)findViewById(R.id.tv_admin_device_mac);
        mSysSN = (TextView)findViewById(R.id.tv_admin_device_sys_sn);

        mDeviceModel.setText(PROJECT_NAME);

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
        if (!TextUtils.isEmpty(getRamInfo()))
        {
            mRAMInfo.setText(getRamInfo());
        }
        if (!TextUtils.isEmpty(getRomInfo()))
        {
            mROMInfo.setText(getRomInfo());
        }
        if (!TextUtils.isEmpty(getVersionName(this)))
        {
            mUIVersion.setText(getVersionName(this));
        }
        if (!TextUtils.isEmpty(getSysSN()))
        {
            mSysSN.setText(getSysSN());
        }
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        LinearLayout layout = (LinearLayout)findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.shape_background);
    }
    
    private static String getSysVersion()
    {
        return SystemProperties.get("ro.build.version.release", "4.4.0");
    }

    private static String getSysVersionDate()
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyy-MM-dd HH:mm");
        long lt = Long.valueOf(SystemProperties.get("ro.build.date.utc", "1513328136"));
        Date date = new Date(lt * 1000);
        return simpleDateFormat.format(date);
    }
    
    public static String getSysSN()
    {
        File sn = new File(EMMC_SERIAL_NODE);
        if (sn.exists())
        {
            FileInputStream fis = null;
            try{
                fis = new FileInputStream(sn);
                byte[] data = new byte[1024];
                int i = fis.read(data);
                String result = new String(data,0,i);
                result = result.substring(2).toUpperCase();
                result = result.replaceAll("\r|\n", "");
                return result;
            }catch(Exception e){
                e.printStackTrace();
            }finally{
                try{
                    fis.close();
                }catch(Exception e){
                    Log.e("DeviceInfoActivity","error in FileInputStream"+e.toString());
                }
            }
        } else {
            return null;
        }
        return null;
    }

    private static String getRamInfo()
    {
        return SystemProperties.get("ro.product.mem.size","1G");
    }
    
    private static String getRomInfo()
    {
        return null;
    }

    /**
     * get App versionCode
     * @param context
     * @return
     */
    public static String getVersionCode(Context context){
        PackageManager packageManager=context.getPackageManager();
        PackageInfo packageInfo;
        String versionCode="";
        try {
            packageInfo=packageManager.getPackageInfo(context.getPackageName(),0);
            versionCode=packageInfo.versionCode+"";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * get App versionName
     * @param context
     * @return
     */
    public static String getVersionName(Context context){
        PackageManager packageManager=context.getPackageManager();
        PackageInfo packageInfo;
        String versionName="";
        try {
            packageInfo=packageManager.getPackageInfo(context.getPackageName(),0);
            versionName=packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    private static String getEthMacAddr()
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
    
    private static String getWifiMacAddr()
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

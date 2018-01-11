package com.whiteskycn.tv.projectorlauncher.admin.presenter;

import android.os.SystemProperties;


import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by xiaoxuan on 2017/12/26.
 */

public class DeviceInfoImpl implements IDeviceInfoActivity
{
    @Override
    public String getSysVersion()
    {
        return SystemProperties.get("ro.sw.ver", "1.0.0");
    }
    
    @Override
    public String getTerminalModel()
    {
        return null;
    }
    
    @Override
    public String getSysVersionDate()
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyy-MM-dd HH:mm");
        long lt = new Long(SystemProperties.get("ro.build.date.utc", "1513328136"));
        Date date = new Date(lt * 1000);
        return simpleDateFormat.format(date);
    }
    
    @Override
    public String getSysSN()
    {
        return null;
    }
    
    @Override
    public String getUIVersion()
    {
        return null;
    }
    
    @Override
    public String getDlpInfo()
    {
        return null;
    }
    
    @Override
    public String getRamInfo()
    {
        return null;
    }
    
    @Override
    public String getRomInfo()
    {
        return null;
    }
    
    @Override
    public String getEthMacAddr()
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
    
    @Override
    public String getWifiMacAddr()
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
}

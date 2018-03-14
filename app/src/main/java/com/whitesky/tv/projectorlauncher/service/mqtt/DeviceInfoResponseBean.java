package com.whitesky.tv.projectorlauncher.service.mqtt;

/**
 * Created by jeff on 18-3-5.
 */

public class DeviceInfoResponseBean {
    private String appVer = "v1.0";
    private String sysVer = "v4.4.2";

    private long freeCapacity = 0;
    private long totalCapacity= 0;

    private long freeMemory = 0;
    private long totalMemory = 0;

    private int isPlaying = 0;
    private long upTime = 0;

    public DeviceInfoResponseBean() {

    }

    public String getAppVer() {
        return appVer;
    }

    public void setAppVer(String appVer) {
        this.appVer = appVer;
    }

    public String getSysVer() {
        return sysVer;
    }

    public void setSysVer(String sysVer) {
        this.sysVer = sysVer;
    }


    public long getFreeCapacity() {
        return freeCapacity;
    }

    public void setFreeCapacity(long freeCapacity) {
        this.freeCapacity = freeCapacity;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getFreeMemory() {
        return freeMemory;
    }

    public void setFreeMemory(long freeMemory) {
        this.freeMemory = freeMemory;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public int isPlaying() {
        return isPlaying;
    }

    public void setPlaying(int playing) {
        if (playing==0) {
            isPlaying = 0;
        } else {
            isPlaying = 1;
        }
    }

    public long getUpTime() {
        return upTime;
    }

    public void setUpTime(long upTime) {
        this.upTime = upTime;
    }
}

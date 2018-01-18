package com.whiteskycn.tv.projectorlauncher.media.bean;

/**
 * Created by jeff on 18-1-18.
 */

public class MediaFileBean {
    public enum MediaTypeEnum
    {
        UNKNOWN,
        PICTURE,
        VIDEO
    }

    public enum MediaSourceEnum
    {
        UNKNOWN,
        CLOUD_FREE,     //云端公司上传的示例视频
        CLOUD_PAY,      //远端付费下载视频
        CLOUD_PRIVATE,  //客户上传到云端的视频
        LOCAL           //客户本地通过USB拷入的视频
    }

    private String mUniqueID;       //所有媒体文件全局唯一的ID
    private MediaTypeEnum mType;
    private MediaSourceEnum mSource;
    private boolean mIsDownloaded;  //是否已经从云端下载到本地
    private String mFilePath;       //文件的存储路径


    public MediaFileBean() {
        mUniqueID = "";
        mType = MediaTypeEnum.UNKNOWN;
        mSource = MediaSourceEnum.UNKNOWN;
        mIsDownloaded = false;
        mFilePath = "";
    }


    public String getmUniqueID() {
        return mUniqueID;
    }

    public void setmUniqueID(String mUniqueID) {
        this.mUniqueID = mUniqueID;
    }

    public MediaTypeEnum getmType() {
        return mType;
    }

    public void setmType(MediaTypeEnum mType) {
        this.mType = mType;
    }

    public MediaSourceEnum getmSource() {
        return mSource;
    }

    public void setmSource(MediaSourceEnum mSource) {
        this.mSource = mSource;
    }

    public boolean ismIsDownloaded() {
        return mIsDownloaded;
    }

    public void setmIsDownloaded(boolean mIsDownloaded) {
        this.mIsDownloaded = mIsDownloaded;
    }

    public String getmFilePath() {
        return mFilePath;
    }

    public void setmFilePath(String mFilePath) {
        this.mFilePath = mFilePath;
    }
}

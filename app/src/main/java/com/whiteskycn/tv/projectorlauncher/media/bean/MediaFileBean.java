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
    private String mName;       //所有媒体文件全局唯一的ID
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

    public MediaFileBean(String id, String name, MediaTypeEnum type, MediaSourceEnum source, boolean isDownloaded, String filePath) {
        this.mUniqueID = id;
        this.mName = name;
        this.mType = type;
        this.mSource = source;
        this.mIsDownloaded = isDownloaded;
        this.mFilePath = filePath;
    }

    public String getUniqueID() {
        return mUniqueID;
    }

    public void setUniqueID(String mUniqueID) {
        this.mUniqueID = mUniqueID;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public MediaTypeEnum getType() {
        return mType;
    }

    public void setType(MediaTypeEnum mType) {
        this.mType = mType;
    }

    public MediaSourceEnum getSource() {
        return mSource;
    }

    public void setSource(MediaSourceEnum mSource) {
        this.mSource = mSource;
    }

    public boolean getIsDownloaded() {
        return mIsDownloaded;
    }

    public void setIsDownloaded(boolean mIsDownloaded) {
        this.mIsDownloaded = mIsDownloaded;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void setFilePath(String mFilePath) {
        this.mFilePath = mFilePath;
    }
}

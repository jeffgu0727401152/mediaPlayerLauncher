package com.whiteskycn.tv.projectorlauncher.media.bean;

/**
 * Created by jeff on 18-1-18.
 */

public class RawMediaBean {
    public static final int MEDIA_UNKNOWN = 0;
    public static final int MEDIA_PICTURE = 1;
    public static final int MEDIA_VIDEO = 2;
    public static final int MEDIA_MUSIC = 3;

    public static final int SOURCE_UNKNOWN = 0;
    public static final int SOURCE_LOCAL = 1;               //客户本地通过USB拷入的视频
    public static final int SOURCE_CLOUD_FREE = 2;          //云端公司上传的示例视频
    public static final int SOURCE_CLOUD_PRIVATE = 3;       //客户上传到云端的视频,并定义为私有
    public static final int SOURCE_CLOUD_PUBLIC = 4;        //客户上传到云端的视频,并定义为共享
    public static final int SOURCE_CLOUD_PAY = 5;           //云端付费下载视频

    private String uniqueID;   //所有媒体文件全局唯一的ID
    private String filePath;       //文件的存储路径
    private int source;
    private int type;
    private boolean isDownload;  //是否已经从云端下载到本地
    private int duration;          //文件的时长
//    private String name;

    public RawMediaBean() {
        uniqueID = "";
        type = MEDIA_UNKNOWN;
        source = SOURCE_UNKNOWN;
        isDownload = false;
        filePath = "";
    }

    public RawMediaBean(String id, String name, int type, int source, boolean isDownload, String filePath, int duration) {
        this.uniqueID = id;
//        this.name = name;
        this.type = type;
        this.source = source;
        this.isDownload = isDownload;
        this.filePath = filePath;
        this.duration = duration;
    }

    public String getUniqueID() {
        return uniqueID;
    }

    public void setUniqueID(String id) {
        this.uniqueID = id;
    }

//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public boolean isDownload() {
        return isDownload;
    }

    public void setDownload(boolean download) {
        isDownload = download;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}

package com.whitesky.tv.projectorlauncher.media.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by jeff on 18-2-4.
 */

@DatabaseTable(tableName = "media")
public class MediaBean {
    // 定义字段在数据库中的字段名
    public static final String COLUMNNAME_TITLE = "title";
    public static final String COLUMNNAME_DESCRIPTION = "description";
    public static final String COLUMNNAME_SOURCE = "source";
    public static final String COLUMNNAME_TYPE = "type";
    public static final String COLUMNNAME_PATH = "path";
    public static final String COLUMNNAME_DURATION = "duration";
    public static final String COLUMNNAME_SIZE = "size";
    public static final String COLUMNNAME_ISDOWNLOAD = "isDownload";

    // 媒体文件类型定义
    public static final int MEDIA_UNKNOWN = 0;
    public static final int MEDIA_PICTURE = 1;
    public static final int MEDIA_VIDEO = 2;
    public static final int MEDIA_MUSIC = 3;

    // 媒体文件来源定义
    public static final int SOURCE_UNKNOWN = 0;
    public static final int SOURCE_LOCAL = 1;               //客户本地通过USB拷入的视频
    public static final int SOURCE_CLOUD_FREE = 2;          //云端公司上传的示例视频
    public static final int SOURCE_CLOUD_PRIVATE = 3;       //客户上传到云端的视频,并定义为私有
    public static final int SOURCE_CLOUD_PUBLIC = 4;        //客户上传到云端的视频,并定义为共享
    public static final int SOURCE_CLOUD_PAY = 5;           //云端付费下载视频

    //文件的存储路径,全局唯一作为主键
    @DatabaseField(id = true, columnName = COLUMNNAME_PATH, useGetSet = true, canBeNull = false,unique = true)
    private String path;

    // 文件的UI显示名,一般就是去掉了扩展名的文件名
    @DatabaseField(columnName = COLUMNNAME_TITLE, useGetSet = true, canBeNull = false, unique = false)
    private String title;

    // 文件描述,暂时不用
    @DatabaseField(columnName = COLUMNNAME_DESCRIPTION, useGetSet = true, canBeNull = true, unique = false)
    private String description;

    // 文件的来源
    @DatabaseField(columnName = COLUMNNAME_SOURCE, useGetSet = true, canBeNull = false, unique = false)
    private int source;

    // 文件的媒体类型
    @DatabaseField(columnName = COLUMNNAME_TYPE, useGetSet = true, canBeNull = false, unique = false)
    private int type;

    //文件的时长
    @DatabaseField(columnName = COLUMNNAME_DURATION, useGetSet = true, canBeNull = true, unique = false)
    private int duration;

    //文件的大小
    @DatabaseField(columnName = COLUMNNAME_SIZE, useGetSet = true, canBeNull = true, unique = false)
    private long size;

    //是否已经从云端下载到本地
    @DatabaseField(columnName = COLUMNNAME_ISDOWNLOAD, useGetSet = true, canBeNull = true, unique = false)
    private boolean isDownload;

    public MediaBean() {
        title = "";
        source = SOURCE_UNKNOWN;
        type = MEDIA_UNKNOWN;
        path = "";
        duration = 0;
        size = 0L;
        isDownload = false;
    }

    public MediaBean(String name, int type, int source, String path, int duration, long size, boolean isDownload) {
        this.title = name;
        this.type = type;
        this.source = source;
        this.path = path;
        this.duration = duration;
        this.size = size;
        this.isDownload = isDownload;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean getIsDownload() {
        return isDownload;
    }

    public void setIsDownload(boolean download) {
        isDownload = download;
    }

    public boolean isDownload() {
        return isDownload;
    }

    public void setDownload(boolean download) {
        isDownload = download;
    }

    @Override
    public String toString() {
        return "MediaBean{" +
                ", path=" + path +
                ", title=" + title +
                ", description=" + description +
                ", type=" + type +
                ", source='" + source +
                ", duration=" + duration +
                ", isDownload=" + isDownload +
                '}';
    }
}

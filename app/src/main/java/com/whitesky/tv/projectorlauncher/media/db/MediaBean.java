package com.whitesky.tv.projectorlauncher.media.db;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.whitesky.tv.projectorlauncher.utils.ChineseCharToEnUtil;

/**
 * Created by jeff on 18-2-4.
 */

@DatabaseTable(tableName = "media")
public class MediaBean  implements Parcelable {

    public static final Creator<MediaBean> CREATOR = new Creator<MediaBean>()
    {
        @Override
        public MediaBean createFromParcel(Parcel in)
        {
            return new MediaBean(in);
        }

        @Override
        public MediaBean[] newArray(int size)
        {
            return new MediaBean[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(path);
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeInt(source);
        dest.writeInt(type);
        dest.writeInt(duration);
        dest.writeLong(size);
        dest.writeInt(downloadState);
        dest.writeLong(downloadProgress);
        dest.writeString(url);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    protected MediaBean(Parcel in)
    {
        path = in.readString();
        id = in.readInt();
        title = in.readString();
        description = in.readString();
        source = in.readInt();
        type = in.readInt();
        duration = in.readInt();
        size = in.readLong();
        downloadState = in.readInt();
        downloadProgress = in.readLong();
        url = in.readString();
    }


    // 定义字段在数据库中的字段名
    public static final String COLUMNNAME_TITLE = "title";
    public static final String COLUMNNAME_ID = "id";
    public static final String COLUMNNAME_ORDER_DESCRIPTION = "description";
    public static final String COLUMNNAME_SOURCE = "source";
    public static final String COLUMNNAME_TYPE = "type";
    public static final String COLUMNNAME_PATH = "path";
    public static final String COLUMNNAME_DURATION = "duration";
    public static final String COLUMNNAME_SIZE = "size";
    public static final String COLUMNNAME_DOWNLOAD_STATE = "downloadState";
    public static final String COLUMNNAME_DOWNLOAD_PROGRESS = "downloadProgress";
    public static final String COLUMNNAME_URL = "url";

    public static final int ID_LOCAL = 0;

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

    /** 没有下载 */
    public static final int STATE_DOWNLOAD_NONE = 0;
    /** 等待下载中 */
    public static final int STATE_DOWNLOAD_WAITING = 1;
    /** 开始下载 */
    public static final int STATE_DOWNLOAD_START = 2;
    /** 下载进行中 */
    public static final int STATE_DOWNLOAD_DOWNLOADING = 3;
    /** 下载暂停 */
    public static final int STATE_DOWNLOAD_PAUSED = 4;
    /** 下载完成 */
    public static final int STATE_DOWNLOAD_DOWNLOADED = 5;
    /** 下载失败 */
    public static final int STATE_DOWNLOAD_ERROR = -1;

    //文件的存储路径,全局唯一作为主键
    @DatabaseField(id = true, columnName = COLUMNNAME_PATH, useGetSet = true, canBeNull = false,unique = true)
    private String path = "";

    // 文件id,本地用U盘拷入的文件ID都为0,从云端下载的媒体文件才有有效ID
    @DatabaseField(columnName = COLUMNNAME_ID, useGetSet = true, canBeNull = false, unique = false)
    private int id = ID_LOCAL;

    // 文件的UI显示名,一般就是去掉了扩展名的文件名
    @DatabaseField(columnName = COLUMNNAME_TITLE, useGetSet = true, canBeNull = false, unique = false)
    private String title = "";

    // 中文拼音首字母描述,用于排序
    @DatabaseField(columnName = COLUMNNAME_ORDER_DESCRIPTION, useGetSet = true, canBeNull = true, unique = false)
    private String description = "";

    // 文件的来源
    @DatabaseField(columnName = COLUMNNAME_SOURCE, useGetSet = true, canBeNull = false, unique = false)
    private int source = SOURCE_UNKNOWN;

    // 文件的媒体类型
    @DatabaseField(columnName = COLUMNNAME_TYPE, useGetSet = true, canBeNull = false, unique = false)
    private int type = MEDIA_UNKNOWN;

    //文件的时长
    @DatabaseField(columnName = COLUMNNAME_DURATION, useGetSet = true, canBeNull = true, unique = false)
    private int duration = 0;

    //文件的大小
    @DatabaseField(columnName = COLUMNNAME_SIZE, useGetSet = true, canBeNull = true, unique = false)
    private long size = 0L;

    //云端文件的下载状态
    @DatabaseField(columnName = COLUMNNAME_DOWNLOAD_STATE, useGetSet = true, canBeNull = true, unique = false)
    private int downloadState = STATE_DOWNLOAD_NONE;

    //云端文件的下载进度
    @DatabaseField(columnName = COLUMNNAME_DOWNLOAD_PROGRESS, useGetSet = true, canBeNull = true, unique = false)
    private long downloadProgress = 0L;

    //云端文件的下载位置
    @DatabaseField(columnName = COLUMNNAME_URL, useGetSet = true, canBeNull = true, unique = false)
    private String url = "";

    public MediaBean() {}

    public MediaBean(String name, int id, int type, int source, String path, int duration, long size) {
        this.title = name;
        this.type = type;
        this.id = id;
        this.source = source;
        this.path = path;
        this.duration = duration;
        this.size = size;
        this.description = ChineseCharToEnUtil.getFirstSpell(name);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getDownloadState() {
        return downloadState;
    }

    public void setDownloadState(int state) {
        downloadState = state;
    }

    public long getDownloadProgress() {
        return downloadProgress;
    }

    public void setDownloadProgress(long downloadProgress) {
        this.downloadProgress = downloadProgress;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "MediaBean{" +
                "path=" + path +
                ", id=" + id +
                ", title=" + title +
                ", description=" + description +
                ", type=" + type +
                ", source=" + source +
                ", duration=" + duration +
                ", size=" + size +
                ", url=" + url +
                ", downloadState=" + downloadState +
                ", downloadProgress=" + downloadProgress +
                '}';
    }
}

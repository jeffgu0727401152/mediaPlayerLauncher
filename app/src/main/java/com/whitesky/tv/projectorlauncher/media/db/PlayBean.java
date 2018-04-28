package com.whitesky.tv.projectorlauncher.media.db;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.whitesky.tv.projectorlauncher.utils.ChineseCharToEnUtil;

/**
 * Created by jeff on 18-2-4.
 */

@DatabaseTable(tableName = "play")
public class PlayBean implements Parcelable, Comparable<PlayBean> {

    public static final Creator<PlayBean> CREATOR = new Creator<PlayBean>()
    {
        @Override
        public PlayBean createFromParcel(Parcel in)
        {
            return new PlayBean(in);
        }

        @Override
        public PlayBean[] newArray(int size)
        {
            return new PlayBean[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(idx);
        dest.writeInt(scale);
        dest.writeInt(time);
        dest.writeParcelable(media,flags);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    protected PlayBean(Parcel in)
    {
        idx = in.readInt();
        scale = in.readInt();
        time = in.readInt();
        media = in.readParcelable(MediaBean.class.getClassLoader());
    }

    // 定义字段在数据库中的字段名
    public static final String COLUMNNAME_IDX = "idx";
    public static final String COLUMNNAME_PLAY_SCALE = "scale";
    public static final String COLUMNNAME_PLAY_TIME = "time";
    public static final String COLUMNNAME_MEDIA = "media";

    public static final int MEDIA_SCALE_FIT_XY = 0;
    public static final int MEDIA_SCALE_FIT_CENTER = 1;
    public static final int MEDIA_SCALE_DEFAULT = MEDIA_SCALE_FIT_XY;

    public static final int PLAY_INDEX_PREVIEW = -2;
    public static final int PLAY_INDEX_WAIT_DEFINE = -1;


    // 播放列表中的位置,为主键
    @DatabaseField(id = true, columnName = COLUMNNAME_IDX, useGetSet = true, canBeNull = false,unique = true)
    private int idx;

    // 播放的比例
    @DatabaseField(columnName = COLUMNNAME_PLAY_SCALE, useGetSet = true, canBeNull = true, unique = false)
    private int scale = MEDIA_SCALE_DEFAULT;

    //文件的播放时长(图片用)
    @DatabaseField(columnName = COLUMNNAME_PLAY_TIME, useGetSet = true, canBeNull = true, unique = false)
    private int time = 0;

    // 文件的来源
    @DatabaseField(columnName = COLUMNNAME_MEDIA, useGetSet = true, canBeNull = false,foreign = true,foreignAutoRefresh = true)
    private MediaBean media;

    public PlayBean() {}

    public PlayBean(MediaBean media) {
        this.idx = PLAY_INDEX_WAIT_DEFINE;
        this.scale = MEDIA_SCALE_DEFAULT;
        this.time = media.getDuration();
        this.media = media;
    }

    public PlayBean(PlayBean bean) {
        this.idx = bean.idx;
        this.scale = bean.scale;
        this.time = bean.time;
        this.media = bean.media;
    }

    public PlayBean(int idx, int scale,int time, MediaBean media) {
        this.idx = idx;
        this.scale = scale;
        this.time = time;
        this.media = media;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = (scale==0 ? 0 : 1);
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public MediaBean getMedia() {
        return media;
    }

    public void setMedia(MediaBean media) {
        this.media = media;
    }

    @Override
    public int compareTo(PlayBean o) {
        int i = this.idx - o.getIdx();
        return i;
    }

    @Override
    public String toString() {
        return "PlayBean{" +
                "idx=" + idx +
                ", scale=" + scale +
                ", time=" + time +
                ", media=" + media +
                '}';
    }
}

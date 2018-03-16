package com.whitesky.tv.projectorlauncher.service.mqtt.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by jeff on 18-3-6.
 */

public class MediaListPushBean implements Parcelable {
    private int id;
    private String name;
    private int scale;
    private int duration;

    public static final Creator<MediaListPushBean> CREATOR = new Creator<MediaListPushBean>()
    {
        @Override
        public MediaListPushBean createFromParcel(Parcel in)
        {
            return new MediaListPushBean(in);
        }

        @Override
        public MediaListPushBean[] newArray(int size)
        {
            return new MediaListPushBean[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeInt(scale);
        dest.writeInt(duration);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    protected MediaListPushBean(Parcel in)
    {
        id = in.readInt();
        name = in.readString();
        scale = in.readInt();
        duration = in.readInt();
    }

    public MediaListPushBean(){
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}

package com.whitesky.tv.projectorlauncher.service.mqtt.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by jeff on 18-3-6.
 */

public class FileListPushBean implements Parcelable {
    private int id;
    private String name;

    public static final Creator<FileListPushBean> CREATOR = new Creator<FileListPushBean>()
    {
        @Override
        public FileListPushBean createFromParcel(Parcel in)
        {
            return new FileListPushBean(in);
        }

        @Override
        public FileListPushBean[] newArray(int size)
        {
            return new FileListPushBean[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(id);
        dest.writeString(name);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    protected FileListPushBean(Parcel in)
    {
        id = in.readInt();
        name = in.readString();
    }

    public FileListPushBean(){
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
}

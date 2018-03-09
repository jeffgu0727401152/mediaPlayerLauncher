package com.whitesky.tv.projectorlauncher.media.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by xiaoxuan on 2017/12/11.
 */

public class Result implements Parcelable
{
    private int id;
    private String name;
    private String url;
    private int source;

    // todo talk about what is category?

    protected Result(Parcel in)
    {
        id = in.readInt();
        name = in.readString();
        url = in.readString();
        source = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(url);
        dest.writeInt(source);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final Creator<Result> CREATOR = new Creator<Result>()
    {
        @Override
        public Result createFromParcel(Parcel in)
        {
            return new Result(in);
        }

        @Override
        public Result[] newArray(int size)
        {
            return new Result[size];
        }
    };

    public void setId(int id)
    {
        this.id = id;
    }

    public int getId()
    {
        return id;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }
}

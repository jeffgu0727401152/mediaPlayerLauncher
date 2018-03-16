package com.whitesky.tv.projectorlauncher.service.mqtt.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by jeff on 18-3-6.
 */

public class PlayModePushBean implements Parcelable {
    private int mask;
    private int playMode;

    public static final Creator<PlayModePushBean> CREATOR = new Creator<PlayModePushBean>()
    {
        @Override
        public PlayModePushBean createFromParcel(Parcel in)
        {
            return new PlayModePushBean(in);
        }

        @Override
        public PlayModePushBean[] newArray(int size)
        {
            return new PlayModePushBean[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mask);
        dest.writeInt(playMode);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    protected PlayModePushBean(Parcel in)
    {
        mask = in.readInt();
        playMode = in.readInt();
    }

    public PlayModePushBean(){
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    public int getPlayMode() {
        return playMode;
    }

    public void setPlayMode(int playMode) {
        this.playMode = playMode;
    }

}

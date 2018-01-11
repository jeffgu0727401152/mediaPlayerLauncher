package com.whiteskycn.tv.projectorlauncher.admin.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by xiaoxuan on 2017/12/11.
 */

public class Result implements Parcelable
{
    private int isGuest;
    
    private String userName;
    
    private String deviceNickname;
    
    private String loginDate;
    
    protected Result(Parcel in)
    {
        isGuest = in.readInt();
        userName = in.readString();
        deviceNickname = in.readString();
        loginDate = in.readString();
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(isGuest);
        dest.writeString(userName);
        dest.writeString(deviceNickname);
        dest.writeString(loginDate);
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
    
    public void setIsGuest(int isGuest)
    {
        this.isGuest = isGuest;
    }
    
    public int getIsGuest()
    {
        return isGuest;
    }
    
    public void setUserName(String userName)
    {
        this.userName = userName;
    }
    
    public String getUserName()
    {
        return userName;
    }
    
    public void setDeviceNickname(String deviceNickname)
    {
        this.deviceNickname = deviceNickname;
    }
    
    public String getDeviceNickname()
    {
        return deviceNickname;
    }
    
    public void setLoginDate(String loginDate)
    {
        this.loginDate = loginDate;
    }
    
    public String getLoginDate()
    {
        return loginDate;
    }
    
}

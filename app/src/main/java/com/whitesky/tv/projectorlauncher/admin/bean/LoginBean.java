package com.whitesky.tv.projectorlauncher.admin.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by xiaoxuan on 2017/12/11.
 */

public class LoginBean implements Parcelable
{
    public static final Creator<LoginBean> CREATOR = new Creator<LoginBean>()
    {
        @Override
        public LoginBean createFromParcel(Parcel in)
        {
            return new LoginBean(in);
        }
        
        @Override
        public LoginBean[] newArray(int size)
        {
            return new LoginBean[size];
        }
    };
    
    private String status;
    
    private String message;
    
    private Result result;
    
    protected LoginBean(Parcel in)
    {
        status = in.readString();
        message = in.readString();
        result = in.readParcelable(Result.class.getClassLoader());
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(status);
        dest.writeString(message);
        dest.writeParcelable(result, flags);
    }
    
    @Override
    public int describeContents()
    {
        return 0;
    }
    
    public String getStatus()
    {
        return status;
    }
    
    public void setStatus(String status)
    {
        this.status = status;
    }
    
    public String getMessage()
    {
        return message;
    }
    
    public void setMessage(String message)
    {
        this.message = message;
    }
    
    public Result getResult()
    {
        return result;
    }
    
    public void setResult(Result result)
    {
        this.result = result;
    }
    
}
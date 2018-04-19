package com.whitesky.tv.projectorlauncher.media.bean;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by xiaoxuan on 2017/12/11.
 */

public class CloudListBean implements Parcelable
{
    public static final Creator<CloudListBean> CREATOR = new Creator<CloudListBean>()
    {
        @Override
        public CloudListBean createFromParcel(Parcel in)
        {
            return new CloudListBean(in);
        }
        
        @Override
        public CloudListBean[] newArray(int size)
        {
            return new CloudListBean[size];
        }
    };
    
    private String status;
    
    private String message;

    private String account;

    private List<Result> result ;

    protected CloudListBean(Parcel in)
    {
        status = in.readString();
        message = in.readString();
        account = in.readString();
        result = in.readArrayList(Result.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(status);
        dest.writeString(message);
        dest.writeString(account);
        dest.writeList(result);
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

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setResult(List<Result> result){
        this.result = result;
    }
    public List<Result> getResult(){
        return this.result;
    }
    
}
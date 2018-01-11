package com.whiteskycn.tv.projectorlauncher.admin.presenter;

import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.orhanobut.logger.Logger;
import com.whiteskycn.tv.projectorlauncher.admin.bean.LoginBean;
import com.whiteskycn.tv.projectorlauncher.admin.model.AdminModel;
import com.whiteskycn.tv.projectorlauncher.common.Contants;
import com.whiteskycn.tv.projectorlauncher.common.HttpConsts;
import com.whiteskycn.tv.projectorlauncher.utils.SharedPreferencesUtil;
import com.whiteskycn.wsd.android.WsdSerialnum;

import java.io.IOException;

import javax.net.ssl.SSLContext;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by xiaoxuan on 2017/12/22.
 */

public class AccountImpl implements IAccountActivity
{
    public final String LOGIN_STATUS_000000 = "000000";
    
    public final String LOGIN_STATUS_200101 = "200101";
    
    private LoginBean mLoginBean;
    
    private Context mContext;
    
    public AccountImpl(Context context)
    {
        this.mContext = context;
    }
    
    @Override
    public void getAccountInfo()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                OkHttpClient mClient = new OkHttpClient();
                if (HttpConsts.GETLOGININFO_URL.contains("https"))
                {
                    try
                    {
                        mClient =
                            new OkHttpClient.Builder().sslSocketFactory(SSLContext.getDefault().getSocketFactory())
                                .build();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    mClient = new OkHttpClient();
                }
                
                FormBody body =
                    new FormBody.Builder().add("method", "post")
                        .add("sn", "123456")//todo new String(WsdSerialnum.read()).toUpperCase())
                        .build();
                Request request = new Request.Builder().url(HttpConsts.GETLOGININFO_URL).post(body).build();
                Call call = mClient.newCall(request);
                call.enqueue(new okhttp3.Callback()
                {
                    // 失败
                    @Override
                    public void onFailure(Call call, IOException e)
                    {
                        Logger.i("onFailure");
                        e.printStackTrace();
                    }
                    
                    // 成功
                    @Override
                    public void onResponse(Call call, Response response)
                        throws IOException
                    {
                        String htmlStr = response.body().string();
                        Gson gson = new Gson();
                        mLoginBean = gson.fromJson(htmlStr, LoginBean.class);
                        Logger.e("get response from server" + htmlStr);
                        if (response.code() == HttpConsts.STATUS_CODE_200)
                        {
                            if (mLoginBean.getStatus().equals(LOGIN_STATUS_000000))
                            {
                                // do sth when logout success
                                if (mLoginBean.getResult() != null)
                                {
                                    Intent intent = new Intent(AdminModel.LOGIN_INFO_URI);
                                    intent.putExtra(AdminModel.INTENT_DATA_LOGINBEAN, mLoginBean); // 向广播接收器传递数据
                                    mContext.sendBroadcast(intent); // 发送广播
                                }
                                Logger.e(" success");
                            }
                        }
                        else if (response.code() == HttpConsts.STATUS_CODE_422)
                        {
                            if (mLoginBean.getStatus().equals(LOGIN_STATUS_200101))
                            {
                                Logger.e("Device not logged in.");
                            }
                        }
                        else if (response.code() == HttpConsts.STATUS_CODE_500)
                        {
                            Logger.e("onResponse  500");
                        }
                        else
                        {
                            Logger.e("onResponse  unknow");
                        }
                    }
                });
            }
        }).start();
    }
    
    @Override
    public void outAccount()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                OkHttpClient mClient = new OkHttpClient();
                if (HttpConsts.DEVICELOGOUT_URL.contains("https"))
                {
                    try
                    {
                        mClient =
                            new OkHttpClient.Builder().sslSocketFactory(SSLContext.getDefault().getSocketFactory())
                                .build();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    mClient = new OkHttpClient();
                }
                
                FormBody body =
                    new FormBody.Builder().add("method", "post")
                        .add("sn", "123456")//todo new String(WsdSerialnum.read()).toUpperCase())
                        .build();
                Request request = new Request.Builder().url(HttpConsts.DEVICELOGOUT_URL).post(body).build();
                Call call = mClient.newCall(request);
                call.enqueue(new okhttp3.Callback()
                {
                    // 失败
                    @Override
                    public void onFailure(Call call, IOException e)
                    {
                        Logger.i("onFailure");
                        e.printStackTrace();
                    }
                    
                    // 成功
                    @Override
                    public void onResponse(Call call, Response response)
                        throws IOException
                    {
                        String htmlStr = response.body().string();
                        Gson gson = new Gson();
                        mLoginBean = gson.fromJson(htmlStr, LoginBean.class);
                        Logger.e("get response from server" + htmlStr);
                        if (response.code() == HttpConsts.STATUS_CODE_200)
                        {
                            if (mLoginBean.getStatus().equals("000000"))
                            {
                                // do sth when logout success
                                SharedPreferencesUtil shared = new SharedPreferencesUtil(mContext, Contants.CONFIG);
                                shared.putBoolean(Contants.IS_SETUP_PASS, false);
                                shared.putBoolean(Contants.IS_ACTIVATE, false);
                                getAccountInfo();
                            }
                        }
                        else if (response.code() == HttpConsts.STATUS_CODE_422)
                        {
                            if (mLoginBean.getStatus().equals("200101"))
                            {
                                Logger.e("Device not logged in.");
                            }
                            
                        }
                        else if (response.code() == HttpConsts.STATUS_CODE_500)
                        {
                            Logger.i("onResponse  500");
                        }
                        else
                        {
                            Logger.i("onResponse  unknow");
                        }
                    }
                });
            }
        }).start();
    }
}

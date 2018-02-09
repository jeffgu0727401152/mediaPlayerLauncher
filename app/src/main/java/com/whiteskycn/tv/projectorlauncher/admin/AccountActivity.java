package com.whiteskycn.tv.projectorlauncher.admin;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.mylhyl.zxing.scanner.encode.QREncode;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.admin.adapter.AccountAdapter;
import com.whiteskycn.tv.projectorlauncher.admin.bean.LoginBean;
import com.whiteskycn.tv.projectorlauncher.common.Contants;
import com.whiteskycn.tv.projectorlauncher.common.HttpConsts;
import com.whiteskycn.tv.projectorlauncher.common.bean.ListViewBean;
import com.whiteskycn.tv.projectorlauncher.utils.SharedPreferencesUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class AccountActivity extends Activity implements View.OnClickListener
{
    
    private final int UPDATE_ACCOUNT_INFO = 0x01;
    
    private final MyHandler mHandler = new MyHandler(this);
    
    private List<ListViewBean> mBeanDatas = new ArrayList<ListViewBean>();
    
    private AccountAdapter mProAdapter;
    
    private ListView mLvAccount;
    
    private TextView mTvAccount;
    
    private Dialog mLoginDialog;
    
    private Button mBtLogin, mBtLogout;
    
    private LoginBean mLoginBean;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_account);
        getAccountInfo();
        mTvAccount = (TextView)findViewById(R.id.tv_admin_account_ac);
        mBtLogin = (Button)findViewById(R.id.bt_admin_account_login);
        mBtLogin.requestFocus();
        mBtLogin.setOnClickListener(this);
        mBtLogout = (Button)findViewById(R.id.bt_admin_account_logout);
        mBtLogout.requestFocus();
        mBtLogout.setOnClickListener(this);
        mLvAccount = (ListView)findViewById(R.id.lv_admin_account_list);
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        LinearLayout layout = (LinearLayout)findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.img_background);
    }
    
    private void updateAccountInfo()
    {
        if (mLoginBean != null)
        {
            if (mLoginBean.getResult().getIsGuest() == 0)
            {
                mBtLogout.setVisibility(View.VISIBLE);
                mBtLogin.setVisibility(View.INVISIBLE);
                mBtLogout.requestFocus();
            }
            else
            {
                mBtLogin.setVisibility(View.VISIBLE);
                mBtLogout.setVisibility(View.INVISIBLE);
                mBtLogin.requestFocus();
            }
            if (mBeanDatas != null)
                mBeanDatas.clear();
            mTvAccount.setText(mLoginBean.getResult().getIsGuest() == 0 ? mLoginBean.getResult().getUserName()
                : getString(R.string.title_admin_account_type_Visitor));
            mBeanDatas.add(new ListViewBean(mLoginBean.getResult().getDeviceNickname(), mLoginBean.getResult()
                .getLoginDate(), 0, 0));
            mProAdapter = new AccountAdapter(getApplicationContext(), mBeanDatas, R.layout.item_list_project);
            mLvAccount.setAdapter(mProAdapter);
        }
    }
    
    private void showDialog()
    {
        final View view = LayoutInflater.from(this).inflate(R.layout.dialog_login2, null);
        mLoginDialog = new Dialog(this, R.style.ReasonDialog);
        mLoginDialog.setContentView(view);
        ImageView imageView = (ImageView)view.findViewById(R.id.iv_admin_dialog_login);
        String loginURL = HttpConsts.LOGIN_URL + DeviceInfoActivity.getSysSN();
        Bitmap bitmap = new QREncode.Builder(this).setColor(getResources().getColor(R.color.colorPrimary))// 二维码颜色
            // .setParsedResultType(ParsedResultType.TEXT)//默认是TEXT类型
            // .setContents(AdminModel.LOGIN_URL + "123456789")// 二维码内容
            .setContents(loginURL)
            // 二维码内容
            // .setLogoBitmap(logoBitmap)// 二维码中间logo
            .build()
            .encodeAsBitmap();
        imageView.setImageBitmap(bitmap);
        mLoginDialog.show();
        mLoginDialog.setCanceledOnTouchOutside(true);
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams lp = mLoginDialog.getWindow().getAttributes();
        lp.width = (int)(display.getWidth() * 0.9);
        lp.alpha = 0.8f;
        mLoginDialog.getWindow().setAttributes(lp);
    }
    
    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.bt_admin_account_login:
                showDialog();
                break;
            case R.id.bt_admin_account_logout:
                outAccount();
                break;
        }
    }
    
    @Override
    protected void onDestroy()
    {
        if (mBeanDatas != null)
        {
            mBeanDatas = null;
        }
        if (mLoginDialog != null)
        {
            mLoginDialog = null;
        }
        super.onDestroy();
    }
    
    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(getApplicationContext(), AdminActivity.class);
        startActivity(intent);
        AccountActivity.this.finish();
        super.onBackPressed();
    }
    
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
                        .add("sn", DeviceInfoActivity.getSysSN())
                        .build();
                Request request = new Request.Builder().url(HttpConsts.GETLOGININFO_URL).post(body).build();
                Call call = mClient.newCall(request);
                call.enqueue(new okhttp3.Callback()
                {
                    // 失败
                    @Override
                    public void onFailure(Call call, IOException e)
                    {
                        e.printStackTrace();
                    }
                    
                    // 成功
                    @Override
                    public void onResponse(Call call, Response response)
                        throws IOException
                    {
                        //todo 处理404
                        String htmlStr = response.body().string();
                        Gson gson = new Gson();
                        mLoginBean = gson.fromJson(htmlStr, LoginBean.class);
                        if (response.code() == HttpConsts.STATUS_CODE_200)
                        {
                            if (mLoginBean.getStatus().equals(HttpConsts.LOGIN_STATUS_000000))
                            {
                                // do sth when logout success
                                if (mLoginBean.getResult() != null)
                                {
                                    mHandler.sendEmptyMessage(UPDATE_ACCOUNT_INFO);
                                }
                            }
                        }
                        else if (response.code() == HttpConsts.STATUS_CODE_422)
                        {
                            if (mLoginBean.getStatus().equals(HttpConsts.LOGIN_STATUS_200101))
                            {
                            }
                        }
                        else if (response.code() == HttpConsts.STATUS_CODE_500)
                        {
                        }
                        else
                        {
                        }
                    }
                });
            }
        }).start();
    }
    
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
                        .add("sn", DeviceInfoActivity.getSysSN())
                        .build();
                Request request = new Request.Builder().url(HttpConsts.DEVICELOGOUT_URL).post(body).build();
                Call call = mClient.newCall(request);
                call.enqueue(new okhttp3.Callback()
                {
                    // 失败
                    @Override
                    public void onFailure(Call call, IOException e)
                    {
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
                        if (response.code() == HttpConsts.STATUS_CODE_200)
                        {
                            if (mLoginBean.getStatus().equals("000000"))
                            {
                                // do sth when logout success
                                SharedPreferencesUtil shared =
                                    new SharedPreferencesUtil(getApplicationContext(), Contants.CONFIG);
                                shared.putBoolean(Contants.IS_SETUP_PASS, false);
                                shared.putBoolean(Contants.IS_ACTIVATE, false);
                                getAccountInfo();
                            }
                        }
                        else if (response.code() == HttpConsts.STATUS_CODE_422)
                        {
                            if (mLoginBean.getStatus().equals("200101"))
                            {
                            }
                        }
                        else if (response.code() == HttpConsts.STATUS_CODE_500)
                        {
                        }
                        else
                        {
                        }
                    }
                });
            }
        }).start();
    }
    
    private static class MyHandler extends Handler
    {
        private final WeakReference<AccountActivity> mActivity;
        
        public MyHandler(AccountActivity activity)
        {
            mActivity = new WeakReference<AccountActivity>(activity);
        }
        
        @Override
        public void handleMessage(Message msg)
        {
            AccountActivity activity = mActivity.get();
            if (activity != null)
            {
                activity.updateAccountInfo();
            }
        }
    }

}

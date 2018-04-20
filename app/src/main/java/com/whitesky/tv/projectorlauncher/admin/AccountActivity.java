package com.whitesky.tv.projectorlauncher.admin;

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
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.mylhyl.zxing.scanner.encode.QREncode;
import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.admin.adapter.AccountAdapter;
import com.whitesky.tv.projectorlauncher.admin.bean.LoginBean;
import com.whitesky.tv.projectorlauncher.common.Contants;
import com.whitesky.tv.projectorlauncher.common.HttpConstants;
import com.whitesky.tv.projectorlauncher.common.bean.ListViewBean;
import com.whitesky.tv.projectorlauncher.media.MediaActivity;
import com.whitesky.tv.projectorlauncher.utils.SharedPreferencesUtil;

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

import static com.whitesky.tv.projectorlauncher.common.HttpConstants.LOGIN_STATUS_SUCCESS;
import static com.whitesky.tv.projectorlauncher.common.HttpConstants.URL_LOGIN_PROJECT_NAME;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class AccountActivity extends Activity implements View.OnClickListener
{
    private final String TAG = this.getClass().getSimpleName();

    private final static int MSG_UPDATE_ACCOUNT_INFO = 1;
    private final static int MSG_SYNC_CLOUD_MEDIA_LIST = 2;

    private final AccountInfoHandler mHandler = new AccountInfoHandler(this);
    
    private List<ListViewBean> mLoginHistoryDatas = new ArrayList<ListViewBean>();
    
    private AccountAdapter mLoginHistoryListAdapter;
    
    private ListView mLvAccount;
    private TextView mTvAccount;
    private Dialog mLoginDialog;
    private Button mBtLogin;
    private Button mBtLogout;

    private LoginBean mLoginBean;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_account);
        mTvAccount = findViewById(R.id.tv_admin_account_ac);
        mBtLogin = findViewById(R.id.bt_admin_account_login);
        mBtLogin.requestFocus();
        mBtLogin.setOnClickListener(this);
        mBtLogout = findViewById(R.id.bt_admin_account_logout);
        mBtLogout.requestFocus();
        mBtLogout.setOnClickListener(this);
        mLvAccount = findViewById(R.id.lv_admin_account_list);

        getAccountInfo();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        RelativeLayout layout = findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.shape_background);
    }
    
    private void updateAccountInfo() {
        if (mLoginBean != null) {
            if (mLoginBean.getResult().getIsGuest() == 0) {
                mBtLogout.setVisibility(View.VISIBLE);
                mBtLogin.setVisibility(View.INVISIBLE);
                mBtLogout.requestFocus();

                mTvAccount.setText(mLoginBean.getResult().getUserName());

                if (mLoginHistoryDatas != null) {
                    mLoginHistoryDatas.clear();
                    mLoginHistoryDatas.add(new ListViewBean(mLoginBean.getResult().getDeviceNickname(), mLoginBean.getResult().getLoginDate(), 0, 0));
                    mLoginHistoryListAdapter = new AccountAdapter(getApplicationContext(), mLoginHistoryDatas, R.layout.item_login_history_list);
                    mLvAccount.setAdapter(mLoginHistoryListAdapter);
                }

            } else {
                mBtLogin.setVisibility(View.VISIBLE);
                mBtLogout.setVisibility(View.INVISIBLE);
                mBtLogin.requestFocus();

                mTvAccount.setText(getString(R.string.title_admin_account_type_visitor));
            }
        }
    }
    
    private void showQRcode()
    {
        final View view = LayoutInflater.from(this).inflate(R.layout.dialog_login2, null);
        mLoginDialog = new Dialog(this, R.style.ReasonDialog);
        mLoginDialog.setContentView(view);
        ImageView imageView = view.findViewById(R.id.iv_admin_dialog_login);
        String loginURL = HttpConstants.URL_LOGIN + DeviceInfoActivity.getSysSn(this) + URL_LOGIN_PROJECT_NAME;
        Bitmap bitmap = new QREncode.Builder(this).setColor(getResources().getColor(R.color.colorPrimary))// 二维码颜色
            // .setParsedResultType(ParsedResultType.TEXT) //默认是TEXT类型
            .setContents(loginURL)                         // 二维码内容
            // .setLogoBitmap(logoBitmap)                  // 二维码中间logo
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
                showQRcode();
                break;
            case R.id.bt_admin_account_logout:
                accountLogout();
                break;
        }
    }
    
    @Override
    protected void onDestroy()
    {
        if (mLoginHistoryDatas != null)
        {
            mLoginHistoryDatas = null;
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

    public void getAccountInfo() {
        OkHttpClient mClient = new OkHttpClient();
        if (HttpConstants.URL_GET_LOGIN_INFO.contains("https")) {
            try {
                mClient = new OkHttpClient.Builder()
                        .sslSocketFactory(SSLContext.getDefault().getSocketFactory())
                        .build();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mClient = new OkHttpClient();
        }

        FormBody body = new FormBody.Builder()
                .add("sn", DeviceInfoActivity.getSysSn(this))
                .build();

        Request request = new Request.Builder().url(HttpConstants.URL_GET_LOGIN_INFO).post(body).build();
        Call call = mClient.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            // 失败
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            // 成功
            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);

                } else if (response.code() == HttpConstants.HTTP_STATUS_SUCCESS) {
                    String htmlBody = response.body().string();

                    try {
                        mLoginBean = new Gson().fromJson(htmlBody, LoginBean.class);
                    } catch (IllegalStateException e) {
                        mLoginBean = null;
                        Log.e(TAG, "Gson parse error!");
                    }


                    if (mLoginBean != null && mLoginBean.getStatus().equals(LOGIN_STATUS_SUCCESS)) {
                        if (mLoginBean.getResult() != null) {
                            mHandler.sendEmptyMessage(MSG_UPDATE_ACCOUNT_INFO);
                        }
                    }

                } else {
                    Log.e(TAG, "getAccountInfo response http code undefine!");
                }
            }
        });
    }

    public void accountLogout() {
        OkHttpClient mClient;
        if (HttpConstants.URL_DEVICE_LOGOUT.contains("https")) {
            try {
                mClient = new OkHttpClient.Builder()
                        .sslSocketFactory(SSLContext.getDefault().getSocketFactory())
                        .build();
            } catch (Exception e) {
                e.printStackTrace();
                mClient = new OkHttpClient();
            }

        } else {
            mClient = new OkHttpClient();
        }

        FormBody body = new FormBody.Builder()
                .add("sn", DeviceInfoActivity.getSysSn(this))
                .build();

        Request request = new Request.Builder().url(HttpConstants.URL_DEVICE_LOGOUT).post(body).build();
        Call call = mClient.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            // 失败
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            // 成功
            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);

                } else if (response.code() == HttpConstants.HTTP_STATUS_SUCCESS) {
                    String htmlBody = response.body().string();

                    try {
                        mLoginBean = new Gson().fromJson(htmlBody, LoginBean.class);
                    } catch (IllegalStateException e) {
                        mLoginBean = null;
                        Log.e(TAG, "Gson parse error!");
                    }

                    if (mLoginBean != null && mLoginBean.getStatus().equals(LOGIN_STATUS_SUCCESS)) {
                        SharedPreferencesUtil shared = new SharedPreferencesUtil(getApplicationContext(), Contants.PREF_CONFIG);
                        shared.putBoolean(Contants.IS_ACTIVATE, false);
                        mHandler.sendEmptyMessage(MSG_SYNC_CLOUD_MEDIA_LIST);
                        getAccountInfo();
                    }
                } else {
                    Log.e(TAG, "logout response http code undefine!");
                }
            }
        });
    }
    
    private static class AccountInfoHandler extends Handler
    {
        private final WeakReference<AccountActivity> mActivity;
        
        public AccountInfoHandler(AccountActivity activity)
        {
            mActivity = new WeakReference<AccountActivity>(activity);
        }
        
        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_UPDATE_ACCOUNT_INFO:
                    AccountActivity activity = mActivity.get();
                    if (activity != null) {
                        activity.updateAccountInfo();
                    }
                    break;

                case MSG_SYNC_CLOUD_MEDIA_LIST:
                    MediaActivity.loadMediaListFromCloud(mActivity.get(), new MediaActivity.cloudListGetCallback() {
                        @Override
                        public void cloudSyncDone(boolean result) {
                            // do nothing
                        }
                    });

                default:
                    break;
            }
        }
    }
}

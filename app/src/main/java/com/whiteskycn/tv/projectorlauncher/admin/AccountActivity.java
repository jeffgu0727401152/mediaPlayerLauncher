package com.whiteskycn.tv.projectorlauncher.admin;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.mylhyl.zxing.scanner.encode.QREncode;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.admin.adapter.AccountAdapter;
import com.whiteskycn.tv.projectorlauncher.admin.bean.LoginBean;
import com.whiteskycn.tv.projectorlauncher.admin.presenter.AccountImpl;
import com.whiteskycn.tv.projectorlauncher.common.Contants;
import com.whiteskycn.tv.projectorlauncher.common.HttpConsts;
import com.whiteskycn.tv.projectorlauncher.common.bean.ListViewBean;
import com.whiteskycn.tv.projectorlauncher.settings.common.SkinSettingManager;
import com.whiteskycn.tv.projectorlauncher.utils.SharedPreferencesUtil;
import com.whiteskycn.wsd.android.WsdSerialnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class AccountActivity extends Activity implements AdapterView.OnItemClickListener, View.OnClickListener,
    UIMessage
{
    public final String LOGIN_INFO_URI = "com.whiteskycn.up100.admin.login_info";
    public final String INTENT_DATA_LOGINBEAN = "LoginBean";

    private final String TAG = this.getClass().getSimpleName();

    private List<ListViewBean> mBeanDatas = new ArrayList<ListViewBean>();
    
    private AccountAdapter mProAdapter;
    
    private ListView mLvAccount;
    
    private TextView mTvAccount;
    
    private SharedPreferencesUtil mShared;
    
    private Dialog mLoginDialog;
    
    private Button mBtLogin, mBtLogout;
    
    private AccountImpl mAccountPresenter;
    
    private UIReceiver uiReceiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_account);
        // 配置存储
        mShared = new SharedPreferencesUtil(getApplicationContext(), Contants.CONFIG);
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        // 注册Ui更新接收器
        uiReceiver = new UIReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LOGIN_INFO_URI);
        registerReceiver(uiReceiver, intentFilter);
        uiReceiver.setMessage(this);

        mAccountPresenter = new AccountImpl(getApplicationContext());
        mAccountPresenter.getAccountInfo();

        LinearLayout layout = (LinearLayout)findViewById(R.id.ll_skin);
        SkinSettingManager mSettingManager = new SkinSettingManager(this, layout);
        mSettingManager.initSkins();
    }
    
    private void showDialog()
    {
        final View view = LayoutInflater.from(this).inflate(R.layout.dialog_login2, null);
        mLoginDialog = new Dialog(this, R.style.ReasonDialog);
        mLoginDialog.setContentView(view);
        ImageView imageView = (ImageView)view.findViewById(R.id.iv_admin_dialog_login);
        //todo
        String loginURL = HttpConsts.LOGIN_URL + "123456";
        //String loginURL = HttpConsts.LOGIN_URL + new String(WsdSerialnum.read()).toUpperCase();
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
    public void updateMessage(final LoginBean loginBean)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (loginBean != null)
                {
                    if (loginBean.getResult().getIsGuest() == 0)
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
                    mTvAccount.setText(loginBean.getResult().getIsGuest() == 0 ? loginBean.getResult().getUserName()
                        : getString(R.string.title_admin_account_type_Visitor));
                    mBeanDatas.add(new ListViewBean(loginBean.getResult().getDeviceNickname(), loginBean.getResult()
                        .getLoginDate(), 0, 0));
                    mProAdapter = new AccountAdapter(getApplicationContext(), mBeanDatas, R.layout.item_list_project);
                    mLvAccount.setAdapter(mProAdapter);
                }
            }
        });
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
                mAccountPresenter.outAccount();
                break;
        }
    }
    
    private class UIReceiver extends BroadcastReceiver
    {
        private UIMessage uiMessage;
        
        @Override
        public void onReceive(Context context, Intent intent)
        {
            LoginBean msg = intent.getParcelableExtra(INTENT_DATA_LOGINBEAN);
            uiMessage.updateMessage(msg);
        }
        
        public void setMessage(UIMessage uiMessage)
        {
            this.uiMessage = uiMessage;
        }
    }
    
    @Override
    protected void onPause()
    {
        unregisterReceiver(uiReceiver);
        if (mAccountPresenter != null)
        {
            mAccountPresenter = null;
        }
        if (mBeanDatas != null)
        {
            mBeanDatas = null;
        }
        if (mLoginDialog != null)
        {
            mLoginDialog = null;
        }
        if (mAccountPresenter != null)
        {
            mAccountPresenter = null;
        }
        super.onPause();
    }
    
}

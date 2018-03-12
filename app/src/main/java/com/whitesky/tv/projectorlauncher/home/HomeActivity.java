package com.whitesky.tv.projectorlauncher.home;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.whitesky.sdk.widget.BorderView;
import com.whitesky.sdk.widget.RoundedFrameLayout;
import com.whitesky.sdk.widget.TvScrollTextView;
import com.whitesky.sdk.widget.focus.FocusBorder;
import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.admin.AdminActivity;
import com.whitesky.tv.projectorlauncher.app.AppActivity;
import com.whitesky.tv.projectorlauncher.application.MainApplication;
import com.whitesky.tv.projectorlauncher.service.MqttSslService;
import com.whitesky.tv.projectorlauncher.media.MediaActivity;
import com.whitesky.tv.projectorlauncher.settings.SysSettingActivity;
import com.whitesky.tv.projectorlauncher.utils.ToastUtil;

import javax.net.ssl.SSLContext;

import okhttp3.OkHttpClient;


public class HomeActivity extends Activity implements View.OnClickListener, View.OnHoverListener, FocusBorder.OnFocusCallback
{
    private final String TAG = this.getClass().getSimpleName();

    private final static int MSG_HOME_REPORT_INFO = 1;

    private ImageView mEthConnectImg;
    private ImageView mWifiConnectImg;
    private ImageView mLogoView;

    private int mLogoClickCount = 0;
    private int mBackClickCount = 0;


    private boolean mLastWifiConnected = false;
    private boolean mLastEthConnected = false;

    private BroadcastReceiver mNetworkStatusReceiver;

    //遥控光标框
    private FocusBorder mFocusBorder;

    public final int SCROLLING_MARQUEE_SPEED = 2;
    public final int SCROLLING_MARQUEE_TIMES = 1314;
    private boolean focusableInTouchMode;

    @Override
    public boolean onHover(View v, MotionEvent event)
    {
        int what = event.getAction();
        switch(what){
            case MotionEvent.ACTION_HOVER_ENTER: //鼠标进入view
                v.setFocusableInTouchMode(true);
                v.requestFocusFromTouch();
                v.bringToFront();
                break;
            case MotionEvent.ACTION_HOVER_EXIT: //鼠标离开view
                break;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home2);

        mEthConnectImg = (ImageView)findViewById(R.id.iv_home2_net);
        mWifiConnectImg = (ImageView)findViewById(R.id.iv_home2_wifi);
        mLogoView = (ImageView)findViewById(R.id.iv_home2_1);
        mLogoView.setOnClickListener(this);

        TvScrollTextView scrollingView = (TvScrollTextView)findViewById(R.id.sv_home2_message);
        scrollingView.setText(getString(R.string.str_home_welcome));
        scrollingView.setClickable(true);
        scrollingView.setSpeed(SCROLLING_MARQUEE_SPEED);
        scrollingView.setTimes(SCROLLING_MARQUEE_TIMES);

        final RoundedFrameLayout mediaPage = (RoundedFrameLayout)findViewById(R.id.rf_home2_media);
        final RoundedFrameLayout peoplePage = (RoundedFrameLayout)findViewById(R.id.rf_home2_admin);
        final RoundedFrameLayout sysPage = (RoundedFrameLayout)findViewById(R.id.rf_home2_sys);

        mediaPage.setOnClickListener(this);
        mediaPage.setOnHoverListener(this);

        peoplePage.setOnClickListener(this);
        peoplePage.setOnHoverListener(this);

        sysPage.setOnClickListener(this);
        sysPage.setOnHoverListener(this);

        BorderView border = new BorderView(getApplicationContext());
        initBorder();
        border.setBackgroundResource(R.drawable.border_white2);
        ViewGroup list = (ViewGroup)findViewById(R.id.rl_home2_list);
        border.attachTo(list);
        mFocusBorder.boundGlobalFocusListener(this);

        // SSL 与 MQTT服务
        startService(new Intent(getApplicationContext(), MqttSslService.class));
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        LinearLayout layout = (LinearLayout)findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.shape_background);

        mNetworkStatusReceiver = new NetworkStateBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkStatusReceiver, filter);

        mLogoClickCount = 0;
        mBackClickCount = 0;

        // 如果有播放列表,直接跳转去mediaActivity
        if (MediaActivity.hasPlaylistConfig(this) && !((MainApplication)getApplication()).mFirstInitDone) {
            Intent intentMedia = new Intent(getApplicationContext(), MediaActivity.class);
            if (intentMedia.resolveActivity(getPackageManager())!=null) {
                startActivity(intentMedia);
            }
        }
    }

    public void setFocusableInTouchMode(boolean focusableInTouchMode) {
        this.focusableInTouchMode = focusableInTouchMode;
    }

    // 监听网络状态变化的广播接收器
    public class NetworkStateBroadcastReceiver extends BroadcastReceiver
    {
        private ConnectivityManager mConnectivityManager;

        private NetworkInfo ethNetInfo;
        private NetworkInfo wifiNetInfo;

        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            ethNetInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
            wifiNetInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            boolean ethConnected = ethNetInfo!=null ? ethNetInfo.isConnected() : false;
            boolean wifiConnected = wifiNetInfo!=null ? wifiNetInfo.isConnected() : false;

            if (ethConnected)
            {
                mEthConnectImg.setVisibility(View.VISIBLE);
                mWifiConnectImg.setVisibility(View.INVISIBLE);
                Log.v(TAG,"use eth connect");
            }
            else if (wifiConnected)
            {
                mWifiConnectImg.setVisibility(View.VISIBLE);
                mEthConnectImg.setVisibility(View.INVISIBLE);
                Log.v(TAG,"use wifi connect");
            }
            else
            {
                mEthConnectImg.setVisibility(View.INVISIBLE);
                mWifiConnectImg.setVisibility(View.INVISIBLE);
                Log.v(TAG,"no connect");
            }

            //真值表推导的
            if (ethConnected)
            {
                if (!mLastEthConnected)
                {
                    //由 mqtt service内部自己保证网络切换以后与服务器的连接
                }
            }
            else if (wifiConnected)
            {
                //由 mqtt service内部自己保证网络切换以后与服务器的连接
            }

            mLastWifiConnected = wifiConnected;
            mLastEthConnected = ethConnected;

        }
    }


    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.rf_home2_admin:
                Intent intentAdmin = new Intent(getApplicationContext(), AdminActivity.class);
                if (intentAdmin.resolveActivity(getPackageManager())!=null) {
                    startActivity(intentAdmin);
                }
                break;

            case R.id.rf_home2_media:
                Intent intentMedia = new Intent(getApplicationContext(), MediaActivity.class);
                if (intentMedia.resolveActivity(getPackageManager())!=null) {
                    startActivity(intentMedia);
                }
                break;

            case R.id.rf_home2_sys:
                Intent intentSys = new Intent(getApplicationContext(), SysSettingActivity.class);
                if (intentSys.resolveActivity(getPackageManager())!=null)
                {
                    startActivity(intentSys);
                }
                break;

            case R.id.iv_home2_1:
                if (mBackClickCount>=5) {
                    mLogoClickCount++;
                }

                if (mLogoClickCount>=5) {
                    mLogoClickCount = 0;
                    mBackClickCount = 0;
                    Log.i(TAG,"go to app list!");
                    Intent intentApp = new Intent(getApplicationContext(), AppActivity.class);
                    if (intentApp.resolveActivity(getPackageManager())!=null)
                    {
                        startActivity(intentApp);
                    }
                }
                break;
            default:
                break;
        }
    }


    @Override
    protected void onDestroy()
    {
        if (mFocusBorder != null)
        {
            mFocusBorder = null;
        }
        super.onDestroy();
    }

    @Override
    final public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        // 主页面屏蔽返回键，就是不让你返回，怎么地吧 ^ * ^
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            mBackClickCount++;
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU)
        {
            Log.v(TAG,"onKeyDown event.getRepeatCount() " + event.getRepeatCount());
            if (event.getRepeatCount() > 0)
            {
                ToastUtil.showToast(getApplicationContext(), "ssss");
                return true;
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }


    private void initBorder()
    {
        mFocusBorder = new FocusBorder.Builder().asColor().shadowWidth(TypedValue.COMPLEX_UNIT_DIP, 18f) // 阴影宽度(方式二)
                .borderColor(getResources().getColor(R.color.white))
                // 边框颜色
                .build(this);
    }

    @Override
    final public FocusBorder.Options onFocus(View oldFocus, View newFocus)
    {
        if (newFocus != null && oldFocus != null)
        {
            switch (newFocus.getId())
            {
                case R.id.rf_home2_admin:
                    return FocusBorder.OptionsFactory.get(1.1f, 1.1f, 0);
                case R.id.rf_home2_media:
                    return FocusBorder.OptionsFactory.get(1.1f, 1.1f, 0);
                case R.id.rf_home2_sys:
                    return FocusBorder.OptionsFactory.get(1.1f, 1.1f, 0);
                default:
                    break;
            }
            mFocusBorder.setVisible(false);
        }
        return null;
    }

    @Override
    protected void onPause()
    {
        unregisterReceiver(mNetworkStatusReceiver);
        super.onPause();
    }


}

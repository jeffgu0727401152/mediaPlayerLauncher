package com.whiteskycn.tv.projectorlauncher.settings;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.whitesky.sdk.widget.BorderView;
import com.whitesky.sdk.widget.RoundedFrameLayout;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.settings.common.SkinSettingManager;


/**
 * Created by xiaoxuan on 2017/7/27.
 */
public class BackgroundActivity extends Activity implements View.OnClickListener
{
    private SkinSettingManager mSettingManager;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_background);
        // 初始化皮肤
        mSettingManager = new SkinSettingManager(this);
        mSettingManager.initSkins();
        BorderView border = new BorderView(this);
        border.setBackgroundResource(R.drawable.border_white2);
        ViewGroup list = (ViewGroup)findViewById(R.id.rl_settings_background_list);
        border.attachTo(list);
        RoundedFrameLayout background0 = (RoundedFrameLayout)findViewById(R.id.rf_settings_background_0);
        background0.setOnClickListener(this);
        RoundedFrameLayout background1 = (RoundedFrameLayout)findViewById(R.id.rf_settings_background_1);
        background1.setOnClickListener(this);
        RoundedFrameLayout background2 = (RoundedFrameLayout)findViewById(R.id.rf_settings_background_2);
        background2.setOnClickListener(this);
        RoundedFrameLayout background3 = (RoundedFrameLayout)findViewById(R.id.rf_settings_background_3);
        background3.setOnClickListener(this);
        RoundedFrameLayout background4 = (RoundedFrameLayout)findViewById(R.id.rf_settings_background_4);
        background4.setOnClickListener(this);
        RoundedFrameLayout background5 = (RoundedFrameLayout)findViewById(R.id.rf_settings_background_5);
        background5.setOnClickListener(this);
        RoundedFrameLayout background6 = (RoundedFrameLayout)findViewById(R.id.rf_settings_background_6);
        background6.setOnClickListener(this);
        RoundedFrameLayout background7 = (RoundedFrameLayout)findViewById(R.id.rf_settings_background_7);
        background7.setOnClickListener(this);
    }
    
    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.rf_settings_background_0:
                mSettingManager.toggleSkins(0);
                break;
            case R.id.rf_settings_background_1:
                mSettingManager.toggleSkins(1);
                break;
            case R.id.rf_settings_background_2:
                mSettingManager.toggleSkins(2);
                break;
            case R.id.rf_settings_background_3:
                mSettingManager.toggleSkins(3);
                break;
            case R.id.rf_settings_background_4:
                mSettingManager.toggleSkins(4);
                break;
            case R.id.rf_settings_background_5:
                mSettingManager.toggleSkins(5);
                break;
            case R.id.rf_settings_background_6:
                mSettingManager.toggleSkins(6);
                break;
            case R.id.rf_settings_background_7:
                mSettingManager.toggleSkins(7);
                break;
            default:
                break;
        }
    }
    
    @Override
    protected void onPause()
    {
        if (mSettingManager != null)
        {
            mSettingManager = null;
        }
        super.onPause();
    }
}

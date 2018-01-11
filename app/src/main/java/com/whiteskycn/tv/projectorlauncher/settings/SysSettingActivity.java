package com.whiteskycn.tv.projectorlauncher.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.whitesky.sdk.widget.BorderView;
import com.whitesky.sdk.widget.RoundedFrameLayout;
import com.whitesky.sdk.widget.focus.FocusBorder;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.settings.common.SkinSettingManager;


/**
 * Created by xiaoxuan on 2017/7/26.
 */

public class SysSettingActivity extends Activity implements View.OnClickListener, FocusBorder.OnFocusCallback
{
    private FocusBorder focusBorder;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sys);
        initBorder(true);
        BorderView border = new BorderView(this);
        border.setBackgroundResource(R.drawable.border_white2);
        ViewGroup list = (ViewGroup)findViewById(R.id.rl_settings_list);
        border.attachTo(list);
        RoundedFrameLayout backgroundPage = (RoundedFrameLayout)findViewById(R.id.rf_sys_bg);
        backgroundPage.setOnClickListener(this);
        RoundedFrameLayout netPage = (RoundedFrameLayout)findViewById(R.id.rf_sys_net);
        netPage.setOnClickListener(this);
        RoundedFrameLayout seoPage = (RoundedFrameLayout)findViewById(R.id.rf_sys_seo);
        seoPage.setOnClickListener(this);
        RoundedFrameLayout dispalyPage = (RoundedFrameLayout)findViewById(R.id.rf_sys_display);
        dispalyPage.setOnClickListener(this);
        RoundedFrameLayout soundPage = (RoundedFrameLayout)findViewById(R.id.rf_sys_sound);
        soundPage.setOnClickListener(this);
        RoundedFrameLayout languagePage = (RoundedFrameLayout)findViewById(R.id.rf_sys_language);
        languagePage.setOnClickListener(this);
        RoundedFrameLayout speedPage = (RoundedFrameLayout)findViewById(R.id.rf_sys_speed);
        speedPage.setOnClickListener(this);
        RoundedFrameLayout updatePage = (RoundedFrameLayout)findViewById(R.id.rf_sys_update);
        updatePage.setOnClickListener(this);
        focusBorder.boundGlobalFocusListener(this);
    }
    
    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.rf_sys_bg:
                Intent intentBackground = new Intent(getApplicationContext(), BackgroundActivity.class);
                startActivity(intentBackground);
                SysSettingActivity.this.finish();
                break;
            case R.id.rf_sys_net:
                Intent intentWifiSettings = new Intent("android.settings.WIFI_SETTINGS");
                //Intent intentWifiSettings = new Intent("android.settings.ETHERNET_SETTINGS");
                startActivity(intentWifiSettings);
                SysSettingActivity.this.finish();
                break;
            case R.id.rf_sys_seo:
                Intent intentSeo = new Intent(getApplicationContext(), QuickenActivity.class);
                startActivity(intentSeo);
                SysSettingActivity.this.finish();
                break;
            case R.id.rf_sys_speed:
                Intent intentSpeed = new Intent(getApplicationContext(), SpeedTestActivity.class);
                startActivity(intentSpeed);
                SysSettingActivity.this.finish();
                break;
            case R.id.rf_sys_update:
                Intent intentUpdate = new Intent(getApplicationContext(), OTAActivity.class);
                startActivity(intentUpdate);
                SysSettingActivity.this.finish();
                break;
            case R.id.rf_sys_display:
                startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
                SysSettingActivity.this.finish();
                break;
            case R.id.rf_sys_sound:
                startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS));
                SysSettingActivity.this.finish();
                break;
            case R.id.rf_sys_language:
                startActivity(new Intent(Settings.ACTION_LOCALE_SETTINGS));
                SysSettingActivity.this.finish();
                break;
            default:
                break;
        }
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        LinearLayout layout = (LinearLayout)findViewById(R.id.ll_skin);
        SkinSettingManager mSettingManager = new SkinSettingManager(this, layout);
        mSettingManager.initSkins();
    }
    
    private void initBorder(boolean isColorful)
    {
        focusBorder = new FocusBorder.Builder().asColor().shadowWidth(TypedValue.COMPLEX_UNIT_DIP, 18f) // 阴影宽度(方式二)
            .borderColor(getResources().getColor(R.color.white))
            // 边框颜色
            .build(this);
    }
    
    @Override
    public FocusBorder.Options onFocus(View oldFocus, View newFocus)
    {
        if (newFocus != null && oldFocus != null)
        {
            switch (newFocus.getId())
            {
                case R.id.rf_sys_bg:
                    return FocusBorder.OptionsFactory.get(1.1f, 1.1f, 0);
                case R.id.rf_sys_net:
                    return FocusBorder.OptionsFactory.get(1.1f, 1.1f, 0);
                case R.id.rf_sys_seo:
                    return FocusBorder.OptionsFactory.get(1.1f, 1.1f, 0);
                case R.id.rf_sys_display:
                    return FocusBorder.OptionsFactory.get(1.1f, 1.1f, 0);
                case R.id.rf_sys_sound:
                    return FocusBorder.OptionsFactory.get(1.1f, 1.1f, 0);
                case R.id.rf_sys_language:
                    return FocusBorder.OptionsFactory.get(1.1f, 1.1f, 0);
                case R.id.rf_sys_speed:
                    return FocusBorder.OptionsFactory.get(1.1f, 1.1f, 0);
                case R.id.rf_sys_update:
                    return FocusBorder.OptionsFactory.get(1.1f, 1.1f, 0);
                default:
                    break;
            }
            focusBorder.setVisible(false);
        }
        return null;
    }
}

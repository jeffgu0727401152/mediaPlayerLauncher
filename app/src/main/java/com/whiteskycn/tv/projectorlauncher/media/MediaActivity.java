package com.whiteskycn.tv.projectorlauncher.media;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.whitesky.sdk.widget.BorderView;
import com.whitesky.sdk.widget.RoundedFrameLayout;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.admin.AccountActivity;
import com.whiteskycn.tv.projectorlauncher.admin.DeviceInfoActivity;
import com.whiteskycn.tv.projectorlauncher.settings.common.SkinSettingManager;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class MediaActivity extends Activity implements View.OnClickListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        BorderView border = new BorderView(this);
        border.setBackgroundResource(R.drawable.border_white2);
        ViewGroup list = (ViewGroup)findViewById(R.id.rl_admin_list);
        border.attachTo(list);
        ImageView mImgAccount = (ImageView)findViewById(R.id.iv_admin_1);
        mImgAccount.requestFocus();
        RoundedFrameLayout mAccount = (RoundedFrameLayout)findViewById(R.id.rf_admin_account);
        RoundedFrameLayout mInfo = (RoundedFrameLayout)findViewById(R.id.rf_admin_device);
        mAccount.setOnClickListener(this);
        mInfo.setOnClickListener(this);
    }
    
    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.rf_admin_account:
                Intent accountIntent = new Intent(getApplicationContext(), AccountActivity.class);
                startActivity(accountIntent);
                MediaActivity.this.finish();
                break;
            case R.id.rf_admin_device:
                Intent deviceIntent = new Intent(getApplicationContext(), DeviceInfoActivity.class);
                startActivity(deviceIntent);
                MediaActivity.this.finish();
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
}

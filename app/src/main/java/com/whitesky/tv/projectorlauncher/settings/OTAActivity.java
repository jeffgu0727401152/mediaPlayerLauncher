package com.whitesky.tv.projectorlauncher.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.whitesky.sdk.widget.BorderView;
import com.whitesky.sdk.widget.RoundedFrameLayout;
import com.whitesky.sdk.widget.focus.FocusBorder;
import com.whitesky.tv.projectorlauncher.R;


/**
 * Created by xiaoxuan on 2017/10/13.
 */
public class OTAActivity extends Activity implements View.OnClickListener ,  View.OnHoverListener, FocusBorder.OnFocusCallback
{
    private FocusBorder focusBorder;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_ota);
        initBorder();
        BorderView border = new BorderView(this);
        border.setBackgroundResource(R.drawable.border_white2);
        ViewGroup list = (ViewGroup)findViewById(R.id.rl_settings_ota_list);
        border.attachTo(list);
        RoundedFrameLayout localPage = (RoundedFrameLayout)findViewById(R.id.rf_settings_ota_local);
        localPage.setOnClickListener(this);
        localPage.setOnHoverListener(this);
        RoundedFrameLayout netPage = (RoundedFrameLayout)findViewById(R.id.rf_settings_ota_net);
        netPage.setOnClickListener(this);
        netPage.setOnHoverListener(this);

        focusBorder.boundGlobalFocusListener(this);
    }
    
    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.rf_settings_ota_local:
                Intent local = new Intent(getApplicationContext(), OTALocalActivity.class);
                startActivity(local);
                break;
            case R.id.rf_settings_ota_net:
                //Intent net = new Intent(getApplicationContext(), OTANetActivity.class);
                //startActivity(net);
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
        layout.setBackgroundResource(R.drawable.shape_background);
    }
    
    private void initBorder()
    {
        focusBorder = new FocusBorder.Builder().asColor().shadowWidth(TypedValue.COMPLEX_UNIT_DIP, 18f) // 阴影宽度(方式二)
            .borderColor(getResources().getColor(R.color.white))
            // 边框颜色
            .build(this);
    }
    
    @Override
    protected void onDestroy()
    {
        if (focusBorder != null)
        {
            focusBorder = null;
        }
        super.onDestroy();
    }

    @Override
    public FocusBorder.Options onFocus(View oldFocus, View newFocus)
    {
        if (newFocus != null && oldFocus != null)
        {
            switch (newFocus.getId())
            {
                case R.id.rf_settings_ota_local:
                    return FocusBorder.OptionsFactory.get(1.1f, 1.1f, 0);
                case R.id.rf_settings_ota_net:
                    return FocusBorder.OptionsFactory.get(1.1f, 1.1f, 0);
                default:
                    break;
            }
            focusBorder.setVisible(false);
        }
        return null;
    }

    @Override
    public boolean onHover(View v, MotionEvent event) {
        int what = event.getAction();
        switch(what){
            case MotionEvent.ACTION_HOVER_ENTER: //鼠标进入view
                v.setFocusableInTouchMode(true);
                v.requestFocusFromTouch();
                v.bringToFront();
                break;
            case MotionEvent.ACTION_HOVER_EXIT: //鼠标离开view
                //v.setFocusable(false);
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(getApplicationContext(), SysSettingActivity.class);
        startActivity(intent);
        OTAActivity.this.finish();
        super.onBackPressed();
    }

}

package com.whiteskycn.tv.projectorlauncher.admin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.whitesky.sdk.widget.BorderView;
import com.whitesky.sdk.widget.RoundedFrameLayout;
import com.whitesky.sdk.widget.focus.FocusBorder;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.home.HomeActivity;
import com.whiteskycn.tv.projectorlauncher.settings.common.SkinSettingManager;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class AdminActivity extends Activity implements View.OnClickListener , View.OnHoverListener, FocusBorder.OnFocusCallback
{
    //遥控光标框
    private FocusBorder mFocusBorder;

    public final int SCROLLING_MARQUEE_SPEED = 2;
    public final int SCROLLING_MARQUEE_TIMES = 1314;
    private boolean focusableInTouchMode;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        initBorder();
        setContentView(R.layout.activity_admin);
        BorderView border = new BorderView(this);
        border.setBackgroundResource(R.drawable.border_white2);
        ViewGroup list = (ViewGroup)findViewById(R.id.rl_admin_list);
        border.attachTo(list);
        mFocusBorder.boundGlobalFocusListener(this);

        ImageView mImgAccount = (ImageView)findViewById(R.id.iv_admin_1);
        mImgAccount.requestFocus();
        final RoundedFrameLayout mAccount = (RoundedFrameLayout)findViewById(R.id.rf_admin_account);
        final RoundedFrameLayout mInfo = (RoundedFrameLayout)findViewById(R.id.rf_admin_device);
        mAccount.setOnClickListener(this);
        mAccount.setOnHoverListener(this);
        mInfo.setOnClickListener(this);
        mInfo.setOnHoverListener(this);
    }

    private void initBorder()
    {
        mFocusBorder = new FocusBorder.Builder().asColor().shadowWidth(TypedValue.COMPLEX_UNIT_DIP, 18f) // 阴影宽度(方式二)
                .borderColor(getResources().getColor(R.color.white))
                // 边框颜色
                .build(this);
    }
    
    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.rf_admin_account:
                Intent accountIntent = new Intent(getApplicationContext(), AccountActivity.class);
                startActivity(accountIntent);
                AdminActivity.this.finish();
                break;
            case R.id.rf_admin_device:
                Intent deviceIntent = new Intent(getApplicationContext(), DeviceInfoActivity.class);
                startActivity(deviceIntent);
                AdminActivity.this.finish();
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
        layout.setBackgroundResource(R.drawable.img_background);
    }

    @Override
    public void onBackPressed()
    {
        Intent deviceIntent = new Intent(getApplicationContext(), HomeActivity.class);
        startActivity(deviceIntent);
        AdminActivity.this.finish();
        super.onBackPressed();
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
    public FocusBorder.Options onFocus(View oldFocus, View newFocus) {
        if (newFocus != null && oldFocus != null)
        {
            switch (newFocus.getId())
            {
                case R.id.rf_admin_account:
                    return FocusBorder.OptionsFactory.get(1.05f, 1.05f, 0);
                case R.id.rf_admin_device:
                    return FocusBorder.OptionsFactory.get(1.05f, 1.05f, 0);
                default:
                    break;
            }
            mFocusBorder.setVisible(false);
        }
        return null;
    }
}

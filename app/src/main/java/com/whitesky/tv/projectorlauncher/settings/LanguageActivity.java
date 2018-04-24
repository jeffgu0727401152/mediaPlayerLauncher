package com.whitesky.tv.projectorlauncher.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.whitesky.sdk.widget.WaveLoadingView;
import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.settings.bean.TaskInfoBean;
import com.whitesky.tv.projectorlauncher.settings.model.TaskInfoProvider;
import com.whitesky.tv.projectorlauncher.utils.ToastUtil;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

/**
 * Created by xiaoxuan on 2017/10/13.
 */
public class LanguageActivity extends Activity implements RadioGroup.OnCheckedChangeListener
{
    private RadioButton mRbZH, mRbEN;
    private boolean ignoreChange = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        RadioGroup mRadioGroup = (RadioGroup)findViewById(R.id.rg_setup_language);
        mRbZH = (RadioButton)findViewById(R.id.rb_setup_language_zh);
        mRbEN = (RadioButton)findViewById(R.id.rb_setup_language_en);
        Drawable mDrawableZH = getResources().getDrawable(R.drawable.selector_radiobutton);
        mDrawableZH.setBounds(0, 0, 55, 55);
        mRbZH.setCompoundDrawables(null, null, mDrawableZH, null);
        Drawable mDrawableEN = getResources().getDrawable(R.drawable.selector_radiobutton);
        mDrawableEN.setBounds(0, 0, 55, 55);
        mRbEN.setCompoundDrawables(null, null, mDrawableEN, null);
        mRbZH.requestFocus();
        mRadioGroup.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onResume()
    {
        String locale = Locale.getDefault().getLanguage();
        ignoreChange = true;
        if (locale.contains("zh")) {
            mRbZH.setChecked(true);
            mRbZH.requestFocus();
        } else {
            mRbEN.setChecked(true);
            mRbEN.requestFocus();
        }
        ignoreChange = false;

        super.onResume();
    }
    
    @Override
    public void finish()
    {
        super.finish();
        overridePendingTransition(R.anim.activity_up_in, R.anim.activity_up_out);
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId)
    {
        if(ignoreChange) {
            return;  //加这一条，否则当我setChecked()时会触发此listener
        }

        if (mRbZH.getId() == checkedId) {
            mRbZH.requestFocus();
            updateLocale(Locale.CHINA);
        } else if (mRbEN.getId() == checkedId) {
            mRbEN.requestFocus();
            updateLocale(Locale.US);
        }

        finish();
    }

    public void updateLocale(Locale locale) {
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            Configuration config = am.getConfiguration();

            config.locale = locale;

            am.updateConfiguration(config);
            BackupManager.dataChanged("com.android.providers.settings");
        } catch (RemoteException e) {
        }
    }
}

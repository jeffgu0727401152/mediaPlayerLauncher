package com.whiteskycn.tv.projectorlauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.orhanobut.logger.Logger;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class SharedPreferencesUtil
{
    private Context mContext;
    
    private SharedPreferences.Editor mEditor;
    
    private SharedPreferences mPreferences;
    
    private String mFileName = "";
    
    private int mMode = 0;
    
    private final String TAG = this.getClass().getSimpleName();
    
    /*
     * public SharedPreferencesUtil(Context context) { this.mContext = context; this.mPreferences =
     * context.getSharedPreferences(MoKeyApplication.SHARE_EASY_TOUCH, Context.MODE_PRIVATE); this.mEditor =
     * this.mPreferences.edit(); mFileName = MoKeyApplication.SHARE_EASY_TOUCH; mMode = Context.MODE_PRIVATE;
     * Log.d(TAG," create SharedPreferencesUtil; name : " + mFileName + "; mode : " + mMode); }
     */
    
    public SharedPreferencesUtil(Context context, String fileName)
    {
        this.mContext = context;
        this.mPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        this.mEditor = this.mPreferences.edit();
        mFileName = fileName;
        mMode = Context.MODE_PRIVATE;
        Logger.d(" create SharedPreferencesUtil; name : " + mFileName + "; mode : " + mMode);
    }
    
    public SharedPreferencesUtil(Context context, String fileName, int mode)
    {
        this.mContext = context;
        this.mPreferences = context.getSharedPreferences(fileName, mode);
        this.mEditor = this.mPreferences.edit();
        mFileName = fileName;
        mMode = mode;
        Logger.d(" create SharedPreferencesUtil; name : " + mFileName + "; mode : " + mMode);
    }
    
    // 读写配置文件
    public boolean putString(String name, String value)
    {
        mEditor.putString(name, value);
        boolean result = mEditor.commit();

        Logger.d(" put key : " + name + ", value : " + value + " to file : " + mFileName + " result: " + result);
        return result;
    }
    
    public boolean putLong(String name, Long value)
    {
        mEditor.putLong(name, value);
        boolean result = mEditor.commit();

        Logger.d(" put key : " + name + ", value : " + value + " to file : " + mFileName + " result: " + result);
        return result;
    }
    
    public boolean putInt(String name, int value)
    {
        mEditor.putInt(name, value);
        boolean result = mEditor.commit();

        Logger.d(" put key : " + name + ", value : " + value + " to file : " + mFileName + " result: " + result);
        return result;
    }
    
    public boolean putBoolean(String name, Boolean value)
    {
        mEditor.putBoolean(name, value);
        boolean result = mEditor.commit();

        Logger.d(" put key : " + name + ", value : " + value + " to file : " + mFileName + " result: " + result);
        return result;
    }
    
    public boolean remove(String name)
    {
        mEditor.remove(name);
        boolean result = mEditor.commit();

        Logger.d(" remove key : " + name + " from file : " + mFileName + " result: " + result);
        return result;
    }
    
    public boolean clear()
    {
        mEditor.clear();
        boolean result = mEditor.commit();

        Logger.d(" clear file : " + mFileName + " result: " + result);
        return result;
    }
    
    public long getLong(String key)
    {
        return mPreferences.getLong(key, 0);
    }
    
    public int getInt(String key)
    {
        return mPreferences.getInt(key, 0);
    }
    
    public Boolean getBoolean(String key)
    {
        return mPreferences.getBoolean(key, false);
    }
    
    public String getString(String key)
    {
        return mPreferences.getString(key, "");
    }
    
    public long getLong(String key, long defValue)
    {
        return mPreferences.getLong(key, defValue);
    }
    
    public int getInt(String key, int defValue)
    {
        return mPreferences.getInt(key, defValue);
    }
    
    public Boolean getBoolean(String key, boolean defValue)
    {
        return mPreferences.getBoolean(key, defValue);
    }
    
    public String getString(String key, String defValue)
    {
        return mPreferences.getString(key, defValue);
    }
    
    public SharedPreferences.Editor getEditor()
    {
        return mEditor;
    }
}

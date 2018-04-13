package com.whitesky.tv.projectorlauncher.utils;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by mac on 17-6-2. Toast统一工具类
 * 
 * @author xiaoxuan
 */
public class ToastUtil
{
    
    protected static Toast toast = null;
    
    private static String oldMsg;
    
    private static long oneTime = 0;
    
    private static long twoTime = 0;
    
    private ToastUtil()
    {
        throw new UnsupportedOperationException("cannot be instantiated");
    }
    
    /**
     * @param context context
     * @param msg 提示信息
     */
    public static void showToast(Context context, String msg)
    {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()){

            Log.e("~~~~~~~~~~~~","call showToast not in UI thread");
            Exception here = new Exception();
            here.printStackTrace();
            return;
        }

        if (toast == null)
        {
            toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
            toast.show();
            oneTime = System.currentTimeMillis();
        }
        else
        {
            twoTime = System.currentTimeMillis();
            if (msg.equals(oldMsg))
            {
                if (twoTime - oneTime > Toast.LENGTH_SHORT)
                {
                    toast.show();
                }
            }
            else
            {
                oldMsg = msg;
                toast.setText(msg);
                toast.show();
            }
        }
        oneTime = twoTime;
    }
    
    /**
     * @param context context
     * @param resId 提示信息的资源id
     */
    public static void showToast(Context context, int resId)
    {
        showToast(context, context.getString(resId));
    }
}
package com.whitesky.sdk.widget.component;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MyButton
{
    private LinearLayout mLayout;
    
    private TextView mTextViewName;
    
    public MyButton(Activity activity, int resourceId)
    {
        mLayout = (LinearLayout)activity.findViewById(resourceId);
        mTextViewName = (TextView)mLayout.getChildAt(0);
        setEnable(true);
        setOnClickListener();
    }
    
    public MyButton(Dialog dialog, int resourceId)
    {
        mLayout = (LinearLayout)dialog.findViewById(resourceId);
        mTextViewName = (TextView)mLayout.getChildAt(0);
        setEnable(true);
        setOnClickListener();
    }
    
    public void setOnClickListener(OnClickListener clickListener)
    {
        mLayout.setOnClickListener(clickListener);
    }
    
    public void setFocused()
    {
        mLayout.setFocusable(true);
        mLayout.setFocusableInTouchMode(true);
        mLayout.requestFocus();
    }
    
    public void setVisibility(int nVisible)
    {
        switch (nVisible)
        {
            case View.VISIBLE:
            case View.INVISIBLE:
            case View.GONE:
                mLayout.setVisibility(nVisible);
                break;
            default:
                break;
        }
    }
    
    public void setEnable(boolean bEnable)
    {
        if (bEnable)
        {
            mTextViewName.setTextColor(Color.WHITE);
        }
        else
        {
            mTextViewName.setTextColor(Color.GRAY);
        }
        mLayout.setEnabled(bEnable);
        mLayout.setFocusable(bEnable);
        mLayout.setFocusableInTouchMode(bEnable);
    }
    
    public void setFocusable(boolean bFocusable)
    {
        mLayout.setFocusable(bFocusable);
        mLayout.setFocusableInTouchMode(bFocusable);
    }
    
    public void setTextInChild(int idx, String str)
    {
        TextView textView = (TextView)mLayout.getChildAt(idx);
        textView.setText(str);
    }
    
    public void doUpdate()
    {
    }
    
    public void setOnFocusChangeListener(OnFocusChangeListener onFocusChangeListener)
    {
        mLayout.setOnFocusChangeListener(onFocusChangeListener);
    }
    
    private void setOnClickListener()
    {
        mLayout.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                doUpdate();
            }
        });
    }
}

package com.whitesky.sdk.widget.component;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;

public class MultiSelector extends AlertDialog.Builder
{
    private boolean[] mSelected = null;
    
    private OnMultiChoiceClickListener mMultiChoiceClickLisener = null;
    
    private OnClickListener mSingleChoiceClickLisener = null;
    
    public MultiSelector(Context context)
    {
        super(context);
    }
    
    public MultiSelector(Context context, int theme)
    {
        super(context, theme);
    }
    
    @Override
    public AlertDialog.Builder setMultiChoiceItems(CharSequence[] items, boolean[] checkedItems,
        OnMultiChoiceClickListener listener)
    {
        mSelected = new boolean[checkedItems.length];
        System.arraycopy(checkedItems, 0, mSelected, 0, checkedItems.length);
        return super.setMultiChoiceItems(items, checkedItems, listener);
    }
    
    public AlertDialog.Builder setMultiChoiceItems(CharSequence[] items, boolean[] checkedItems)
    {
        mSelected = new boolean[checkedItems.length];
        System.arraycopy(checkedItems, 0, mSelected, 0, checkedItems.length);
        
        mMultiChoiceClickLisener = new OnMultiChoiceClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked)
            {
                mSelected[which] = isChecked;
            }
        };
        return super.setMultiChoiceItems(items, checkedItems, mMultiChoiceClickLisener);
    }
    
    public AlertDialog.Builder setSingleChoiceItems(CharSequence[] items, int checkedItem)
    {
        // initial result selected array
        mSelected = new boolean[items.length];
        clearSelectedArray();
        mSelected[checkedItem] = true;
        
        mSingleChoiceClickLisener = new OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                if (true == mSelected[which])
                {
                    mSelected[which] = false;
                }
                else
                {
                    clearSelectedArray();
                    mSelected[which] = true;
                }
            }
        };
        return super.setSingleChoiceItems(items, checkedItem, mSingleChoiceClickLisener);
    }
    
    public boolean[] getResults()
    {
        return mSelected;
    }
    
    public boolean getResult(int idx)
    {
        if (null != mSelected && idx >= 0 && idx < mSelected.length)
        {
            return mSelected[idx];
        }
        return false;
    }
    
    private boolean clearSelectedArray()
    {
        if (null != mSelected)
        {
            for (int i = 0; i < mSelected.length; i++)
            {
                mSelected[i] = false;
            }
            return true;
        }
        else
        {
            return false;
        }
    }
}

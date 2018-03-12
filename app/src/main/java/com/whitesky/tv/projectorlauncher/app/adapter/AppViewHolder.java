package com.whitesky.tv.projectorlauncher.app.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.whitesky.tv.projectorlauncher.common.adapter.BaseRecyclerViewHolder;


/**
 * Created by xiaoxuan on 2017/6/27.
 */

public class AppViewHolder extends BaseRecyclerViewHolder
{
    private View mView;

    protected LinearLayout appLayout;

    protected ImageView appIcon;

    protected TextView appName;

    public AppViewHolder(View itemView)
    {
        super(itemView);
        mView = itemView;
    }
    
    @Override
    protected View getView()
    {
        return mView;
    }
}

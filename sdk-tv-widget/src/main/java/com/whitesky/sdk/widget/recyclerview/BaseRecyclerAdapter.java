package com.whitesky.sdk.widget.recyclerview;

import android.support.v7.widget.RecyclerView;

public abstract class BaseRecyclerAdapter extends RecyclerView.Adapter
{
    protected OnRecyclerItemClick onRecyclerItemClick;
    
    public void setOnRecyclerItemClick(OnRecyclerItemClick onRecyclerItemClick)
    {
        this.onRecyclerItemClick = onRecyclerItemClick;
    }
    
}

package com.whitesky.sdk.widget;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class SpaceItemDecoration extends RecyclerView.ItemDecoration
{
    private int space;
    
    public SpaceItemDecoration(int space)
    {
        this.space = space;
    }
    
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state)
    {
        
        outRect.left = space + 4;
        outRect.right = space + 4;
        outRect.bottom = space;
        
        // Add top margin only for the first item to avoid double space between items
        // if(parent.getChildAdapterPosition(view) == 0)
        outRect.top = space;
    }
}
package com.whitesky.sdk.widget;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class SpacesItemDecoration extends RecyclerView.ItemDecoration
{
    /** 间距 **/
    private int space;
    
    /** 列数 **/
    private int column = 1;
    
    public SpacesItemDecoration(int space, int column)
    {
        this.space = space;
        this.column = column;
    }
    
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state)
    {
        outRect.left = space;
        outRect.bottom = space;
        outRect.top = space;
        outRect.right = space;
        /*
         * int position = parent.getChildPosition(view); if(position%column == 0){ //最左边的item 左间距为0 outRect.left = 0;
         * outRect.bottom = space*3; outRect.top = space; outRect.right = space; }else if(position%column ==
         * (column-1)){ //最右边的item 右间距为0 outRect.right = 0; outRect.bottom = space*3; outRect.top = space; outRect.left
         * = space; }else{ outRect.right = space; outRect.bottom = space*3; outRect.top = space; outRect.left = space; }
         */
    }
}

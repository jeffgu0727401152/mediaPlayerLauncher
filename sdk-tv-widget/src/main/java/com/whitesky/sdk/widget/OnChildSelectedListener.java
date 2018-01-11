package com.whitesky.sdk.widget;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public interface OnChildSelectedListener
{
    public void onChildSelected(RecyclerView parent, View view, int position, int dy);
}

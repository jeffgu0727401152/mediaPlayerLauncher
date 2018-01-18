package com.whiteskycn.tv.projectorlauncher.media.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.whitesky.sdk.widget.ViewHolder;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.common.adapter.CommonAdapter;
import com.whiteskycn.tv.projectorlauncher.media.bean.PlayListBean;


import java.util.Collections;
import java.util.List;

import static android.widget.AdapterView.INVALID_POSITION;

/**
 * Created by jeff on 18-1-16.
 */

public class PlayListAdapter extends CommonAdapter<PlayListBean>
{
    public PlayListAdapter(Context context, List<PlayListBean> data)
    {
        super(context, data, R.layout.item_play_list);
    }

    @Override
    public void convert(ViewHolder holder, final int position, PlayListBean item)
    {
        holder.setText(R.id.tv_media_name, item.getTitle());
        holder.setImageResource(R.id.iv_media_ico,R.drawable.arrow);
        holder.getButton(R.id.bt_media_remove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.i("playlist remove position "+position);
                removeItem(getItem(position));
                refresh();
            }
        });

        switch (item.getState())
        {
            case 0: // 绿色
                //holder.setImageResource(R.id.img_project_state, R.mipmap.icon_user_default);
                holder.getButton(R.id.bt_media_remove).setVisibility(View.VISIBLE);
                break;
            case 1:
                holder.getButton(R.id.bt_media_remove).setVisibility(View.INVISIBLE);
                break;
        }
        // switch (item.getCollect()){
        // case 0:
        // holder.setImageResource(R.id.img_project_collect, R.mipmap.collect_normal);
        // break;
        // case 1:
        // holder.setImageResource(R.id.img_project_collect, R.mipmap.collect_selected);
        // break;
        // }
        // final int collect = item.getCollect();
        // holder.getImageView(R.id.img_project_collect).setOnClickListener(new View.OnClickListener() {
        // @Override
        // public void onClick(View v) {
        // setSelect(collect, position);
        // }
        // });
    }

    // public void setSelect(int state, int position){
    // if(state == 1){
    // this.listDatas.get(position).setCollect(0);
    // } else {
    // this.listDatas.get(position).setCollect(1);
    // }
    // refresh();
    // }
    public boolean exchange(int src, int dst) {
        if (src != INVALID_POSITION && dst != INVALID_POSITION) {
            Collections.swap(getListDatas(), src, dst);
            refresh();
            return true;
        }
        return false;
    }
}

package com.whitesky.tv.projectorlauncher.admin.adapter;

import android.content.Context;

import com.whitesky.sdk.widget.ViewHolder;
import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.common.adapter.CommonAdapter;
import com.whitesky.tv.projectorlauncher.common.bean.ListViewBean;

import java.util.List;

/**
 * Created by xiaoxuan on 2017/12/2.
 */
public class AccountAdapter extends CommonAdapter<ListViewBean>
{
    
    public AccountAdapter(Context context, List<ListViewBean> data, int layoutId)
    {
        super(context, data, layoutId);
    }
    
    @Override
    public void convert(ViewHolder holder, final int position)
    {
        holder.setText(R.id.tv_people_account, getItem(position).getTitle());
        holder.setText(R.id.tv_people_date, getItem(position).getDiscribe());
        switch (getItem(position).getState())
        {
            case 0: // 绿色
                holder.setImageResource(R.id.img_project_state, R.mipmap.icon_user_default);
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
}

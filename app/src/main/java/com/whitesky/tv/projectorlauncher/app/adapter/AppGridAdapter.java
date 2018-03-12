package com.whitesky.tv.projectorlauncher.app.adapter;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.whitesky.sdk.widget.MarqueeText;
import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.app.bean.AppBean;
import com.whitesky.tv.projectorlauncher.common.adapter.BaseRecyclerViewAdapter;
import com.whitesky.tv.projectorlauncher.common.adapter.BaseRecyclerViewHolder;

/**
 * Created by xiaoxuan on 2017/6/27.
 */

public class AppGridAdapter extends BaseRecyclerViewAdapter<AppBean> implements View.OnFocusChangeListener
{
    private Context mContext;
    
    public AppGridAdapter(Context mContext)
    {
        this.mContext = mContext;
    }
    
    @Override
    protected BaseRecyclerViewHolder createItem(ViewGroup parent, int viewType)
    {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_app_launcher, null);
        AppViewHolder viewHolder = new AppViewHolder(itemView);
        viewHolder.appIcon = (ImageView)itemView.findViewById(R.id.appIcon);
        viewHolder.appName = (TextView)itemView.findViewById(R.id.appName);
        viewHolder.appLayout = (LinearLayout)itemView.findViewById(R.id.ll_app);
        return viewHolder;
    }
    
    /** 绑定数据 */
    @Override
    protected void bindData(BaseRecyclerViewHolder holder, int position)
    {
        ((AppViewHolder)holder).appIcon.setImageDrawable(getItemData(position).getIcon());
        ((AppViewHolder)holder).appName.setText(getItemData(position).getName());
        ((AppViewHolder)holder).appLayout.setOnFocusChangeListener(this);
    }
    
    private float dipToPx(Context context, float value)
    {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics);
    }
    
    @Override
    public void onFocusChange(View view, boolean hasFocus)
    {
        // 滚动提示
        MarqueeText tvTitle = (MarqueeText)view.findViewById(R.id.appName);
        tvTitle.setSelected(hasFocus);
        if (hasFocus)
        {
            tvTitle.startScroll();
        }
        else
        {
            tvTitle.stopScroll();
        }
    }
}

package com.whiteskycn.tv.projectorlauncher.common.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.whiteskycn.tv.projectorlauncher.R;


/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class MyGridViewAdapter extends BaseAdapter
{
    private int layoutId;
    
    public MyGridViewAdapter(int resId)
    {
        layoutId = resId;
    }
    
    @Override
    public int getCount()
    {
        return 200;
    }
    
    @Override
    public Object getItem(int position)
    {
        return null;
    }
    
    @Override
    public long getItemId(int position)
    {
        return 0;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        
        ViewHolder viewHolder;
        if (convertView == null)
        {
            convertView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.text = (TextView)convertView.findViewById(R.id.textView);
            convertView.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolder)convertView.getTag();
        }
        
        viewHolder.text.setText("text" + position);
        return convertView;
    }
    
    private class ViewHolder
    {
        public TextView text;
    }
}

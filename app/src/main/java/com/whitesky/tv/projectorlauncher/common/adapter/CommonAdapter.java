package com.whitesky.tv.projectorlauncher.common.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.whitesky.sdk.widget.ViewHolder;

import java.util.List;

/**
 * Created by xiaoxuan on 2017/6/2.
 */
public abstract class CommonAdapter<T> extends BaseAdapter
{
    
    protected Context mContext;
    
    protected List<T> listDatas = null;

    protected static final Object listLock = new Object();
    
    protected int mLayoutId;
    
    public CommonAdapter(Context context, List<T> data, int layoutId)
    {
        this.mContext = context;
        this.listDatas = data;
        this.mLayoutId = layoutId;
    }
    
    @Override
    public int getCount()
    {
        synchronized (listLock) {
            return listDatas.size();
        }
    }
    
    @Override
    public T getItem(int position)
    {
        synchronized (listLock) {
            return listDatas.get(position);
        }
    }
    
    @Override
    public long getItemId(int position)
    {
        return position;
    }
    
    /**
     * 添加单条数据项
     * 
     * @param item
     */
    public void addItem(T item)
    {
        synchronized (listLock) {
            this.listDatas.add(item);
        }
    }

    /**
     * 添加单条数据项
     *
     * @param item
     */
    public void removeItem(T item)
    {
        synchronized (listLock) {
            this.listDatas.remove(item);
        }
    }

    /**
     * 设置数据源
     * 
     * @param data
     */
    public void setListDatas(List<T> data)
    {
        synchronized (listLock) {
            this.listDatas = data;
        }
    }

    /**
     * 取出数据源
     *
     */
    public List<T> getListDatas()
    {
        synchronized (listLock) {
            return this.listDatas;
        }
    }

    /**
     * 清除数据源
     */
    public void clear()
    {
        synchronized (listLock) {
            this.listDatas.clear();
        }
    }
    
    /**
     * 刷新数据源
     */
    public void refresh()
    {
        this.notifyDataSetChanged();
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder = ViewHolder.get(mContext, convertView, parent, mLayoutId);

        synchronized (listLock) {
            convert(holder, position);
        }

        return holder.getConvertView();
    }
    
    /**
     * 在子类中实现该方法
     * 
     * @param holder 列表项
     * @param position 当前刷新的位置
     */
    public abstract void convert(ViewHolder holder, int position);
}

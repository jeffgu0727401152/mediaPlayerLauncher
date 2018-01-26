package com.whiteskycn.tv.projectorlauncher.media.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.whiteskycn.tv.projectorlauncher.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeff on 18-1-24.
 */

public class UsbPartitionAdapter extends ArrayAdapter<String> implements SpinnerAdapter {
    List<String> datas = new ArrayList<>();
    Context mContext;

    public UsbPartitionAdapter(Context context, int textViewResourceId, List<String> objects) {
        super(context, textViewResourceId, objects);
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return datas==null?0:datas.size();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHodler holder = null;
        if (convertView == null) {
            holder = new ViewHodler();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_usb_partition_spinner, null);
            holder.mTextView = (TextView) convertView;
            convertView.setTag(holder);
        } else {
            holder = (ViewHodler) convertView.getTag();
        }

        holder.mTextView.setText(datas.get(position));

        return convertView;
    }

    private static class ViewHodler{
        TextView mTextView;
    }
}
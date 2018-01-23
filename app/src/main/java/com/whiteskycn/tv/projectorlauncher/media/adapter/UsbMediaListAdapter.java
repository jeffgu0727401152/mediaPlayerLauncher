package com.whiteskycn.tv.projectorlauncher.media.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.whitesky.sdk.widget.ViewHolder;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.common.adapter.CommonAdapter;
import com.whiteskycn.tv.projectorlauncher.media.bean.UsbMediaListBean;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static android.widget.AdapterView.INVALID_POSITION;

/**
 * Created by jeff on 18-1-16.
 */

public class UsbMediaListAdapter extends CommonAdapter<UsbMediaListBean>
{
    private final String TAG = this.getClass().getSimpleName();

    public UsbMediaListAdapter(Context context, List<UsbMediaListBean> data)
    {
        super(context, data, R.layout.item_usb_media_list);
    }

    @Override
    public void convert(ViewHolder holder, final int position, UsbMediaListBean item) {
        holder.setText(R.id.tv_media_name, item.getTitle());

        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(item.getMediaData().getDuration());
        holder.setText(R.id.tv_media_duration, hms);

        holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_type_video);
        holder.getButton(R.id.bt_media_copy_to_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"copy to media list" + position);
                //todo file copy
                refresh();
            }
        });
    }


    public boolean exchange(int src, int dst) {
        if (src != INVALID_POSITION && dst != INVALID_POSITION) {
            Collections.swap(getListDatas(), src, dst);
            refresh();
            return true;
        }
        return false;
    }
}

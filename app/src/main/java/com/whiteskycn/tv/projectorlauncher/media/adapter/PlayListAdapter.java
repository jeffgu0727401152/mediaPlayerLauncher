package com.whiteskycn.tv.projectorlauncher.media.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.whitesky.sdk.widget.ViewHolder;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.common.adapter.CommonAdapter;
import com.whiteskycn.tv.projectorlauncher.media.bean.PlayListBean;


import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static android.widget.AdapterView.INVALID_POSITION;

/**
 * Created by jeff on 18-1-16.
 */

public class PlayListAdapter extends CommonAdapter<PlayListBean>
{
    private final String TAG = this.getClass().getSimpleName();

    public PlayListAdapter(Context context, List<PlayListBean> data)
    {
        super(context, data, R.layout.item_play_list);
    }

    @Override
    public void convert(ViewHolder holder, final int position, PlayListBean item) {
        holder.setText(R.id.tv_media_name, item.getTitle());

        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(item.getMediaData().getDuration());
        holder.setText(R.id.tv_media_duration, hms);

        holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_type_video);
        holder.getButton(R.id.bt_media_remove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"playlist remove position " + position);
                removeItem(getItem(position));
                refresh();
            }
        });

        holder.getButton(R.id.bt_media_remove).setVisibility(View.VISIBLE);
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

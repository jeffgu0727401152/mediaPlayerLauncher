package com.whiteskycn.tv.projectorlauncher.media.adapter;

import android.content.Context;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.whitesky.sdk.widget.ViewHolder;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.common.adapter.CommonAdapter;
import com.whiteskycn.tv.projectorlauncher.media.bean.MediaFileBean;
import com.whiteskycn.tv.projectorlauncher.media.bean.MediaListBean;

import java.util.Collections;
import java.util.List;

import static android.widget.AdapterView.INVALID_POSITION;

/**
 * Created by jeff on 18-1-16.
 */

public class AllMediaListAdapter extends CommonAdapter<MediaListBean>
{
    public AllMediaListAdapter(Context context, List<MediaListBean> data)
    {
        super(context, data, R.layout.item_all_media_list);
    }

    @Override
    public void convert(ViewHolder holder, final int position, MediaListBean item) {
        holder.setText(R.id.tv_media_name, item.getTitle());

        MediaFileBean.MediaTypeEnum type = item.getMediaData().getType();

        switch (type)
        {
            case PICTURE:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_pause);
                break;
            case VIDEO:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_type_video);
                break;
            default:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_pause);
                break;
        }

        MediaFileBean.MediaSourceEnum source = item.getMediaData().getSource();

        switch (source)
        {
            case LOCAL:
                holder.setImageResource(R.id.iv_media_source, R.drawable.img_media_source_local);
                break;
            case CLOUD_FREE:
            case CLOUD_PAY:
            case CLOUD_PRIVATE:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_source_cloud);
                break;
            default:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_pause);
                break;
        }



        holder.getButton(R.id.bt_media_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.i("playlist remove position " + position);
                removeItem(getItem(position));
                refresh();
            }
        });

        holder.getButton(R.id.bt_media_preview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.i("playlist remove position " + position);
                refresh();
            }
        });

        holder.getButton(R.id.bt_media_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.i("playlist remove position " + position);
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

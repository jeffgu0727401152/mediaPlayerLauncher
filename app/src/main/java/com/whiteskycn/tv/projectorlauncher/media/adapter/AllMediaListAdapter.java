package com.whiteskycn.tv.projectorlauncher.media.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.whitesky.sdk.widget.ViewHolder;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.common.adapter.CommonAdapter;
import com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean;
import com.whiteskycn.tv.projectorlauncher.media.bean.AllMediaListBean;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static android.widget.AdapterView.INVALID_POSITION;
import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.MEDIA_PICTURE;
import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.MEDIA_VIDEO;
import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.SOURCE_CLOUD_FREE;
import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.SOURCE_CLOUD_PAY;
import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.SOURCE_CLOUD_PRIVATE;
import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.SOURCE_CLOUD_PUBLIC;
import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.SOURCE_LOCAL;

/**
 * Created by jeff on 18-1-16.
 */

public class AllMediaListAdapter extends CommonAdapter<AllMediaListBean>
{
    private final String TAG = this.getClass().getSimpleName();

    private OnAllMediaListItemEventListener mOnALlMediaListItemEventListener;

    public AllMediaListAdapter(Context context, List<AllMediaListBean> data)
    {
        super(context, data, R.layout.item_all_media_list);
    }

    @Override
    public void convert(ViewHolder holder, final int position, AllMediaListBean item) {
        holder.setText(R.id.tv_media_name, item.getTitle());

        switch (item.getMediaData().getType())
        {
            case MEDIA_PICTURE:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_pause);
                break;
            case MEDIA_VIDEO:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_type_video);
                break;
            default:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_pause);
                break;
        }

        switch (item.getMediaData().getSource())
        {
            case SOURCE_LOCAL:
                holder.setImageResource(R.id.iv_media_source, R.drawable.img_media_source_local);
                break;
            case SOURCE_CLOUD_FREE:
            case SOURCE_CLOUD_PAY:
            case SOURCE_CLOUD_PRIVATE:
            case SOURCE_CLOUD_PUBLIC:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_source_cloud);
                break;
            default:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_pause);
                break;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(item.getMediaData().getDuration());
        holder.setText(R.id.tv_media_duration, hms);

        holder.getButton(R.id.bt_media_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"playlist remove position " + position);
                if (mOnALlMediaListItemEventListener !=null)
                {
                    mOnALlMediaListItemEventListener.doItemDelete(position);
                }
            }
        });

        holder.getButton(R.id.bt_media_preview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"playlist remove position " + position);
                //to do preview
                refresh();
            }
        });

        holder.getButton(R.id.bt_media_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"playlist remove position " + position);
                //to do download
                refresh();
            }
        });

        holder.getCheckBox(R.id.cb_media_selected).setChecked(item.isSelected());
        holder.getCheckBox(R.id.cb_media_selected).setOnClickListener(new View.OnClickListener () {
            @Override
            public void onClick(View view) {
                getListDatas().get(position).setSelected(!getListDatas().get(position).isSelected());
            }
        });
    }

    public interface OnAllMediaListItemEventListener {
        public void doItemDelete(int position);
    }

    public void setOnALlMediaListItemListener(OnAllMediaListItemEventListener onAllMediaListItemEventListener) {
        this.mOnALlMediaListItemEventListener = onAllMediaListItemEventListener;
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

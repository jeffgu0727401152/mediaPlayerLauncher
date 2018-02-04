package com.whiteskycn.tv.projectorlauncher.media.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.whitesky.sdk.widget.ViewHolder;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.common.adapter.CommonAdapter;
import com.whiteskycn.tv.projectorlauncher.media.bean.AllMediaListBean;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static android.widget.AdapterView.INVALID_POSITION;
import static com.whiteskycn.tv.projectorlauncher.media.db.MediaBean.MEDIA_MUSIC;
import static com.whiteskycn.tv.projectorlauncher.media.db.MediaBean.MEDIA_PICTURE;
import static com.whiteskycn.tv.projectorlauncher.media.db.MediaBean.MEDIA_VIDEO;
import static com.whiteskycn.tv.projectorlauncher.media.db.MediaBean.SOURCE_CLOUD_FREE;
import static com.whiteskycn.tv.projectorlauncher.media.db.MediaBean.SOURCE_CLOUD_PAY;
import static com.whiteskycn.tv.projectorlauncher.media.db.MediaBean.SOURCE_CLOUD_PRIVATE;
import static com.whiteskycn.tv.projectorlauncher.media.db.MediaBean.SOURCE_CLOUD_PUBLIC;
import static com.whiteskycn.tv.projectorlauncher.media.db.MediaBean.SOURCE_LOCAL;

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

    public boolean hasItemSelected()
    {
        for(AllMediaListBean data: getListDatas())
        {
            if (data.isSelected())
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void convert(ViewHolder holder, final int position, AllMediaListBean item) {
        // 设置标号
        holder.setText(R.id.tv_media_list_pos, String.valueOf(position + 1) + ".");

        // 设置媒体文件类型图标
        switch (item.getMediaData().getType())
        {
            case MEDIA_PICTURE:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_type_picture);
                break;
            case MEDIA_VIDEO:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_type_video);
                break;
            case MEDIA_MUSIC:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_type_music);
                break;
            default:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_type_unknown);
                break;
        }

        holder.setText(R.id.tv_media_title, item.getMediaData().getTitle());

        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(item.getMediaData().getDuration());
        holder.setText(R.id.tv_media_duration, hms);

        // 设置来源图标
        switch (item.getMediaData().getSource())
        {
            case SOURCE_LOCAL:
                holder.setImageResource(R.id.iv_media_source, R.drawable.img_media_source_local);
                break;
            case SOURCE_CLOUD_FREE:
            case SOURCE_CLOUD_PAY:
            case SOURCE_CLOUD_PRIVATE:
            case SOURCE_CLOUD_PUBLIC:
                if (item.getMediaData().isDownload()) {
                    holder.setImageResource(R.id.iv_media_source, R.drawable.img_media_source_cloud_download);
                } else {
                    holder.setImageResource(R.id.iv_media_source, R.drawable.img_media_source_cloud);
                }
                break;
            default:
                holder.setImageResource(R.id.iv_media_source, R.drawable.img_media_type_unknown);
                break;
        }

        // 决定功能按钮使能与否
        if (item.getMediaData().getSource()!=SOURCE_LOCAL && !item.getMediaData().isDownload()) {
            holder.getButton(R.id.bt_media_download).setEnabled(true);
        } else {
            holder.getButton(R.id.bt_media_download).setEnabled(false);
        }

        holder.getButton(R.id.bt_media_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"media list remove position " + position);
                if (mOnALlMediaListItemEventListener !=null)
                {
                    mOnALlMediaListItemEventListener.doItemDelete(position);
                }
            }
        });

        holder.getButton(R.id.bt_media_preview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"media list preview position " + position);
                if (mOnALlMediaListItemEventListener !=null)
                {
                    mOnALlMediaListItemEventListener.doItemPreview(position);
                }
            }
        });

        holder.getButton(R.id.bt_media_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"media list download position " + position);
                if (mOnALlMediaListItemEventListener !=null)
                {
                    mOnALlMediaListItemEventListener.doItemDownLoad(position);
                }
            }
        });

        holder.getCheckBox(R.id.cb_media_selected).setChecked(item.isSelected());
        holder.getCheckBox(R.id.cb_media_selected).setOnClickListener(new View.OnClickListener () {
            @Override
            public void onClick(View view) {
                listDatas.get(position).setSelected(!getListDatas().get(position).isSelected());
                if (mOnALlMediaListItemEventListener !=null) {
                    mOnALlMediaListItemEventListener.itemSelectedChange();
                }
            }
        });
    }

    public interface OnAllMediaListItemEventListener {
        public void doItemDelete(int position);
        public void doItemPreview(int position);
        public void doItemDownLoad(int position);
        public void itemSelectedChange();
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

package com.whitesky.tv.projectorlauncher.media.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.whitesky.sdk.widget.ViewHolder;
import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.common.adapter.CommonAdapter;
import com.whitesky.tv.projectorlauncher.media.bean.PlayListBean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;


import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static android.widget.AdapterView.INVALID_POSITION;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_MUSIC;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_PICTURE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_VIDEO;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.SOURCE_LOCAL;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_DOWNLOADED;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_NONE;

/**
 * Created by jeff on 18-1-16.
 */

public class PlayListAdapter extends CommonAdapter<PlayListBean>
{
    private final String TAG = this.getClass().getSimpleName();

    private OnPlaylistItemEventListener mOnPlaylistItemEventListener = null;

    public PlayListAdapter(Context context, List<PlayListBean> data)
    {
        super(context, data, R.layout.item_play_list);
    }

    @Override
    public void convert(ViewHolder holder, final int position, PlayListBean item) {
        holder.setText(R.id.tv_media_title, item.getMediaData().getTitle());

        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(item.getMediaData().getDuration());
        holder.setText(R.id.tv_media_duration, hms);

        switch (item.getMediaData().getType())
        {
            case MEDIA_PICTURE:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_type_picture);
                holder.getView(R.id.bt_media_time_add).setVisibility(View.VISIBLE);
                holder.getView(R.id.bt_media_time_minus).setVisibility(View.VISIBLE);
                break;
            case MEDIA_VIDEO:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_type_video);
                holder.getView(R.id.bt_media_time_add).setVisibility(View.INVISIBLE);
                holder.getView(R.id.bt_media_time_minus).setVisibility(View.INVISIBLE);
                break;
            case MEDIA_MUSIC:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_type_music);
                break;
            default:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_type_unknown);
                break;
        }

        // 歌曲播放位置指示
        holder.getImageView(R.id.iv_media_play_indicator).setVisibility(item.isPlaying() ? View.VISIBLE : View.INVISIBLE);

        //scale选择spinner
        ArrayAdapter adapter = new ArrayAdapter<String>(mContext, R.layout.item_media_play_scale_spinner, R.id.tv_media_play_scale_idx);
        adapter.add(mContext.getResources().getString(R.string.str_media_scale_fix_xy));
        adapter.add(mContext.getResources().getString(R.string.str_media_scale_fix_center));
        ((Spinner) holder.getView(R.id.sp_media_scale)).setAdapter(adapter);
        ((Spinner) holder.getView(R.id.sp_media_scale)).setSelection(item.getPlayScale());
        ((Spinner) holder.getView(R.id.sp_media_scale)).setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                getItem(position).setPlayScale(pos);
                if (mOnPlaylistItemEventListener != null) {
                    mOnPlaylistItemEventListener.onPlaylistChange();
                    mOnPlaylistItemEventListener.onScaleChange(position, pos);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        holder.getButton(R.id.bt_media_remove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"playlist remove position " + position);
                removeItem(position);
                refresh();
            }
        });

        holder.getButton(R.id.bt_media_time_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"playlist add time");
                int duration = getItem(position).getMediaData().getDuration();
                getItem(position).getMediaData().setDuration(duration + 5000);
                if (mOnPlaylistItemEventListener != null) {
                    mOnPlaylistItemEventListener.onPlaylistChange();
                }
                refresh();
            }
        });

        holder.getButton(R.id.bt_media_time_minus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"playlist minus time" + position);
                int duration = getItem(position).getMediaData().getDuration();
                if (duration > 10000) {
                    getItem(position).getMediaData().setDuration(duration - 5000);
                    if (mOnPlaylistItemEventListener != null) {
                        mOnPlaylistItemEventListener.onPlaylistChange();
                    }
                }
                refresh();
            }
        });
    }


    public boolean exchange(int src, int dst) {
        if (src != INVALID_POSITION && dst != INVALID_POSITION) {
            Collections.swap(getListDatas(), src, dst);

            if (mOnPlaylistItemEventListener != null) {
                mOnPlaylistItemEventListener.onPlaylistChange();
            }

            refresh();
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        super.clear();
        if (mOnPlaylistItemEventListener != null) {
            mOnPlaylistItemEventListener.onPlaylistChange();
        }
        refresh();
    }

    @Override
    public void addItem(PlayListBean item) {
        super.addItem(item);
        if (mOnPlaylistItemEventListener != null) {
            mOnPlaylistItemEventListener.onPlaylistChange();
        }
        refresh();
    }

    @Override
    public void removeItem(PlayListBean item) {
        super.removeItem(item);
        if (mOnPlaylistItemEventListener != null) {
            mOnPlaylistItemEventListener.onPlaylistChange();
        }
        refresh();
    }

    public void removeItem(int position) {
        super.removeItem(getItem(position));
        if (mOnPlaylistItemEventListener != null) {
            mOnPlaylistItemEventListener.onPlaylistChange();
        }
        refresh();
    }

    public boolean isAllItemsFromCloud() {
        synchronized (getListDatas()) {
            for (PlayListBean bean:getListDatas()) {
                if (bean.getMediaData().getSource()==SOURCE_LOCAL) {
                    return false;
                }
            }
            return true;
        }
    }

    public boolean hasPlayableItem() {
        synchronized (getListDatas()) {
            for (PlayListBean bean:getListDatas()) {
                if (bean.getMediaData().getDownloadState()==STATE_DOWNLOAD_DOWNLOADED) {
                    return true;
                }
            }
            return false;
        }
    }

    public synchronized void update(MediaBean data) {
        for (PlayListBean it:listDatas) {
            if (it.getMediaData().getPath().equals(data.getPath())) {
                it.setMediaData(data);
                return;
            }
        }
    }

    public interface OnPlaylistItemEventListener {
        void onPlaylistChange();
        void onScaleChange(int position, int scaleType);
    }

    public void setOnPlaylistItemEventListener(OnPlaylistItemEventListener onPlaylistItemEventListener) {
        this.mOnPlaylistItemEventListener = onPlaylistItemEventListener;
    }

}

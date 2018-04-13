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
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_DOWNLOADING;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_ERROR;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_NONE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_PAUSED;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_START;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_WAITING;

/**
 * Created by jeff on 18-1-16.
 */

public class PlayListAdapter extends CommonAdapter<PlayListBean>
{
    private final String TAG = this.getClass().getSimpleName();

    public static final int CHANGE_EVENT_ADD = 0;
    public static final int CHANGE_EVENT_REMOVE = 1;
    public static final int CHANGE_EVENT_EXCHANGE = 2;
    public static final int CHANGE_EVENT_SCALE = 3;
    public static final int CHANGE_EVENT_TIME = 4;

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
                if (getItem(position).getPlayScale() == pos) {
                    return;
                }

                getItem(position).setPlayScale(pos);
                if (mOnPlaylistItemEventListener != null) {
                    mOnPlaylistItemEventListener.onPlaylistChange(CHANGE_EVENT_SCALE,getItem(position));
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
                    mOnPlaylistItemEventListener.onPlaylistChange(CHANGE_EVENT_TIME,getItem(position));
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
                        mOnPlaylistItemEventListener.onPlaylistChange(CHANGE_EVENT_TIME,getItem(position));
                    }
                }
                refresh();
            }
        });

        if (item.getMediaData().getSource()==SOURCE_LOCAL) {
            holder.getTextView(R.id.tv_media_state).setVisibility(View.INVISIBLE);
        } else {
            if (item.getMediaData().getDownloadState() == STATE_DOWNLOAD_NONE) {
                holder.getTextView(R.id.tv_media_state).setVisibility(View.INVISIBLE);
            } else if (item.getMediaData().getDownloadState() == STATE_DOWNLOAD_WAITING) {
                holder.getTextView(R.id.tv_media_state).setVisibility(View.VISIBLE);
                holder.setText(R.id.tv_media_state, mContext.getResources().getString(R.string.str_media_download_waiting));
            } else if (item.getMediaData().getDownloadState() == STATE_DOWNLOAD_DOWNLOADING) {
                holder.getTextView(R.id.tv_media_state).setVisibility(View.VISIBLE);
                holder.setText(R.id.tv_media_state, String.valueOf(item.getMediaData().getDownloadProgress()*100/item.getMediaData().getSize()) + "%");
            } else if(item.getMediaData().getDownloadState() == STATE_DOWNLOAD_PAUSED) {
                holder.getTextView(R.id.tv_media_state).setVisibility(View.VISIBLE);
                holder.setText(R.id.tv_media_state,
                        mContext.getResources().getString(R.string.str_media_download_pause) + String.valueOf(item.getMediaData().getDownloadProgress()*100/item.getMediaData().getSize()) + "%");
            } else if (item.getMediaData().getDownloadState() == STATE_DOWNLOAD_ERROR) {
                holder.getTextView(R.id.tv_media_state).setVisibility(View.VISIBLE);
                holder.setText(R.id.tv_media_state, mContext.getResources().getString(R.string.str_media_download_error));
            } else if (item.getMediaData().getDownloadState() == STATE_DOWNLOAD_START) {
                holder.getTextView(R.id.tv_media_state).setVisibility(View.VISIBLE);
                holder.setText(R.id.tv_media_state, "...");
            } else {
                holder.getTextView(R.id.tv_media_state).setVisibility(View.INVISIBLE);
            }
        }

    }


    public boolean exchange(int src, int dst) {
        if (src != INVALID_POSITION && dst != INVALID_POSITION) {
            Collections.swap(getListDatas(), src, dst);

            if (mOnPlaylistItemEventListener != null) {
                mOnPlaylistItemEventListener.onPlaylistChange(CHANGE_EVENT_EXCHANGE,null);
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
            mOnPlaylistItemEventListener.onPlaylistChange(CHANGE_EVENT_REMOVE,null);
        }
        refresh();
    }

    @Override
    public void addItem(PlayListBean item) {
        super.addItem(item);
        if (mOnPlaylistItemEventListener != null) {
            mOnPlaylistItemEventListener.onPlaylistChange(CHANGE_EVENT_ADD,item);
        }
        refresh();
    }

    @Override
    public void removeItem(PlayListBean item) {
        super.removeItem(item);
        if (mOnPlaylistItemEventListener != null) {
            mOnPlaylistItemEventListener.onPlaylistChange(CHANGE_EVENT_REMOVE,item);
        }
        refresh();
    }

    @Override
    // 重写方法不改变内部数据对象的指向
    public void setListDatas(List<PlayListBean> items){
        listDatas.clear();
        if (items == null) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            listDatas.add(items.get(i));
        }
    }

    public void removeItem(int position) {
        PlayListBean removeBean = getItem(position);
        super.removeItem(removeBean);
        if (mOnPlaylistItemEventListener != null) {
            mOnPlaylistItemEventListener.onPlaylistChange(CHANGE_EVENT_REMOVE,removeBean);
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
        void onPlaylistChange(int event,PlayListBean bean);
        void onScaleChange(int position, int scaleType);
    }

    public void setOnPlaylistItemEventListener(OnPlaylistItemEventListener onPlaylistItemEventListener) {
        this.mOnPlaylistItemEventListener = onPlaylistItemEventListener;
    }

}

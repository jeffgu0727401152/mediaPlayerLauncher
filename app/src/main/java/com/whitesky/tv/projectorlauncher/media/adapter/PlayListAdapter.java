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
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.media.db.PlayBean;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import static com.whitesky.tv.projectorlauncher.utils.MediaScanUtil.genTimeString;

/**
 * Created by jeff on 18-1-16.
 */

public class PlayListAdapter extends CommonAdapter<PlayBean>
{
    private final String TAG = this.getClass().getSimpleName();
    static final Object listLock = new Object();

    public static final int CHANGE_EVENT_ADD = 0;
    public static final int CHANGE_EVENT_REMOVE = 1;
    public static final int CHANGE_EVENT_CLEAR = 2;
    public static final int CHANGE_EVENT_EXCHANGE = 3;
    public static final int CHANGE_EVENT_SCALE = 4;
    public static final int CHANGE_EVENT_TIME = 5;
    public static final int CHANGE_EVENT_UPDATE = 6;

    private OnPlaylistItemEventListener mOnPlaylistItemEventListener = null;

    public int getPlayIndex() {
        return playIndex;
    }

    public void setPlayIndex(int playIndex) {
        this.playIndex = playIndex;
        refresh();
    }

    // 数组中的下标, 可以数据库当ID用
    private int playIndex = 0;

    public PlayListAdapter(Context context, List<PlayBean> data)
    {
        super(context, data, R.layout.item_play_list);
    }

    @Override
    public void convert(ViewHolder holder, final int position, PlayBean item) {
        holder.setText(R.id.tv_media_title, item.getMedia().getTitle());
        holder.setText(R.id.tv_media_duration, genTimeString(item.getTime()));

        switch (item.getMedia().getType())
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
                holder.getView(R.id.bt_media_time_add).setVisibility(View.INVISIBLE);
                holder.getView(R.id.bt_media_time_minus).setVisibility(View.INVISIBLE);
                break;
            default:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_type_unknown);
                holder.getView(R.id.bt_media_time_add).setVisibility(View.INVISIBLE);
                holder.getView(R.id.bt_media_time_minus).setVisibility(View.INVISIBLE);
                break;
        }

        // 歌曲播放位置指示
        holder.getImageView(R.id.iv_media_play_indicator).setVisibility(playIndex==position ? View.VISIBLE : View.INVISIBLE);

        //scale选择spinner
        ArrayAdapter adapter = new ArrayAdapter<String>(mContext, R.layout.item_media_play_scale_spinner, R.id.tv_media_play_scale_idx);
        adapter.add(mContext.getResources().getString(R.string.str_media_scale_fix_xy));
        adapter.add(mContext.getResources().getString(R.string.str_media_scale_fix_center));
        ((Spinner) holder.getView(R.id.sp_media_scale)).setAdapter(adapter);
        ((Spinner) holder.getView(R.id.sp_media_scale)).setSelection(item.getScale());
        ((Spinner) holder.getView(R.id.sp_media_scale)).setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                PlayBean bean = null;
                synchronized (listLock) {
                    if (position>=getListDatas().size()) {
                        Log.w(TAG, "delete play list too quick!");
                        return;
                    } else {
                        bean = getItem(position);
                    }
                }


                if (bean == null || bean.getScale() == pos) {
                    return;
                }

                bean.setScale(pos);
                if (mOnPlaylistItemEventListener != null) {
                    ArrayList<PlayBean> changeList =  new ArrayList<>();
                    changeList.add(bean);
                    mOnPlaylistItemEventListener.onPlayListChanged(CHANGE_EVENT_SCALE, changeList);
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
                removeItemNotifyChange(position);
                refresh();
            }
        });

        holder.getButton(R.id.bt_media_time_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"playlist add time" + position);
                PlayBean bean = getItem(position);
                int duration = bean.getTime();
                bean.setTime(duration + 5000);
                if (mOnPlaylistItemEventListener != null) {
                    ArrayList<PlayBean> changeList =  new ArrayList<>();
                    changeList.add(bean);
                    mOnPlaylistItemEventListener.onPlayListChanged(CHANGE_EVENT_TIME, changeList);
                }
                refresh();
            }
        });

        holder.getButton(R.id.bt_media_time_minus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"playlist minus time" + position);
                PlayBean bean = getItem(position);
                int duration = bean.getTime();
                if (duration > 10000) {
                    bean.setTime(duration - 5000);
                    if (mOnPlaylistItemEventListener != null) {
                        ArrayList<PlayBean> changeList =  new ArrayList<>();
                        changeList.add(bean);
                        mOnPlaylistItemEventListener.onPlayListChanged(CHANGE_EVENT_TIME, changeList);
                    }
                }
                refresh();
            }
        });

        if (item.getMedia().getSource()==SOURCE_LOCAL) {
            holder.getTextView(R.id.tv_media_state).setVisibility(View.INVISIBLE);
        } else {
            // 下载状态显示
            if (item.getMedia().getDownloadState() == STATE_DOWNLOAD_NONE) {
                holder.getTextView(R.id.tv_media_state).setVisibility(View.INVISIBLE);
            } else if (item.getMedia().getDownloadState() == STATE_DOWNLOAD_WAITING) {
                holder.getTextView(R.id.tv_media_state).setVisibility(View.VISIBLE);
                holder.setText(R.id.tv_media_state, mContext.getResources().getString(R.string.str_media_download_waiting));
            } else if (item.getMedia().getDownloadState() == STATE_DOWNLOAD_DOWNLOADING) {
                holder.getTextView(R.id.tv_media_state).setVisibility(View.VISIBLE);
                holder.setText(R.id.tv_media_state, String.valueOf(item.getMedia().getDownloadProgress()*100/item.getMedia().getSize()) + "%");
            } else if(item.getMedia().getDownloadState() == STATE_DOWNLOAD_PAUSED) {
                holder.getTextView(R.id.tv_media_state).setVisibility(View.VISIBLE);
                holder.setText(R.id.tv_media_state,
                        mContext.getResources().getString(R.string.str_media_download_pause) + String.valueOf(item.getMedia().getDownloadProgress()*100/item.getMedia().getSize()) + "%");
            } else if (item.getMedia().getDownloadState() == STATE_DOWNLOAD_ERROR) {
                holder.getTextView(R.id.tv_media_state).setVisibility(View.VISIBLE);
                holder.setText(R.id.tv_media_state, mContext.getResources().getString(R.string.str_media_download_error));
            } else if (item.getMedia().getDownloadState() == STATE_DOWNLOAD_START) {
                holder.getTextView(R.id.tv_media_state).setVisibility(View.VISIBLE);
                holder.setText(R.id.tv_media_state, "...");
            } else {
                holder.getTextView(R.id.tv_media_state).setVisibility(View.INVISIBLE);
            }
        }

        // 下载的视频文件没有播放时长, 认为是格式错误的
        if (item.getMedia().getType() == MEDIA_VIDEO
                && item.getMedia().getDownloadState() == STATE_DOWNLOAD_DOWNLOADED
                && item.getMedia().getDuration() == 0) {
            holder.setText(R.id.tv_media_state, mContext.getResources().getString(R.string.str_media_format_error));
            holder.getTextView(R.id.tv_media_state).setVisibility(View.VISIBLE);
        }
    }


    public boolean exchangeNotifyChange(int src, int dst) {
        if (src != INVALID_POSITION && dst != INVALID_POSITION) {

            ArrayList<PlayBean> changeList =  new ArrayList<>();

            synchronized (listLock) {
                Collections.swap(getListDatas(), src, dst);

                PlayBean srcBean = getItem(src);
                PlayBean dstBean = getItem(dst);
                int dstIndex = dstBean.getIdx();
                dstBean.setIdx(srcBean.getIdx());
                srcBean.setIdx(dstIndex);

                changeList.add(srcBean);
                changeList.add(dstBean);
            }

            if (mOnPlaylistItemEventListener != null) {
                mOnPlaylistItemEventListener.onPlayListChanged(CHANGE_EVENT_EXCHANGE, changeList);
            }

            refresh();
            return true;
        }
        return false;
    }

    public void clearNotifyChange() {
        synchronized (listLock) {
            super.clear();
        }
        if (mOnPlaylistItemEventListener != null) {
            mOnPlaylistItemEventListener.onPlayListChanged(CHANGE_EVENT_CLEAR,null);
        }
    }

    @Override
    public void clear() {
        clearNotifyChange();
    }

    public void addItemNotifyChange(PlayBean item) {
        synchronized (listLock) {
            item.setIdx(getCount());
            super.addItem(item);
        }

        if (mOnPlaylistItemEventListener != null) {
            ArrayList<PlayBean> changeList =  new ArrayList<>();
            changeList.add(item);
            mOnPlaylistItemEventListener.onPlayListChanged(CHANGE_EVENT_ADD, changeList);
        }
    }

    @Override
    public void addItem(PlayBean item) {
        addItemNotifyChange(item);
    }

    public void removeItemNotifyChange(int position) {
        PlayBean removeBean = getItem(position);
        removeItem(removeBean);
    }


    @Override
    public void removeItem(PlayBean item) {
        List<PlayBean> removeList = new ArrayList<>();
        removeList.add(item);
        removeItemNotifyChange(removeList);
    }

    public void removeItemNotifyChange(List<PlayBean> items) {
        if (items==null || items.isEmpty()) {
            Log.w(TAG, "items empty to remove!");
            return;
        }

        if (mOnPlaylistItemEventListener != null) {
            List<PlayBean> newList = mOnPlaylistItemEventListener.onPlayListChanged(CHANGE_EVENT_REMOVE,items);

            synchronized (listLock) {
                listDatas.clear();
                for (int i = 0; i < newList.size(); i++) {
                    listDatas.add(newList.get(i));
                }
            }

        } else {

            synchronized (listLock) {
                for (PlayBean item: items) {
                    super.removeItem(item);
                }
            }
        }
    }

    // 外部使用此函数,这样日后在阅读代码的时候可以清晰知道会回调到数据库
    public void setListDatasNotifyChange(List<PlayBean> items) {
        clearNotifyChange();

        if (items == null) {
            return;
        }

        ArrayList<PlayBean> changeList =  new ArrayList<>();

        synchronized (listLock) {
            for (int i = 0; i < items.size(); i++) {
                items.get(i).setIdx(i);
                listDatas.add(items.get(i));
                changeList.add(items.get(i));
            }
        }

        if (mOnPlaylistItemEventListener != null) {
            mOnPlaylistItemEventListener.onPlayListChanged(CHANGE_EVENT_ADD, changeList);
        }
    }

    @Override
    // 重写父类方法,保证修改list内容会同步记录到数据库
    public void setListDatas(List<PlayBean> items){
        setListDatasNotifyChange(items);
    }

    public boolean hasPlayableItem() {
        synchronized (listLock) {
            for (PlayBean bean:getListDatas()) {
                if (bean.getMedia().getDownloadState()==STATE_DOWNLOAD_DOWNLOADED) {
                    return true;
                }
            }
            return false;
        }
    }

    public int firstPlayableItemIndex(int index) {
        synchronized (listLock) {
            for (int i = index; i<getCount(); i++) {
                if (getItem(i).getMedia().getDownloadState()==STATE_DOWNLOAD_DOWNLOADED) {
                    return i;
                }
            }

            for (int i = 0; i<index; i++) {
                if (getItem(i).getMedia().getDownloadState()==STATE_DOWNLOAD_DOWNLOADED) {
                    return i;
                }
            }
            return INVALID_POSITION;
        }
    }

    public void updateNotifyChange(MediaBean data) {
        ArrayList<PlayBean> changeList =  new ArrayList<>();

        synchronized (listLock) {
            for (PlayBean it : listDatas) {
                if (it.getMedia().getPath().equals(data.getPath())) {
                    it.setMedia(data);
                    if (data.getDownloadState() == STATE_DOWNLOAD_DOWNLOADED) {
                        if (it.getTime() == 0) {
                            it.setTime(data.getDuration());
                        }
                    }
                    changeList.add(it);
                }
            }
        }

        if (mOnPlaylistItemEventListener != null) {
            mOnPlaylistItemEventListener.onPlayListChanged(CHANGE_EVENT_UPDATE, changeList);
        }
    }

    public interface OnPlaylistItemEventListener {
        List<PlayBean> onPlayListChanged(int event,List<PlayBean> changeBeans);
    }

    public void setOnPlaylistItemEventListener(OnPlaylistItemEventListener onPlaylistItemEventListener) {
        this.mOnPlaylistItemEventListener = onPlaylistItemEventListener;
    }

}

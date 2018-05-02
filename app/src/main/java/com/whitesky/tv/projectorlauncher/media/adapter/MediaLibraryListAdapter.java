package com.whitesky.tv.projectorlauncher.media.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.whitesky.sdk.widget.ViewHolder;
import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.common.adapter.CommonAdapter;
import com.whitesky.tv.projectorlauncher.media.bean.MediaLibraryListBean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static android.widget.AdapterView.INVALID_POSITION;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_DOWNLOADED;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_MUSIC;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_PICTURE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_VIDEO;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.SOURCE_CLOUD_FREE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.SOURCE_CLOUD_PAY;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.SOURCE_CLOUD_PRIVATE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.SOURCE_CLOUD_PUBLIC;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.SOURCE_LOCAL;
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

public class MediaLibraryListAdapter extends CommonAdapter<MediaLibraryListBean>
{
    private final String TAG = this.getClass().getSimpleName();

    private final Object mListLock = new Object();

    private OnAllMediaListItemEventListener mOnALlMediaListItemEventListener;

    public MediaLibraryListAdapter(Context context, List<MediaLibraryListBean> data)
    {
        super(context, data, R.layout.item_all_media_list);
    }

    public boolean hasItemSelected()
    {
        for(MediaLibraryListBean data: getListDatas())
        {
            if (data.isSelected())
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void convert(ViewHolder holder, final int position, MediaLibraryListBean item) {
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

        // 设置文件名 与 播放时长
        holder.setText(R.id.tv_media_title, item.getMediaData().getTitle());
        holder.setText(R.id.tv_media_duration,  genTimeString(item.getMediaData().getDuration()));

        // 设置文件来源图标
        switch (item.getMediaData().getSource())
        {
            case SOURCE_LOCAL:
                holder.setImageResource(R.id.iv_media_source, R.drawable.img_media_source_local);
                break;
            case SOURCE_CLOUD_FREE:
            case SOURCE_CLOUD_PAY:
            case SOURCE_CLOUD_PRIVATE:
            case SOURCE_CLOUD_PUBLIC:
                if (item.getMediaData().getDownloadState()== STATE_DOWNLOAD_DOWNLOADED) {
                    holder.setImageResource(R.id.iv_media_source, R.drawable.img_media_source_cloud_download);
                } else {
                    holder.setImageResource(R.id.iv_media_source, R.drawable.img_media_source_cloud);
                }
                break;
            default:
                holder.setImageResource(R.id.iv_media_source, R.drawable.img_media_type_unknown);
                break;
        }

        // 下载状态显示
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

        // 决定功能按钮使能
        if (item.getMediaData().getSource()!=SOURCE_LOCAL) {
            if (item.getMediaData().getDownloadState() == STATE_DOWNLOAD_DOWNLOADED) {
                holder.getButton(R.id.bt_media_download).setBackgroundResource(R.drawable.selector_media_download_btn);
                holder.getButton(R.id.bt_media_download).setEnabled(false);
                holder.getButton(R.id.bt_media_preview).setEnabled(true);
                holder.getButton(R.id.bt_media_delete).setEnabled(true);
            } else if (item.getMediaData().getDownloadState() == STATE_DOWNLOAD_NONE) {
                holder.getButton(R.id.bt_media_download).setBackgroundResource(R.drawable.selector_media_download_btn);
                holder.getButton(R.id.bt_media_download).setEnabled(true);
                holder.getButton(R.id.bt_media_preview).setEnabled(false);
                holder.getButton(R.id.bt_media_delete).setEnabled(false);
            } else if (item.getMediaData().getDownloadState() == STATE_DOWNLOAD_WAITING
                    || item.getMediaData().getDownloadState() == STATE_DOWNLOAD_START
                    || item.getMediaData().getDownloadState() == STATE_DOWNLOAD_DOWNLOADING) {
                holder.getButton(R.id.bt_media_download).setBackgroundResource(R.drawable.selector_media_download_pause_btn);
                holder.getButton(R.id.bt_media_download).setEnabled(true);
                holder.getButton(R.id.bt_media_preview).setEnabled(false);
                holder.getButton(R.id.bt_media_delete).setEnabled(true);
            } else if (item.getMediaData().getDownloadState() == STATE_DOWNLOAD_PAUSED
                    || item.getMediaData().getDownloadState() == STATE_DOWNLOAD_ERROR) {
                holder.getButton(R.id.bt_media_download).setBackgroundResource(R.drawable.selector_media_download_btn);
                holder.getButton(R.id.bt_media_download).setEnabled(true);
                holder.getButton(R.id.bt_media_preview).setEnabled(false);
                holder.getButton(R.id.bt_media_delete).setEnabled(true);
            }
        } else {
            holder.getButton(R.id.bt_media_download).setBackgroundResource(R.drawable.selector_media_download_btn);
            holder.getButton(R.id.bt_media_download).setEnabled(false);
            holder.getButton(R.id.bt_media_preview).setEnabled(true);
            holder.getButton(R.id.bt_media_delete).setEnabled(true);
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
        void doItemDelete(int position);
        void doItemPreview(int position);
        void doItemDownLoad(int position);
        void itemSelectedChange();
    }

    public void setOnALlMediaListItemListener(OnAllMediaListItemEventListener onAllMediaListItemEventListener) {
        this.mOnALlMediaListItemEventListener = onAllMediaListItemEventListener;
    }

    public void update(MediaBean data) {
        synchronized (mListLock) {
            for (MediaLibraryListBean it : listDatas) {
                if (it.getMediaData().getPath().equals(data.getPath())) {
                    it.setMediaData(data);
                    return;
                }
            }
        }
    }

    public void removeItem(MediaBean data) {
        synchronized (mListLock) {
            MediaLibraryListBean found = null;
            for (MediaLibraryListBean it : listDatas) {
                if (it.getMediaData().getPath().equals(data.getPath())) {
                    found = it;
                    break;
                }
            }

            if (found!=null) {
                removeItem(found);
            }
        }
    }
}

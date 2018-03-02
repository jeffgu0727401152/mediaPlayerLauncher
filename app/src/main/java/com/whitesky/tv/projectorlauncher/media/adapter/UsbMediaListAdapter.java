package com.whitesky.tv.projectorlauncher.media.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.whitesky.sdk.widget.ViewHolder;
import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.common.adapter.CommonAdapter;
import com.whitesky.tv.projectorlauncher.media.bean.UsbMediaListBean;
import com.whitesky.tv.projectorlauncher.utils.FileUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.widget.AdapterView.INVALID_POSITION;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_MUSIC;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_PICTURE;
import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_VIDEO;

/**
 * Created by jeff on 18-1-16.
 */

public class UsbMediaListAdapter extends CommonAdapter<UsbMediaListBean>
{
    private final String TAG = this.getClass().getSimpleName();

    private OnUsbItemEventListener mOnUsbItemEventListener = null;

    public UsbMediaListAdapter(Context context, List<UsbMediaListBean> data)
    {
        super(context, data, R.layout.item_usb_media_list);
    }

    public boolean hasItemSelected()
    {
        for(UsbMediaListBean data: getListDatas())
        {
            if (data.isSelected())
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void convert(ViewHolder holder, final int position, UsbMediaListBean item) {
        holder.setText(R.id.tv_media_title, item.getTitle());
        holder.setText(R.id.tv_media_size, FileUtil.formatFileSize(item.getSize()));
        holder.setText(R.id.tv_media_list_pos, String.valueOf(position + 1) + ".");

        switch (item.getType())
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

        holder.getButton(R.id.bt_media_copy_to_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"bt_media_copy_to_left click " + position);
                if (mOnUsbItemEventListener !=null)
                {
                    mOnUsbItemEventListener.doItemCopy(position);
                }
            }
        });

        holder.getCheckBox(R.id.cb_media_selected).setChecked(item.isSelected());
        holder.getCheckBox(R.id.cb_media_selected).setOnClickListener(new View.OnClickListener () {
            @Override
            public void onClick(View view) {
                getListDatas().get(position).setSelected(!getListDatas().get(position).isSelected());
                if (mOnUsbItemEventListener !=null) {
                    mOnUsbItemEventListener.itemSelectedChange();
                }
            }
        });
    }

    public interface OnUsbItemEventListener {
        public void doItemCopy(int position);
        public void itemSelectedChange();
    }

    public void setOnUsbItemEventListener(OnUsbItemEventListener onUsbItemEventListener) {
        this.mOnUsbItemEventListener = onUsbItemEventListener;
    }

    private Comparator<UsbMediaListBean> comparator = new Comparator<UsbMediaListBean>() {

        @Override
        public int compare(UsbMediaListBean lhs, UsbMediaListBean rhs) {
            return lhs.getSize() > rhs.getSize() ? 0 : 1;
        }
    };

    public void sort()
    {
        Collections.sort(getListDatas(), comparator);
        notifyDataSetChanged();
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

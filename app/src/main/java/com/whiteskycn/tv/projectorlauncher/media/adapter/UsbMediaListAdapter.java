package com.whiteskycn.tv.projectorlauncher.media.adapter;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import com.github.mjdev.libaums.fs.UsbFile;
import com.whitesky.sdk.widget.ViewHolder;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.common.adapter.CommonAdapter;
import com.whiteskycn.tv.projectorlauncher.media.MediaActivity;
import com.whiteskycn.tv.projectorlauncher.media.bean.UsbMediaListBean;
import com.whiteskycn.tv.projectorlauncher.utils.FileUtil;
import com.whiteskycn.tv.projectorlauncher.utils.MediaScanUtil;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import static android.widget.AdapterView.INVALID_POSITION;

/**
 * Created by jeff on 18-1-16.
 */

public class UsbMediaListAdapter extends CommonAdapter<UsbMediaListBean>
{
    private final String TAG = this.getClass().getSimpleName();

    private OnUsbItemCopyListener mOnUsbItemCopyListener = null;

    public UsbMediaListAdapter(Context context, List<UsbMediaListBean> data)
    {
        super(context, data, R.layout.item_usb_media_list);
    }

    @Override
    public void convert(ViewHolder holder, final int position, UsbMediaListBean item) {
        holder.setText(R.id.tv_media_name, item.getTitle());
        holder.setText(R.id.tv_media_size, FileUtil.covertFormatFileSize(item.getSize()));

        MediaScanUtil.MediaTypeEnum type = item.getType();
        switch (type)
        {
            case PICTURE:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_pause);
                break;
            case VIDEO:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_type_video);
                break;
            default:
                holder.setImageResource(R.id.iv_media_ico, R.drawable.img_media_add);
                break;
        }

        holder.getButton(R.id.bt_media_copy_to_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"bt_media_copy_to_left click " + position);
                if (mOnUsbItemCopyListener!=null)
                {
                    mOnUsbItemCopyListener.doItemCopy(position);
                }
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

    public interface OnUsbItemCopyListener {
        public void doItemCopy(int position);
    }

    public void setOnUsbItemCopyListener(OnUsbItemCopyListener onUsbItemCopyListener) {
        this.mOnUsbItemCopyListener = onUsbItemCopyListener;
    }

    /**
     * Class to compare {@link UsbFile}s. If the {@link UsbFile} is an directory
     * it is rated lower than an file, ie. directories come first when sorting.
     */
    private Comparator<UsbMediaListBean> comparator = new Comparator<UsbMediaListBean>() {

        @Override
        public int compare(UsbMediaListBean lhs, UsbMediaListBean rhs) {
            return lhs.getSize() > rhs.getSize() ? 0 : 1;
        }
    };

    public void sort()
    {
        Collections.sort(listDatas, comparator);
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

package com.whiteskycn.tv.projectorlauncher.media.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.whitesky.sdk.widget.ViewHolder;
import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.common.Contants;
import com.whiteskycn.tv.projectorlauncher.common.adapter.CommonAdapter;
import com.whiteskycn.tv.projectorlauncher.media.bean.PlayListBean;
import com.whiteskycn.tv.projectorlauncher.utils.SharedPreferencesUtil;


import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static android.widget.AdapterView.INVALID_POSITION;
import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.MEDIA_MUSIC;
import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.MEDIA_PICTURE;
import static com.whiteskycn.tv.projectorlauncher.media.bean.RawMediaBean.MEDIA_VIDEO;

/**
 * Created by jeff on 18-1-16.
 */

public class PlayListAdapter extends CommonAdapter<PlayListBean>
{
    private static final String CONFIG_PLAYLIST = "playList";
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

        holder.getImageView(R.id.iv_media_play_indicator).setVisibility(item.isPlaying() ? View.VISIBLE : View.INVISIBLE);

        holder.getButton(R.id.bt_media_remove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"playlist remove position " + position);
                removeItem(getItem(position));
                saveToConfig();
                refresh();
            }
        });

        holder.getButton(R.id.bt_media_remove).setVisibility(View.VISIBLE);
    }


    public boolean exchange(int src, int dst) {
        if (src != INVALID_POSITION && dst != INVALID_POSITION) {
            Collections.swap(getListDatas(), src, dst);
            saveToConfig();
            refresh();
            return true;
        }
        return false;
    }

    public void loadFromConfig() {
        Gson gson = new Gson();
        SharedPreferencesUtil config = new SharedPreferencesUtil(mContext, Contants.CONFIG);
        String jsonStr = config.getString(CONFIG_PLAYLIST, null);
        Type type = new TypeToken<List<PlayListBean>>(){}.getType();
        if (jsonStr!=null)
        {
            List<PlayListBean> datas = gson.fromJson(jsonStr,type);
            clear();
            for(int i = 0; i < datas.size(); i++)
            {
                addItem(datas.get(i));
            }
        }
    }

    public void saveToConfig() {
        SharedPreferencesUtil shared = new SharedPreferencesUtil(mContext, Contants.CONFIG);
        Gson gson = new Gson();
        String jsonStr = gson.toJson(getListDatas());
        shared.putString(CONFIG_PLAYLIST, jsonStr);
    }
}

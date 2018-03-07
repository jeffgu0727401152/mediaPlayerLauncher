package com.whitesky.tv.projectorlauncher.media.bean;

import com.whitesky.tv.projectorlauncher.media.db.MediaBean;

/**
 * Created by jeff on 18-1-16.
 */

public class PlayListBean {
    public static final int MEDIA_SCALE_FIT_XY = 0;
    public static final int MEDIA_SCALE_FIT_CENTER = 1;
    public static final int MEDIA_SCALE_DEFAULT = MEDIA_SCALE_FIT_XY;


    private MediaBean mMediaData;
    private boolean isPlaying = false;
    private int playScale = MEDIA_SCALE_FIT_XY;

    public PlayListBean() {
    }

    public PlayListBean(MediaBean data) {
        mMediaData = data;
    }

    public MediaBean getMediaData() {
        return mMediaData;
    }

    public void setMediaData(MediaBean mediaData) {
        this.mMediaData = mediaData;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public int getPlayScale() {
        return playScale;
    }

    public void setPlayScale(int playScale) {
        this.playScale = (playScale==0 ? 0 : 1);
    }
}

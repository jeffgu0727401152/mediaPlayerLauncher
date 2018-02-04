package com.whiteskycn.tv.projectorlauncher.media.bean;

import com.whiteskycn.tv.projectorlauncher.media.db.MediaBean;
import com.whiteskycn.tv.projectorlauncher.utils.FileUtil;

/**
 * Created by jeff on 18-1-16.
 */

public class PlayListBean {
    private MediaBean mMediaData;
    private boolean isPlaying = false;
    private int playScale = 0;

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
        this.playScale = playScale;
    }
}

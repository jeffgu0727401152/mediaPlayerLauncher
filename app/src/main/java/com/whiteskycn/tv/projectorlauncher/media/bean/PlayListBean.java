package com.whiteskycn.tv.projectorlauncher.media.bean;

import com.whiteskycn.tv.projectorlauncher.utils.FileUtil;

/**
 * Created by jeff on 18-1-16.
 */

public class PlayListBean {

    private RawMediaBean mMediaData;

    private String title = "";

    private String description = "";

    private int duration = 0;

    private boolean isPlaying = false;

    public PlayListBean() {
    }

    public PlayListBean(RawMediaBean data) {
        mMediaData = data;
        if (mMediaData.getSource()==RawMediaBean.SOURCE_LOCAL) {
            title = FileUtil.getFilePrefix(data.getFilePath());
        } else {
            title = "todo";
        }
    }

    public PlayListBean(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public RawMediaBean getMediaData() {
        return mMediaData;
    }

    public void setMediaData(RawMediaBean mediaData) {
        this.mMediaData = mediaData;
    }

    public int getDuration() {
        return duration;
    }

    public void setDurationTime(int durationTime) {
        this.duration = durationTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }
}

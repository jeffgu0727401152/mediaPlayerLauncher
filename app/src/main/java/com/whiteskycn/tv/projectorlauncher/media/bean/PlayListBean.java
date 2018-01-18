package com.whiteskycn.tv.projectorlauncher.media.bean;

/**
 * Created by jeff on 18-1-16.
 */

public class PlayListBean {

    private MediaFileBean mMediaData;

    private String title = "";

    private String description = "";

    private int durationTime = 0;

    public PlayListBean() {
    }

    public PlayListBean(MediaFileBean date) {
        mMediaData = date;
        title = date.getName();
    }

    public PlayListBean(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public MediaFileBean getMediaData() {
        return mMediaData;
    }

    public void setMediaData(MediaFileBean mediaData) {
        this.mMediaData = mediaData;
    }

    public int getDurationTime() {
        return durationTime;
    }

    public void setDurationTime(int durationTime) {
        this.durationTime = durationTime;
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
}

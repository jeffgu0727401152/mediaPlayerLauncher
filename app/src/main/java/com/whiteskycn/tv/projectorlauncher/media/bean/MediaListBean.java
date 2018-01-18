package com.whiteskycn.tv.projectorlauncher.media.bean;

/**
 * Created by jeff on 18-1-16.
 */

public class MediaListBean {

    private MediaFileBean mMediaData;

    private String title = "";
    private String description = "";
    private boolean selected = false;

    public MediaListBean() {
    }

    public MediaListBean(MediaFileBean date) {
        mMediaData = date;
        title = date.getName();
    }

    public MediaListBean(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public MediaFileBean getMediaData() {
        return mMediaData;
    }

    public void setMediaData(MediaFileBean mediaData) {
        this.mMediaData = mediaData;
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

    public boolean getSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}

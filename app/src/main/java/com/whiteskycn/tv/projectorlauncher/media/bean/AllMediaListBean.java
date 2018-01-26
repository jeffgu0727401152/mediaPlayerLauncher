package com.whiteskycn.tv.projectorlauncher.media.bean;

/**
 * Created by jeff on 18-1-16.
 */

public class AllMediaListBean {

    private MediaFileBean mMediaData;

    private String title = "";
    private String description = "";
    private String duration = "";

    private boolean isSelected = false;

    public AllMediaListBean() {
    }

    public AllMediaListBean(MediaFileBean data) {
        mMediaData = data;
        title = data.getName();
    }

    public AllMediaListBean(String title, String description) {
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

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }
}

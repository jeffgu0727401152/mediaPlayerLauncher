package com.whiteskycn.tv.projectorlauncher.media.bean;

import com.whiteskycn.tv.projectorlauncher.utils.FileUtil;

/**
 * Created by jeff on 18-1-16.
 */

public class AllMediaListBean {

    private RawMediaBean mMediaData;

    private String title = "";
    private String description = "";

    private boolean isSelected = false;

    public AllMediaListBean() {
    }

    public AllMediaListBean(RawMediaBean data) {
        mMediaData = data;
        if (mMediaData.getSource()==RawMediaBean.SOURCE_LOCAL) {
            title = FileUtil.getFilePrefix(data.getFilePath());
        } else {
            title = "todo";
        }
    }

    public AllMediaListBean(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public RawMediaBean getMediaData() {
        return mMediaData;
    }

    public void setMediaData(RawMediaBean mediaData) {
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

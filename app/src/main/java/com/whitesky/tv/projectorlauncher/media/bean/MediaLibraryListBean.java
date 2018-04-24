package com.whitesky.tv.projectorlauncher.media.bean;
import com.whitesky.tv.projectorlauncher.media.db.MediaBean;

/**
 * Created by jeff on 18-1-16.
 */

public class MediaLibraryListBean {

    private MediaBean mMediaData;
    private boolean isSelected = false;

    public MediaLibraryListBean() {
    }

    public MediaLibraryListBean(MediaBean data) {
        mMediaData = data;
        isSelected = false;
    }

    public MediaBean getMediaData() {
        return mMediaData;
    }

    public void setMediaData(MediaBean mediaData) {
        this.mMediaData = mediaData;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }
}

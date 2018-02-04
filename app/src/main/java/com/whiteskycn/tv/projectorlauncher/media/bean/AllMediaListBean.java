package com.whiteskycn.tv.projectorlauncher.media.bean;
import com.whiteskycn.tv.projectorlauncher.media.db.MediaBean;
import com.whiteskycn.tv.projectorlauncher.utils.FileUtil;

/**
 * Created by jeff on 18-1-16.
 */

public class AllMediaListBean {

    private MediaBean mMediaData;
    private boolean isSelected = false;

    public AllMediaListBean() {
    }

    public AllMediaListBean(MediaBean data) {
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

package com.whitesky.tv.projectorlauncher.media.bean;

import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.MEDIA_UNKNOWN;

/**
 * Created by jeff on 18-1-16.
 */

public class UsbMediaListBean {

    private int type = MEDIA_UNKNOWN;
    private String title = "";
    private String description = "";
    private String path = "";
    private long size = 0;
    private boolean isSelected = false;

    public UsbMediaListBean() {
        title = "";
        path = "";
        description = "";
        size = 0;
        isSelected = false;
    }

    public UsbMediaListBean(String title, int type, String path, long size) {
        this.type = type;
        description = "";
        this.size = size;
        this.path = path;
        this.title = title;
        isSelected = false;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public long getSize() {
        return size;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}

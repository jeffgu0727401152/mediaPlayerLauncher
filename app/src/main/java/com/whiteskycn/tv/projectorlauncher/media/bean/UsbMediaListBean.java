package com.whiteskycn.tv.projectorlauncher.media.bean;

import com.github.mjdev.libaums.fs.UsbFile;
import com.whiteskycn.tv.projectorlauncher.utils.MediaScanUtil;

/**
 * Created by jeff on 18-1-16.
 */

public class UsbMediaListBean {

    private UsbFile mediaFile;
    private MediaScanUtil.MediaTypeEnum type = MediaScanUtil.MediaTypeEnum.UNKNOWN;
    private String title = "";
    private String description = "";
    private String name = "";
    private String path = "";
    private long size = 0;
    private boolean isSelected = false;

    public UsbMediaListBean() {
        mediaFile = null;
        title = "";
        path = "";
        description = "";
        size = 0;
        isSelected = false;
    }

    public UsbMediaListBean(MediaScanUtil.MediaTypeEnum type, String name, String path, long size) {
        this.type = type;
        this.name = name;
        this.title = name;
        description = "";
        this.size = size;
        this.path = path;
        isSelected = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public MediaScanUtil.MediaTypeEnum getType() {
        return type;
    }

    public void setType(MediaScanUtil.MediaTypeEnum type) {
        this.type = type;
    }
}

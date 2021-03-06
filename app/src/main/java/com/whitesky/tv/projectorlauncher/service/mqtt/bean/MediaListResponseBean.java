package com.whitesky.tv.projectorlauncher.service.mqtt.bean;

import com.whitesky.tv.projectorlauncher.media.db.MediaBean;
import com.whitesky.tv.projectorlauncher.media.db.PlayBean;

import static com.whitesky.tv.projectorlauncher.media.db.PlayBean.MEDIA_SCALE_DEFAULT;


/**
 * Created by jeff on 18-3-5.
 */

public class MediaListResponseBean {
    int id;
    String name;
    int type;
    int scale;
    int duration;

    public MediaListResponseBean(PlayBean data) {
        this.id = data.getMedia().getId();
        this.name = data.getMedia().getPath();
        this.type = data.getMedia().getType();
        this.scale = data.getScale();
        this.duration = data.getTime();
    }

    public MediaListResponseBean(MediaBean data) {
        this.id = data.getId();
        this.name = data.getPath();
        this.type = data.getType();
        this.scale = MEDIA_SCALE_DEFAULT;
        this.duration = data.getDuration();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}

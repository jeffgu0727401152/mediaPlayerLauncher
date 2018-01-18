package com.whiteskycn.tv.projectorlauncher.media.bean;

/**
 * Created by jeff on 18-1-16.
 */

public class MediaListBean {

    private MediaFileBean mMediaData;

    private String title = "";

    private String description = "";

     private int state = 0;

    private int collect = 0;

    public MediaListBean() {
    }

    /**
     * @param title    标题
     * @param description 描述
     * @param state    图片状态：0-绿色；1-灰色；2-红色；
     * @param collect  是否收藏：0-未收藏；1-收藏；
     */
    public MediaListBean(String title, String description, int state, int collect) {
        this.title = title;
        this.description = description;
        this.state = state;
        this.collect = collect;
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

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getCollect() {
        return collect;
    }

    public void setCollect(int collect) {
        this.collect = collect;
    }
}

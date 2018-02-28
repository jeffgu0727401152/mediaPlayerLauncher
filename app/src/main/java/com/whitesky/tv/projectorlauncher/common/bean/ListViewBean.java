package com.whitesky.tv.projectorlauncher.common.bean;

/**
 * Created by xiaoxuan on 2017/6/2.
 */
public class ListViewBean
{
    
    /**
     * 标题
     */
    private String title = "";
    
    /**
     * 描述
     */
    private String discribe = "";
    
    /**
     * 状态：0-绿色；1-灰色；2-红色；
     */
    private int state = 0;
    
    /**
     * 收藏状态：0-未收藏；1-收藏；
     */
    private int collect = 0;
    
    public ListViewBean()
    {
    }
    
    /**
     *
     * @param title 标题
     * @param discribe 描述
     * @param state 图片状态：0-绿色；1-灰色；2-红色；
     * @param collect 是否收藏：0-未收藏；1-收藏；
     */
    public ListViewBean(String title, String discribe, int state, int collect)
    {
        this.title = title;
        this.discribe = discribe;
        this.state = state;
        this.collect = collect;
    }
    
    public String getTitle()
    {
        return title;
    }
    
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    public String getDiscribe()
    {
        return discribe;
    }
    
    public void setDiscribe(String discribe)
    {
        this.discribe = discribe;
    }
    
    public int getState()
    {
        return state;
    }
    
    public void setState(int state)
    {
        this.state = state;
    }
    
    public int getCollect()
    {
        return collect;
    }
    
    public void setCollect(int collect)
    {
        this.collect = collect;
    }
}

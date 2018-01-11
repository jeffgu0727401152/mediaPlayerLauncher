package com.whiteskycn.tv.projectorlauncher.home;

/**
 * Created by xiaoxuan on 2017/11/15.
 */

public interface MqttMessageCallback
{
    public void distributeMessage(String msg);
}

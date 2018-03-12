package com.whitesky.tv.projectorlauncher.common;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class Contants
{
    public static final String PROJECT_NAME = "PS500";

    public static final String ACTION_PUSH_PLAYLIST = "com.whitesky.tv.push.playList";
    public static final String ACTION_PUSH_PLAYMODE = "com.whitesky.tv.push.playMode";
    public static final String EXTRA_PUSH_CONTEXT = "mqtt_push_context";

    public static final String PREF_CONFIG = "PREF_CONFIG";        // 存放app的所有配置信息
    public static final String CONFIG_PLAYLIST = "playList";       // 播放列表
    public static final String CONFIG_REPLAY_MODE = "replayMode";  // 播放下一首的选择顺序
    public static final String CONFIG_SHOW_MASK = "showMask";      // 是否显示polygon mask

    public static final String IS_ACTIVATE = "false"; // 设备是否被激活
    public static final String IS_SETUP_PASS = "false"; // 设置引导是否跳过

    // MediaActivity +++
    public static final String LOCAL_SATA_MOUNT_PATH = "/mnt/sata";  // 硬盘的固定挂载目录
    public static final String LOCAL_MASS_STORAGE_PATH = "/mnt/sata/disk";  // 硬盘的固定挂载目录
    public static final String CLOUD_MEDIA_FOLDER = "cloud";                // 云端下载文件的储存目录
    public static final String LOCAL_MEDIA_FOLDER = "local";                // usb导入本地的文件位置

    public static final String USB_DEVICE_DEFAULT_SEARCH_MEDIA_FOLDER = "media";  //扫描usb媒体文件的路径
    public static final String COPY_TO_USB_MEDIA_EXPORT_FOLDER = "export";        //导出到usb设备的路径
    // 排除在外的挂载目录,设备上的所有的非usb的会被系统挂载的移动盘
    public static final String[] mMountExceptList = new String[]{"/mnt/sdcard", "/storage/emulated/0", LOCAL_MASS_STORAGE_PATH};
    // MediaActivity ---
}

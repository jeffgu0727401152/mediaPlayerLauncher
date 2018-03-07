package com.whitesky.tv.projectorlauncher.common;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class Contants
{
    public static final String PROJECT_NAME = "PS500";

    public static final String ACTION_PUSH_PLAYLIST = "com.whitesky.tv.push.playlist";
    public static final String EXTRA_PUSH_CONTEXT = "playlist";

    public static final String PERF_CONFIG = "PERF_CONFIG";// 存放用户信息
    public static final String CONFIG_PLAYLIST = "playList";
    public static final String CONFIG_PLAYMODE = "playMode";

    public static final String IS_ACTIVATE = "false"; // 设备是否被激活
    public static final String IS_SETUP_PASS = "false"; // 设置引导是否跳过
    public static final String ACTIVE_TOKEN = "active_token";// app是否首次安装
    public static final String SELF_CHECK = "SELF_CHECK";
    public static final String APP_INSTALL = "APP_INSTALL";
    public static final String APP_UNINSTALL = "APP_UNINSTALL";
    public static final String TRAP_EDITOR = "TRAP_EDITOR";
    public static boolean isEditor = false;
    public static final String IS_WIFI = "isWifi";

    // MediaActivity +++
    public static final String LOCAL_MASS_STORAGE_PATH = "/mnt/sata/disk";
    public static final String CLOUD_MEDIA_FOLDER = "cloud";
    public static final String LOCAL_MEDIA_FOLDER = "local";

    public static final String USB_DEVICE_DEFAULT_SEARCH_MEDIA_FOLDER = "media";
    public static final String COPY_TO_USB_MEDIA_EXPORT_FOLDER = "export";
    // 排除在外的挂载目录,非移动硬盘
    public static final String[] mMountExceptList = new String[]{"/mnt/sdcard", "/storage/emulated/0", LOCAL_MASS_STORAGE_PATH};
    // MediaActivity ---
}

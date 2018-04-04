package com.whitesky.tv.projectorlauncher.common;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class Contants
{
    public static final String PROJECT_NAME = "PS500";

    public static final String ACTION_CMD_PLAY_CONTROL = "com.whitesky.tv.cmd.playControl";
    public static final String ACTION_CMD_QRCODE_CONTROL = "com.whitesky.tv.push.qrCodeControl";

    public static final String ACTION_PUSH_PLAYLIST = "com.whitesky.tv.push.playList";
    public static final String ACTION_PUSH_PLAYMODE = "com.whitesky.tv.push.playMode";
    public static final String ACTION_PUSH_DELETE = "com.whitesky.tv.push.delete";
    public static final String ACTION_PUSH_DOWNLOAD_NEED_SYNC = "com.whitesky.tv.push.downloadSync";

    public static final String EXTRA_MQTT_ACTION_CONTEXT = "mqtt_action_context";

    public static final String ACTION_DOWNLOAD_OTA_PROGRESS = "com.whitesky.tv.download.OTA_PROGRESS";
    public static final String ACTION_DOWNLOAD_OTA_INSTALL_FAILED = "com.whitesky.tv.download.OTA_INSTALL_FAILED";
    public static final String ACTION_DOWNLOAD_STATE_UPDATE = "com.whitesky.tv.download.state";
    public static final String EXTRA_DOWNLOAD_STATE_CONTEXT = "mqtt_push_context";

    public static final String PREF_CONFIG = "PREF_CONFIG";             // 存放app的所有配置信息
    public static final String CONFIG_PLAYLIST = "playList";            // 播放列表
    public static final String CONFIG_REPLAY_MODE = "replayMode";       // 播放下一首的选择顺序
    public static final String CONFIG_SHOW_MASK = "showMask";           // 是否显示polygon mask
    public static final String CONFIG_MEDIA_LIST_ORDER = "mediaOrder";  // 列表的显示排序
    public static final String CONFIG_SHOW_QRCODE = "showQRcode";       // 是否显示控制二维码
    public static final String CONFIG_QRCODE_URL = "QRcodeURL";       // 储存控制二维码

    public static final int MEDIA_LIST_ORDER_NAME = 0;  // 列表的显示排序
    public static final int MEDIA_LIST_ORDER_DURATION = 1;  // 列表的显示排序
    public static final int MEDIA_LIST_ORDER_SOURCE = 2;  // 列表的显示排序
    public static final int MEDIA_LIST_ORDER_DEFAULT = MEDIA_LIST_ORDER_SOURCE;  // 列表的显示排序

    public static final String IS_ACTIVATE = "false"; // 设备是否被激活
    public static final String IS_SETUP_PASS = "false"; // 设置引导是否跳过

    // MediaActivity +++
    public static final String LOCAL_SATA_MOUNT_PATH = "/mnt/sata";         // 硬盘的固定挂载目录
    public static final String MASS_STORAGE_PATH = "/mnt/sata/disk";  // 硬盘的固定挂载目录
    public static final String LOCAL_MEDIA_FOLDER = "local";                // usb导入本地的文件位置
    public static final String CLOUD_MEDIA_FOLDER = "cloud";                // 云端下载文件的储存总目录
    public static final String CLOUD_MEDIA_PRIVATE_FOLDER = "private";      // 用户私人上传文件的储存目录
    public static final String CLOUD_MEDIA_FREE_FOLDER = "free";            // 公司或用户上传的共享文件的储存目录

    public static final String USB_DEVICE_DEFAULT_SEARCH_MEDIA_FOLDER = "media";  //扫描usb媒体文件的路径
    public static final String COPY_TO_USB_MEDIA_EXPORT_FOLDER = "export";        //导出到usb设备的路径
    // 排除在外的挂载目录,设备上的所有的非usb的会被系统挂载的移动盘
    public static final String[] mMountExceptList = new String[]{"/mnt/sdcard", "/storage/emulated/0", MASS_STORAGE_PATH};
    // MediaActivity ---

    public static final String UPDATE_APK_DOWNLOAD_PATH = "/mnt/sdcard/update/ota.apk";
}

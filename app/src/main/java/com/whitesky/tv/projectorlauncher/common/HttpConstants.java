package com.whitesky.tv.projectorlauncher.common;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class HttpConstants
{
    public static final String URL_LOGIN = "https://screen.whitesky.com.cn/device/bind/";
    public static final String URL_LOGIN_PROJECT_NAME = "?project=PS500";

    public static final String URL_GET_LOGIN_INFO = "https://screen.whitesky.com.cn:5443/api/getLoginInfo";
    public static final String URL_DEVICE_LOGOUT = "https://screen.whitesky.com.cn:5443/api/deviceLogout";
    public static final String URL_CHECK_VERSION = "https://screen.whitesky.com.cn:5443/api/checkVersion";
    public static final String URL_HEARTBEAT = "https://screen.whitesky.com.cn:5443/api/heartBeat";
    public static final String URL_GET_SHARE_LIST = "https://screen.whitesky.com.cn:5443/api/getShareList";
    public static final String URL_GET_QRCODE = "https://screen.whitesky.com.cn/api/getQrcode";

    public static final String LOGIN_STATUS_SUCCESS = "000000";
    public static final String LOGIN_STATUS_NOT_YET = "200101";

    public static final String VERSION_CHECK_STATUS_SUCCESS = "000000";
    public static final String VERSION_CHECK_STATUS_NO_AVAILABLE = "200001";

    public static final String QRCODE_GET_STATUS_SUCCESS = "000000";

    public static int HTTP_STATUS_SUCCESS = 200;
    
}

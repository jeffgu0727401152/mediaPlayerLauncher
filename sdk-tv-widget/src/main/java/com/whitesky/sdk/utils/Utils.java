package com.whitesky.sdk.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mac on 17-6-2. SDK版本工具类
 *
 * @author xiaoxuan
 */
public class Utils
{
    /**
     * 获取SDK版本
     */
    public static int getSDKVersion()
    {
        int version = 0;
        try
        {
            version = Integer.valueOf(android.os.Build.VERSION.SDK);
        }
        catch (NumberFormatException e)
        {
        }
        return version;
    }
    
    /** 去掉字符串中的 回车 换行 空格 */
    public static String replaceBlank(String str)
    {
        String dest = "";
        if (str != null)
        {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest;
    }
}

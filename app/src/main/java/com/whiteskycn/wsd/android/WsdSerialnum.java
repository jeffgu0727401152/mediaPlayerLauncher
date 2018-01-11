package com.whiteskycn.wsd.android;

/**
 * Created by ivan on 17-6-1.
 */

public class WsdSerialnum
{
    static
    {
        System.loadLibrary("serialnum_jni");
    }
    
    public static native byte[] read();
}

package com.whiteskycn.wsd.android;

/**
 * Created by jeff on 18-2-12.
 */

public class NativeCertification {
    private static String TAG = "NativeCertification";

    static {
        System.loadLibrary("AtcCert");
    }

    public static native byte[] getPkcs12();
}

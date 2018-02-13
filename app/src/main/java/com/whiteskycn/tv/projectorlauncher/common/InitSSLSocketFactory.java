package com.whiteskycn.tv.projectorlauncher.common;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.whiteskycn.wsd.android.NativeCertification;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * Created by lei on 17-12-1.
 */

public class InitSSLSocketFactory extends Service
{
    private final String TAG = this.getClass().getSimpleName();

    private final String keyStorePassword = "wxgh#2561";
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        initSSL();
        return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
    
    void initSSL() {
        try {
            byte[] bufferPkcs12 = NativeCertification.getPkcs12();
            if (bufferPkcs12.length > 0) {
                InputStream kmin = new ByteArrayInputStream(bufferPkcs12);
                KeyStore kmkeyStore = KeyStore.getInstance("PKCS12");
                kmkeyStore.load(kmin, keyStorePassword.toCharArray());
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
                kmf.init(kmkeyStore, keyStorePassword.toCharArray());

                // Create an SSLContext that uses our TrustManager
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(kmf.getKeyManagers(), null, null);

                SSLContext.setDefault(context);
                Log.d(TAG, "init SSLContext done!");
            } else {
                Log.e(TAG,"init SSLContext fail!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

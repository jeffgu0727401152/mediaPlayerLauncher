package com.whiteskycn.tv.projectorlauncher.utils;


import com.whiteskycn.tv.projectorlauncher.settings.model.SpeedModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class ReadFileUtil
{
    public static byte[] ReadFileFromURL(String URL, SpeedModel info)
    {
        int FileLenth = 0;
        long startTime = 0;
        long intervalTime = 0;
        byte[] b = null;
        URL mUrl = null;
        URLConnection mUrlConnection = null;
        InputStream inputStream = null;
        try
        {
            mUrl = new URL(URL);
            mUrlConnection = mUrl.openConnection();
            mUrlConnection.setConnectTimeout(15000);
            mUrlConnection.setReadTimeout(15000);
            FileLenth = mUrlConnection.getContentLength();
            inputStream = mUrlConnection.getInputStream();
            SpeedModel.totalBytes = FileLenth;
            b = new byte[FileLenth];
            startTime = System.currentTimeMillis();
            BufferedReader bufferReader = new BufferedReader(new InputStreamReader(mUrlConnection.getInputStream()));
            String line;
            byte buffer[];
            while (SpeedModel.FILECANREAD && ((line = bufferReader.readLine()) != null)
                && FileLenth > SpeedModel.FinishBytes)
            {
                buffer = line.getBytes();
                intervalTime = System.currentTimeMillis() - startTime;
                SpeedModel.FinishBytes = SpeedModel.FinishBytes + buffer.length;
                if (intervalTime == 0)
                {
                    SpeedModel.Speed = 1000;
                }
                else
                {
                    SpeedModel.Speed = SpeedModel.FinishBytes / intervalTime;
                    double a = (double) SpeedModel.FinishBytes / SpeedModel.totalBytes * 100;
                    SpeedModel.progress = (int)a;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (inputStream != null)
                {
                    inputStream.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return b;
    }
}

package com.whitesky.sdk.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class BitmapUtils
{
    public static Bitmap blurImageAmeliorate(Bitmap bmp)
    {
        long start = System.currentTimeMillis();
        // 高斯矩阵
        int[] gauss = new int[] {1, 2, 1, 2, 4, 2, 1, 2, 1};
        
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        
        int pixR = 0;
        int pixG = 0;
        int pixB = 0;
        
        int pixColor = 0;
        
        int newR = 0;
        int newG = 0;
        int newB = 0;
        
        int delta = 16; // 值越小图片会越亮，越大则越暗
        
        int idx = 0;
        int[] pixels = new int[width * height];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 1, length = height - 1; i < length; i++)
        {
            for (int k = 1, len = width - 1; k < len; k++)
            {
                idx = 0;
                for (int m = -1; m <= 1; m++)
                {
                    for (int n = -1; n <= 1; n++)
                    {
                        pixColor = pixels[(i + m) * width + k + n];
                        pixR = Color.red(pixColor);
                        pixG = Color.green(pixColor);
                        pixB = Color.blue(pixColor);
                        
                        newR = newR + (int)(pixR * gauss[idx]);
                        newG = newG + (int)(pixG * gauss[idx]);
                        newB = newB + (int)(pixB * gauss[idx]);
                        idx++;
                    }
                }
                
                newR /= delta;
                newG /= delta;
                newB /= delta;
                
                newR = Math.min(255, Math.max(0, newR));
                newG = Math.min(255, Math.max(0, newG));
                newB = Math.min(255, Math.max(0, newB));
                
                pixels[i * width + k] = Color.argb(255, newR, newG, newB);
                
                newR = 0;
                newG = 0;
                newB = 0;
            }
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        long end = System.currentTimeMillis();
        return bitmap;
    }
}

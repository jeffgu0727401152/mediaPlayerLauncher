package com.whiteskycn.tv.projectorlauncher.media;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;

import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.wsd.android.NativeMask;

/**
 * Created by jeff on 18-2-5.
 */

public class MaskControl {
    private final String TAG = this.getClass().getSimpleName();

    public static final int SCREEN_MASK_MODE_NONE = 0;
    public static final int SCREEN_MASK_MODE_NET = 1;
    public static final int SCREEN_MASK_MODE_GRAY_1 = 2;
    public static final int SCREEN_MASK_MODE_GRAY_2 = 3;
    public static final int SCREEN_MASK_MODE_GRAY_3 = 4;
    public static final int SCREEN_MASK_MODE_WINDOW = 5;

    private ImageView maskArea;
    private PaintableImageView paintWindow;
    private int maskState;
    private Activity mAttachActivity;

    public MaskControl(Activity attach, ImageView mask, PaintableImageView view) {
        maskArea = mask;
        maskArea.setScaleType(ImageView.ScaleType.FIT_XY);
        mAttachActivity = attach;
        maskState = SCREEN_MASK_MODE_NONE;
        paintWindow = view;
        hide();
    }

    public void showNet()
    {
        maskArea.setImageDrawable(mAttachActivity.getResources().getDrawable(R.drawable.img_media_net_mask));
        maskArea.setVisibility(View.VISIBLE);
        paintWindow.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_NET;
    }

    public void showGray1()
    {
        maskArea.setImageDrawable(mAttachActivity.getResources().getDrawable(R.drawable.shape_media_gray_1));
        maskArea.setVisibility(View.VISIBLE);
        paintWindow.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_GRAY_1;
    }

    public void showGray2()
    {
        maskArea.setImageDrawable(mAttachActivity.getResources().getDrawable(R.drawable.shape_media_gray_2));
        maskArea.setVisibility(View.VISIBLE);
        paintWindow.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_GRAY_2;
    }

    public void showGray3()
    {
        maskArea.setImageDrawable(mAttachActivity.getResources().getDrawable(R.drawable.shape_media_gray_3));
        maskArea.setVisibility(View.VISIBLE);
        paintWindow.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_GRAY_3;
    }

    public void showPaintWindow()
    {
        maskArea.setVisibility(View.INVISIBLE);
        paintWindow.setVisibility(View.VISIBLE);
        paintWindow.bringToFront();
        maskState = SCREEN_MASK_MODE_WINDOW;
        test();
    }

    public void hide()
    {
        maskArea.setVisibility(View.INVISIBLE);
        paintWindow.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_NONE;
    }

    public int getMaskState()
    {
        return maskState;
    }

    private void test()
    {
        int[] polygonBufferArray = new int[1280 * 720];

        int polygonCountArray[] = {4, 4};
        int xPointArray[] = {300,100,300,600,300+600,100+600,300+600,600+600};
        int yPointArray[] = {100,300,600,300,100,300,600,300};

        NativeMask.createPolygonBuffer(
                polygonBufferArray,
                1280,
                720,
                polygonCountArray,
                xPointArray,
                yPointArray,
                0x00000000,
                0x7F0000FF);

        Bitmap bitmap = Bitmap.createBitmap(
                polygonBufferArray,
                1280,
                720,
                Bitmap.Config.ARGB_8888);

        paintWindow.setBackgroundColor(Color.TRANSPARENT);
        paintWindow.setImageBitmap(bitmap);
    }
}

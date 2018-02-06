package com.whiteskycn.tv.projectorlauncher.media;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;

import com.whiteskycn.tv.projectorlauncher.R;

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
    private PaintableImageView window;
    private int maskState;
    private Activity mAttachActivity;

    public MaskControl(Activity attach, ImageView mask, PaintableImageView view) {
        maskArea = mask;
        maskArea.setScaleType(ImageView.ScaleType.FIT_XY);
        mAttachActivity = attach;
        maskState = SCREEN_MASK_MODE_NONE;
        window = view;
        hide();
    }

    public void showNet()
    {
        maskArea.setImageDrawable(mAttachActivity.getResources().getDrawable(R.drawable.img_media_net_mask));
        maskArea.setVisibility(View.VISIBLE);
        window.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_NET;
    }

    public void showGray1()
    {
        maskArea.setImageDrawable(mAttachActivity.getResources().getDrawable(R.drawable.shape_media_gray_1));
        maskArea.setVisibility(View.VISIBLE);
        window.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_GRAY_1;
    }

    public void showGray2()
    {
        maskArea.setImageDrawable(mAttachActivity.getResources().getDrawable(R.drawable.shape_media_gray_2));
        maskArea.setVisibility(View.VISIBLE);
        window.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_GRAY_2;
    }

    public void showGray3()
    {
        maskArea.setImageDrawable(mAttachActivity.getResources().getDrawable(R.drawable.shape_media_gray_3));
        maskArea.setVisibility(View.VISIBLE);
        window.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_GRAY_3;
    }

    public void showPaintWindow()
    {
        maskArea.setVisibility(View.INVISIBLE);
        window.setVisibility(View.VISIBLE);
        window.bringToFront();
        maskState = SCREEN_MASK_MODE_WINDOW;
    }

    public void hide()
    {
        maskArea.setVisibility(View.INVISIBLE);
        window.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_NONE;
    }

    public int getMaskState()
    {
        return maskState;
    }
}

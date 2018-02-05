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
    public static final int SCREEN_MASK_MODE_GRAY = 2;
    public static final int SCREEN_MASK_MODE_WINDOW = 3;

    private ImageView maskArea;
    private int maskState;
    private Activity mAttachActivity;

    public MaskControl(Activity attach, ImageView mask) {
        maskArea = mask;
        maskArea.setScaleType(ImageView.ScaleType.FIT_XY);
        mAttachActivity = attach;
        maskState = SCREEN_MASK_MODE_NONE;
        hide();
    }

    public void showNet()
    {
        maskArea.setImageDrawable(mAttachActivity.getResources().getDrawable(R.drawable.img_media_net_mask));
        maskArea.setVisibility(View.VISIBLE);
        maskState = SCREEN_MASK_MODE_NET;
    }

    public void showGray()
    {
        maskArea.setImageDrawable(mAttachActivity.getResources().getDrawable(R.drawable.img_media_gray_mask3));
        maskArea.setVisibility(View.VISIBLE);
        maskState = SCREEN_MASK_MODE_GRAY;
    }

    public void hide()
    {
        maskArea.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_NONE;
    }

    public int getMaskState()
    {
        return maskState;
    }
}

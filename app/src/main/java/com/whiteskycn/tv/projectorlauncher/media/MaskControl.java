package com.whiteskycn.tv.projectorlauncher.media;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.tv.projectorlauncher.media.polygonWindow.PolygonWindow;
import com.whiteskycn.tv.projectorlauncher.utils.FileUtil;

/**
 * Created by jeff on 18-2-5.
 */

public class MaskControl implements PolygonWindow.OnPolygonWindowEventListener {
    private final String TAG = this.getClass().getSimpleName();

    private static final String PAINT_SAVE_NAME = "polygon.png";

    public static final int SCREEN_MASK_MODE_NONE = 0;
    public static final int SCREEN_MASK_MODE_NET = 1;
    public static final int SCREEN_MASK_MODE_GRAY_1 = 2;
    public static final int SCREEN_MASK_MODE_GRAY_2 = 3;
    public static final int SCREEN_MASK_MODE_GRAY_3 = 4;
    public static final int SCREEN_MASK_MODE_PAINT = 5;
    public static final int SCREEN_MASK_MODE_POLYGON = 6;

    private ImageView maskArea;
    private PolygonWindow paintWindow;
    private int maskState;
    private int lastMaskStateBeforePaint;
    private Activity mAttachActivity;
    private ProgressDialog waitBmpDialog;

    public MaskControl(Activity attach, ImageView mask, PolygonWindow window) {
        maskArea = mask;
        maskArea.setScaleType(ImageView.ScaleType.FIT_XY);
        mAttachActivity = attach;
        maskState = SCREEN_MASK_MODE_NONE;
        lastMaskStateBeforePaint = SCREEN_MASK_MODE_NONE;
        paintWindow = window;
        paintWindow.setOnPolygonWindowEventListener(this);
        showDefaultMask();
    }

    // OnPolygonWindowEventListener +++
    @Override
    public void onGenerateBegin() {
        waitBmpDialog = ProgressDialog.show(mAttachActivity,
                mAttachActivity.getResources().getString(R.string.str_media_dialog_mask_generate_title),
                mAttachActivity.getResources().getString(R.string.str_media_dialog_mask_generate_message));
    }


    @Override
    public void onGenerateDone(Bitmap bmp) {
        if (bmp==null) {
            FileUtil.deleteFileFromData(mAttachActivity,PAINT_SAVE_NAME);
        } else {
            FileUtil.saveBitmapToData(mAttachActivity, bmp, PAINT_SAVE_NAME, 100);
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
            System.gc();
        }

        if (waitBmpDialog!=null) {
            waitBmpDialog.dismiss();
        }

        showDefaultMask();
    }

    @Override
    public void onCancel() {
        showMask(lastMaskStateBeforePaint);
    }
    // OnPolygonWindowEventListener ---

    public void showMask(int maskType) {
        switch (maskType) {
            case SCREEN_MASK_MODE_NONE:
                showNoneMask();
                break;
            case SCREEN_MASK_MODE_NET:
                showNetMask();
                break;
            case SCREEN_MASK_MODE_GRAY_1:
                showGray1Mask();
                break;
            case SCREEN_MASK_MODE_GRAY_2:
                showGray2Mask();
                break;
            case SCREEN_MASK_MODE_GRAY_3:
                showGray3Mask();
                break;
            case SCREEN_MASK_MODE_PAINT:
                showPaintWindow();
                break;
            case SCREEN_MASK_MODE_POLYGON:
                showPolygonMask();
                break;
            default:
                Log.e(TAG,"unknown maskType!");
                break;
        }
    }

    public boolean showPolygonMask()
    {
        if (!FileUtil.fileExistInData(mAttachActivity,PAINT_SAVE_NAME)){
            return false;
        }

        Bitmap bmp = FileUtil.getBitmapFromData(mAttachActivity,PAINT_SAVE_NAME);
        if (bmp==null) {
            return false;
        }

        maskArea.setImageBitmap(bmp);

        maskArea.setVisibility(View.VISIBLE);
        paintWindow.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_POLYGON;
        return true;
    }

    public void showNetMask()
    {
        maskArea.setImageDrawable(mAttachActivity.getResources().getDrawable(R.drawable.img_media_net_mask));
        maskArea.setVisibility(View.VISIBLE);
        paintWindow.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_NET;
    }

    public void showGray1Mask()
    {
        maskArea.setImageDrawable(mAttachActivity.getResources().getDrawable(R.drawable.shape_media_gray_1));
        maskArea.setVisibility(View.VISIBLE);
        paintWindow.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_GRAY_1;
    }

    public void showGray2Mask()
    {
        maskArea.setImageDrawable(mAttachActivity.getResources().getDrawable(R.drawable.shape_media_gray_2));
        maskArea.setVisibility(View.VISIBLE);
        paintWindow.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_GRAY_2;
    }

    public void showGray3Mask()
    {
        maskArea.setImageDrawable(mAttachActivity.getResources().getDrawable(R.drawable.shape_media_gray_3));
        maskArea.setVisibility(View.VISIBLE);
        paintWindow.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_GRAY_3;
    }

    public void showPaintWindow()
    {
        //maskArea.setVisibility(View.INVISIBLE);
        paintWindow.setVisibility(View.VISIBLE);
        lastMaskStateBeforePaint = maskState;
        maskState = SCREEN_MASK_MODE_PAINT;
    }

    public void showPaintWindow(Point controlBarPosition)
    {
        paintWindow.adjustPaintControlBarPosition(controlBarPosition);
        showPaintWindow();
    }

    public void showNoneMask()
    {
        maskArea.setVisibility(View.INVISIBLE);
        paintWindow.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_NONE;
    }

    public void showDefaultMask() {
        if (!showPolygonMask()) {
            showNoneMask();
        }
    }


    public int getMaskState()
    {
        return maskState;
    }
}

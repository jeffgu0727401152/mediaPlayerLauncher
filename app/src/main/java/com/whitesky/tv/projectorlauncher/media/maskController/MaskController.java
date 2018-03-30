package com.whitesky.tv.projectorlauncher.media.maskController;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.common.Contants;
import com.whitesky.tv.projectorlauncher.utils.FileUtil;
import com.whitesky.tv.projectorlauncher.utils.SharedPreferencesUtil;

import static com.whitesky.tv.projectorlauncher.common.Contants.CONFIG_SHOW_MASK;

/**
 * Created by jeff on 18-2-5.
 */

public class MaskController extends FrameLayout implements View.OnClickListener, PolygonWindow.OnPolygonWindowEventListener {
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
    private Button netMaskBtn;
    private Button grayMaskBtn;

    private int maskState;
    private int lastMaskStateBeforePaint;
    private Context attachedContext;
    private ProgressDialog waitBmpDialog;

    public MaskController(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.mask_controller, this, true);
        maskArea = (ImageView) findViewById(R.id.iv_mask);
        maskArea.setScaleType(ImageView.ScaleType.FIT_XY);
        paintWindow = (PolygonWindow) findViewById(R.id.iv_polygon_paint_window);
        paintWindow.setOnPolygonWindowEventListener(this);
        // 使用小bmp图,节约内存
        paintWindow.setPolygonBmpSize(960,540);

        netMaskBtn = (Button) findViewById(R.id.btn_netMask);
        grayMaskBtn = (Button) findViewById(R.id.btn_grayMask);
        grayMaskBtn.setOnClickListener(this);
        netMaskBtn.setOnClickListener(this);

        attachedContext = context;
        maskState = SCREEN_MASK_MODE_NONE;
        lastMaskStateBeforePaint = SCREEN_MASK_MODE_NONE;

        showDefaultMask();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_netMask:
                if (maskState != MaskController.SCREEN_MASK_MODE_NET) {
                    showNetMask();
                } else {
                    showDefaultMask();
                }
                break;

            case R.id.btn_grayMask:
                if (maskState != MaskController.SCREEN_MASK_MODE_GRAY_1
                        && maskState != MaskController.SCREEN_MASK_MODE_GRAY_2
                        && maskState != MaskController.SCREEN_MASK_MODE_GRAY_3) {
                    showGray1Mask();
                } else {
                    switch (maskState) {
                        case MaskController.SCREEN_MASK_MODE_GRAY_1:
                            showGray2Mask();
                            break;

                        case MaskController.SCREEN_MASK_MODE_GRAY_2:
                            showGray3Mask();
                            break;

                        case MaskController.SCREEN_MASK_MODE_GRAY_3:
                            showDefaultMask();
                            break;

                        default:
                            break;
                    }
                }
                break;

            default:
                break;
        }
    }

    // OnPolygonWindowEventListener +++
    @Override
    public void onGenerateBegin() {
        waitBmpDialog = ProgressDialog.show(attachedContext,
                attachedContext.getResources().getString(R.string.str_media_dialog_mask_generate_title),
                attachedContext.getResources().getString(R.string.str_media_dialog_mask_generate_message));
    }


    @Override
    public void onGenerateDone(Bitmap bmp) {
        if (bmp==null) {
            FileUtil.deleteFileFromData((Activity) attachedContext,PAINT_SAVE_NAME);
        } else {
            FileUtil.saveBitmapToData((Activity) attachedContext, bmp, PAINT_SAVE_NAME, 100);
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
        if (lastMaskStateBeforePaint == SCREEN_MASK_MODE_NONE) {
            // 远程将polygon mask关闭以后,设计成只要点击出paint window就保存设置为true
            // 所以在paint window cancel后,使用showDefaultMask()检查设置并show出polygon mask
            showDefaultMask();
        } else {
            showMask(lastMaskStateBeforePaint);
        }
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
        if (!FileUtil.fileExistInData( (Activity) attachedContext, PAINT_SAVE_NAME)){
            return false;
        }

        Bitmap bmp = FileUtil.getBitmapFromData( (Activity) attachedContext, PAINT_SAVE_NAME);
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
        maskArea.setImageDrawable(attachedContext.getResources().getDrawable(R.drawable.img_media_net_mask));
        maskArea.setVisibility(View.VISIBLE);
        paintWindow.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_NET;
    }

    public void showGray1Mask()
    {
        maskArea.setImageDrawable(attachedContext.getResources().getDrawable(R.drawable.shape_media_gray_1));
        maskArea.setVisibility(View.VISIBLE);
        paintWindow.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_GRAY_1;
    }

    public void showGray2Mask()
    {
        maskArea.setImageDrawable(attachedContext.getResources().getDrawable(R.drawable.shape_media_gray_2));
        maskArea.setVisibility(View.VISIBLE);
        paintWindow.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_GRAY_2;
    }

    public void showGray3Mask()
    {
        maskArea.setImageDrawable(attachedContext.getResources().getDrawable(R.drawable.shape_media_gray_3));
        maskArea.setVisibility(View.VISIBLE);
        paintWindow.setVisibility(View.INVISIBLE);
        maskState = SCREEN_MASK_MODE_GRAY_3;
    }

    public void showPaintWindow()
    {
        // 进入多边形编辑功能一次,则打开显示默认mask功能
        SharedPreferencesUtil config = new SharedPreferencesUtil(attachedContext, Contants.PREF_CONFIG);
        config.putBoolean(CONFIG_SHOW_MASK,true);

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
        SharedPreferencesUtil config = new SharedPreferencesUtil(attachedContext, Contants.PREF_CONFIG);
        if (config.getBoolean(CONFIG_SHOW_MASK, true)) {
            Log.i(TAG,"default will show polygon mask");
            if (!showPolygonMask()) {
                showNoneMask();
            } else {
                lastMaskStateBeforePaint = SCREEN_MASK_MODE_NONE;
            }
        } else {
            showNoneMask();
        }
    }

    public void updateControlButtonVisible(boolean show) {
        grayMaskBtn.setVisibility(show?View.VISIBLE:View.INVISIBLE);
        netMaskBtn.setVisibility(show?View.VISIBLE:View.INVISIBLE);

        // 显示控制按钮的时候，一定是由全屏变回半屏的时候
        // 调用onCancel设置取消正在进行中的绘制
        if (show) {
            onCancel();
        }
    }
}

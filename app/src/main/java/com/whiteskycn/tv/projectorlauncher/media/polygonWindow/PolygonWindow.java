package com.whiteskycn.tv.projectorlauncher.media.polygonWindow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.MathUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;

import com.whiteskycn.tv.projectorlauncher.R;
import com.whiteskycn.wsd.android.NativeMask;

/**
 * Created by jeff on 18-2-22.
 */

public class PolygonWindow extends FrameLayout implements View.OnClickListener, PaintPolygonView.OnPointDownListener {
    private final String TAG = this.getClass().getSimpleName();

    private static final int DEFAULT_INSIDE_COLOR = 0x00000000;
    private static final int DEFAULT_OUTSIDE_COLOR = 0xff000000;

    private GridLayout paintControlBar;
    private Button cancelBtn;
    private Button undoBtn;
    private Button nextBtn;
    private Button doneBtn;
    private PaintPolygonView paintView;

    private OnPolygonWindowEventListener eventListener;

    private int insideColor;
    private int outsideColor;

    private int bmpWidth;
    private int bmpHeight;

    private int displayWidth;
    private int displayHeight;

    private boolean showControlBarWithVisible = true;

    public interface OnPolygonWindowEventListener {
        public void onGenerateBegin();
        public void onGenerateDone(Bitmap bmp);
        public void onCancel();
    }

    public void setOnPolygonWindowEventListener(OnPolygonWindowEventListener onPolygonWindowEventListener) {
        this.eventListener = onPolygonWindowEventListener;
    }

    private Handler generateDoneHandler =new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            paintView.clearAllPolygonPoints();
            updateBtnActiveState();
            if (eventListener!=null) {
                eventListener.onGenerateDone((Bitmap)msg.obj);
            }
        }
    };

    public PolygonWindow(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.polygon_window, this, true);
        paintView = (PaintPolygonView) findViewById(R.id.iv_paint_view);
        paintControlBar = (GridLayout) findViewById(R.id.ll_paint_controlBar);
        cancelBtn = (Button) findViewById(R.id.btn_paint_cancel);
        undoBtn = (Button) findViewById(R.id.btn_paint_undo);
        nextBtn = (Button) findViewById(R.id.btn_paint_next);
        doneBtn = (Button) findViewById(R.id.btn_paint_done);

        cancelBtn.setOnClickListener(this);
        undoBtn.setOnClickListener(this);
        nextBtn.setOnClickListener(this);
        doneBtn.setOnClickListener(this);

        eventListener = null;
        paintView.setOnPointDownListener(this);

        insideColor = DEFAULT_INSIDE_COLOR;
        outsideColor = DEFAULT_OUTSIDE_COLOR;

        WindowManager manager =  (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(metrics);
        bmpWidth = displayWidth = metrics.widthPixels;
        bmpHeight = displayHeight = metrics.heightPixels;
        Log.d(TAG,"display size:" + displayWidth + "*" + displayHeight);
    }

    public void updateBtnActiveState() {
        undoBtn.setEnabled(paintView.hasPointToDelete());
        nextBtn.setEnabled(paintView.couldDrawNextPolygon());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_paint_cancel:
                paintView.clearAllPolygonPoints();
                updateBtnActiveState();
                if (eventListener!=null) {
                    eventListener.onCancel();
                }
                break;

            case R.id.btn_paint_done:

                if (eventListener!=null) {
                    eventListener.onGenerateBegin();
                }

                paintView.drawNextPolygonPoints();
                final int polygonCountArray[] = paintView.getPolygonPointsCount();
                int totalPointCount = 0;

                if (polygonCountArray!=null) {
                    for (int i:polygonCountArray){
                        totalPointCount += i;
                    }
                }

                if (totalPointCount>2){
                    final int xPointArray[] = new int[totalPointCount];
                    final int yPointArray[] = new int[totalPointCount];

                    paintView.getPolygonPointsXY(xPointArray,yPointArray);

                    new Thread(){
                        public void run(){

                            Bitmap bmp = generateBitmap(polygonCountArray,xPointArray,yPointArray);

                            Message msg = generateDoneHandler.obtainMessage();
                            msg.what = 0;
                            msg.obj = bmp;
                            generateDoneHandler.sendMessage(msg);
                        }
                    }.start();
                } else {
                    // 用户没有画点,发送null
                    Message msg = generateDoneHandler.obtainMessage();
                    msg.what = 0;
                    msg.obj = null;
                    generateDoneHandler.sendMessage(msg);
                }
                break;

            case R.id.btn_paint_undo:
                if (paintView.hasPointToDelete()) {
                    paintView.deleteLastPoint();
                    updateBtnActiveState();
                }
                break;

            case R.id.btn_paint_next:
                if (paintView.couldDrawNextPolygon()) {
                    paintView.drawNextPolygonPoints();
                    updateBtnActiveState();
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void setVisibility(int visibility)
    {
        super.setVisibility(visibility);
        paintView.setVisibility(visibility);

        if (showControlBarWithVisible) {
            showControlBar(visibility);
        }

        updateBtnActiveState();

        if(visibility==View.VISIBLE) {
            paintView.bringToFront();
            paintControlBar.bringToFront();
        }
    }

    @Override
    public void onPointDown(int idx, Point p) {
        if (idx==1) {
            adjustPaintControlBarPosition(p);
            showControlBar(VISIBLE);
        }

        updateBtnActiveState();
    }

    // 抠图内部的颜色,一般使用透明色
    public void setInsideColor(@ColorInt int color) {
        insideColor = color;
    }

    // 抠图外部的颜色,一般使用不透明色
    public void setOutsideColor(@ColorInt int color) {
        outsideColor = color;
    }

    // 设置需要生成的bmp图的大小,1920*1080的屏幕,也可以使用960*540的设置,节约内存,但是会有锯齿
    public void setBmpSize(int width, int height) {
        bmpWidth = width;
        bmpHeight = height;
    }

    public PaintPolygonView getPaintView() {
        return paintView;
    }

    public void showControlBarWithVisible(boolean isNeed) {
        showControlBarWithVisible = isNeed;
    }

    public void showControlBar(int visibility) {
        paintControlBar.setVisibility(visibility);
    }

    public void adjustPaintControlBarPosition(Point position) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) paintControlBar.getLayoutParams();

        int controlBarWidth = paintControlBar.getWidth();
        int controlBarHeight = paintControlBar.getHeight();

        params.leftMargin = (int)MathUtils.min(displayWidth - controlBarWidth, position.x);
        params.topMargin = (int)MathUtils.min(displayHeight - controlBarHeight, position.y);

        paintControlBar.setLayoutParams(params);
    }

    // 参数中传入的都是原始鼠标点击屏幕的位置,如果设置bmp图片的宽高比屏幕小,则在此处会进行比例缩小
    private Bitmap generateBitmap(int[] polygonCountArray,int[] xPointArray,int [] yPointArray)
    {
        int[] polygonBufferArray = new int[bmpWidth * bmpHeight];

        if (displayHeight != bmpHeight) {
            float scale = (float)(bmpHeight)/(float)displayHeight;
            for (int i = 0; i<yPointArray.length; i++) {
                yPointArray[i] = (int)((float)(yPointArray[i])*scale);
            }
        }

        if (displayWidth != bmpWidth) {
            float scale = (float)(bmpWidth)/(float)displayWidth;
            for (int i = 0; i<xPointArray.length; i++) {
                xPointArray[i] = (int)((float)(xPointArray[i])*scale);
            }
        }

        NativeMask.createPolygonBuffer(
                polygonBufferArray,
                bmpWidth,
                bmpHeight,
                polygonCountArray,
                xPointArray,
                yPointArray,
                insideColor,
                outsideColor);

        Bitmap bitmap = Bitmap.createBitmap(
                polygonBufferArray,
                bmpWidth,
                bmpHeight,
                Bitmap.Config.ARGB_8888);

        return bitmap;
    }
}

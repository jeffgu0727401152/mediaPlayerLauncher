package com.whitesky.tv.projectorlauncher.media.maskController;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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

import com.whitesky.tv.projectorlauncher.R;

import java.util.ArrayList;
import java.util.List;

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
        paintView = findViewById(R.id.iv_paint_view);
        paintControlBar = findViewById(R.id.ll_paint_controlBar);
        cancelBtn = findViewById(R.id.btn_paint_cancel);
        undoBtn = findViewById(R.id.btn_paint_undo);
        nextBtn = findViewById(R.id.btn_paint_next);
        doneBtn = findViewById(R.id.btn_paint_done);

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
                if (eventListener != null) {
                    eventListener.onCancel();
                }
                break;

            case R.id.btn_paint_done:

                if (eventListener != null) {
                    eventListener.onGenerateBegin();
                }

                paintView.drawNextPolygonPoints();

                final List<List<Point>> target = paintView.getPolygonPoints();

                if (target.size() >= 1) {

                    new Thread() {
                        public void run() {
                            Bitmap map = generateBitmap(target);
                            Message msg = generateDoneHandler.obtainMessage();
                            msg.what = 0;
                            msg.obj = map;
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
    public void setPolygonInsideColor(@ColorInt int color) {
        insideColor = color;
    }

    // 抠图外部的颜色,一般使用不透明色
    public void setPolygonOutsideColor(@ColorInt int color) {
        outsideColor = color;
    }

    // 设置需要生成的bmp图的大小,1920*1080的屏幕,也可以使用960*540的设置,节约内存,但是会有锯齿
    public void setPolygonBmpSize(int width, int height) {
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
    private Bitmap generateBitmap(List<List<Point>> target)
    {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(outsideColor);
        paint.setStyle(Paint.Style.FILL);

        Bitmap bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(insideColor);

        Canvas bmpCanvas = new Canvas(bitmap);
        List<Path> polygonPaths = new ArrayList<Path>();

        if (displayHeight != bmpHeight || displayWidth != bmpWidth) {
            float yScale = (float) (bmpHeight) / (float) displayHeight;
            float xScale = (float) (bmpWidth) / (float) displayWidth;
            for (int i = 0; i < target.size(); i++) {
                for (int j = 0; j < target.get(i).size(); j++) {
                    target.get(i).get(j).x = (int) ((float) (target.get(i).get(j).x) * xScale);
                    target.get(i).get(j).y = (int) ((float) (target.get(i).get(j).y) * yScale);
                }
            }
        }

        for (int i = 0; i < target.size(); i++) {
            Path tmp = new Path();
            for (int j = 0; j < target.get(i).size(); j++) {
                if (j == 0) {
                    tmp.moveTo(target.get(i).get(j).x, target.get(i).get(j).y);
                } else {
                    tmp.lineTo(target.get(i).get(j).x, target.get(i).get(j).y);
                }
            }
            tmp.close();
            polygonPaths.add(tmp);
        }

        polygonPaths.get(0).setFillType(Path.FillType.INVERSE_EVEN_ODD);
        for (int i = 1; i < polygonPaths.size(); i++) {
            polygonPaths.get(0).op(polygonPaths.get(i),Path.Op.DIFFERENCE);
        }

        bmpCanvas.drawPath(polygonPaths.get(0), paint);

        return bitmap;
    }
}

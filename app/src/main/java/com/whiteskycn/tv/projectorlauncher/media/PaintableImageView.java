package com.whiteskycn.tv.projectorlauncher.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeff on 18-2-6.
 */

public class PaintableImageView extends AppCompatImageView {

    private List<PointF> PointList; // 点列表
    private PointF currentPoint; // 当前线条

    private Paint normalPaint = new Paint();
    private static final float NORMAL_LINE_STROKE = 5.0f;

    private Drawable drawable;
    private Bitmap bitmap;

    {
        PointList = new ArrayList<>();
        normalPaint.setColor(Color.RED);
        normalPaint.setStrokeWidth(NORMAL_LINE_STROKE);
    }

    public PaintableImageView(Context context) {
        super(context);
    }

    public PaintableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PaintableImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float xPos = event.getX();
        float yPos = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPoint = new PointF();
                currentPoint.set(xPos,yPos);
                PointList.add(currentPoint);
                invalidate();
                return true; // return true消费掉ACTION_DOWN事件，否则不会触发ACTION_UP
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
                break;
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (PointF point : PointList) {
            drawPoint(canvas, point, normalPaint);
        }

        drawNormalLine(canvas, PointList, normalPaint);
    }

    private void drawPoint(Canvas canvas, PointF point, Paint p)
    {
        canvas.drawCircle(point.x, point.y, 5, p);// 画圆，圆心的坐标(cx,cy)和半径radius
    }


    /**
     * 绘制普通线条
     * @param canvas
     */
    private void drawNormalLine(Canvas canvas, List<PointF> pList , Paint p) {
        if (pList.size() <= 1) {
            return;
        }

        for (int i = 0; i < pList.size() - 1; i++) {
            PointF startPoint  = pList.get(i);
            PointF endPoint  = pList.get(i + 1);

            canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, p);
        }
    }

    /**
     * 删除最后添加的线
     */
    public void withDrawLastLine() {
        if (PointList.size() > 0) {
            PointList.remove(PointList.size() - 1);
            invalidate();
        }
    }

    /**
     * 判断是否可以继续撤销
     * @return
     */
    public boolean canStillWithdraw() {
        return PointList.size() > 0;
    }
}

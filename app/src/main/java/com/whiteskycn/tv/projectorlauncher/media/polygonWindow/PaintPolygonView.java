package com.whiteskycn.tv.projectorlauncher.media.polygonWindow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeff on 18-2-6.
 */

public class PaintPolygonView extends AppCompatImageView {

    private final String TAG = this.getClass().getSimpleName();

    private static final float DEFAULT_LINE_STROKE = 2.0f;
    private static final int DEFAULT_LINE_COLOR = Color.RED;

    private static final float DEFAULT_POINT_STROKE = 2.0f;
    private static final int DEFAULT_POINT_COLOR = Color.GREEN;
    private static final int DEFAULT_POINT_RADIUS = 3;

    private List<List<Point>> totalPolygonPoints;  // 所有的点
    private List<Point> currentPolygonPoints;      // 当前操作的点的列表
    private Point currentPoint;                    // 当前鼠标点击的点

    private Paint drawLinePaint = new Paint();
    private Paint drawPointPaint = new Paint();

    private int PointRadius;

    private OnFirstPointListener firstPointListener;

    {
        totalPolygonPoints = new ArrayList<List<Point>>();
        currentPolygonPoints = new ArrayList<>();

        drawLinePaint.setColor(DEFAULT_LINE_COLOR);
        drawLinePaint.setStrokeWidth(DEFAULT_LINE_STROKE);

        drawPointPaint.setColor(DEFAULT_POINT_COLOR);
        drawPointPaint.setStrokeWidth(DEFAULT_POINT_STROKE);

        PointRadius = DEFAULT_POINT_RADIUS;

        firstPointListener = null;
    }

    public PaintPolygonView(Context context) {
        super(context);
    }

    public PaintPolygonView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PaintPolygonView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public interface OnFirstPointListener {
        public void onFirstPointDown(Point p);
    }

    public void SetOnFirstPointListener(OnFirstPointListener onFirstPointListener) {
        this.firstPointListener = onFirstPointListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float xPos = event.getX();
        float yPos = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPoint = new Point();
                currentPoint.set((int)(xPos + 0.5f),(int)(yPos + 0.5f));                //四舍五入

                if (firstPointListener!=null && currentPolygonPoints.size()==0) {
                    firstPointListener.onFirstPointDown(currentPoint);
                }

                currentPolygonPoints.add(currentPoint);
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

        for (List<Point> pointGroup : totalPolygonPoints) {
            for (Point point : pointGroup) {
                drawPoint(canvas, point, drawPointPaint);
            }

            drawLine(canvas, pointGroup, true, drawLinePaint);
        }

        for (Point point : currentPolygonPoints) {
            drawPoint(canvas, point, drawPointPaint);
        }

        drawLine(canvas, currentPolygonPoints, false, drawLinePaint);
    }

    /**
     * 设置画点用的画笔
     * @inparam paint 画笔
     */
    public void setPointPaint(@NonNull Paint paint) {
        drawPointPaint = paint;
    }

    /**
     * 设置画线用的画笔
     * @inparam paint 画笔
     */
    public void setLinePaint(@NonNull Paint paint) {
        drawLinePaint = paint;
    }

    /**
     * 设置画点的半径
     * @inparam radius 点的半径
     */
    public void setPointRadius(int radius) {
        PointRadius = radius;
    }

    /**
     * 后面开始画的点为下一个多边形
     */
    public void drawNextPolygonPoints()
    {
        if (couldDrawNextPolygon()) {
            totalPolygonPoints.add(new ArrayList<>(currentPolygonPoints));
            currentPolygonPoints.clear();
            invalidate();
        }
    }

    /**
     * 清除所有的点
     */
    public void clearAllPolygonPoints()
    {
        totalPolygonPoints.clear();
        currentPolygonPoints.clear();
        invalidate();
    }

    /**
     * 删除最后添加的点
     */
    public void deleteLastPoint() {
        if (currentPolygonPoints.size() > 0) {
            currentPolygonPoints.remove(currentPolygonPoints.size() - 1);
            invalidate();
        } else {
            if (totalPolygonPoints.size() > 0) {
                currentPolygonPoints.addAll(totalPolygonPoints.get(totalPolygonPoints.size()-1));
                totalPolygonPoints.remove(totalPolygonPoints.size()-1);
                invalidate();
            } else {
                // 一个点也没有了,神码都不用做了
            }
        }
    }

    /**
     * 判断是否可以继续撤销
     * @return
     */
    public boolean stillHasPointToDelete() {
        if (currentPolygonPoints.size() > 0 || totalPolygonPoints.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断是否可以继续去画下一个
     * @return
     */
    public boolean couldDrawNextPolygon() {
        return currentPolygonPoints.size() > 2;
    }

    /**
     * 获取目前所有的点 所组成的 多变形的数量 以及 各多边形的点数
     * @return 描述多边形的数组,可能为null
     */
    public int[] getPolygonPointsCount() {
        int donePolygonCount = totalPolygonPoints.size();
        int currentPolygonCount = (currentPolygonPoints.size()>0) ? 1:0;


        if (donePolygonCount > 0 || currentPolygonCount > 0) {

            int[] retCount = new int[donePolygonCount + currentPolygonCount];

            for (int i=0; i<donePolygonCount; i++) {
                retCount[i] = totalPolygonPoints.get(i).size();
            }

            if (currentPolygonCount > 0) {
                retCount[donePolygonCount + currentPolygonCount - 1] = currentPolygonPoints.size();
            }

            return retCount;

        } else {
            Log.e(TAG,"nothing to return!");
            return null;

        }
    }

    /**
     * 获取目前所有的点的x与y
     * @outparam xArray 所有点的横坐标
     * @outparam yArray 所有点的纵坐标
     * @return
     */
    public void getPolygonPointsXY(int[] xArray, int[] yArray) {
        int donePolygonCount = totalPolygonPoints.size();
        int currentPolygonCount = (currentPolygonPoints.size()>0) ? 1:0;
        int retArrayLength = 0;
        int pointIdx = 0;

        if (xArray!=null && yArray!=null && xArray.length==yArray.length)
        {
            retArrayLength = xArray.length;
        } else {
            Log.e(TAG,"param array error!");
            return;
        }

        if (donePolygonCount > 0 || currentPolygonCount > 0) {

            for (int i=0; i<donePolygonCount; i++) {

                for (Point p:totalPolygonPoints.get(i)) {

                    if (retArrayLength <= pointIdx) {
                        Log.e(TAG,"param array to short!");
                        return;
                    }

                    xArray[pointIdx] = p.x;
                    yArray[pointIdx] = p.y;
                    pointIdx++;
                }
            }

            if (currentPolygonCount > 0) {

                for (Point p:currentPolygonPoints) {

                    if (retArrayLength <= pointIdx) {
                        Log.e(TAG,"param array to short!");
                        return;
                    }

                    xArray[pointIdx] = p.x;
                    yArray[pointIdx] = p.y;
                    pointIdx++;
                }
            }
            return;

        } else {
            Log.e(TAG,"nothing to return!");
            return;
        }
    }

    private void drawPoint(Canvas canvas, Point point, Paint p)
    {
        canvas.drawCircle(point.x, point.y, PointRadius, p);// 画圆，圆心的坐标(cx,cy)和半径radius
    }


    /**
     * 绘制普通线条
     * @param canvas
     */
    private void drawLine(Canvas canvas, List<Point> pList , boolean drawCloseLine, Paint p) {
        if (pList.size() <= 1) {
            return;
        }

        for (int i = 0; i < pList.size() - 1; i++) {
            Point startPoint  = pList.get(i);
            Point endPoint  = pList.get(i + 1);

            canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, p);
        }

        if (drawCloseLine) {
            // 画上最后一条线,把这些点画成一个封闭的多边形
            Point startPoint  = pList.get(0);
            Point endPoint  = pList.get(pList.size() - 1);
            canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, p);
        }
    }

}

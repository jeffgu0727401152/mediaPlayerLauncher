package com.whitesky.sdk.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.whitesky.sdk.R;
import com.whitesky.sdk.utils.BitmapUtils;

/**
 * Created by xiaoxuan on 2017/7/4.
 */

public class TvTrapezoidView extends ImageView
{
    private Context mContext;
    
    private int width = 0;
    
    private int height = 0;
    
    private int translucent;// 半透明色 蒙版用
    
    public TvTrapezoidView(Context context)
    {
        super(context);
        mContext = context;
        translucent = getResources().getColor(R.color.translucent);
    }
    
    public TvTrapezoidView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mContext = context;
    }
    
    public TvTrapezoidView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        mContext = context;
        translucent = getResources().getColor(R.color.translucent);
    }
    
    @Override
    protected void onDraw(Canvas canvas)
    {
        Drawable drawable = getDrawable();
        if (drawable == null)
        {
            return;
        }
        if (getWidth() == 0 || getHeight() == 0)
        {
            return;
        }
        this.measure(0, 0);
        if (drawable.getClass() == NinePatchDrawable.class)
            return;
        Bitmap b = ((BitmapDrawable)drawable).getBitmap();
        Bitmap bitmap = b.copy(Bitmap.Config.ARGB_8888, true);
        // 保证重新读取图片后不会因为图片大小而改变控件宽、高的大小（针对宽、高为wrap_content布局的imageview，但会导致margin无效）
        // if (defaultWidth != 0 && defaultHeight != 0) {
        // LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        // defaultWidth, defaultHeight);
        // setLayoutParams(params);
        // }
        XYPoint leftTop = new XYPoint(0, 0);
        XYPoint rightTop = new XYPoint(width, 0);
        XYPoint leftBottom = new XYPoint(width, height * 2 / 3);
        XYPoint rightBottom = new XYPoint(0, height);
        bitmap = BitmapUtils.blurImageAmeliorate(bitmap);
        Bitmap roundBitmap = getCroppedTrapezoidBitmap(bitmap, leftTop, rightTop, leftBottom, rightBottom);
        
        // 模糊
        RenderScript rs = RenderScript.create(mContext);
        Allocation overlayAlloc = Allocation.createFromBitmap(rs, roundBitmap);
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, overlayAlloc.getElement());
        blur.setInput(overlayAlloc);
        blur.setRadius(8);
        blur.forEach(overlayAlloc);
        overlayAlloc.copyTo(roundBitmap);
        canvas.drawBitmap(roundBitmap, 0, 0, null);
        rs.destroy();
        // 半透明蒙版
        Paint paint = new Paint();
        paint.setColor(translucent);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        Path path = new Path();
        path.reset();
        path.moveTo(0, 0);
        path.lineTo(width, 0);
        path.lineTo(width, height * 2 / 3);
        path.lineTo(0, height);
        path.lineTo(0, 0);
        canvas.drawPath(path, paint);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = getWidth();
        height = getHeight();
    }
    
    /**
     * 获取裁剪后的圆形图片
     */
    public Bitmap getCroppedTrapezoidBitmap(Bitmap bmp, XYPoint leftTop, XYPoint rightTop, XYPoint leftBottom,
        XYPoint rightBottom)
    {
        Bitmap scaledSrcBmp;
        // 为了防止宽高不相等，造成圆形图片变形，因此截取长方形中处于中间位置最大的正方形图片
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();
        int squareWidth = 0, squareHeight = 0;
        int x = 0, y = 0;
        Bitmap squareBitmap;
        if (bmpHeight > bmpWidth)
        {// 高大于宽
            squareWidth = squareHeight = bmpWidth;
            x = 0;
            y = (bmpHeight - bmpWidth) / 2;
            // 截取正方形图片
            squareBitmap = Bitmap.createBitmap(bmp, x, y, squareWidth, squareHeight);
        }
        else if (bmpHeight < bmpWidth)
        {// 宽大于高
            squareWidth = squareHeight = bmpHeight;
            x = (bmpWidth - bmpHeight) / 2;
            y = 0;
            squareBitmap = Bitmap.createBitmap(bmp, x, y, squareWidth, squareHeight);
        }
        else
        {
            squareBitmap = bmp;
        }
        scaledSrcBmp = Bitmap.createScaledBitmap(squareBitmap, width, width, true);
        // scaledSrcBmp = Bitmap.createScaledBitmap(squareBitmap, width, height, true);
        Bitmap output = Bitmap.createBitmap(scaledSrcBmp.getWidth(), scaledSrcBmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, scaledSrcBmp.getWidth(), scaledSrcBmp.getHeight());
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        // canvas.drawCircle(scaledSrcBmp.getWidth() / 2, scaledSrcBmp.getHeight() / 2, scaledSrcBmp.getWidth() / 2,
        // paint);
        Path path = new Path();
        path.reset();
        path.moveTo(leftTop.x, leftTop.y); // 左顶点 也即起始点
        path.lineTo(rightTop.x, rightTop.y); // 右顶点
        path.lineTo(leftBottom.x, leftBottom.y); // 右底部
        path.lineTo(rightBottom.x, rightBottom.y); // 左底部
        path.lineTo(leftTop.x, leftTop.y);
        canvas.drawPath(path, paint);
        
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(scaledSrcBmp, rect, rect, paint);
        
        // bitmap回收(recycle导致在布局文件XML看不到效果)
        // bmp.recycle();
        // squareBitmap.recycle();
        // scaledSrcBmp.recycle();
        bmp = null;
        squareBitmap = null;
        scaledSrcBmp = null;
        return output;
    }
    
    private class XYPoint
    {
        public XYPoint(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
        
        public int x;
        
        public int y;
    }
}
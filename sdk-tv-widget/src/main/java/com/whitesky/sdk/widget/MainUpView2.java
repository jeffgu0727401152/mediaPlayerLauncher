package com.whitesky.sdk.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.whitesky.sdk.R;
import com.whitesky.sdk.bridge.IAnimBridge;
import com.whitesky.sdk.bridge.OpenBaseAnimBridge;


public class MainUpView2 extends FrameLayout
{
    private static final float DEFUALT_SCALE = 1.0f;
    
    private static final int DEFULAT_SIZE = 500;
    
    private Drawable mDrawableShadow;
    
    private Drawable mDrawableUpRect;
    
    private Context mContext;
    
    public MainUpView2(Context context)
    {
        super(context, null, 0);
        init(context, null);
    }
    
    public MainUpView2(Context context, View view)
    {
        super(context, null, 0);
        // 如果是单独添加，就将view加进来.
        if (view != null)
        {
            ViewGroup viewGroup = (ViewGroup)view.getRootView();
            if (viewGroup != null && this.getParent() != viewGroup)
            {
                LayoutParams layParams = new LayoutParams(DEFULAT_SIZE, DEFULAT_SIZE);
                viewGroup.addView(this, layParams);
            }
        }
        init(context, null);
    }
    
    public MainUpView2(Context context, AttributeSet attrs)
    {
        super(context, attrs, 0);
        init(context, attrs);
    }
    
    public MainUpView2(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    
    private void init(Context context, AttributeSet attrs)
    {
        setWillNotDraw(false);
        mContext = context;
        // 初始化.
        if (attrs != null)
        {
            TypedArray tArray = context.obtainStyledAttributes(attrs, R.styleable.MainUpView);// 获取配置属性
            mDrawableUpRect = tArray.getDrawable(R.styleable.MainUpView_upImageRes); // 顶层图片.
            mDrawableShadow = tArray.getDrawable(R.styleable.MainUpView_shadowImageRes); // 阴影图片.
            tArray.recycle();
        }
        IAnimBridge baseAnimBridge = new OpenBaseAnimBridge();
        baseAnimBridge.onInitBridge(this);
        baseAnimBridge.setMainUpView(this);
        setAnimBridge(baseAnimBridge);
    }
    
    public void setUpRectResource(int resId)
    {
        try
        {
            this.mDrawableUpRect = mContext.getResources().getDrawable(resId); // 移动的边框.
            invalidate();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    /**
     * 设置最上层的边框.
     */
    public void setUpRectDrawable(Drawable upRectDrawable)
    {
        this.mDrawableUpRect = upRectDrawable;
        invalidate();
    }
    
    public Drawable getUpRectDrawable()
    {
        return this.mDrawableUpRect;
    }
    
    public void setShadowResource(int resId)
    {
        try
        {
            this.mDrawableShadow = mContext.getResources().getDrawable(resId); // 移动的边框.
            invalidate();
        }
        catch (Exception e)
        {
            this.mDrawableShadow = null;
            e.printStackTrace();
        }
    }
    
    public Drawable getShadowDrawable()
    {
        return this.mDrawableShadow;
    }
    
    /**
     * 当图片边框不自带阴影的话，可以自行设置阴影图片. 设置阴影.
     */
    public void setShadowDrawable(Drawable shadowDrawable)
    {
        this.mDrawableShadow = shadowDrawable;
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas)
    {
        if (this.mAnimBridge != null)
            if (this.mAnimBridge.onDrawMainUpView(canvas))
                return;
        super.onDraw(canvas);
    }
    
    private Rect mUpPaddingRect = new Rect(0, 0, 0, 0);
    
    private Rect mShadowPaddingRect = new Rect(0, 0, 0, 0);
    
    /**
     * 根据阴影图片边框 自行 填写 相差的边距. <br>
     * 比如 res/drawble/white_shadow.9.png的图片，边距就差很多.
     */
    public void setDrawShadowPadding(int size)
    {
        setDrawShadowRectPadding(new Rect(size, size, size, size));
    }
    
    /**
     * 根据阴影图片边框 自行 填写 相差的边距. <br>
     * 比如 res/drawble/white_shadow.9.png的图片，边距就差很多.
     */
    public void setDrawShadowRectPadding(Rect rect)
    {
        mShadowPaddingRect.set(rect);
        invalidate();
    }
    
    public Rect getDrawShadowRect()
    {
        return this.mShadowPaddingRect;
    }
    
    /**
     * 根据图片边框 自行 填写 相差的边距. <br>
     * 比如 res/drawble/white_light_10.9.png的图片，边距就差很多.
     */
    public void setDrawUpRectPadding(int size)
    {
        setDrawUpRectPadding(new Rect(size, size, size, size));
    }
    
    /**
     * 根据图片边框 自行 填写 相差的边距. <br>
     * 比如 res/drawble/white_light_10.9.png的图片，边距就差很多.
     */
    public void setDrawUpRectPadding(Rect rect)
    {
        mUpPaddingRect.set(rect);
        invalidate();
    }
    
    public Rect getDrawUpRect()
    {
        return this.mUpPaddingRect;
    }
    
    /**
     * 设置焦点子控件的移动和放大.
     */
    public void setFocusView(View view, float scale)
    {
        this.mAnimBridge.onFocusView(view, scale, scale);
    }
    
    public void setFocusView(View view, float scaleX, float scaleY)
    {
        if (this.mAnimBridge != null)
            this.mAnimBridge.onFocusView(view, scaleX, scaleY);
    }
    
    public void setFocusView(View newView, View oldView, float scale)
    {
        setFocusView(newView, scale);
        setUnFocusView(oldView);
    }
    
    public void setUnFocusView(View view, float scaleX, float scaleY)
    {
        if (this.mAnimBridge != null)
            this.mAnimBridge.onOldFocusView(view, scaleX, scaleY);
    }
    
    /**
     * 设置无焦点子控件还原.
     */
    public void setUnFocusView(View view)
    {
        setUnFocusView(view, DEFUALT_SCALE, DEFUALT_SCALE);
    }
    
    IAnimBridge mAnimBridge;
    
    public void setAnimBridge(IAnimBridge adapter)
    {
        this.mAnimBridge = adapter;
        if (this.mAnimBridge != null)
        {
            this.mAnimBridge.onInitBridge(this);
            this.mAnimBridge.setMainUpView(this);
            invalidate();
        }
    }
    
    public IAnimBridge getAnimBridge()
    {
        return this.mAnimBridge;
    }
    
}

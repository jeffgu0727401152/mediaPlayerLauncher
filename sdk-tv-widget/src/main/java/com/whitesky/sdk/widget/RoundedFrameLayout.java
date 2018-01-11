package com.whitesky.sdk.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class RoundedFrameLayout extends FrameLayout implements RoundImpl.RoundedView
{
    
    RoundImpl round;
    
    public RoundedFrameLayout(Context context)
    {
        super(context);
        init(context, null, 0);
    }
    
    public RoundedFrameLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs, 0);
    }
    
    public RoundedFrameLayout(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }
    
    private void init(Context context, AttributeSet attrs, int defStyle)
    {
        round = new RoundImpl(this, context, attrs, defStyle);
        setWillNotDraw(false);
    }
    
    @Override
    public void draw(Canvas canvas)
    {
        round.draw(canvas);
        super.draw(canvas);
    }
    
    @Override
    public void drawSuper(Canvas canvas)
    {
        super.draw(canvas);
    }
    
    @Override
    public RoundImpl getRoundImpl()
    {
        return round;
    }
}

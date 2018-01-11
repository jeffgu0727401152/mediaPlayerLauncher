package com.whitesky.sdk.widget.component;

import com.mstar.android.tv.TvCommonManager;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SeekBarButton implements IUpdateSysData
{
    private static final int TEXT_VIEW_NAME_IDX = 0;
    
    private static final int PROGRESS_BAR_IDX = 2;
    
    private static final int TEXT_VIEW_PROGRESS_IDX = 1;
    
    private final int TTS_DELAY_TIME_500MS = 500;
    
    private int step;
    
    private int decimalDigits = 0;
    
    private boolean isSelectedDifferent = false;
    
    private LinearLayout mLayout;
    
    TextView textViewName;
    
    SeekBar seekbar;
    
    TextView textViewProgress;
    
    private void onCreate()
    {
        if (mLayout != null)
        {
            textViewName = (TextView)mLayout.getChildAt(TEXT_VIEW_NAME_IDX);
            seekbar = (SeekBar)mLayout.getChildAt(PROGRESS_BAR_IDX);
            textViewProgress = (TextView)mLayout.getChildAt(TEXT_VIEW_PROGRESS_IDX);
            textViewProgress.setText(String.valueOf(seekbar.getProgress()));
            mLayout.setOnKeyListener(new OnKeyListener()
            {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event)
                {
                    if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)
                    {
                        if (mLayout.isSelected())
                        {
                            mLayout.setSelected(false);
                        }
                        else
                        {
                            mLayout.setSelected(true);
                        }
                        return false;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP)
                    {
                        mLayout.setSelected(false);
                    }
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DPAD_LEFT:
                            if (event.getAction() == KeyEvent.ACTION_DOWN
                                && (mLayout.isSelected() || !isSelectedDifferent))
                            {
                                decreaseProgress();
                                doUpdate();
                                return true;
                            }
                            break;
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                            if (event.getAction() == KeyEvent.ACTION_DOWN
                                && (mLayout.isSelected() || !isSelectedDifferent))
                            {
                                increaseProgress();
                                doUpdate();
                                return true;
                            }
                            break;
                        case KeyEvent.KEYCODE_VOLUME_DOWN:
                            if (event.getAction() == KeyEvent.ACTION_DOWN
                                && (mLayout.isSelected() || !isSelectedDifferent))
                            {
                                decreaseProgress();
                                doUpdate();
                                return true;
                            }
                            break;
                        case KeyEvent.KEYCODE_VOLUME_UP:
                            if (event.getAction() == KeyEvent.ACTION_DOWN
                                && (mLayout.isSelected() || !isSelectedDifferent))
                            {
                                increaseProgress();
                                doUpdate();
                                return true;
                            }
                            break;
                    }
                    return false;
                }
            });
            mLayout.setOnTouchListener(new OnTouchListener()
            {
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    if (event.getAction() == KeyEvent.ACTION_UP)
                    {
                        SeekBarButton.this.setFocused();
                        seekbar.setVisibility(View.VISIBLE);
                        return true;
                    }
                    if (event.getAction() == KeyEvent.ACTION_DOWN)
                    {
                        SeekBarButton.this.setFocused();
                        seekbar.setVisibility(View.VISIBLE);
                        return true;
                    }
                    return false;
                }
            });
            
            seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
            {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar)
                {
                    textViewProgress.setText(String.valueOf(seekBar.getProgress()));
                    doUpdate();
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar)
                {
                }
                
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
                {
                }
            });
        }
    }
    
    public SeekBarButton(Activity context, int resId, int step, boolean isSelectedDiff)
    {
        this.isSelectedDifferent = isSelectedDiff;
        this.step = step;
        mLayout = (LinearLayout)context.findViewById(resId);
        onCreate();
    }
    
    public SeekBarButton(Activity context, LinearLayout ll, int step, boolean isSelectedDiff)
    {
        this.isSelectedDifferent = isSelectedDiff;
        this.step = step;
        mLayout = (LinearLayout)ll;
        onCreate();
    }
    
    public SeekBarButton(Activity context, int resId, int step, boolean isSelectedDiff, int decimalDigits)
    {
        this.isSelectedDifferent = isSelectedDiff;
        this.step = step;
        this.decimalDigits = decimalDigits;
        mLayout = (LinearLayout)context.findViewById(resId);
        if (mLayout != null)
        {
            textViewName = (TextView)mLayout.getChildAt(TEXT_VIEW_NAME_IDX);
            seekbar = (SeekBar)mLayout.getChildAt(PROGRESS_BAR_IDX);
            textViewProgress = (TextView)mLayout.getChildAt(TEXT_VIEW_PROGRESS_IDX);
            textViewProgress.setText(String.valueOf(seekbar.getProgress()));
            mLayout.setOnKeyListener(new OnKeyListener()
            {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event)
                {
                    if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)
                    {
                        if (mLayout.isSelected())
                        {
                            mLayout.setSelected(false);
                        }
                        else
                        {
                            mLayout.setSelected(true);
                        }
                        return false;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP)
                    {
                        mLayout.setSelected(false);
                    }
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DPAD_LEFT:
                            if (event.getAction() == KeyEvent.ACTION_DOWN
                                && (mLayout.isSelected() || !isSelectedDifferent))
                            {
                                decreaseProgress();
                                doUpdate();
                                return true;
                            }
                            break;
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                            if (event.getAction() == KeyEvent.ACTION_DOWN
                                && (mLayout.isSelected() || !isSelectedDifferent))
                            {
                                increaseProgress();
                                doUpdate();
                                return true;
                            }
                            break;
                        case KeyEvent.KEYCODE_VOLUME_DOWN:
                            if (event.getAction() == KeyEvent.ACTION_DOWN
                                && (mLayout.isSelected() || !isSelectedDifferent))
                            {
                                decreaseProgress();
                                doUpdate();
                                return true;
                            }
                            break;
                        case KeyEvent.KEYCODE_VOLUME_UP:
                            if (event.getAction() == KeyEvent.ACTION_DOWN
                                && (mLayout.isSelected() || !isSelectedDifferent))
                            {
                                increaseProgress();
                                doUpdate();
                                return true;
                            }
                            break;
                    }
                    return false;
                }
            });
            mLayout.setOnTouchListener(new OnTouchListener()
            {
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    if (event.getAction() == KeyEvent.ACTION_UP)
                    {
                        SeekBarButton.this.setFocused();
                        seekbar.setVisibility(View.VISIBLE);
                        return true;
                    }
                    if (event.getAction() == KeyEvent.ACTION_DOWN)
                    {
                        SeekBarButton.this.setFocused();
                        seekbar.setVisibility(View.VISIBLE);
                        return true;
                    }
                    return false;
                }
            });
            
            seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
            {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar)
                {
                    textViewProgress.setText(String.valueOf(seekBar.getProgress()));
                    doUpdate();
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar)
                {
                }
                
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
                {
                }
            });
        }
    }
    
    public void setSeletedDifferent(boolean b)
    {
        isSelectedDifferent = b;
    }
    
    public SeekBarButton(Dialog dialog, int resId, int step)
    {
        this.step = step;
        mLayout = (LinearLayout)dialog.findViewById(resId);
        textViewName = (TextView)mLayout.getChildAt(TEXT_VIEW_NAME_IDX);
        seekbar = (SeekBar)mLayout.getChildAt(PROGRESS_BAR_IDX);
        textViewProgress = (TextView)mLayout.getChildAt(TEXT_VIEW_PROGRESS_IDX);
        textViewProgress.setText(String.valueOf(seekbar.getProgress()));
        mLayout.setOnKeyListener(new OnKeyListener()
        {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                switch (keyCode)
                {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        if (event.getAction() == KeyEvent.ACTION_DOWN)
                        {
                            decreaseProgress();
                            doUpdate();
                            return true;
                        }
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        if (event.getAction() == KeyEvent.ACTION_DOWN)
                        {
                            increaseProgress();
                            doUpdate();
                            return true;
                        }
                        break;
                }
                return false;
            }
        });
    }
    
    protected void increaseProgress()
    {
        seekbar.incrementProgressBy(this.step);
        String strProgress = convertProgressToString(seekbar.getProgress());
        textViewProgress.setText(strProgress);
        ttsSepakProgress();
    }
    
    protected void decreaseProgress()
    {
        seekbar.incrementProgressBy(-this.step);
        String strProgress = convertProgressToString(seekbar.getProgress());
        textViewProgress.setText(strProgress);
        ttsSepakProgress();
    }
    
    public short getProgress()
    {
        return (short)seekbar.getProgress();
    }
    
    public void setProgress(short progress)
    {
        seekbar.setProgress(progress);
        String strProgress = convertProgressToString(seekbar.getProgress());
        textViewProgress.setText(strProgress);
    }
    
    public int getProgressInt()
    {
        return seekbar.getProgress();
    }
    
    public void setProgressInt(int progress)
    {
        seekbar.setProgress(progress);
        String strProgress = convertProgressToString(seekbar.getProgress());
        textViewProgress.setText(strProgress);
    }
    
    public int getMax()
    {
        return seekbar.getMax();
    }
    
    @Override
    public void doUpdate()
    {
    }
    
    public void setFocused()
    {
        mLayout.setFocusable(true);
        mLayout.setFocusableInTouchMode(true);
        mLayout.requestFocus();
    }
    
    public void setOnFocusChangeListener(OnFocusChangeListener onFocusChangeListener)
    {
        mLayout.setOnFocusChangeListener(onFocusChangeListener);
    }
    
    public void setOnClickListener(OnClickListener onClickListener)
    {
        mLayout.setOnClickListener(onClickListener);
    }
    
    public void setFocusable(boolean b)
    {
        if (b)
        {
            textViewName.setTextColor(Color.WHITE);
            textViewProgress.setTextColor(Color.WHITE);
        }
        else
        {
            textViewName.setTextColor(Color.GRAY);
            textViewProgress.setTextColor(Color.GRAY);
        }
        mLayout.setFocusable(b);
    }
    
    public void setEnable(boolean b)
    {
        mLayout.setEnabled(b);
    }
    
    public void setVisibility(boolean b)
    {
        if (b)
        {
            mLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            mLayout.setVisibility(View.GONE);
        }
    }
    
    public LinearLayout getLayout()
    {
        return mLayout;
    }
    
    private String convertProgressToString(int progress)
    {
        String str = new String();
        if (0 == this.decimalDigits)
        {
            str = String.valueOf(progress);
        }
        else
        {
            int digit = Double.valueOf(Math.pow(10, this.decimalDigits)).intValue();
            str = String.valueOf(progress / digit);
            str += ".";
            str += String.valueOf(progress % digit);
        }
        return str;
    }
    
    private void ttsSepakProgress()
    {
        String strProgress = convertProgressToString(seekbar.getProgress());
        TvCommonManager.getInstance().speakTtsDelayed(strProgress,
            TvCommonManager.TTS_QUEUE_FLUSH,
            TvCommonManager.TTS_SPEAK_PRIORITY_NORMAL,
            TTS_DELAY_TIME_500MS);
    }
}

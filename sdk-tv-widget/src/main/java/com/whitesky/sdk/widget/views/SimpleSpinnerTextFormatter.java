package com.whitesky.sdk.widget.views;

import android.text.Spannable;
import android.text.SpannableString;

public class SimpleSpinnerTextFormatter implements SpinnerTextFormatter
{
    
    @Override
    public Spannable format(String text)
    {
        return new SpannableString(text);
    }
}

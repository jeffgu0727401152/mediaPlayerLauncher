package com.whitesky.sdk.bridge;


import android.graphics.Canvas;
import android.view.View;

import com.whitesky.sdk.widget.MainUpView2;

public interface IAnimBridge {

    public void onInitBridge(MainUpView2 view);

    public boolean onDrawMainUpView(Canvas canvas);

    public void onOldFocusView(View oldFocusView, float scaleX, float scaleY);

    public void onFocusView(View focusView, float scaleX, float scaleY);

    void setMainUpView(MainUpView2 view);

    MainUpView2 getMainUpView();
}

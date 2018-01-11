package com.whitesky.sdk.widget.component;


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
import android.widget.TextView;

import com.mstar.android.tv.TvCommonManager;

public class ComboButton implements IUpdateSysData {

    private final int TTS_DELAY_TIME = 100;

    private LinearLayout mLayout;

    private TextView mItemNameView;

    private TextView mItemValueView;

    private String[] mItems;

    private boolean[] mItemEnableFlags;

    private int mIndex = 0;

    private Activity mActivity;

    private int mLeftKeyCode = KeyEvent.KEYCODE_DPAD_LEFT;

    private int mRightKeyCode = KeyEvent.KEYCODE_DPAD_RIGHT;

    private Thread mExecuteThread = null;

    private boolean mIsThreadModeEnable = false;

    private int mNumOfItems = 0;

    /*
     * If mIsNeedSelectBeforeSwitch = true, ComboButton need to selected before it can be change index by pressing L/R key
     * Otherwise, it can be change index by press L/R key defined in setLRListener() directly.
     * This variable is ignored if user click ComboButton using mouse.
     * This variable is also ignored if the item list has only one item in it.
     */
    private boolean mIsNeedSelectBeforeSwitch = true;

    public static final boolean NEED_SELECTED_BEFORE_SWITCH = true;

    public static final boolean DIRECT_SWITCH = false;

    /** Definition of extraFlags */
    /** FLAG_NULL indicates no extra flags */
    public static final int FLAG_NULL = 0x0;
    /** FLAG_RUN_IN_NEW_THREAD indicates the doUpdate() would be executed in a new thread */
    public static final int FLAG_RUN_IN_NEW_THREAD = 0x1;

    public ComboButton(Activity activity, String[] items, int resourceId, int idxNameView,
            int idxIndicator, boolean isNeedSelectBeforeSwitch) {
        this(activity, items, resourceId, idxNameView, idxIndicator, isNeedSelectBeforeSwitch, FLAG_NULL);
    }

    public ComboButton(Activity activity, String[] items, int resourceId, int idxNameView,
            int idxIndicator, boolean isNeedSelectBeforeSwitch, int extraFlag) {
        mActivity = activity;
        mIsNeedSelectBeforeSwitch = isNeedSelectBeforeSwitch;
        mLayout = (LinearLayout) mActivity.findViewById(resourceId);
        System.out.println(mLayout+"=====");
        mItemNameView = (TextView) mLayout.getChildAt(idxNameView);

        mItemValueView = (TextView) mLayout.getChildAt(idxIndicator);
        if ((extraFlag & FLAG_RUN_IN_NEW_THREAD) != FLAG_NULL) {
            mIsThreadModeEnable = true;
        }
        setLRListener();
        initItems(items);
        initItemEnableFlags();
        setEnable(true);
        setDefaultUiOnFocusChangeListener();
        setDefaultUiOnClickListener();
    }
    public ComboButton(View view, String[] items, int resourceId, int idxNameView,
                       int idxIndicator, boolean isNeedSelectBeforeSwitch, int extraFlag) {
        mIsNeedSelectBeforeSwitch = isNeedSelectBeforeSwitch;
        mLayout = (LinearLayout) view.findViewById(resourceId);
        System.out.println(mLayout+"=====");
        mItemNameView = (TextView) mLayout.getChildAt(idxNameView);

        mItemValueView = (TextView) mLayout.getChildAt(idxIndicator);
        if ((extraFlag & FLAG_RUN_IN_NEW_THREAD) != FLAG_NULL) {
            mIsThreadModeEnable = true;
        }
        setLRListener();
        initItems(items);
        initItemEnableFlags();
        setEnable(true);
        setDefaultUiOnFocusChangeListener();
        setDefaultUiOnClickListener();
    }
    public ComboButton(Dialog dialogContext, String[] items, int resourceId, int idxNameView,
            int idxIndicator) {
        mLayout = (LinearLayout) dialogContext.findViewById(resourceId);
        mItemNameView = (TextView) mLayout.getChildAt(idxNameView);
        mItemValueView = (TextView) mLayout.getChildAt(idxIndicator);
        setLRListener();
        initItems(items);
        initItemEnableFlags();
        setEnable(true);
        setDefaultUiOnFocusChangeListener();
        setDefaultUiOnClickListener();
    }

    private void initItems(String[] items) {
        setLRListener();
        mItems = items;
        // Set index 0 if the given index is Out of Boundary
        if (null != mItems && mItems.length > 0) {
            mItemValueView.setText(mItems[0]);
        } else {
            mItemValueView.setText("" + mIndex);
        }
    }

    private void initItemEnableFlags() {
        if (null != mItems) {
            int len = mItems.length;
            mNumOfItems = len;
            mItemEnableFlags = new boolean[len];
            for (int i = 0; i < len; i++) {
                mItemEnableFlags[i] = true;
            }
        }
    }

    public void setLRKeyCode(int leftKeyCode, int rightKeyCode) {
        mLeftKeyCode = leftKeyCode;
        mRightKeyCode = rightKeyCode;
    }

    private void setLRListener() {
        mLayout.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (KeyEvent.KEYCODE_ENTER == keyCode && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (true == mLayout.isSelected()) {
                        mLayout.setSelected(false);
                    } else {
                        /* Disable ENTER key if only one item is available */
                        if ((1 < mNumOfItems) || (null == mItems)) {
                            mLayout.setSelected(true);
                        }
                    }
                    return false;
                }

                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if ((true == mLayout.isSelected()) || ((DIRECT_SWITCH == mIsNeedSelectBeforeSwitch) && ((1 < mNumOfItems) || (null == mItems)))) {
                        if ((keyCode == mLeftKeyCode) || (keyCode == mRightKeyCode)) {
                            if (false == mIsThreadModeEnable) {
                                if (keyCode == mLeftKeyCode) {
                                   decreaseIdx();
                                } else {
                                   increaseIdx();
                                }
                               doUpdate();
                            } else {
                                if (true == isThreadAvaliable()) {
                                    if (keyCode == mLeftKeyCode) {
                                        decreaseIdx();
                                    } else {
                                        increaseIdx();
                                    }
                                    runThread();
                                }
                            }
                           return true;
                        }
                    }
                }
                return false;
            }
        });
        mLayout.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (true == mLayout.isFocusable()) {
                        if ((1 < mNumOfItems) || (null == mItems)) {
                            ComboButton.this.setFocused();
                            if (false == mIsThreadModeEnable) {
                                increaseIdx();
                                doUpdate();
                            } else {
                                if (true == isThreadAvaliable()) {
                                    increaseIdx();
                                    runThread();
                                }
                            }
                        }
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void runThread() {
        if ((mIsThreadModeEnable == true) && (isThreadAvaliable() == true)) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        doUpdate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            mExecuteThread = new Thread(runnable);
            mExecuteThread.start();
        }
    }

    private boolean isThreadAvaliable() {
        if ((null == mExecuteThread) || (mExecuteThread.getState() == Thread.State.TERMINATED)) {
            return true;
        }
        return false;
    }

    public void setIdx(int idx) {
        mIndex = idx;
        if (null != mItems) {
            // Set index 0 if the given index is Out of Boundary
            if (mItems.length <= mIndex || 0 > mIndex) {
                mIndex = 0;
            }
            mItemValueView.setText(mItems[mIndex]);
        } else {
            mItemValueView.setText("" + mIndex);
        }
    }

    public int getItemLength() {
        return mItems.length;
    }

    public short getIdx() {
        return (short) mIndex;
    }

    public int getIntIndex() {
        return mIndex;
    }

    public String getItemNameByIdx(int itemIndex) {
        if (null != mItems) {
            if (mItems.length > itemIndex && 0 <= itemIndex) {
                return mItems[itemIndex];
            }
        }
        return "";
    }

    public void setItemEnable(int itemIndex, boolean bEnable) {
        if (null != mItemEnableFlags) {
            if (mItemEnableFlags.length > itemIndex && 0 <= itemIndex) {
                mItemEnableFlags[itemIndex] = bEnable;
            }
            mNumOfItems = 0;
            for (boolean b : mItemEnableFlags) {
                if (true == b) {
                    mNumOfItems++;
                }
            }
        }
    }

    public boolean getIsItemEnable(int itemIndex) {
        if (null != mItemEnableFlags) {
            if (mItemEnableFlags.length > itemIndex && 0 <= itemIndex) {
                return mItemEnableFlags[itemIndex];
            }
        }
        return false;
    }

    protected void increaseIdx() {
        String str = "";
        if (null != mItems && mItems.length > 0) {
            for (int idx = mIndex + 1; idx < mIndex + mItems.length; idx++) {
                if (true == mItemEnableFlags[idx % mItems.length]) {
                    mIndex = idx % mItems.length;
                    str = mItems[mIndex];
                    mItemValueView.setText(str);
                    break;
                }
            }
        } else {
            mIndex++;
            str = "" + mIndex;
            mItemValueView.setText(str);
        }
        if (!str.equals("")) {
            TvCommonManager.getInstance().speakTtsDelayed(
                str
                , TvCommonManager.TTS_QUEUE_FLUSH
                , TvCommonManager.TTS_SPEAK_PRIORITY_NORMAL
                , TTS_DELAY_TIME);
        }
    }

    protected void decreaseIdx() {
        String str = "";
        if (null != mItems && mItems.length > 0) {
            for (int idx = mIndex + mItems.length - 1; idx > mIndex; idx--) {
                if (true == mItemEnableFlags[idx % mItems.length]) {
                    mIndex = idx % mItems.length;
                    str = mItems[mIndex];
                    mItemValueView.setText(str);
                    break;
                }
            }
        } else {
            mIndex--;
            str = "" + mIndex;
            mItemValueView.setText(str);
        }
        if (!str.equals("")) {
            TvCommonManager.getInstance().speakTtsDelayed(
                str
                , TvCommonManager.TTS_QUEUE_FLUSH
                , TvCommonManager.TTS_SPEAK_PRIORITY_NORMAL
                , TTS_DELAY_TIME);
        }
    }

    public void doUpdate() {
    }

    public void setFocused() {
        mLayout.setFocusable(true);
        mLayout.setFocusableInTouchMode(true);
        mLayout.requestFocus();
    }

    public void setOnFocusChangeListener(OnFocusChangeListener onFocusChangeListener) {
        mLayout.setOnFocusChangeListener(onFocusChangeListener);
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        mLayout.setOnClickListener(onClickListener);
    }

    public void setTextInChild(int idx, String str) {
        TextView textView = (TextView) mLayout.getChildAt(idx);
        textView.setText(str);
    }

    /**
     * set Combo Button Focusable or not
     * @param bFocusable Focusable or not
     */
    @Deprecated
    public void setFocusable(boolean bFocusable) {
        if (bFocusable) {
            mItemNameView.setTextColor(Color.WHITE);
            mItemValueView.setTextColor(Color.WHITE);
        } else {
            mItemNameView.setTextColor(Color.GRAY);
            mItemValueView.setTextColor(Color.GRAY);
        }
        mLayout.setFocusable(bFocusable);
        mLayout.setFocusableInTouchMode(bFocusable);
    }

    /**
     * set Combo Button Enable or Disable
     * @param bEnable Enable or Disable
     */
    public void setEnable(boolean bEnable) {
        if (bEnable) {
            mItemNameView.setTextColor(Color.WHITE);
            mItemValueView.setTextColor(Color.WHITE);
        } else {
            mItemNameView.setTextColor(Color.GRAY);
            mItemValueView.setTextColor(Color.GRAY);
        }
        mLayout.setEnabled(bEnable);
        mLayout.setFocusable(bEnable);
        mLayout.setFocusableInTouchMode(bEnable);
    }

    public void setVisibility(boolean bVisible) {
        if (bVisible) {
            mLayout.setVisibility(View.VISIBLE);
        } else {
            mLayout.setVisibility(View.GONE);
        }
    }

    public void setVisibility(int nVisible) {
        switch (nVisible) {
            case View.VISIBLE:
            case View.INVISIBLE:
            case View.GONE:
                mLayout.setVisibility(nVisible);
                break;
            default:
                break;
        }
    }

    public void setmItemValueViewColor(int color) {
        mItemValueView.setTextColor(color);
    }

    public LinearLayout getLayout() {
        return mLayout;
    }

    private void setDefaultUiOnFocusChangeListener() {
        mLayout.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                LinearLayout container = (LinearLayout) v;
                if (true == hasFocus) {
                    if ((DIRECT_SWITCH == mIsNeedSelectBeforeSwitch) && ((1 < mNumOfItems) || (null == mItems))) {
                        if (container.getChildAt(3) != null) {
                            container.getChildAt(0).setVisibility(View.VISIBLE);
                            container.getChildAt(3).setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    if (container.getChildAt(3) != null) {
                        container.getChildAt(0).setVisibility(View.GONE);
                        container.getChildAt(3).setVisibility(View.GONE);
                    }
                    mLayout.setSelected(false);
                }
            }
        });
    }

    private void setDefaultUiOnClickListener() {
        mLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout container = (LinearLayout) v;
                if (container.isSelected()) {
                    if (container.getChildAt(3) != null) {
                        container.getChildAt(0).setVisibility(View.VISIBLE);
                        container.getChildAt(3).setVisibility(View.VISIBLE);
                    }
                } else {
                    if (container.getChildAt(3) != null) {
                        container.getChildAt(0).setVisibility(View.GONE);
                        container.getChildAt(3).setVisibility(View.GONE);
                    }
                }
            }
        });
    }
}

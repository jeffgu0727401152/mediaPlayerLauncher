package com.whitesky.sdk.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.whitesky.sdk.R;

/**
 * Created by mac on 17-6-2. 自定义对话框
 *
 * @author xiaoxuan
 */
public class LoginDialog extends Dialog
{
    private Button nextBtn; // 确定按钮
    
    // 接口的实例
    private Button cancleBtn; // 确定按钮
    
    private OnNextListener mOnNextListener; // 确定监听实例
    
    private OnExitListener mOnExitListener; // 退出监听实例
    
    private Context mContext;
    
    public LoginDialog(Context context)
    {
        // 关联style
        super(context, R.style.ReasonDialog);
        Window window = getWindow();
        // 设置动画效果
        window.setWindowAnimations(R.style.MyReasonDialogAnim);
        this.mContext = context;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // 关联布局样式
        setContentView(R.layout.dialog_login);
        // 按空白处不能取消动画
        setCanceledOnTouchOutside(false);
        setCancelable(false);
        // 初始化界面控件
        initView();
        // 初始化界面数据
        initEvent();
    }
    
    /**
     * 初始化界面控件的显示数据
     */
    private void initEvent()
    {
        nextBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mOnNextListener.nextclick();
            }
        });
        cancleBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mOnExitListener.exitclick();
            }
        });
    }
    
    /**
     * 确定实例赋值
     */
    public void setnextclickListener(OnNextListener onNextListener)
    {
        this.mOnNextListener = onNextListener;
    }
    
    /**
     * 退出实例赋值
     */
    public void setexitListener(OnExitListener onExitListener)
    {
        this.mOnExitListener = onExitListener;
    }
    
    /**
     * 初始化界面控件
     */
    private void initView()
    {
        nextBtn = (Button)findViewById(R.id.bt_dialog_ota);
        nextBtn.requestFocus();
        cancleBtn = (Button)findViewById(R.id.bt_dialog_ota_cancle);
    }
    
    /**
     * 确定钮接口
     */
    public interface OnNextListener
    {
        void nextclick();
    }
    
    /**
     * 退出钮接口
     */
    public interface OnExitListener
    {
        void exitclick();
    }
    
}
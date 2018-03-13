package com.whitesky.tv.projectorlauncher.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.whitesky.sdk.bridge.RecyclerViewBridge;
import com.whitesky.sdk.widget.FocusListenerLinearLayout;
import com.whitesky.sdk.widget.MainUpView;
import com.whitesky.sdk.widget.OnChildSelectedListener;
import com.whitesky.sdk.widget.SpaceItemDecoration;
import com.whitesky.sdk.widget.SpacesItemDecoration;
import com.whitesky.sdk.widget.TvGridLayoutManager;
import com.whitesky.sdk.widget.TvRecyclerView;
import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.app.adapter.AppGridAdapter;
import com.whitesky.tv.projectorlauncher.app.bean.AppBean;
import com.whitesky.tv.projectorlauncher.common.adapter.BaseRecyclerViewAdapter;
import com.whitesky.tv.projectorlauncher.utils.AppUtil;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by mac on 17-6-2.
 *
 * @author xiaoxuan
 */
public class AppActivity extends Activity implements View.OnFocusChangeListener,
    FocusListenerLinearLayout.OnFocusSearchListener, BaseRecyclerViewAdapter.OnItemClickListener,
        OnChildSelectedListener
{
    private TvRecyclerView mAppGridView;

    private AppReceiver mAppReceiver;

    private List<AppBean> mAppData = new ArrayList<AppBean>();

    private AppGridAdapter mApater;

    private AppUtil mAppRead;

    private FocusListenerLinearLayout mFocusLayout;

    private TvGridLayoutManager mGridlayoutManager;

    private MainUpView mMainUpView;

    private RecyclerViewBridge mRecyclerViewBridge;

    private SpacesItemDecoration mSpacesItemDecoration;

    private View mOldView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);
        mAppReceiver = new AppReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        registerReceiver(mAppReceiver, intentFilter);
        mAppRead = new AppUtil(this);
        // 读取所有的应用
        mAppData = mAppRead.getLaunchAppList();
        mAppGridView = (TvRecyclerView)findViewById(R.id.rv_app_list);
        mFocusLayout = (FocusListenerLinearLayout)findViewById(R.id.layoutContent);
        mFocusLayout.setOnFocusSearchListener(this);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.item_space);
        mAppGridView.addItemDecoration(new SpaceItemDecoration(spacingInPixels));
        mAppGridView.setItemAnimator(new DefaultItemAnimator());
        mAppGridView.setLayoutManager(mGridlayoutManager);
        mMainUpView = (MainUpView)findViewById(R.id.mainUpView1);
        mMainUpView.setEffectBridge(new RecyclerViewBridge());
        mRecyclerViewBridge = (RecyclerViewBridge)mMainUpView.getEffectBridge();
        mRecyclerViewBridge.setUpRectResource(R.drawable.item_rectangle);
        mRecyclerViewBridge.setShadowResource(R.drawable.shadow7);
        mRecyclerViewBridge.setTranDurAnimTime(200);
        mRecyclerViewBridge.setShadowResource(R.drawable.item_shadow);
        if (mAppData.size() <= 20)
        {
            // 设置2行
            mGridlayoutManager = new TvGridLayoutManager(this, 2);
            // 兼容版本
            if (Build.VERSION.SDK_INT >= 19)
            {
                LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        (int)getResources().getDimension(R.dimen.x480));
                mSpacesItemDecoration = new SpacesItemDecoration(2, 2);
                mAppGridView.setLayoutParams(params);
            }
            else
            {
                RecyclerView.LayoutParams params =
                    new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        (int)getResources().getDimension(R.dimen.x480));
                mSpacesItemDecoration = new SpacesItemDecoration(2, 2);
                mAppGridView.setLayoutParams(params);
            }
        }
        else
        {
            mGridlayoutManager = new TvGridLayoutManager(this, 3);
            mSpacesItemDecoration = new SpacesItemDecoration(2, 3);
        }
        mGridlayoutManager.setOnChildSelectedListener(this);
        // 布局方式垂直布局
        mGridlayoutManager.setOrientation(GridLayoutManager.HORIZONTAL);
        mAppGridView.setLayoutManager(mGridlayoutManager);
        mAppGridView.setFocusable(false);
        mApater = new AppGridAdapter(this);
        mAppGridView.addItemDecoration(mSpacesItemDecoration);
        mAppGridView.setLayoutManager(mGridlayoutManager);
        mApater.setOnItemClickListener(this);
        mApater.setData(mAppData);
        mAppGridView.setAdapter(mApater);
    }

    // 更新显示的app
    private void updateAllApp()
    {
        if (mAppData != null)
        {
            mAppData.clear();
            mAppData = mAppRead.getLaunchAppList();
            mApater.setData(mAppData);
            mAppGridView.setAdapter(mApater);
            mApater.notifyDataSetChanged();
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus)
    {
        mRecyclerViewBridge.setFocusView(v, mOldView, 1.1f);
        mOldView = v;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        LinearLayout layout = (LinearLayout) findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.shape_background);
    }

    @Override
    protected void onDestroy()
    {
        unregisterReceiver(mAppReceiver);
        super.onDestroy();
    }

    @Override
    public View onFocusSearch(View focused, int direction)
    {
        View res = null;
        // 焦点控制
        switch (direction)
        {
            case View.FOCUS_DOWN:
                break;
            case View.FOCUS_UP:
                if (mAppGridView.hasFocus())
                {
                    res = focused;
                }
                break;
            case View.FOCUS_LEFT:
                break;
            case View.FOCUS_RIGHT:
                break;
        }
        return res;
    }
    
    @Override
    public void onItemClick(int position, Object data)
    {
        PackageManager manager = getPackageManager();
        String packageName = ((AppBean)data).getPackageName();
        Intent intent =     manager.getLaunchIntentForPackage(packageName);
        startActivity(intent);
    }
    
    @Override
    public void onItemLongClick(int position, Object data)
    {
        String packageName = ((AppBean)data).getPackageName();
        AppUtil.uninstallApp(this,packageName);
    }
    
    @Override
    public void onChildSelected(RecyclerView parent, View focusview, int position, int dy)
    {
        focusview.bringToFront();
        mRecyclerViewBridge.setFocusView(focusview, mOldView, 1.1f);
        mRecyclerViewBridge.setUnFocusView(mOldView);
        mOldView = focusview;
    }
    
    private class AppReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // 安装广播
            if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED"))
            {
                updateAllApp();
            }
            // 卸载广播
            if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED"))
            {
                updateAllApp();
            }
        }
    }
}
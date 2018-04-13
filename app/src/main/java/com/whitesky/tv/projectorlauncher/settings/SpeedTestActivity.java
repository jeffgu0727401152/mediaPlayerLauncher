package com.whitesky.tv.projectorlauncher.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.whitesky.tv.projectorlauncher.R;
import com.whitesky.tv.projectorlauncher.settings.model.SpeedModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class SpeedTestActivity extends Activity implements OnClickListener
{
    private final int PROGRESSCHANGE = 0;
    private final int SPEEDUPDATE = 1;
    private final int SPEED_FINISH = 2;
    private Button DidNotStart;// 未开始
    private Button InStart;// 已开始
    private Button StartAgain;// 再次开始
    private LinearLayout DidNotStartLayout;
    private LinearLayout InStartLayout;
    private LinearLayout StartAgainLayout;
    private long CurrenSpeed = 0;// 当前速度
    private long AverageSpeed = 0;// 平均速度
    private long SpeedTaital = 0;
    private byte[] FileData = null;
    private SpeedModel networkSpeedInfo = null;
    private String URL = "http://gdown.baidu.com/data/wisegame/6546ec811c58770b/labixiaoxindamaoxian_8.apk";
    private List<Long> list = new ArrayList<Long>();
    private ProgressBar SpeedProgressBar;
    
    private TextView Speed;
    
    private TextView percent;
    
    private TextView Movie_TYPE;
    
    private int progress;

    private Thread thread;
    
    private Boolean THREADCANRUN = true;
    
    private Boolean PROGRESSTHREADCANRUN = true;

    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case PROGRESSCHANGE:
                    progress = SpeedModel.progress;
                    percent.setText(progress + "%");
                    if (progress < 100)
                    {
                        SpeedProgressBar.setProgress(progress);
                    }
                    else
                    {
                        InStart.performClick();
                        PROGRESSTHREADCANRUN = false;
                        progress = 0;
                        SpeedProgressBar.setProgress(progress);
                    }
                    break;
                case SPEEDUPDATE:
                    CurrenSpeed = SpeedModel.Speed;
                    list.add(CurrenSpeed);
                    for (long speed : list)
                    {
                        SpeedTaital += speed;
                    }
                    AverageSpeed = SpeedTaital / list.size();
                    Speed.setText(AverageSpeed + "kb/s");
                    if (AverageSpeed <= 200)
                    {
                        Movie_TYPE.setText("");
                    }
                    else if (AverageSpeed <= 400)
                    {
                        Movie_TYPE.setText("");
                    }
                    else if (AverageSpeed > 400)
                    {
                        Movie_TYPE.setText("");
                    }
                    SpeedTaital = 0;
                    break;
                case SPEED_FINISH:
                    Speed.setText(AverageSpeed + "kb/s");
                    if (AverageSpeed <= 200)
                    {
                        Movie_TYPE.setText("");
                    }
                    else if (AverageSpeed <= 400)
                    {
                        Movie_TYPE.setText("");
                    }
                    else if (AverageSpeed > 400)
                    {
                        Movie_TYPE.setText("");
                    }
                    PROGRESSTHREADCANRUN = false;
                    THREADCANRUN = false;
                    SpeedModel.FILECANREAD = false;
                    break;
            }
        }
        
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_speed);
        Init();
    }
    
    public void Init()
    {
        networkSpeedInfo = new SpeedModel();
        DidNotStart = findViewById(R.id.speedtest_btn_start);
        InStart = findViewById(R.id.speedtset_btn_stoptest);
        StartAgain = findViewById(R.id.speedtest_btn_startagain);
        DidNotStart.setOnClickListener(this);
        InStart.setOnClickListener(this);
        StartAgain.setOnClickListener(this);
        DidNotStartLayout = findViewById(R.id.speedtset_didinotlayout);
        InStartLayout = findViewById(R.id.speedtest_instartlayout);
        StartAgainLayout = findViewById(R.id.speedtest_startagainlayout);
        SpeedProgressBar = findViewById(R.id.speedtest_progressBar);
        Speed = findViewById(R.id.speedtest_speed);
        Movie_TYPE = findViewById(R.id.speed_movietype);
        percent = findViewById(R.id.speed_test_percent);
    }
    
    @Override
    public void onClick(View arg0)
    {
        switch (arg0.getId())
        {
            case R.id.speedtest_btn_start:
                DidNotStartLayout.setVisibility(View.GONE);
                InStartLayout.setVisibility(View.VISIBLE);
                StartAgainLayout.setVisibility(View.GONE);
                InStart.requestFocus();
                InStart.requestFocusFromTouch();
                PROGRESSTHREADCANRUN = true;
                THREADCANRUN = true;
                SpeedModel.FILECANREAD = true;
                new Thread()
                {
                    
                    @Override
                    public void run()
                    {
                        FileData = ReadFileFromURL(URL, networkSpeedInfo);
                    }
                }.start();
                thread = new Thread()
                {
                    
                    @Override
                    public void run()
                    {
                        while (THREADCANRUN)
                        {
                            try
                            {
                                sleep(50);
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                            handler.sendEmptyMessage(SPEEDUPDATE);
                            if (SpeedModel.FinishBytes >= SpeedModel.totalBytes)
                            {
                                handler.sendEmptyMessage(SPEED_FINISH);
                                SpeedModel.FinishBytes = 0;
                            }
                        }
                    }
                };
                thread.start();
                
                new Thread()
                {
                    
                    @Override
                    public void run()
                    {
                        while (PROGRESSTHREADCANRUN)
                        {
                            try
                            {
                                sleep(500);
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                            handler.sendEmptyMessage(PROGRESSCHANGE);
                        }
                    }
                }.start();
                break;
            case R.id.speedtset_btn_stoptest:
                StartAgainLayout.setVisibility(View.VISIBLE);
                InStartLayout.setVisibility(View.GONE);
                DidNotStartLayout.setVisibility(View.GONE);
                StartAgain.requestFocus();
                StartAgain.requestFocusFromTouch();
                SpeedModel.progress = 0;
                SpeedModel.FinishBytes = 0;
                handler.sendEmptyMessage(SPEED_FINISH);
                break;
            case R.id.speedtest_btn_startagain:
                DidNotStartLayout.setVisibility(View.VISIBLE);
                StartAgainLayout.setVisibility(View.GONE);
                InStartLayout.setVisibility(View.GONE);
                break;
        }
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        RelativeLayout layout = findViewById(R.id.ll_skin);
        layout.setBackgroundResource(R.drawable.shape_background);
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(getApplicationContext(), SysSettingActivity.class);
        startActivity(intent);
        SpeedTestActivity.this.finish();
        super.onBackPressed();
    }

    public static byte[] ReadFileFromURL(String URL, SpeedModel info) {
        int FileLength = 0;
        long startTime = 0;
        long intervalTime = 0;
        byte[] b = null;
        java.net.URL mUrl = null;
        URLConnection mUrlConnection = null;
        InputStream inputStream = null;
        try
        {
            mUrl = new URL(URL);
            mUrlConnection = mUrl.openConnection();
            mUrlConnection.setConnectTimeout(15000);
            mUrlConnection.setReadTimeout(15000);
            FileLength = mUrlConnection.getContentLength();// todo 经检返回值
            inputStream = mUrlConnection.getInputStream();
            SpeedModel.totalBytes = FileLength;
            b = new byte[FileLength];
            startTime = System.currentTimeMillis();
            BufferedReader bufferReader = new BufferedReader(new InputStreamReader(mUrlConnection.getInputStream()));
            String line;
            byte buffer[];
            while (SpeedModel.FILECANREAD && ((line = bufferReader.readLine()) != null)
                    && FileLength > SpeedModel.FinishBytes)
            {
                buffer = line.getBytes();
                intervalTime = System.currentTimeMillis() - startTime;
                SpeedModel.FinishBytes = SpeedModel.FinishBytes + buffer.length;
                if (intervalTime == 0)
                {
                    SpeedModel.Speed = 1000;
                }
                else
                {
                    SpeedModel.Speed = SpeedModel.FinishBytes / intervalTime;
                    double a = (double) SpeedModel.FinishBytes / SpeedModel.totalBytes * 100;
                    SpeedModel.progress = (int)a;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (inputStream != null)
                {
                    inputStream.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return b;
    }
}

package com.dji.FPVDemo;

/**
 * Created by lenovo on 2016/11/19.
 */

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.dji.FPVDemo.chart.DistanceTimeChart;


import org.achartengine.GraphicalView;

import java.util.Timer;
import java.util.TimerTask;


public class ChartActivity extends Activity {
    private LinearLayout linearLayout;
    private GraphicalView mView;
    private DistanceTimeChart chart;
    private Timer timer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.distance_time_chart);

        linearLayout = (LinearLayout) findViewById(R.id.distance_time_curve);
        chart = new DistanceTimeChart(this);
        chart.setXYMultipleSeriesDataset("实时最短距离监测");
        chart.setXYMultipleSeriesRenderer();
        mView = chart.getmGraphicalView() ;
        linearLayout.addView(mView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.sendMessage(handler.obtainMessage());
            }
        },10,100);

    }
    private int t = 0;
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg){
            chart.updateChart(t, Math.random()*20);
            t+=1;
        }
    };

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(timer!=null){
            timer.cancel();
        }
    }
}

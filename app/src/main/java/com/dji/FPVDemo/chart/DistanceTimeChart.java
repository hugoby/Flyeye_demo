package com.dji.FPVDemo.chart;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by lenovo on 2016/11/19.
 */

public class DistanceTimeChart extends AbstractDemoChart {

    private Timer timer=new Timer();
    private GraphicalView chart;
    private TimerTask task;
    private GraphicalView mGraphicalView;
    private XYMultipleSeriesRenderer multipleSeriesRenderer;
    private XYMultipleSeriesDataset multipleSeriesDataset;
    private XYSeries mSeries;
    private XYSeriesRenderer mRenderer;
    private Context context;

    public DistanceTimeChart(Context context){
        this.context=context;
    }

    /**
     * Returns the chart name.
     *
     * @return the chart name
     */
    @Override
    public String getName() {
        return "DistanceTimeChart";
    }

    /**
     * Returns the chart description.
     *
     * @return the chart description
     */
    @Override
    public String getDesc() {
        return "HugoHugoHugoHugo";
    }

    /**
     * Executes the chart demo.
     *
     * @param context the context
     * @return the built intent
     */
    @Override
    public Intent execute(Context context) {
        return null;
    }

    public void setXYMultipleSeriesDataset(String curveTitle) {
        multipleSeriesDataset = new XYMultipleSeriesDataset();
        mSeries = new XYSeries(curveTitle);
        multipleSeriesDataset.addSeries(mSeries);
    }

    public void setXYMultipleSeriesRenderer(){
        int[] colors = new int[]{Color.BLUE};
        PointStyle[] styles = new PointStyle[]{PointStyle.CIRCLE};
        multipleSeriesRenderer = buildRenderer(colors, styles);
        int length=multipleSeriesRenderer.getSeriesRendererCount();
        for(int i=0;i<length;i++){
            ((XYSeriesRenderer) multipleSeriesRenderer.getSeriesRendererAt(i)).setFillPoints(true);
        }
        setChartSettings(multipleSeriesRenderer,"实时最短距离图","时间/s","距离/m",0,20,0,20, Color.LTGRAY, Color.LTGRAY);
        multipleSeriesRenderer.setXLabels(12);
        multipleSeriesRenderer.setYLabels(10);
        multipleSeriesRenderer.setShowGrid(true);
        multipleSeriesRenderer.setXLabelsAlign(Paint.Align.RIGHT);
        multipleSeriesRenderer.setYLabelsAlign(Paint.Align.RIGHT);
        multipleSeriesRenderer.setZoomButtonsVisible(true);
        multipleSeriesRenderer.setPanLimits(new double[] { 0, 3600, 0, 20 });
        multipleSeriesRenderer.setZoomLimits(new double[] { 0, 3600, 0, 20 });
    }

    public GraphicalView getmGraphicalView() {
//        initial the x and y.
//        String[] titles=new String[]{"main"};
//        List<double[]> times = new ArrayList<double[]>();
//        double[]time1=new double[20];
//        for (int i = 0; i < 20; i++) {
//            time1[i] = i;
//        }
//        times.add(time1);
//        List<double[]> distances = new ArrayList<double[]>();
//        double[] distance1=new double[20];
//        for (int i = 0; i < 20; i++) {
//            distance1[i] = 0;
//        }
//        distances.add(distance1);
//        multipleSeriesDataset=buildDataset(titles, times, distances);


        mGraphicalView = ChartFactory.getCubeLineChartView(context,multipleSeriesDataset ,
                multipleSeriesRenderer, 0.33f);
        return mGraphicalView;
    }

    public void updateChart(double x, double y) {
        mSeries.add(x, y);
        mGraphicalView.repaint();//此处也可以调用invalidate()
    }
    public void updateChart(List<Double> xList, List<Double> yList){
        for(int i=0;i<xList.size();i++){
            mSeries.add(xList.get(i),yList.get(i));
        }
        mGraphicalView.repaint();
    }
}

package com.dji.FPVDemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import dji.common.camera.CameraSystemState;
import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.camera.DJICamera;
import dji.sdk.camera.DJICamera.CameraReceivedVideoDataCallback;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.base.DJIBaseProduct;


public class MainActivity extends Activity implements SurfaceTextureListener,OnClickListener{

    private static final String TAG = MainActivity.class.getName();
    protected DJICamera.CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    protected TextureView mVideoSurface = null;
    private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn,mBlueToothBtn,mChartBtn;
    private ToggleButton mRecordBtn;
    private TextView recordingTime,data_show_screen;

    //Hugo 20161030
    private final static int REQUEST_CONNECT_DEVICE = 1;    //宏定义查询设备句柄

    private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //SPP服务UUID号
    private ScrollView sv;      //翻页句柄
    private InputStream is;    //输入流，用来接收蓝牙数据
    //private TextView text0;    //提示栏解句柄
    private String smsg = "";    //显示用数据缓存
    private String fmsg = "";    //保存用数据缓存
    private float mid_dis=0;
    private float angle=0, distance=0;
    public String filename=""; //用来保存存储的文件名
    BluetoothDevice _device = null;     //蓝牙设备
    BluetoothSocket _socket = null;      //蓝牙通信socket
    boolean _discoveryFinished = false;
    boolean bRun = true;
    boolean bThread = false;
    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();    //获取本地蓝牙适配器，即蓝牙设备

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //Hugo set full screen 20161028
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //end 20161028

        setContentView(R.layout.activity_main);
        initUI();

        //Hugo 20161101
        //如果打开本地蓝牙设备不成功，提示信息，结束程序
        if (_bluetooth == null){
            Toast.makeText(this, "无法打开手机蓝牙，请确认手机是否有蓝牙功能！", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 设置设备可以被搜索
        new Thread(){
            public void run(){
                if(!_bluetooth.isEnabled()){
                    _bluetooth.enable();
                }
            }
        }.start();


        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new CameraReceivedVideoDataCallback() {

            @Override
            public void onResult(byte[] videoBuffer, int size) {
                if(mCodecManager != null){
                    // Send the raw H264 video data to codec manager for decoding
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }else {
                    Log.e(TAG, "mCodecManager is null");
                }
            }
        };

        DJICamera camera = FPVDemoApplication.getCameraInstance();

        if (camera != null) {

            camera.setDJICameraUpdatedSystemStateCallback(new DJICamera.CameraUpdatedSystemStateCallback() {
                @Override
                public void onResult(CameraSystemState cameraSystemState) {
                    if (null != cameraSystemState) {

                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime % 3600) / 60;
                        int seconds = recordTime % 60;

                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        final boolean isVideoRecording = cameraSystemState.isRecording();

                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                recordingTime.setText(timeString);

                                /*
                                 * Update recordingTime TextView visibility and mRecordBtn's check state
                                 */
                                if (isVideoRecording){
                                    recordingTime.setVisibility(View.VISIBLE);
                                }else
                                {
                                    recordingTime.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            });

        }

    }

    protected void onProductChange() {
        initPreviewer();
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();

        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        uninitPreviewer();

        //Hugo 20161030
        if(_socket!=null)  //关闭连接socket
            try{
                _socket.close();
            }catch(IOException e){}
        //end
    }

    private void initUI() {

        recordingTime=(TextView)findViewById(R.id.timer);
        mRecordVideoModeBtn=(Button)findViewById(R.id.btn_record_video_mode);
        mShootPhotoModeBtn=(Button)findViewById(R.id.btn_shoot_photo_mode);
        mCaptureBtn=(Button)findViewById(R.id.btn_capture);
        mVideoSurface=(TextureView) findViewById(R.id.video_previewer_surface);
        mRecordBtn=(ToggleButton) findViewById(R.id.btn_record);
        mBlueToothBtn=(Button)findViewById(R.id.btn_bluetooth);
        data_show_screen=(TextView) findViewById(R.id.in);
        sv = (ScrollView)findViewById(R.id.ScrollView01);  //得到翻页句柄

        //Hugo 201611119 begin
        mChartBtn=(Button)findViewById(R.id.btn_chart);
        //Hugo 201611119 end


        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        mCaptureBtn.setOnClickListener(this);
        //Hugo 20161119 begin
        mChartBtn.setOnClickListener(this);
        //Hugo20161119 end
        mRecordBtn.setOnClickListener(this);
        mShootPhotoModeBtn.setOnClickListener(this);
        mRecordVideoModeBtn.setOnClickListener(this);
        mBlueToothBtn.setOnClickListener(this);

        recordingTime.setVisibility(View.INVISIBLE);
//        data_show_screen.setVisibility(View.VISIBLE);

        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startRecord();
                } else {
                    stopRecord();
                }
            }
        });
    }

    private void initPreviewer() {

        DJIBaseProduct product = FPVDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UnknownAircraft)) {
                DJICamera camera = product.getCamera();
                if (camera != null){
                    // Set the callback
                    camera.setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallBack);
                }
            }
        }
    }

    private void uninitPreviewer() {
        DJICamera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            FPVDemoApplication.getCameraInstance().setDJICameraReceivedVideoDataCallback(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_capture:{
                captureAction();
                break;
            }
            case R.id.btn_shoot_photo_mode:{
                switchCameraMode(DJICameraSettingsDef.CameraMode.ShootPhoto);
                break;
            }
            case R.id.btn_record_video_mode:{
                switchCameraMode(DJICameraSettingsDef.CameraMode.RecordVideo);
                break;
            }
            //Hugo 20161119 begin
            case R.id.btn_chart:{
                Intent intent=new Intent(this,ChartActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.btn_bluetooth:{
                if(!_bluetooth.isEnabled()){  //如果蓝牙服务不可用则提示
                    Toast.makeText(this, " 打开蓝牙中...", Toast.LENGTH_LONG).show();
                    return;
                }
                if(_socket==null){
                    Intent serverIntent = new Intent(this, DeviceListActivity.class); //跳转程序设置
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);  //设置返回宏定义
                }
                else{
                    //关闭连接socket
                    try{

                        is.close();
                        _socket.close();
                        _socket = null;
                        bRun = false;
                    }catch(IOException e){}
                }
                break;
            }
            default:
                break;
        }
    }

    private void switchCameraMode(DJICameraSettingsDef.CameraMode cameraMode){

        DJICamera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setCameraMode(cameraMode, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        showToast("Switch Camera Mode Succeeded");
                    } else {
                        showToast(error.getDescription());
                    }
                }
            });
            }

    }

    // Method for taking photo
    private void captureAction(){

        DJICameraSettingsDef.CameraMode cameraMode = DJICameraSettingsDef.CameraMode.ShootPhoto;

        final DJICamera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {

            DJICameraSettingsDef.CameraShootPhotoMode photoMode = DJICameraSettingsDef.CameraShootPhotoMode.Single; // Set the camera capture mode as Single mode
            camera.startShootPhoto(photoMode, new DJICommonCallbacks.DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        showToast("take photo: success");
                    } else {
                        showToast(error.getDescription());
                    }
                }

            }); // Execute the startShootPhoto API
        }
    }

    // Method for starting recording
    private void startRecord(){

        DJICameraSettingsDef.CameraMode cameraMode = DJICameraSettingsDef.CameraMode.RecordVideo;
        final DJICamera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new DJICommonCallbacks.DJICompletionCallback(){
                @Override
                public void onResult(DJIError error)
                {
                    if (error == null) {
                        showToast("Record video: success");
                    }else {
                        showToast(error.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord(){

        DJICamera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new DJICommonCallbacks.DJICompletionCallback(){

                @Override
                public void onResult(DJIError error)
                {
                    if(error == null) {
                        showToast("Stop recording: success");
                    }else {
                        showToast(error.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }
    }


    //接收活动结果，响应startActivityForResult()
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case REQUEST_CONNECT_DEVICE:     //连接结果，由DeviceListActivity设置返回
                // 响应返回结果
                if (resultCode == Activity.RESULT_OK) {   //连接成功，由DeviceListActivity设置返回
                    // MAC地址，由DeviceListActivity设置返回
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // 得到蓝牙设备句柄
                    _device = _bluetooth.getRemoteDevice(address);

                    // 用服务号得到socket
                    try{
                        _socket = _device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
                    }catch(IOException e){
                        Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                    }
                    //连接socket
                    try{
                        _socket.connect();
                        Toast.makeText(this, "连接"+_device.getName()+"成功！", Toast.LENGTH_SHORT).show();
                    }catch(IOException e){
                        try{
                            Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                            _socket.close();
                            _socket = null;
                        }catch(IOException ee){
                            Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                        }

                        return;
                    }

                    //打开接收线程
                    try{
                        is = _socket.getInputStream();   //得到蓝牙数据输入流
                        Toast.makeText(this, "蓝牙数据输入流开启！", Toast.LENGTH_SHORT).show();
                    }catch(IOException e){
                        Toast.makeText(this, "接收数据失败！", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(!bThread){
                        ReadThread.start();
                        bThread=true;
                    }else{
                        bRun = true;
                    }
                }
                break;
            default:
                break;
        }
    }

    float[] min_dist = new float[10];
    private float Middle_Slider_Fitler(float[] temp, float current,int num)
    {
        int count, i, j, k;
        float TempChange = 0;
        float[] TempBuf = new float[30];
        for (k = 0; k < num - 1; k++)
        {
            temp[k] = temp[k + 1];
        }
        temp[num - 1] = current;
        for (count = 0; count < num; count++)
        {
            TempBuf[count] = temp[count];
        }
        for (j = 0; j < num; j++)
        {
            for (i = 0; i < num - j; i++)
            {
                if (TempBuf[i] > TempBuf[i + 1])
                {
                    TempChange = TempBuf[i];
                    TempBuf[i] = TempBuf[i + 1];
                    TempBuf[i + 1] = TempChange;
                }
            }
        }
        return TempBuf[1];
    }

    Thread ReadThread=new Thread(){

        public void run(){
            int num = 0;
            byte[] buffer = new byte[1024];
            bRun = true;
            //接收线程
            while(true){
                try{
                    while(is.available()==0){
                        while(bRun == false){}
                    }
                    while(true){
                        num = is.read(buffer);         //读入数据

                        String s0 = new String(buffer,0,num);
                        fmsg+=s0;    //保存收到数据

                        if((buffer[0]& 0xff)==0x11&&(buffer[3]& 0xff)==0x22)
                        {
                            float current_dis=(float)((buffer[1]& 0xff)<<8|(buffer[2]& 0xff));
                            mid_dis=Middle_Slider_Fitler(min_dist, current_dis,10);
                            smsg=String.valueOf("mid_dis: "+mid_dis);
                        }
                        //                        if((buffer[0]& 0xff)==0x11&&(buffer[7]& 0xff)==0x22)
//                        {
//                            mid_dis=(buffer[1]& 0xff)<<8|(buffer[2]& 0xff);
//                            angle=((buffer[3]& 0xff)<<8|(buffer[4]& 0xff))/10;
//                            distance=(buffer[5]& 0xff)<<8|(buffer[6]& 0xff);
//                            smsg=String.valueOf("mid_dis: "+mid_dis);
//                            //smsg=String.valueOf("angle: "+angle+"distance: "+distance);
//                        }
                        if(is.available()==0)break;  //短时间没有数据才跳出进行显示
                    }
                    //发送显示消息，进行显示刷新
                    handler.sendMessage(handler.obtainMessage());
                }catch(IOException e){
                }
            }
        }
    };

    //消息处理队列
    Handler handler= new Handler(){
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            data_show_screen.setText(smsg);   //显示数据
            Log.e(MainActivity.TAG, smsg);
            sv.scrollTo(0,data_show_screen.getMeasuredHeight()); //跳至数据最后一页
        }
    };
}

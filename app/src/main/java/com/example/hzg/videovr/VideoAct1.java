package com.example.hzg.videovr;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.hzg.videovr.utils.Filtering;
import com.example.hzg.videovr.videoio.VideoReader;
import com.example.hzg.videovr.videoio.VideoReaderForVideo;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * Created by hzg on 2017/2/21.
 */

public class VideoAct1 extends AppCompatActivity implements SensorEventListener {
    VideoShowView videoShowView;
    private String dataPath;
    private VideoCapture videoCapture;
    private Button btnStart;
    private SensorManager sensorManager;
    private float dataY;
    private float dataX;
    private float dataZ;
    private boolean isRun=false;
    private boolean isChange=false;
    private boolean isRight;
    private double angleCount;
    private int angleChangecount;
   Filtering xFiltering=new Filtering(3);
   Filtering yFiltering=new Filtering(3);
    private VideoReaderForVideo videoReader;
    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    videoReader=new VideoReaderForVideo(dataPath);
                    break;
                default:
                    super.onManagerConnected(status);
            }

        }
    };
    private boolean isRecord=false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.video_act);
        videoShowView= (VideoShowView) findViewById(R.id.videoshowview);
        dataPath=getIntent().getStringExtra("path");
        Log.d("VideoAct","UI Thread:"+Thread.currentThread());

        Log.d("VideoAct","OnCreate#Intent datapath："+dataPath);
        if (dataPath==null)
        {
            Log.d("Vide0Act","OnCreate#eerror,please give a path");
            finish();
        }
        sensorManager= (SensorManager) getSystemService(SENSOR_SERVICE);
        btnStart= (Button) findViewById(R.id.btn_start);
        btnStart.setText("状态：" + (isRun ? "开启" : "停止"));
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (isRun==true) {
                    isRun = false;
                    btnStart.setText("状态：停止");
                }
                else {
                    isRun=true;
                    btnStart.setText("状态：开启");
                    if (videoReader==null)
                   videoReader = new VideoReaderForVideo(dataPath);
                    final Mat mat=new Mat();
                     final int count =videoReader.getLength()-1;
                    final double countper=count/180.0;
                    final  int Index=count/2;
                    final  int initX=videoReader.getSensor(Index);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("VideoAct","new Thread:"+Thread.currentThread());
                            int startX= (int) dataX;
                            while (isRun) {
                                if (isRecord) {
                                    int positon =0;
                                    int nowX = (int) dataX;

                                    if (videoReader.getType() == VideoReader.TYPE_HORIZONTAL) {
                                        positon = (nowX - startX +Index+360) % 360;
                                    } else
                                        positon = (nowX-startX + Index+360) % 360;

                                    Log.d("position", "" + positon);
                                   // positon = positon < 1 ?1 : (positon > count ? count : positon);
                                    Log.d("VidoeAct", "dataX: " + nowX + "  startX:" + startX + " position:" + positon + " count:" + count);
                                    if (positon>0&&positon<count) {
                                        videoReader.readMat(positon, mat);
                                        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGBA);
                                        videoShowView.showBitmap(mat);
                                    }
                                    isRecord = false;

                                }
                            }

                        }
                    }).start();
                }
            }
        });

}


//以前的尝试，弃用
    private void readvideo1(Mat mat, double counteverangle, double count) {
        int i;
        Log.d("VideoAct","dataX:"+dataX);
        double positon=dataX*counteverangle<count?dataX*counteverangle:count;
        Log.d("VideoAct","position:"+positon);
        Log.d("VideoAct","isRight:"+isRight);
        videoCapture.set(Videoio.CAP_PROP_POS_FRAMES,positon);
        i=0;
        if (isRight) {
            while (i < angleCount*counteverangle && videoCapture.read(mat)) {
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGBA);
                videoShowView.showBitmap(mat);
                i++;
            }
        }
        else
        {
            while (i < angleCount*counteverangle) {
            videoCapture.set(Videoio.CAP_PROP_POS_FRAMES,positon-i);
               if (videoCapture.read(mat)) {
                   Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGBA);
                   videoShowView.showBitmap(mat);
                   i++;
               }
            }
        }
        isChange=false;
    }

    protected void onPause() {
        super.onPause();
        isRun=false;
        sensorManager.unregisterListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION));
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_GAME);
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, baseLoaderCallback);
        } else {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        }

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType())
        {
            case  Sensor.TYPE_ORIENTATION:
                xFiltering.put(sensorEvent.values[0]);
                yFiltering.put(sensorEvent.values[1]);
                if (!isRecord) {
                    if (videoReader!=null&&videoReader.getType() == VideoReader.TYPE_HORIZONTAL)
                        dataX = xFiltering.getResult();
                    else dataX = yFiltering.getResult();
                    isRecord=true;
                }
                break;

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onDestroy() {
        isRun=false;
        super.onDestroy();
    }
}

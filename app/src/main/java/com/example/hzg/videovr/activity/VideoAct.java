package com.example.hzg.videovr.activity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.hzg.videovr.R;
import com.example.hzg.videovr.VideoShowView;
import com.example.hzg.videovr.videoio.VideoReaderForVideo;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * Created by hzg on 2017/2/21.
 */

public class VideoAct extends AppCompatActivity implements SensorEventListener {
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
    private float[] wfFloats=new float[5];
    private int wfIndex=0;
    private VideoReaderForVideo videoReader;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_act);
        videoShowView= (VideoShowView) findViewById(R.id.videoshowview);
        dataPath=getIntent().getStringExtra("path");
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
                    if (videoCapture==null)
                    videoCapture = new VideoCapture(dataPath);
                     final double count = videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
                    final double counteverangle=count/360;
                    Log.d("VideoAct","VideoCapture is Open?"+videoCapture.isOpened());
                    Log.d("VideoAct","VideoCapture Frame Count:"+count+"\nper angle frame count:"+counteverangle);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final Mat mat = new Mat();
                            int i= 0;
                            while (isRun) {
                                final double positon=dataX*counteverangle;
                                videoCapture.set(Videoio.CAP_PROP_POS_FRAMES,positon);
                                videoCapture.read(mat);
                                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGBA);
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        videoShowView.showBitmap(mat);
                                    }
                                }).start();
                                i++;
                                Log.d("VidoeAct","show count: "+i +"  angle change count"+angleChangecount);
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


    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType())
        {
            case  Sensor.TYPE_ORIENTATION:



//                    float x=sensorEvent.values[0];
//                    float y=sensorEvent.values[1];
//                    float z=sensorEvent.values[2];

//                    if (Math.abs(dataY-y)<5&&Math.abs(dataZ-z)<5&&Math.abs(dataX-x)>5)
//                    {
//                        // Log.d("record","x:"+x);
//
//                        isChange=true;
//                        if (dataX-x<0)
//                        isRight=false;
//                        else isRight=true;
//                        angleCount=Math.abs(dataX-x);
//                        dataX=x;
//                    }
                wfFloats[wfIndex++%wfFloats.length]=sensorEvent.values[0];
                float wfX=(wfFloats[0]+wfFloats[1]+wfFloats[2]+wfFloats[3]+wfFloats[4])/wfFloats.length;
                 dataX=wfX;
                //dataX=sensorEvent.values[0];
                 angleChangecount++;

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

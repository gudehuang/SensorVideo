package com.example.hzg.videovr;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
public class MainActivity1 extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
,SensorEventListener, View.OnClickListener {
    private CameraJavaView cameraview;
    private String TAG="MainActivity1";
    private  String sensorTextFomart="方位角：%s\n仰俯角：%s\n横滚角：%s";
    private  String dataDir= Environment.getExternalStorageDirectory().getPath()+"/360video";
    private final int SHOW_SENSOR_TEXT=0x001;
    private final int CANCEL_PROGRESSDIALOG=0x002;
    private  boolean isStart=false,isRecord=false;
    private HashMap<Integer,Mat> dataMap;
    private  float dataX,dataY,dataZ;
    private TextView sensorTv;
    private Button startBtn,showBtn;
    private SensorManager sensorManager;
    private ProgressDialog progressDialog;
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case  SHOW_SENSOR_TEXT:
                   float[] resutlt= (float[]) msg.obj;
                     sensorTv.setText(String.format(sensorTextFomart,resutlt[0],resutlt[1],resutlt[2]));
                    break;
                case  CANCEL_PROGRESSDIALOG:
                      progressDialog.dismiss();
                    progressDialog=null;
                    String result= (String) msg.obj;
                    Toast.makeText(MainActivity1.this,result,Toast.LENGTH_LONG);
                default:

            }
        }
    };
    private BaseLoaderCallback baseLoaderCallback=new BaseLoaderCallback(this) {
    @Override
    public void onManagerConnected(int status) {
        switch (status)
        {
            case  BaseLoaderCallback.SUCCESS:
                cameraview.enableView();
                break;
            default:
                super.onManagerConnected(status);
        }

    }
};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);
        }
        cameraview= (CameraJavaView) findViewById(R.id.javacameraview);
        cameraview.setVisibility(SurfaceView.VISIBLE);
        cameraview.setCvCameraViewListener(this);
        init();
    }

    private void init() {
        sensorTv= (TextView) findViewById(R.id.sensor_text);
        startBtn= (Button) findViewById(R.id.start_btn);
        showBtn= (Button) findViewById(R.id.show_btn);
        startBtn.setOnClickListener(this);
        showBtn.setOnClickListener(this);
        sensorManager= (SensorManager) getSystemService(SENSOR_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},2);
        }
        File file=new File(dataDir);
        if (!file.exists())file.mkdir();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode==1)
        {
            if (grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {}
            else {
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraview!=null)
            cameraview.disableView();
        sensorManager.unregisterListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION));
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_UI);

        if (!OpenCVLoader.initDebug())
        {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0,this,baseLoaderCallback);
        }
        else {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraview!=null)
        {cameraview.disableView();}
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
         Log.d("onCameraViewStarted",width+"*"+height);

    }

    @Override
    public void onCameraViewStopped() {
Log.d("onCameraViewStopped","");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
           Mat mRgba=inputFrame.rgba();
           // Core.flip(mRgba,mRgba,1); //flip（源文件，目标文件，0/1/-1）0表示绕x轴翻转。1绕y轴，-1绕x、y轴
           Core.transpose(mRgba, mRgba);
           Core.flip(mRgba,mRgba,1);
              if (isStart&&isRecord)
              {
                  isRecord=false;
                  dataMap.put((int) dataX,mRgba);
                  Log.d("put","x"+dataX+" size="+dataMap.size());
              }
        return mRgba;

    }
    private  int getPreviewDegree()
    {
        WindowManager windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        Log.d(TAG,"rotation is :"+rotation);
        int degree=0;
        switch (rotation)
        {
            case Surface.ROTATION_0:

                degree=90;
                break;
            case Surface.ROTATION_90:
                degree=0;
                break;
            case Surface.ROTATION_180:
                degree=0;
                break;
            case  Surface.ROTATION_270:
                degree=180;
                break;
        }
        return degree;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType())
        {
            case  Sensor.TYPE_ORIENTATION:
                Message message=Message.obtain();
                message.what=SHOW_SENSOR_TEXT;
                message.obj=sensorEvent.values;
                handler.sendMessage(message);

                if (isStart)
                {
                    float x=sensorEvent.values[0];
                    float y=sensorEvent.values[1];
                    float z=sensorEvent.values[2];
                    if (Math.abs(dataY-y)<5&&Math.abs(dataZ-z)<5&&!dataMap.containsKey((int)x))
                    {
                        Log.d("record","x:"+x);
                        isRecord=true;
                        dataX=x;
                    }

                }
                dataY=sensorEvent.values[1];
                dataZ=sensorEvent.values[2];
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    @Override
    public void onClick(View view) {

        switch (view.getId())
        {
            case R.id.start_btn:
                     isStart=!isStart;
                if (isStart)
                {
                    dataMap=new HashMap<>();
                    startBtn.setBackgroundColor(Color.RED);
                   startBtn.setText("停止");
                }

                else {
                   progressDialog= ProgressDialog.show(this,"文件保存中","正在处理，请耐心等待",false,false);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                            String currentDateandTime = sdf.format(new Date());
                            String filename=dataDir+"/"+currentDateandTime+".vr";
                            Set<Integer> keys=dataMap.keySet();
                            HashMap<Integer,String> map=new HashMap<>();
                            for (int  i:keys)
                            {
                                Mat mat=dataMap.get(i);
                                map.put(i,OpenCvUtils.matToJson(mat,i));
                            }
                            try {
                                ObjectOutputStream oos=new ObjectOutputStream(new FileOutputStream(filename));
                                oos.writeObject(map);
                                oos.close();
                                dataMap=null;
                                map=null;
                                Message message=Message.obtain();
                                message.what=CANCEL_PROGRESSDIALOG;
                                message.obj="保存成功"+filename;
                                handler.sendMessage(message);
                                Log.d("writeObject","success: name="+filename);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Message message=Message.obtain();
                                message.what=CANCEL_PROGRESSDIALOG;
                                message.obj="保存失败"+filename+"\n"+e.getMessage();
                                handler.sendMessage(message);
                                Log.d("writeObject","error "+e.getMessage());
                            }
                        }
                    }).start();



                    startBtn.setBackgroundColor(Color.CYAN);
                    startBtn.setText("开启");
                }
                break;
            case R.id.show_btn:

                AlertDialog.Builder builder=new AlertDialog.Builder(this);
                builder.setTitle("选择文件");
                File file=new File(dataDir);
                final String[] list=file.list();
                final int[] select = {0};
                builder.setSingleChoiceItems(list, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        select[0] =i;
                    }
                });
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent=new Intent(MainActivity1.this,ListAct.class);
                        intent.putExtra("path",dataDir+"/"+list[select[0]]);
                        startActivity(intent);
                    }
                });
                builder.setNegativeButton("取消",null);
                builder.show();
                break;

        }
    }
}

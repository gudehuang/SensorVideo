package com.example.hzg.videovr;

import android.Manifest;
import android.app.ProgressDialog;
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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import static com.example.hzg.videovr.myUtils.showFilesDialog;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
        , SensorEventListener, View.OnClickListener {
    private CameraBridgeViewBase cameraview;
    private String TAG = "MainActivity";
    private String sensorTextFomart = "方位角：%s\n仰俯角：%s\n横滚角：%s";
    private String dataDir = Environment.getExternalStorageDirectory().getPath() + "/360video";
    private final int SHOW_SENSOR_TEXT = 0x001;
    private final int CANCEL_PROGRESSDIALOG = 0x002;
    private boolean isStart = false, isRecord = false;
    private int dataX;
    private int countk;
    private TextView sensorTv;
    private Button startBtn, starBtn1;
    private SensorManager sensorManager;
    private ProgressDialog progressDialog;
    private Size framesize;
    private String filename;
    private  VideoRecoder videoRecoder;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_SENSOR_TEXT:
                    float[] resutlt = (float[]) msg.obj;
                    sensorTv.setText(String.format(sensorTextFomart, resutlt[0], resutlt[1], resutlt[2]));
                    break;
                case CANCEL_PROGRESSDIALOG:
                    progressDialog.dismiss();
                    progressDialog = null;
                    Toast.makeText(MainActivity.this, filename + "已保存", Toast.LENGTH_LONG).show();
                default:

            }
        }
    };
    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        cameraview = (CameraBridgeViewBase) findViewById(R.id.javacameraview);
        cameraview.setVisibility(SurfaceView.VISIBLE);
        cameraview.setCvCameraViewListener(this);
        init();

    }

    private void init() {
        sensorTv = (TextView) findViewById(R.id.sensor_text);
        startBtn = (Button) findViewById(R.id.start_btn);
        starBtn1 = (Button) findViewById(R.id.show_btn);
        startBtn.setOnClickListener(this);
        starBtn1.setOnClickListener(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
        }
        File file = new File(dataDir);
        if (!file.exists()) file.mkdir();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraview != null)
            cameraview.disableView();
        sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION));
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);

        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, baseLoaderCallback);
        } else {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraview != null) {
            cameraview.disableView();
        }
        videoRecoder=null;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d("onCameraViewStarted", width + "*" + height);
        framesize = new Size(height, width);

    }

    @Override
    public void onCameraViewStopped() {
        Log.d("onCameraViewStopped", "");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mRgba = inputFrame.rgba();
//        // Core.flip(mRgba,mRgba,1); //flip（源文件，目标文件，0/1/-1）0表示绕x轴翻转。1绕y轴，-1绕x、y轴
//        Core.transpose(mRgba, mRgba);//transpose（src,targe） 将目标倒转 如840*480变为480*840
//        Core.flip(mRgba, mRgba, 1);
        if (isStart) {
            if (isRecord&&videoRecoder.isOpened()) {
                videoRecoder.write(mRgba,dataX);
                isRecord=false;
            }
        }

        return mRgba;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ORIENTATION:
                Message message = Message.obtain();
                message.what = SHOW_SENSOR_TEXT;
                message.obj = sensorEvent.values;
                handler.sendMessage(message);
                int x= (int) sensorEvent.values[0];
                if(isStart&&!isRecord&&!videoRecoder.contains(x)) {
                    dataX = x;
                    isRecord=true;
                    Log.d("test",""+countk++);
                }
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 100, 0, "查看-图片(list)").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 300, 0, "查看-图片(set)").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 200, 0, "查看-视频").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case 100:
                showFilesDialog(this, dataDir,"list" ,ListAct.class);
                break;
            case 200:
                showFilesDialog(this, dataDir, "",VideoAct.class);
                break;
            case 300:
                showFilesDialog(this,dataDir,"set",ListAct.class);
                break;
        }
        return true;
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.start_btn:
                if (isStart)
                {
                    isStart=false;
                    videoRecoder.saveToSdcard();
                    videoRecoder=null;
                    startBtn.setText("录制（List）");
                    startBtn.setBackgroundColor(Color.argb(0,0,0,0));
                    starBtn1.setClickable(true);

                }
                else
                {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                    String currentDateandTime = sdf.format(new Date());
                    filename = dataDir + "/" + currentDateandTime + ".avi";
                    videoRecoder=new VideoRecoderList(this,filename,VideoWriter.fourcc('M', 'J', 'P', 'G'),18,framesize);
                    startBtn.setText("录制中");
                    startBtn.setBackgroundColor(Color.RED);
                    starBtn1.setClickable(false);
                    isStart=true;
                }
                break;
            case R.id.show_btn:
                if (isStart)
                {
                    isStart=false;
                    videoRecoder.saveToSdcard();
                    videoRecoder=null;
                    starBtn1.setText("录制（Set）");
                    starBtn1.setBackgroundColor(Color.argb(0,0,0,0));
                    startBtn.setClickable(true);

                }
                else
                {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                    String currentDateandTime = sdf.format(new Date());
                    filename = dataDir + "/" + currentDateandTime + ".avi";
                    videoRecoder=new VideoRecoderSet(this,filename,VideoWriter.fourcc('M', 'J', 'P', 'G'),18,framesize);
                    starBtn1.setText("录制中");
                    starBtn1.setBackgroundColor(Color.RED);
                    startBtn.setClickable(false);
                    isStart=true;
                }
                break;

        }
    }


}

package com.example.hzg.videovr;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
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
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.example.hzg.videovr.myUtils.showFilesDialog;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
        , SensorEventListener, View.OnClickListener {
    private CameraJavaView cameraview;
    private String TAG = "MainActivity1";
    private String sensorTextFomart = "方位角：%s\n仰俯角：%s\n横滚角：%s";
    private String dataDir = Environment.getExternalStorageDirectory().getPath() + "/360video";
    private final int SHOW_SENSOR_TEXT = 0x001;
    private final int CANCEL_PROGRESSDIALOG = 0x002;
    private boolean isStart = false, isRecord = false, isRecordSensor = false;
    private float dataY, dataZ;
    private float dataX;
    private TextView sensorTv;
    private Button startBtn, showBtn;
    private SensorManager sensorManager;
    private ProgressDialog progressDialog;
    private VideoWriter videoWriter;
    private Size framesize;
    private int i = 0;
    private Mat matTemp;
    private Thread writerThread;
    private ArrayList<Mat> MatData = new ArrayList<>();
    private ArrayList<Mat> cacheMatData=new ArrayList<>() ;
    private ArrayList<Mat> tempMatData ;
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
    private ArrayList<Float> sensorData;
    private String filename;
    private boolean isWriteImmediately = false;
    private int k=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        cameraview = (CameraJavaView) findViewById(R.id.javacameraview);
        cameraview.setVisibility(SurfaceView.VISIBLE);
        cameraview.setCvCameraViewListener(this);
        init();
    }

    private void init() {
        sensorTv = (TextView) findViewById(R.id.sensor_text);
        startBtn = (Button) findViewById(R.id.start_btn);
        showBtn = (Button) findViewById(R.id.show_btn);
        startBtn.setOnClickListener(this);
        showBtn.setOnClickListener(this);
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
        // Core.flip(mRgba,mRgba,1); //flip（源文件，目标文件，0/1/-1）0表示绕x轴翻转。1绕y轴，-1绕x、y轴
        Core.transpose(mRgba, mRgba);//transpose（src,targe） 将目标倒转 如840*480变为480*840
        Core.flip(mRgba, mRgba, 1);
        if (isStart) {
            if (videoWriter.isOpened()) {
                sensorData.add(dataX);
                if (isWriteImmediately)
                    videoWriter.write(mRgba);
                else {
                    MatData.add(mRgba.clone());
                    Log.d("read",""+i++);
//                    if (isRecord&&MatData.size()>20) {
//                        isRecord=false;
//                        cacheMatData=MatData;
//                        MatData=new ArrayList<>();
//                        Log.d("swap",MatData+"#"+cacheMatData);
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                for (Mat mat:cacheMatData) {
//                                    videoWriter.write(mat);
//                                    Log.d("write",""+k++);
//                                }
//                                cacheMatData.clear();
//                                cacheMatData=null;
//                                isRecord=true;
//                            }
//                        }).start();
//                    }
                }
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
                dataX = sensorEvent.values[0];
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 100, 0, "查看-图片").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 200, 0, "查看-视频").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case 100:
                showFilesDialog(this, dataDir, ListAct.class);
                break;
            case 200:
                showFilesDialog(this, dataDir, VideoAct.class);
                break;


        }
        return true;
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.start_btn:
                startbtn(true);
                if (isStart) {
                    startBtn.setText("录制中");
                    showBtn.setClickable(false);
                } else {
                    startBtn.setText("录制（直接写入慢）");
                    showBtn.setClickable(true);
                }
                break;
            case R.id.show_btn:
                startbtn(false);
                if (isStart) {
                    isRecord=true;
                    showBtn.setText("录制中");
                    startBtn.setClickable(false);
                } else {
                    showBtn.setText("录制（间接写入快）");
                    startBtn.setClickable(true);
                }
                break;

        }
    }

    private void startbtn(boolean isWriteImmediately) {
        if (!isStart) {
            startRecordVideo(isWriteImmediately);

        } else {
            stopRecordVideo(isWriteImmediately);

        }
    }

    private void stopRecordVideo(boolean isWriteImmediately) {
        isStart = false;
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename + ".vr"));
            oos.writeObject(sensorData);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (sensorData != null) sensorData.clear();
        if (isWriteImmediately) {
            videoWriter.release();
            videoWriter = null;
            Toast.makeText(MainActivity.this, filename + "已保存", Toast.LENGTH_LONG).show();
        } else {
            progressDialog = ProgressDialog.show(this, "视频保存中", "视频保存中,请耐心等待....", false, false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (Mat mat : MatData) {
                        videoWriter.write(mat);
                    }
                    if (MatData != null) MatData.clear();
                    videoWriter.release();
                    Log.d(TAG, "videoWriter is release?" + videoWriter.isOpened());
                    Log.d(TAG, "MatData is clear?" + MatData.size());
                    videoWriter = null;
                    Message msg = Message.obtain();
                    msg.what = CANCEL_PROGRESSDIALOG;
                    handler.sendMessage(msg);
                }
            }).start();
        }
    }

    private void startRecordVideo(boolean isWriteImmediately) {
        this.isWriteImmediately = isWriteImmediately;
        if (videoWriter == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            String currentDateandTime = sdf.format(new Date());
            filename = dataDir + "/" + currentDateandTime + ".avi";
            videoWriter = new VideoWriter(filename, VideoWriter.fourcc('M', 'J', 'P', 'G'), 20, framesize);
            Log.d("VideoWriter", "videoWriter is create:" + filename + " size:" + framesize);
            Log.d("VideoWriter", "videoWriter is open:" + videoWriter.isOpened());
        }
        if (sensorData == null) {
            sensorData = new ArrayList<>();
            Log.d("VideoWriter", "create sensorData:" + sensorData);
        }
        if (sensorData.size() != 0) {
            sensorData.clear();
            Log.d("VideoWriter", "sensorData clear:" + sensorData);
        }
        isStart = true;
    }

    //    private void showFilesDialog(final Context context, final String dirPath, final Class target) {
//        File file = new File(dirPath);
//        final String[] list = file.list();
//        final ArrayList<String> arrayList = new ArrayList<String>();
//        for (String line : list) {
//            if (!line.contains(".vr"))
//                arrayList.add(line);
//
//        }
//        final String[] results = new String[arrayList.size()];
//        arrayList.toArray(results);
//        if (results.length < 1) {
//
//            Toast.makeText(context, "没有文件", Toast.LENGTH_LONG).show();
//        } else {
//
//
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle("选择文件");
//
//
//            final int[] select = {0};
//            builder.setSingleChoiceItems(results, 0, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    select[0] = i;
//                }
//            });
//            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    Intent intent = new Intent(MainActivity.this, target);
//                    intent.putExtra("path", dataDir + "/" + results[select[0]]);
//                    startActivity(intent);
//
//                }
//            });
//            builder.setNegativeButton("取消", null);
//            builder.setNeutralButton("删除", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    new File(dataDir + "/" + results[select[0]]).delete();
//                    new File(dataDir + "/" + results[select[0]]+".vr").delete();
//                    showFilesDialog(context, dirPath, target);
//                }
//            });
//            builder.show();
//        }
//    }

}

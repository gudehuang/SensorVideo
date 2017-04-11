package com.example.hzg.videovr;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hzg.videovr.show.FileActivity;
import com.example.hzg.videovr.utils.Filtering;
import com.example.hzg.videovr.utils.myUtils;
import com.example.hzg.videovr.videoio.VideoReader;
import com.example.hzg.videovr.videoio.VideoRecoderList;
import com.hoan.dsensor_master.DProcessedSensor;
import com.hoan.dsensor_master.DSensor;
import com.hoan.dsensor_master.DSensorEvent;
import com.hoan.dsensor_master.DSensorManager;
import com.hoan.dsensor_master.interfaces.DProcessedEventListener;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.example.hzg.videovr.utils.myUtils.showFilesDialog;

public class MainActivityCv4Land extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
        , SensorEventListener, View.OnClickListener, DProcessedEventListener {

    private CameraBridgeViewBase cameraview;
    private String TAG = "ShowActivity";
    private String sensorTextFomart = "方位角：%s\n仰俯角：%s\n横滚角：%s";
   public static String dataDir = Environment.getExternalStorageDirectory().getPath() + "/360video";
   public static String dataDirV = dataDir+"/"+"vertical";
   public static String dataDirH = dataDir+"/"+"horizontal";
   public static String dataDirA = dataDir+"/"+"all";
    private final int SHOW_SENSOR_TEXT = 0x001;
    private final int CANCEL_PROGRESSDIALOG = 0x002;
    private static final int SHOW_RECORD_TEXT =0x003 ;
    private boolean isStart = false, isRecord = false;
    private int dataX;
    private int countk;
    private TextView sensorTv;
    private SensorManager sensorManager;
    private ProgressDialog progressDialog;
    private Size framesize;
    private String filename;
    private VideoRecoderList videoRecoder;
    private Filtering xFlitering = new Filtering(3);
    private Filtering yFlitering = new Filtering(3);
    private Filtering zFlitering = new Filtering(3);
    private ButtonOnclick buttonOnclick;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_SENSOR_TEXT:
                    float[] resutlt = (float[]) msg.obj;
                    sensorTv.setText(String.format(sensorTextFomart, resutlt[0], resutlt[1], resutlt[2]));
                    Log.d("sensor show","time");
                    break;
                case CANCEL_PROGRESSDIALOG:
                    progressDialog.dismiss();
                    progressDialog = null;
                    Toast.makeText(MainActivityCv4Land.this, filename + "已保存", Toast.LENGTH_LONG).show();
                    break;
                case SHOW_RECORD_TEXT:
                    if (videoRecoder.isOpened())
                    recodeTv.setText("已写入"+videoRecoder.getSize());
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
    private int dataY;
    private  float dataG;
    private ImageButton btn_record,btn_show,btn_switch;
    private  TextView recodeTv;
    private Chronometer chronometer;
    private CoordinatorLayout coordinatorLayout;
    private int courseangle;
    private int pitchangle;
    private int mDProcessedSensorType= DProcessedSensor.TYPE_3D_COMPASS ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main_land);




        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        cameraview = (CameraBridgeViewBase) findViewById(R.id.javacameraview);
        cameraview.setVisibility(SurfaceView.VISIBLE);
        cameraview.setCvCameraViewListener(this);
        init();
        Log.d("Activity","UI Thread:"+Thread.currentThread());
    }

    private void init() {
        sensorTv = (TextView) findViewById(R.id.sensor_text);
        recodeTv= (TextView) findViewById(R.id.tv_show_record);
        btn_record= (ImageButton) findViewById(R.id.imgbtn_record);
        btn_switch= (ImageButton) findViewById(R.id.imgbtn_switch);
        btn_show= (ImageButton) findViewById(R.id.imgbtn_show);
        chronometer= (Chronometer) findViewById(R.id.chronometer_record);
        coordinatorLayout= (CoordinatorLayout) findViewById(R.id.coordinator);
        chronometer.setVisibility(View.GONE);
        recodeTv.setVisibility(View.GONE);
        buttonOnclick = new ButtonOnclick(new ImageButton[]{btn_record,btn_show,btn_switch});
        btn_record.setOnClickListener(this);
        btn_switch.setOnClickListener(this);
        btn_show.setOnClickListener(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
        }
        File file = new File(dataDir);
        File file1=new File(dataDirV);
        File file2=new File(dataDirH);
        File file3=new File(dataDirA);
        File[] files=new File[]{file,file1,file2,file3};
        for (File temp:files)
        {
            if (!temp.exists()) temp.mkdir();
        }

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
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_UI);
        DSensorManager.startDProcessedSensor(this, mDProcessedSensorType,SensorManager.SENSOR_DELAY_UI, this);
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
        videoRecoder = null;
        Log.d(TAG,"onDestroy");
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d("onCameraViewStarted", width + "*" + height);
        framesize = new Size(width, height);

    }

    @Override
    public void onCameraViewStopped() {
        Log.d("onCameraViewStopped", "");
    }

    @Override
    public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat mat=inputFrame.rgba();
        if (isStart) {
            if (isRecord && videoRecoder.isOpened()) {
                if (videoRecoder.getType()== VideoReader.TYPE_HORIZONTAL&&!videoRecoder.contains(dataX)) {
                    videoRecoder.write(mat, dataX);
                    handler.sendEmptyMessage(SHOW_RECORD_TEXT);
                }
                else  if (videoRecoder.getType()==VideoReader.TYPE_VERCICAL&&!videoRecoder.contains(dataY)) {
                    videoRecoder.write(mat, dataY);
                    handler.sendEmptyMessage(SHOW_RECORD_TEXT);
                }
                else  if (videoRecoder.getType()==VideoReader.TYPE_UNKNOW&&!videoRecoder.contains(dataX,dataY)) {
                    videoRecoder.write(mat,dataX,dataY);
                    handler.sendEmptyMessage(SHOW_RECORD_TEXT);
                }

            }
            isRecord = false;
        }


        return mat;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ORIENTATION:
                yFlitering.put(sensorEvent.values[1]);
                zFlitering.put(sensorEvent.values[2]);
                Message message = Message.obtain();
                message.what = SHOW_SENSOR_TEXT;
                sensorEvent.values[0] = xFlitering.getResult();
                sensorEvent.values[1] = yFlitering.getResult();
                if (dataG<=0)
                sensorEvent.values[2] = zFlitering.getResult();
                else  sensorEvent.values[2] = -zFlitering.getResult();
                message.obj = sensorEvent.values;
                handler.sendMessage(message);
                int x = 0;
                int y=0;
                if (isStart) {
                    x = (int) sensorEvent.values[0];
                    y = (int) (sensorEvent.values[2]);

                    if (!isRecord ) {
                        if (videoRecoder.getType()==VideoReader.TYPE_UNKNOW&&!videoRecoder.contains(x,y)) {
                            if ((Math.abs(dataX-x)<1&&Math.abs(dataY-y)>1)||(Math.abs(dataY-y)<1&&Math.abs(dataX-x)>1))
                                break;
                            dataX = x;
                            dataY = y;
                            isRecord = true;
                        }
                        else if (videoRecoder.getType()==VideoReader.TYPE_HORIZONTAL&&!videoRecoder.contains(x))
                        {
                            dataX=x;
                            isRecord = true;
                        }
                        else if (videoRecoder.getType()==VideoReader.TYPE_VERCICAL&&!videoRecoder.contains(y))
                        {
                            dataY=y;
                            isRecord = true;
                        }

                        Log.d("sensor", "get sensor :" + countk++);
                    }
                }
                break;
            case Sensor.TYPE_GRAVITY:
                  dataG=sensorEvent.values[2];
                break;

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }



    @Override
    public void onClick(View view) {
        int position = buttonOnclick.getPosition(view.getId());
        switch (view.getId()) {
            case R.id.imgbtn_record:
                if (isStart) {
                    buttonOnclick.OnclickStop(position);
                } else {
                    buttonOnclick.OnclickStart(position,VideoReader.TYPE_UNKNOW);

                }
                break;
            case R.id.imgbtn_show:
                //startActivity(new Intent(this, FileActivity.class));
                myUtils.showFilesDialog(this,dataDirV,"",ListAct.class);
                break;
            case  R.id.imgbtn_switch:
                startActivity(new Intent(this, MainActivityCv4.class));
                finish();
               break;

        }
    }

    @Override
    public void onProcessedValueChanged(DSensorEvent dSensorEvent) {
        if (dSensorEvent.sensorType == DSensor.TYPE_DEPRECIATED_ORIENTATION) {

        } else {
            if (Float.isNaN(dSensorEvent.values[0])) {

            } else {
                int valueInDegree = (int) Math.round(Math.toDegrees(dSensorEvent.values[0]));
                if (valueInDegree < 0)
                    valueInDegree = (valueInDegree + 360) % 360;
                    xFlitering.put(valueInDegree);

            }
        }
    }

    /***
     * 用来处理录制按钮点击的类
     *
     */
    class ButtonOnclick {

        private ImageButton[] buttons;

        public ButtonOnclick(ImageButton[] buttons) {
            this.buttons = buttons;
        }

        /**
         * 开始录制视频
         *
         * @param position 按钮在数组的位置
         *
         * @param type     录制视频的类型  361横向  -361 纵向
         */
        public void OnclickStart(int position,int type) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            String currentDateandTime = sdf.format(new Date());
            filename =currentDateandTime;
            videoRecoder = new VideoRecoderList(MainActivityCv4Land.this, type, filename, VideoWriter.fourcc('M', 'J', 'P', 'G'), 18, framesize);
            buttons[position].setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            buttons[position].setImageResource(R.drawable.square_red);

            for (ImageButton button : buttons) {
                if (button != buttons[position]) {
                    button.setVisibility(View.GONE);
                }
            }
            recodeTv.setVisibility(View.VISIBLE);
            chronometer.setVisibility(View.VISIBLE);
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
            isStart = true;


        }

        /**
         * 停止录制视频
         *
         * @param position 按钮在数组的位置
         *
         */
        public void OnclickStop(int position) {
            isStart = false;
            videoRecoder.saveToSdcard(coordinatorLayout);
            videoRecoder = null;
            buttons[position].setScaleType(ImageView.ScaleType.CENTER);
            buttons[position].setImageResource(R.drawable.circle_red);
            for (ImageButton button : buttons) {
                button.setVisibility(View.VISIBLE);
            }
            recodeTv.setVisibility(View.GONE);
            chronometer.setVisibility(View.GONE);
            chronometer.stop();
        }

        /**
         * 查询按钮在按钮数组的位置
         *
         * @param ViewID 按钮的id
         * @return 按钮在按钮数组的位置
         */
        public int getPosition(int ViewID) {
            int positon = -1;
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i].getId() == ViewID) {
                    positon = i;
                    break;
                }
            }
            return positon;
        }

    }


}

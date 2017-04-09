package com.example.hzg.videovr;

import android.Manifest;
import android.app.ProgressDialog;
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
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hzg.videovr.show.FileActivity;
import com.example.hzg.videovr.utils.Filtering;
import com.example.hzg.videovr.videoio.VideoReader;
import com.example.hzg.videovr.videoio.VideoRecoder;
import com.example.hzg.videovr.videoio.VideoRecoderList;

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
        , SensorEventListener, View.OnClickListener {
    private CameraBridgeViewBase cameraview;
    private String TAG = "ShowActivity";
    private String sensorTextFomart = "方位角：%s\n仰俯角：%s\n横滚角：%s";
    private String dataDir = Environment.getExternalStorageDirectory().getPath() + "/360video";
    private final int SHOW_SENSOR_TEXT = 0x001;
    private final int CANCEL_PROGRESSDIALOG = 0x002;
    private boolean isStart = false, isRecord = false, isThread = false;
    private int dataX;
    private int countk;
    private TextView sensorTv;
    private Button startBtn, starBtn1;
    private SensorManager sensorManager;
    private ProgressDialog progressDialog;
    private Size framesize;
    private String filename;
    private VideoRecoder videoRecoder;
    private Mat[] mats = new Mat[2];
    private int[] sensors = new int[2];
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
    private FloatingActionButton btn_menu;
    private PopupMenu popupMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main_land);
        btn_menu= (FloatingActionButton) findViewById(R.id.float_menu);
        btn_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //openOptionsMenu();
                onPopupButtonClick(view);
                Log.d("Act","FloatActionButton OnClick");
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        cameraview = (CameraBridgeViewBase) findViewById(R.id.javacameraview);
        cameraview.setVisibility(SurfaceView.VISIBLE);
        cameraview.setCvCameraViewListener(this);
        init();
        Log.d("Activity","UI Thread:"+Thread.currentThread());
    }
    public  void  onPopupButtonClick(View button)
    {
        if (popupMenu==null) {
            popupMenu = new PopupMenu(this, button);
            popupMenu.getMenuInflater().inflate(R.menu.menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {

                    switch (item.getItemId()) {
                        case R.id.menu_photo:
                            showFilesDialog(MainActivityCv4Land.this, dataDir, "list", ListAct.class);
                            break;
                        case R.id.menu_video:
                            showFilesDialog(MainActivityCv4Land.this, dataDir, "", VideoAct1.class);
                            break;
                        case R.id.menu_vr:
                            startActivity(new Intent(MainActivityCv4Land.this, FileActivity.class));
                            finish();
                            break;
                        case R.id.menu_land:
                            startActivity(new Intent(MainActivityCv4Land.this, MainActivityCv4.class));
                            finish();
                            break;
                    }
                    return true;
                }
            });
        }
        popupMenu.show();
    }
    private void init() {
        sensorTv = (TextView) findViewById(R.id.sensor_text);
        startBtn = (Button) findViewById(R.id.start_btn);

        startBtn.setOnClickListener(this);

        buttonOnclick = new ButtonOnclick(new Button[]{startBtn});
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
        sensorManager.unregisterListener(this);
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
                if (videoRecoder.getType()== VideoReader.TYPE_HORIZONTAL)
                videoRecoder.write(mat, dataX);
                else  if (videoRecoder.getType()==VideoReader.TYPE_VERCICAL)
                videoRecoder.write(mat, dataY);
                else  if (videoRecoder.getType()==VideoReader.TYPE_UNKNOW)
                videoRecoder.write(mat, dataX,dataY);
            }
            isRecord = false;
        }


        return mat;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ORIENTATION:
                xFlitering.put(sensorEvent.values[0]);
                yFlitering.put(sensorEvent.values[1]);
                zFlitering.put(sensorEvent.values[2]);
                Message message = Message.obtain();
                message.what = SHOW_SENSOR_TEXT;
                sensorEvent.values[0] = xFlitering.getResult();
                sensorEvent.values[1] = yFlitering.getResult();
                sensorEvent.values[2] = zFlitering.getResult();
                message.obj = sensorEvent.values;
                handler.sendMessage(message);
                int x = 0;
                int y=0;
                if (isStart) {
                    x = (int) sensorEvent.values[0];
                    y = (int) sensorEvent.values[1];

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

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    @Override
    public void onClick(View view) {
        int position = buttonOnclick.getPosition(view.getId());
        switch (view.getId()) {
            case R.id.start_btn:
                if (isStart) {

                    buttonOnclick.OnclickStop(position, getResources().getString(R.string.btn_name1), Color.argb(0, 0, 0, 0));
                } else {
                    buttonOnclick.OnclickStart(position, getResources().getString(R.string.btn_name1), getResources().getColor(R.color.colorA), VideoReader.TYPE_UNKNOW);

                }
                break;
            case R.id.show_btn:
                if (isStart) {
                    buttonOnclick.OnclickStop(position, getResources().getString(R.string.btn_name2), Color.argb(0, 0, 0, 0));


                } else {
                    buttonOnclick.OnclickStart(position, getResources().getString(R.string.btn_name2), Color.RED, VideoReader.TYPE_UNKNOW);

                }
                break;

        }
    }

    /***
     * 用来处理录制按钮点击的类
     *
     */
    class ButtonOnclick {

        private Button[] buttons;

        public ButtonOnclick(Button[] buttons) {
            this.buttons = buttons;
        }

        /**
         * 开始录制视频
         *
         * @param position 按钮在数组的位置
         * @param text     按钮要设置的文本
         * @param color    按钮要设置的颜色
         * @param type     录制视频的类型  361横向  -361 纵向
         */
        public void OnclickStart(int position, String text, int color, int type) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            String currentDateandTime = sdf.format(new Date());
            filename = dataDir + "/" + currentDateandTime + ".avi";
            videoRecoder = new VideoRecoderList(MainActivityCv4Land.this, type, filename, VideoWriter.fourcc('M', 'J', 'P', 'G'), 18, framesize);
            buttons[position].setText(text);
            buttons[position].setBackgroundColor(color);

            for (Button button : buttons) {
                if (button != buttons[position]) {
                    button.setClickable(false);
                    button.setBackgroundColor(Color.DKGRAY);
                }
            }
            isStart = true;

        }

        /**
         * 停止录制视频
         *
         * @param position 按钮在数组的位置
         * @param text     按钮要设置的文本
         * @param color    按钮要设置的颜色
         */
        public void OnclickStop(int position, String text, int color) {
            isStart = false;
            videoRecoder.saveToSdcard();
            videoRecoder = null;
            buttons[position].setText(text);
            buttons[position].setBackgroundColor(color);
            for (Button button : buttons) {

                button.setClickable(true);
                button.setBackgroundColor(getResources().getColor(R.color.colorTouMing));

            }
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

//    private class RecoderWorker implements Runnable {
//
//        @Override
//        public void run() {
//            do {
//                boolean hasFrame = false;
//                synchronized (MainActivity2.this) {
//                    try {
//                        while (!mCameraFrameReady && !mStopThread) {
//                            MainActivity2.this.wait();
//                        }
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    if (mCameraFrameReady) {
//                        mChainIdx = 1 - mChainIdx;
//                        mCameraFrameReady = false;
//                        hasFrame = true;
//                    }
//                }
//
//                if (!mStopThread && hasFrame) {
//                    if (!mats[1 - mChainIdx].empty() && !videoRecoder.contains(sensors[1 - mChainIdx]))
//
//                    {
//                        videoRecoder.write(mats[1 - mChainIdx], sensors[1 - mChainIdx]);
//                        mats[1 - mChainIdx].release();
//                    }
//
//                }
//            } while (!mStopThread);
//            Log.d(TAG, "Finish processing thread");
//        }
//    }
}

package com.example.hzg.videovr.activity;

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

import com.example.hzg.videovr.ListAct;
import com.example.hzg.videovr.R;
import com.example.hzg.videovr.VideoAct1;
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

public class MainActivity1 extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
        , SensorEventListener, View.OnClickListener {
    private CameraBridgeViewBase cameraview;
    private String TAG = "ShowActivity";
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
    private VideoRecoder videoRecoder;
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
                    sensorTv.setText(String.format(sensorTextFomart, Math.toDegrees(resutlt[0]), Math.toDegrees(resutlt[1]), Math.toDegrees(resutlt[2])));
                    break;
                case CANCEL_PROGRESSDIALOG:
                    progressDialog.dismiss();
                    progressDialog = null;
                    Toast.makeText(MainActivity1.this, filename + "已保存", Toast.LENGTH_LONG).show();
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
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];
    private boolean isUpdate,isRunning;
    private Object object=new Object();

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
        isRunning=true;
        isUpdate=true;
    //   new Thread(new OrientationTherad()).start();

    }

    private void init() {
        sensorTv = (TextView) findViewById(R.id.sensor_text);
        startBtn = (Button) findViewById(R.id.start_btn);
        starBtn1 = (Button) findViewById(R.id.show_btn);
        startBtn.setOnClickListener(this);
        starBtn1.setOnClickListener(this);
        buttonOnclick = new ButtonOnclick(new Button[]{startBtn, starBtn1});
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
        isUpdate=false;
    }

    @Override
    protected void onResume() {
        super.onResume();

       // sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
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
        isRunning=false;
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
            if (isRecord && videoRecoder.isOpened()) {
                videoRecoder.write(mRgba, dataX);
            }
            isRecord = false;
        }


        return mRgba;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(sensorEvent.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(sensorEvent.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);

            updateOrientationAngles();
            Message message = handler.obtainMessage(SHOW_SENSOR_TEXT);
            message.obj = mOrientationAngles;
            handler.sendMessage(message);

        }
//        switch (sensorEvent.sensor.getType()) {
//
//            case Sensor.TYPE_ORIENTATION:
//                xFlitering.put(sensorEvent.values[0]);
//                yFlitering.put(sensorEvent.values[1]);
//                zFlitering.put(sensorEvent.values[2]);
//                Message message = Message.obtain();
//                message.what = SHOW_SENSOR_TEXT;
//                sensorEvent.values[0] = xFlitering.getResult();
//                sensorEvent.values[1] = yFlitering.getResult();
//                sensorEvent.values[2] = zFlitering.getResult();
//                message.obj = sensorEvent.values;
//                handler.sendMessage(message);
//                int x = 0;
//                if (isStart) {
//                    if (videoRecoder.getType() ==VideoReader.TYPE_VERCICAL)
//                        x = (int) sensorEvent.values[1];
//                    else x = (int) sensorEvent.values[0];
//                }
//                if (isStart && !isRecord && !videoRecoder.contains(x)) {
//                    dataX = x;
//                    isRecord = true;
//                    Log.d("test", "" + countk++);
//                }
//                break;
//        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        sensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        sensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        // "mOrientationAngles" now has up-to-date information.
    }

    class  OrientationTherad implements Runnable {
        @Override
        public void run() {
            while (isRunning)
            {
                Log.d("OrientationTherad","Thread start...");

                    while (isUpdate) {
                        updateOrientationAngles();
                        Message message = handler.obtainMessage(SHOW_SENSOR_TEXT);
                        message.obj = mOrientationAngles;
                        handler.sendMessage(message);
                        Log.d("OrientationTherad","Thread work");
                    }
            }
            Log.d("OrientationTherad","Thread finish...");

        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 100, 0, "查看-图片").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 300, 0, "展示").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 200, 0, "查看-视频").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case 100:
                showFilesDialog(this, dataDir, "list", ListAct.class);
                break;
            case 200:
                showFilesDialog(this, dataDir, "", VideoAct1.class);
                break;
            case 300:
                //showFilesDialog(this, dataDir, "", ShowActivity.class);
                break;
        }
        return true;
    }

    /**
     * 处理点击事件
     * @param view
     */
    @Override
    public void onClick(View view) {
        int position = buttonOnclick.getPosition(view.getId());
        switch (view.getId()) {
            case R.id.start_btn:
                if (isStart) {

                    buttonOnclick.OnclickStop(position, getResources().getString(R.string.btn_name1), Color.argb(0, 0, 0, 0));
                } else {
                    buttonOnclick.OnclickStart(position, getResources().getString(R.string.btn_name1), Color.RED, VideoReader.TYPE_HORIZONTAL);

                }
                break;
            case R.id.show_btn:
                if (isStart) {
                    buttonOnclick.OnclickStop(position, getResources().getString(R.string.btn_name2), Color.argb(0, 0, 0, 0));


                } else {
                    buttonOnclick.OnclickStart(position, getResources().getString(R.string.btn_name2), Color.RED, VideoReader.TYPE_VERCICAL);

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
            videoRecoder = new VideoRecoderList(MainActivity1.this, type, filename, VideoWriter.fourcc('M', 'J', 'P', 'G'), 18, framesize);
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

}

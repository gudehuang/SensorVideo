package com.example.hzg.videovr.show;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hzg.videovr.R;
import com.example.hzg.videovr.utils.Filtering;
import com.example.hzg.videovr.videoio.VideoReader;
import com.hoan.dsensor_master.DProcessedSensor;
import com.hoan.dsensor_master.DSensor;
import com.hoan.dsensor_master.DSensorEvent;
import com.hoan.dsensor_master.DSensorManager;
import com.hoan.dsensor_master.interfaces.DProcessedEventListener;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class ShowActivity extends Activity
        implements SensorEventListener , DProcessedEventListener  {

    private static final String TAG = "ShowActivity";
    private Bitmap bitmap;
    private SensorManager sensorManager;
    private float gravity = 0, pitchangle = 0;
    private int courseangle = 0;
    private int initialangle ;
    public static int screenwidth;
//    private ProgressBar progressBar;
    private FrameLayout root;
    private Context context;
    private String Path;
    private Mat mat ;
    private int mDProcessedSensorType;
    private Filtering courseangleFiltering = new Filtering(3);
    private Filtering pitchangleFiltering = new Filtering(3);
    private Filtering gravityFiltering = new Filtering(3);
    private Filtering fFiltering = new Filtering(3);
    private int fangle =0 ;
    private VideoReaderForVideo videoReaderForVideo ;
    private RelativeLayout btn_layout ;
    private Button back , style ;
    private TextView styletext ;
    private boolean showbtn = false ;
    private int showtime = 3 ;
    private boolean run = true ;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0x123) {
                back.setBackgroundColor(Color.argb(0, 0, 0, 0));
                style.setBackgroundColor(Color.argb(0, 0, 0, 0));
                styletext.setText("");
                back.setClickable(false);
                style.setClickable(false);
            } else if (msg.what == 0x456) {
                back.setClickable(true);
                style.setClickable(true);
                back.setBackgroundResource(R.drawable.back);
                if (MySurfaceView2.style % 3 == MySurfaceView2.VR) {
                    style.setBackgroundResource(R.drawable.vrmode);
                    styletext.setText("VR模式");
                } else if (MySurfaceView2.style % 3 == MySurfaceView2.COMMON) {
                    style.setBackgroundResource(R.drawable.commonmode);
                    styletext.setText("普通模式模式");
                } else {
                    styletext.setText("分屏模式");
                    style.setBackgroundResource(R.drawable.splitmode);
                }
            }
        }
    };
  private  boolean isFristLoad=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //无title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
//        btn_layout = (RelativeLayout) findViewById(R.id.btn_layout);
//        style = (Button) findViewById(R.id.style);
//        back = (Button) findViewById(R.id.back);
        Path = getIntent().getStringExtra("Path");
//        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        mDProcessedSensorType = DProcessedSensor.TYPE_3D_COMPASS ;
        root = (FrameLayout) findViewById(R.id.root);
        context = this;


    }

    class PictureTask extends AsyncTask<Void, Integer, Boolean> {

        protected void onPreExecute() {
//            progressBar.setProgress(0); // 显示进度对话框
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            handleVideo();
            System.out.println("finish2");
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // 在这里更新下载进度
        }

        @Override
        protected void onPostExecute(Boolean result) {
//            progressBar.setVisibility(View.GONE); // 关闭进度对话框
            final MySurfaceView2 mySurfaceView2 = new MySurfaceView2(context);
            root.addView(mySurfaceView2);
            addView();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    switch (videoReaderForVideo.getType()) {
                        case VideoReader.TYPE_360:
                            while (run) {
                                Mat mat = SearchMat(courseangle);
                                Utils.matToBitmap(mat, bitmap);
                                mySurfaceView2.show(bitmap, pitchangle, gravity);
                            }
                        case VideoReader.TYPE_HORIZONTAL:
                            while (run) {
                                Mat mat = SearchMat(courseangle);
                                Utils.matToBitmap(mat, bitmap);
                                mySurfaceView2.show(bitmap, courseangle);
                            }
                        case VideoReader.TYPE_VERCICAL:
                            while (run) {
                                if (gravity>0)
                                    pitchangle = -pitchangle ;
                                Mat mat = SearchMat((int) pitchangle);
                                Utils.matToBitmap(mat, bitmap);
                                mySurfaceView2.show(bitmap, (int) pitchangle);
                            }
                    }
                }
            }).start();
        }
    }

    private void addView() {
        btn_layout = new RelativeLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        btn_layout.setLayoutParams(params);
        back = new Button(context);
        style = new Button(context);
        styletext = new TextView(context);
        RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(140,140);
        params.setMargins(20,20,0,0);
        back.setLayoutParams(params1);
        back.setBackgroundColor(Color.argb(0,0,0,0));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                run = false ;

                finish();
            }
        });
        btn_layout.addView(back);
        params1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        params1.setMargins(180,0,0,0);
        styletext.setLayoutParams(params1);
        styletext.setTextColor(Color.RED);
        styletext.setTextSize(30);
        styletext.setText("");
        btn_layout.addView(styletext);
        params1 = new RelativeLayout.LayoutParams(100,100);
        params1.setMargins(0,0,20,0);
        params1.addRule(RelativeLayout.CENTER_VERTICAL);
        params1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        style.setBackgroundColor(Color.argb(0,0,0,0));
        style.setLayoutParams(params1);
        style.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MySurfaceView2.style = ++MySurfaceView2.style%3;
                if (MySurfaceView2.style  == MySurfaceView2.VR) {
                    style.setBackgroundResource(R.drawable.vrmode);
                    styletext.setText("VR模式");
                } else if (MySurfaceView2.style  == MySurfaceView2.COMMON) {
                    style.setBackgroundResource(R.drawable.commonmode);
                    styletext.setText("普通模式模式");
                } else {
                    styletext.setText("分屏模式");
                    style.setBackgroundResource(R.drawable.splitmode);
                }
            }
        });
        btn_layout.addView(style);
        btn_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (showbtn) {
                    showbtn = false;
                    handler.sendEmptyMessage(0x123);
                } else {
                    showbtn = true;
                    handler.sendEmptyMessage(0x456);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(showtime * 1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            showbtn = false;
                            handler.sendEmptyMessage(0x123);
                        }
                    }).start();
                }
            }
        });
        root.addView(btn_layout);
    }

    private void handleVideo() {
        mat = new Mat();
        screenwidth = (int) getScreenWidthSize();
        videoReaderForVideo = new VideoReaderForVideo(Path);
        System.out.println(videoReaderForVideo.getType());
        bitmap = Bitmap.createBitmap((int) videoReaderForVideo.getVideoCapture().get(Videoio.CAP_PROP_FRAME_WIDTH),
                (int) videoReaderForVideo.getVideoCapture().get(Videoio.CAP_PROP_FRAME_HEIGHT), Bitmap.Config.ARGB_8888);
        if (bitmap.getHeight()>bitmap.getWidth())
            videoReaderForVideo.setType(VideoReader.TYPE_360);
        Log.i("TYPE", String.valueOf(videoReaderForVideo.getType()));
    }

    private double getScreenWidthSize() {
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(point);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        double x = point.x / dm.xdpi;
        x = x * 25.4;
        return x;
    }

    private Mat SearchMat(int angle) {
        int abs;
        int a = videoReaderForVideo.getSensor(0), b = 0;
        abs = a - angle;
        abs = Math.abs(abs);
        for (int i = 1; i < videoReaderForVideo.size(); i++) {
            a = videoReaderForVideo.getSensor(i);
            if (abs > Math.abs(a - angle)) {
                b = i;
                abs = Math.abs(a - angle);
            }
        }
        videoReaderForVideo.readMat(b,mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGB);
        return mat;
    }

    @Override
    protected void onResume() {
        super.onResume();
        DSensorManager.startDProcessedSensor(this, mDProcessedSensorType,SensorManager.SENSOR_DELAY_GAME, this);
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                sensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                sensorManager.SENSOR_DELAY_GAME);
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        DSensorManager.stopDSensor();
         sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoReaderForVideo.release();


    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    if (!isFristLoad)
                    {
                        new PictureTask().execute();
                        isFristLoad=true;
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        int sensorType = event.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ORIENTATION:
                float y = values[2];
                pitchangleFiltering.put(y);
                pitchangle = pitchangleFiltering.getResult();
                float f = values[1];
                fFiltering.put(f);
                fangle = (int) fFiltering.getResult();
                break;
            case Sensor.TYPE_GRAVITY:
                float z = values[2];
                gravityFiltering.put(z);
                gravity = gravityFiltering.getResult();
                break;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onProcessedValueChanged(DSensorEvent dSensorEvent) {
        if (dSensorEvent.sensorType == DSensor.TYPE_DEPRECIATED_ORIENTATION) {
            Toast.makeText(context, "fail", Toast.LENGTH_SHORT).show();
        } else {
            if (Float.isNaN(dSensorEvent.values[0])) {
                Toast.makeText(context, "fail", Toast.LENGTH_SHORT).show();
            } else {
                int valueInDegree = (int) Math.round(Math.toDegrees(dSensorEvent.values[0]));
                if (valueInDegree < 0)
                    valueInDegree = (valueInDegree + 360) % 360;
                if (pitchangle>25){
                courseangleFiltering.put(valueInDegree);
                courseangle = (int) courseangleFiltering.getResult();}
            }
        }
    }

}
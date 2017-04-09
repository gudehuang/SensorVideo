package com.example.hzg.videovr.show;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.hzg.videovr.R;
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

public class MainActivity extends Activity
        implements SensorEventListener , DProcessedEventListener {

    private static final String TAG = "MainActivity";
    private Bitmap bitmap;
    private SensorManager sensorManager;
    private float gravity = 0, pitchangle = 0;
    private int courseangle = 0;
    private int initialangle ;
    public static int screenwidth;
    private ProgressBar progressBar;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //无title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        Path = getIntent().getStringExtra("Path");
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        mDProcessedSensorType = DProcessedSensor.TYPE_3D_COMPASS ;
        root = (FrameLayout) findViewById(R.id.root);
        context = this;
        Log.i(TAG, "Trying to load OpenCV library");
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, context, mLoaderCallback)) {
            Log.e(TAG, "Cannot connect to OpenCV Manager");
        } else {
            Log.i(TAG, "connect to OpenCV Manager ");
        }
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                sensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                sensorManager.SENSOR_DELAY_GAME);

    }

    class PictureTask extends AsyncTask<Void, Integer, Boolean> {

        protected void onPreExecute() {
            progressBar.setProgress(0); // 显示进度对话框
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
            progressBar.setVisibility(View.GONE); // 关闭进度对话框
            final MySurfaceView2 mySurfaceView2 = new MySurfaceView2(context);
            root.addView(mySurfaceView2);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    switch (videoReaderForVideo.getType()) {
                        case VideoReader.TYPE_360:
                            while (true) {
                                Mat mat = SearchMat(courseangle);
                                Utils.matToBitmap(mat, bitmap);
                                mySurfaceView2.show(bitmap, pitchangle, gravity, courseangle);
                            }
                        case VideoReader.TYPE_HORIZONTAL:
                            int i = 0 ;
                            int min = 0 , max = 0 , length = 0  ;
                            initialangle = courseangle ;
                            length = videoReaderForVideo.getLength();
                            min = initialangle ;
                            max = initialangle + length ;
                            while (true) {
                                if (max > 360 && courseangle<min)
                                    courseangle += 360;
                                if (courseangle < min)
                                    i = 0;
                                else if (courseangle > max)
                                    i = length - 1;
                                else
                                    i = courseangle - min;
                                videoReaderForVideo.readMat(i, mat);
                                Log.i("MAT", String.valueOf(i));
                                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGB);
                                Utils.matToBitmap(mat, bitmap);
                                mySurfaceView2.show(bitmap);
                            }
                            case VideoReader.TYPE_VERCICAL:
                                while (true) {
                                    Mat mat = SearchMat(fangle);
                                    Utils.matToBitmap(mat, bitmap);
                                    mySurfaceView2.show(bitmap);
                                }
                    }
                }
            }).start();
        }
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
        DSensorManager.startDProcessedSensor(this, mDProcessedSensorType, this);
    }

    @Override
    protected void onPause() {
        DSensorManager.stopDSensor();

        super.onPause();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    new PictureTask().execute();
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
package com.example.hzg.videovr.cameraview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;

import org.opencv.BuildConfig;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

/**
 * This class is an implementation of the Bridge View between OpenCV and Java Camera.
 * This class relays on the functionality available in base class and only implements
 * required functions:
 * connectCamera - opens Java camera and sets the PreviewCallback to be delivered.
 * disconnectCamera - closes the camera and stops preview.
 * When frame is delivered via callback from Camera - it processed via OpenCV to be
 * converted to RGBA32 and then passed to the external callback for modifications if required.
 */
public class MyCameraView1 extends CameraBridgeViewBase implements PreviewCallback {

    private static final int MAGIC_TEXTURE_ID = 10;
    private static final String TAG = "JavaCameraView";

    private byte mBuffer[];
    private Mat[] mFrameChain;
    private int mChainIdx = 0;
    private int mMatIdx = 0;
    private Thread mMatWorkThread;
    private Thread mCameraThread;
    private boolean mStopThread;

    protected Camera mCamera;
    protected JavaCameraFrame[] mCameraFrame;
    private SurfaceTexture mSurfaceTexture;
    private byte[] tempFrame;
    private Bitmap mCacheBitmap;
    private  Mat[] mats;
    private boolean mCameraMatReady;
    private  Object object=new Object();

    public static class JavaCameraSizeAccessor implements ListItemAccessor {

        @Override
        public int getWidth(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.width;
        }

        @Override
        public int getHeight(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.height;
        }
    }

    public MyCameraView1(Context context, int cameraId) {
        super(context, cameraId);
    }

    public MyCameraView1(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected boolean initializeCamera(int width, int height) {
        Log.d(TAG, "Initialize java camera");
        boolean result = true;
        synchronized (this) {
            mCamera = null;
            if (mCameraIndex == CAMERA_ID_ANY) {
                Log.d(TAG, "Trying to open camera with old open()");
                try {
                    mCamera = Camera.open();
                }
                catch (Exception e){
                    Log.e(TAG, "Camera is not available (in use or does not exist): " + e.getLocalizedMessage());
                }

                if(mCamera == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    boolean connected = false;
                    for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                        Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(camIdx) + ")");
                        try {
                            mCamera = Camera.open(camIdx);
                            connected = true;
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Camera #" + camIdx + "failed to open: " + e.getLocalizedMessage());
                        }
                        if (connected) break;
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    int localCameraIndex = mCameraIndex;
                    if (mCameraIndex == CAMERA_ID_BACK) {
                        Log.i(TAG, "Trying to open back camera");
                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                            Camera.getCameraInfo( camIdx, cameraInfo );
                            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                                localCameraIndex = camIdx;
                                break;
                            }
                        }
                    } else if (mCameraIndex == CAMERA_ID_FRONT) {
                        Log.i(TAG, "Trying to open front camera");
                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                            Camera.getCameraInfo( camIdx, cameraInfo );
                            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                                localCameraIndex = camIdx;
                                break;
                            }
                        }
                    }
                    if (localCameraIndex == CAMERA_ID_BACK) {
                        Log.e(TAG, "Back camera not found!");
                    } else if (localCameraIndex == CAMERA_ID_FRONT) {
                        Log.e(TAG, "Front camera not found!");
                    } else {
                        Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(localCameraIndex) + ")");
                        try {
                            mCamera = Camera.open(localCameraIndex);
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Camera #" + localCameraIndex + "failed to open: " + e.getLocalizedMessage());
                        }
                    }
                }
            }

            if (mCamera == null)
                return false;

            /* Now set camera parameters */
            try {
                Camera.Parameters params = mCamera.getParameters();
                Log.d(TAG, "getSupportedPreviewSizes()");
                List<Camera.Size> sizes = params.getSupportedPreviewSizes();

                if (sizes != null) {
                    /* Select the size that fits surface considering maximum size allowed */
                    Size frameSize = calculateCameraFrameSize(sizes, new JavaCameraSizeAccessor(), width, height);
                  //设置预览图像格式，NV21即YUV420SP
                    params.setPreviewFormat(ImageFormat.NV21);
                    Log.d(TAG, "Set preview size to " + Integer.valueOf((int)frameSize.width) + "x" + Integer.valueOf((int)frameSize.height));

                    params.setPreviewSize((int)frameSize.width, (int)frameSize.height);
                    params.setPreviewSize(1280,720);
                    //params.setPreviewSize(1920,1080);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && !Build.MODEL.equals("GT-I9100"))
                        //此函数是提高MediaRecorder录制摄像头视频性能的。
                        params.setRecordingHint(true);

                    List<String> FocusModes = params.getSupportedFocusModes();
                    if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                    {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }

                    mCamera.setParameters(params);
                    params = mCamera.getParameters();
                     //竖屏下获取的摄像头图像是横着的，需要倒转
                    mFrameWidth = params.getPreviewSize().width;
                    mFrameHeight = params.getPreviewSize().height;
                    Log.d("test","mFrameWidth:"+mFrameWidth+"#mFrameHeight："+mFrameHeight);
                    //缩放比例
                    if ((getLayoutParams().width == LayoutParams.MATCH_PARENT) && (getLayoutParams().height == LayoutParams.MATCH_PARENT))
                        mScale = Math.min(((float)height)/mFrameWidth, ((float)width)/mFrameHeight);
                    else
                        mScale = 0;
                    //帧率显示
                    if (mFpsMeter != null) {
                        mFpsMeter.setResolution(mFrameWidth, mFrameHeight);
                    }
                    //摄像头缓存大小
                    int size = mFrameWidth * mFrameHeight;
                    size  = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
                    mBuffer = new byte[size];

                    mCamera.addCallbackBuffer(mBuffer);
                    mCamera.setPreviewCallbackWithBuffer(this);

                    myinit();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        mSurfaceTexture = new SurfaceTexture(MAGIC_TEXTURE_ID);
                        mCamera.setPreviewTexture(mSurfaceTexture);
                    } else
                       mCamera.setPreviewDisplay(null);

                    /* Finally we are ready to start the preview */
                    Log.d(TAG, "startPreview");
                    mCamera.startPreview();
                }
                else
                    result = false;
            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            }
        }

        return result;
    }

    void myinit() {
        mFrameChain = new Mat[2];

        //Yuv420 （width*height）对应Mat（行，列）关系：
        // 行=height*1.5
        //列=width
        mFrameChain[0] = new Mat(mFrameHeight*3/2 , mFrameWidth, CvType.CV_8UC1);
        mFrameChain[1] = new Mat(mFrameHeight*3/2, mFrameWidth, CvType.CV_8UC1);

        mats=new Mat[2];
        mats[0] = new Mat(mFrameWidth , mFrameHeight,CvType.CV_8UC4);
        mats[1] = new Mat(mFrameWidth , mFrameHeight,CvType.CV_8UC4);
//                    mFrameChain[0] = new Mat(mFrameWidth*3/2 , mFrameHeight, CvType.CV_8UC1);
//                    mFrameChain[1] = new Mat(mFrameWidth*3/2, mFrameHeight, CvType.CV_8UC1);


        //创建缓存bitmap
        // AllocateCache();
        setmCacheBitmap();
        mCacheBitmap= Bitmap.createBitmap(mFrameHeight,mFrameWidth, Bitmap.Config.ARGB_8888);
        mCameraFrame = new JavaCameraFrame[2];
        mCameraFrame[0] = new JavaCameraFrame(mFrameChain[0], mFrameWidth, mFrameHeight);
        mCameraFrame[1] = new JavaCameraFrame(mFrameChain[1], mFrameWidth, mFrameHeight);

//                    mCameraFrame[0] = new JavaCameraFrame(mFrameChain[0], mFrameHeight, mFrameWidth);
//                    mCameraFrame[1] = new JavaCameraFrame(mFrameChain[1], mFrameHeight, mFrameWidth);
    }

    protected void releaseCamera() {
        synchronized (this) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);

                mCamera.release();
            }
            mCamera = null;
            if (mFrameChain != null) {
                mFrameChain[0].release();
                mFrameChain[1].release();
            }
            if (mCameraFrame != null) {
                mCameraFrame[0].release();
                mCameraFrame[1].release();
            }
        }
    }

    private boolean mCameraFrameReady = false;

    @Override
    protected boolean connectCamera(int width, int height) {

        /* 1. We need to instantiate camera
         * 2. We need to start thread which will be getting frames
         */
        /* First step - initialize camera connection */
        Log.d(TAG, "Connecting to camera");
        if (!initializeCamera(width, height))
            return false;

        mCameraFrameReady = false;

        /* now we can start update thread */
        Log.d(TAG, "Starting processing thread");
        mStopThread = false;
        mMatWorkThread =new Thread(new MatWorker());
        mCameraThread = new Thread(new CameraWorker());
        mMatWorkThread.start();
        mCameraThread.start();


        return true;
    }

    @Override
    protected void disconnectCamera() {
        /* 1. We need to stop thread which updating the frames
         * 2. Stop camera and release it
         */
        Log.d(TAG, "Disconnecting from camera");
        try {
            mStopThread = true;
            Log.d(TAG, "Notify thread");
            synchronized (this) {
                this.notify();
            }
            Log.d(TAG, "Wating for thread");
            if (mMatWorkThread != null)
                mMatWorkThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mMatWorkThread =  null;
        }

        /* Now release camera */
        releaseCamera();

        mCameraFrameReady = false;
    }

    @Override
    public void onPreviewFrame(byte[] frame, Camera arg1) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Preview Frame received. Frame size: " + frame.length);
        Log.d("onPreviewFrame","Thread:"+Thread.currentThread());
        synchronized (this) {
            mFrameChain[mChainIdx].put(0, 0, frame);
            mCameraFrameReady = true;
            this.notify();
        }
        if (mCamera != null)
            mCamera.addCallbackBuffer(mBuffer);
    }


    @Override
    protected Size calculateCameraFrameSize(List<?> supportedSizes, ListItemAccessor accessor, int surfaceWidth, int surfaceHeight) {
        int calcWidth = 0;
        int calcHeight = 0;

        int maxAllowedWidth = (mMaxWidth != -1&& mMaxWidth < surfaceWidth)? mMaxWidth : surfaceWidth;
        int maxAllowedHeight = (mMaxHeight != -1 && mMaxHeight < surfaceHeight)? mMaxHeight : surfaceHeight;

        for (Object size : supportedSizes) {
            int width = accessor.getWidth(size);
            int height = accessor.getHeight(size);

           //if (width <= maxAllowedWidth && height <= maxAllowedHeight)
            if (width <= maxAllowedHeight && height <= maxAllowedWidth)
            {
                if (width >= calcWidth && height >= calcHeight) {
                    calcWidth = (int) width;
                    calcHeight = (int) height;
                }
            }
        }

        return new Size(calcWidth, calcHeight);
    }

    private class JavaCameraFrame implements CvCameraViewFrame {
        @Override
        public Mat gray() {
            return mYuvFrameData.submat(0, mHeight, 0, mWidth);
        }

        @Override
        public Mat rgba() {
            Imgproc.cvtColor(mYuvFrameData,mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            Core.transpose(mRgba, mRgba);//transpose（src,targe） 将目标倒转 如840*480变为480*840
            Core.flip(mRgba, mRgba, 1);
            return mRgba;
        }

        public Mat test() {
            Imgproc.cvtColor(mYuvFrameData,mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            if (mRgba.width()!=mHeight)
            MyCameraView1.this.AllocateCache();
            return mRgba;
        }

        public JavaCameraFrame(Mat Yuv420sp, int width, int height) {
            super();
            mWidth = width;
            mHeight = height;
            mYuvFrameData = Yuv420sp;
            mRgba = new Mat();


        }

        public void release() {
            mRgba.release();

        }

        private Mat mYuvFrameData;
        private Mat mRgba;
        private int mWidth;
        private int mHeight;
    };

    private class CameraWorker implements Runnable {

        @Override
        public void run() {
            do {
                Log.d("CameraWorker","Thread:"+Thread.currentThread());
                boolean hasMat = false;
                synchronized (object) {
                    try {
                        while (!mCameraMatReady && !mStopThread) {
                            object.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mCameraMatReady)
                    {
                        mMatIdx=1-mMatIdx;
                        mCameraMatReady = false;
                        hasMat = true;
                    }
                }

                if (!mStopThread && hasMat) {

                    if (!mats[1 - mMatIdx].empty())
                    {
                        deliverAndDrawFrame(mats[1 - mMatIdx]);
                    }

                }
            } while (!mStopThread);
            Log.d(TAG, "Finish processing thread");
        }
    }

    private class MatWorker implements Runnable {

        @Override
        public void run() {
            do {
                Log.d("MatWorker","Thread:"+Thread.currentThread());
                boolean hasFrame = false;
                synchronized (MyCameraView1.this) {
                    try {
                        while (!mCameraFrameReady && !mStopThread) {
                            MyCameraView1.this.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mCameraFrameReady)
                    {
                        mChainIdx = 1 - mChainIdx;
                        mCameraFrameReady = false;
                       hasFrame=true;
                    }
                }
                 synchronized (object)
                 {
                if (!mStopThread && hasFrame) {

                    if (!mFrameChain[1 - mChainIdx].empty()) {

                        mCameraMatReady = true;

                        mats[mMatIdx] = mCameraFrame[1 - mChainIdx].test();
                        object.notify();
                    }
                }

                }


            } while (!mStopThread);
            Log.d(TAG, "Finish processing thread");
        }
    }


}

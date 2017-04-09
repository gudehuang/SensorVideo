package com.example.hzg.videovr.activity;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import java.util.List;

/**
 * Created by hzg on 2017/2/16.
 */

public class CameraJavaView extends JavaCameraView {
 private  Context mcontext;
    private String TAG="CameraJavaView";
    private static final int MAX_UNSPECIFIED = -1;
    public CameraJavaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mcontext=context;
    }

    @Override
    protected boolean connectCamera(int width, int height) {
        return super.connectCamera(width, height);
    }

    public List<Camera.Size> getResolutionList()
    {
        return  mCamera.getParameters().getSupportedPreviewSizes();
    }

    public  void  setResolution(Camera.Size resolution)
    {
        disconnectCamera();
        connectCamera(resolution.width,resolution.height);
    }
    public  void  setResolution(int width,int height)
    {
        disconnectCamera();
        connectCamera(width,height);
    }
    public  void  RecordVideo(String filename,Size size)
    {
        VideoCapture videoCapture=new VideoCapture(0);
        VideoWriter videoWriter=new VideoWriter(filename,VideoWriter.fourcc('M','J','P','G'),20, size);

    }

    @Override
    protected Size calculateCameraFrameSize(List<?> supportedSizes, ListItemAccessor accessor, int surfaceWidth, int surfaceHeight) {

        int calcWidth = 0;
        int calcHeight = 0;
     {
           int maxAllowedWidth = (mMaxWidth != MAX_UNSPECIFIED && mMaxWidth < surfaceWidth) ? mMaxWidth : surfaceWidth;
           int maxAllowedHeight = (mMaxHeight != MAX_UNSPECIFIED && mMaxHeight < surfaceHeight) ? mMaxHeight : surfaceHeight;

           for (Object size : supportedSizes) {
               int width = accessor.getWidth(size);
               int height = accessor.getHeight(size);

               if (width <= maxAllowedWidth && height <= maxAllowedHeight)
//               if (width <= maxAllowedHeight && height <= maxAllowedWidth) {
                   if (width >= calcWidth && height >= calcHeight) {
                       calcWidth = (int) width;
                       calcHeight = (int) height;
                   }

           }
       }
        return new Size(calcWidth, calcHeight);

    }

    @Override
    protected void AllocateCache() {
        super.setmCacheBitmap();
    }
}

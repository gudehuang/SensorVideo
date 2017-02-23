package com.example.hzg.videovr;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

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
    public CameraJavaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mcontext=context;
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
    public  void  RecordVideo(String filename,Size size)
    {
        VideoCapture videoCapture=new VideoCapture(0);
        VideoWriter videoWriter=new VideoWriter(filename,VideoWriter.fourcc('M','J','P','G'),20, size);

    }

}

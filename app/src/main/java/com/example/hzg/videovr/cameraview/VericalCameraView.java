package com.example.hzg.videovr.cameraview;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

/**
 * Created by hzg on 2017/3/21.
 */

public class VericalCameraView extends JavaCameraView{
    public VericalCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onPreviewFrame(byte[] frame, Camera arg1) {
        super.onPreviewFrame(frame, arg1);
    }
}

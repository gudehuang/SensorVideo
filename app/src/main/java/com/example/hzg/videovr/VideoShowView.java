package com.example.hzg.videovr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

/**
 * Created by hzg on 2017/2/21.
 */

public class VideoShowView extends SurfaceView implements SurfaceHolder.Callback{
    private  SurfaceHolder holder;
    private  Bitmap bitmap;
    private  int i=0;
    public VideoShowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder=getHolder();

    }

    public  void showBitmap(Mat mat)
    {
        Canvas mCanvas = holder.lockCanvas();
        if (bitmap==null)
        bitmap=Bitmap.createBitmap(mat.width(),mat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat,bitmap);
        mCanvas.drawBitmap(bitmap, 0, 0, null);
        holder.unlockCanvasAndPost(mCanvas);
        Log.d("VideoAct","showBitmap count:"+i++);
    }
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

}

}

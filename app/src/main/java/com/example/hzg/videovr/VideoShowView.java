package com.example.hzg.videovr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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
    private  Paint mPaint;
    public VideoShowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder=getHolder();
        mPaint = new Paint();
        mPaint.setColor(Color.BLUE);
        mPaint.setTextSize(20);
    }

    public  void showBitmap(Mat mat)
    {
        Log.d("VideoShowView","Thread:"+Thread.currentThread());
        Canvas mCanvas = holder.lockCanvas();
        if (bitmap==null)
            bitmap=Bitmap.createBitmap(mat.width(),mat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat,bitmap);
        float mScale=mCanvas.getHeight()/(float)bitmap.getHeight();
        int ws= (int) ((mCanvas.getWidth()-mScale*bitmap.getWidth())/2);
        int hs= (int) ((mCanvas.getHeight()-mScale*bitmap.getHeight())/2);
        int w= (int) (ws+mScale*bitmap.getWidth());
        int h= (int) (hs+mScale*bitmap.getHeight());
        Log.d("ShowBitmap","mScale:"+mScale+"#(l,t,r,b):"+ws+","+w+","+hs+","+h);
        mCanvas.drawBitmap(bitmap,new Rect(0,0,bitmap.getWidth(),bitmap.getHeight())
                ,new Rect(ws,hs,w,h), null);
        holder.unlockCanvasAndPost(mCanvas);
        Log.d("VideoAct","showBitmap count:"+i++);
    }
    public  void showBitmap(Mat mat,int sensor)

    {
        Log.d("VideoShowView","Thread:"+Thread.currentThread());
        Canvas mCanvas = holder.lockCanvas();
        if (bitmap==null)
            bitmap=Bitmap.createBitmap(mat.width(),mat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat,bitmap);
        float mScale=mCanvas.getHeight()/(float)bitmap.getHeight();
        int ws= (int) ((mCanvas.getWidth()-mScale*bitmap.getWidth())/2);
        int hs= (int) ((mCanvas.getHeight()-mScale*bitmap.getHeight())/2);
        int w= (int) (ws+mScale*bitmap.getWidth());
        int h= (int) (hs+mScale*bitmap.getHeight());
        Log.d("ShowBitmap","mScale:"+mScale+"#(l,t,r,b):"+ws+","+w+","+hs+","+h);
        mCanvas.drawBitmap(bitmap,new Rect(0,0,bitmap.getWidth(),bitmap.getHeight())
                ,new Rect(ws,hs,w,h), null);
        mCanvas.drawText(String.valueOf(sensor),20,30,mPaint);
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

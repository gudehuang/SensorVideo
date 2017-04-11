package com.example.hzg.videovr.show;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.hzg.videovr.R;

/**
 * Created by william on 2016/11/29.
 */
public class MySurfaceView2 extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Canvas mCanvas;//用于绘图的canvas
    private Paint mPaint;
    private int viewwidth, viewheight;
    //    public static Btn btn;
    private static Context context;
    public static boolean ShowBtn = false;
    public static final int COMMON = 0;
    public static final int VR = 1;
    public static final int SPLIT = 2;
    public static int style = 1;
    private Rect TargetRect, ShowRect, SplitVrShowRect1, SplitVrShowRect2, VrShowRect1, VrShowRect2;
    private Bitmap background;
    int PupilDistance, x;

    public MySurfaceView2(Context context) {
        super(context);
        this.context = context;
        initview();
    }

    public MySurfaceView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initview();
    }

    public MySurfaceView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initview();
    }

    private void initview() {
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mHolder = getHolder();
        mHolder.addCallback(this);//注册回调
        setFocusable(true);
        setFocusableInTouchMode(true);
        this.setKeepScreenOn(true);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setColor(Color.RED);
        mPaint.setTextSize(100);
        background = BitmapFactory.decodeResource(getResources(), R.drawable.background);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        viewwidth = this.getWidth();// 获取屏幕的宽
        viewheight = this.getHeight();// 获取屏幕的高
        background = Bitmap.createScaledBitmap(background, viewwidth, viewheight, false);
//        btn = new Btn(viewwidth, viewheight, context);
        ShowRect = new Rect(0, 0, viewwidth, viewheight);
        SplitVrShowRect1 = new Rect(0, 0, viewwidth / 2, viewheight);
        SplitVrShowRect2 = new Rect(viewwidth / 2, 0, viewwidth, viewheight);
        PupilDistance = 64;            //默认瞳距
        //通过瞳距计算出屏幕两边显示的图片的宽度
        x = (int) (viewwidth * (ShowActivity.screenwidth / 2 - PupilDistance / 2) * 2 / ShowActivity.screenwidth);
    }

    public void show(Bitmap bitmap, float pitchangle, float gravity) {
        mCanvas = mHolder.lockCanvas();
        int targeth;
        if (mCanvas != null)
            mCanvas.drawColor(Color.BLACK);
        int y = viewwidth / bitmap.getWidth() * bitmap.getHeight();  //通过宽度计算出等比例放大缩小的图片的高度
        if (gravity < 0)
            targeth = (int) ((y - viewheight) / 2 * pitchangle / 90);
        else
            targeth = (int) ((y - viewheight) - (y - viewheight) / 2 * pitchangle / 90);
        if (style % 3 == SPLIT) {
            VrShowRect1 = new Rect(0, 0, x, viewheight);  //设置Vr模式下观看的区域
            VrShowRect2 = new Rect(viewwidth - x, 0, viewwidth, viewheight);
            bitmap = Bitmap.createScaledBitmap(bitmap, x, y, false);  //放大缩小图片为x,y大小
            TargetRect = new Rect(0, targeth, x, viewheight + targeth);  //选取图片的目标区域
            if (mCanvas != null) {
                mCanvas.drawBitmap(bitmap, TargetRect, VrShowRect1, mPaint);
                mCanvas.drawBitmap(bitmap, TargetRect, VrShowRect2, mPaint);
            }
        } else if (style % 3 == COMMON) {
            bitmap = Bitmap.createScaledBitmap(bitmap, viewwidth, y, false);  //放大缩小图片为x,y大小
            TargetRect = new Rect(0, targeth, viewwidth, viewheight + targeth);  //选取图片的目标区域
            if (mCanvas != null)
                mCanvas.drawBitmap(bitmap, TargetRect, ShowRect, mPaint);
        } else {
            bitmap = Bitmap.createScaledBitmap(bitmap, viewwidth, y, false);  //放大缩小图片为x,y大小
            TargetRect = new Rect(viewwidth / 4, targeth, viewwidth * 3 / 4, viewheight + targeth);
            if (mCanvas != null) {
                mCanvas.drawBitmap(bitmap, TargetRect, SplitVrShowRect1, mPaint);
                mCanvas.drawBitmap(bitmap, TargetRect, SplitVrShowRect2, mPaint);
                mCanvas.drawBitmap(background, 0, 0, mPaint);
            }
        }
        if (mCanvas != null)
            mHolder.unlockCanvasAndPost(mCanvas);//对画布内容进行提交

    }

    public void show(Bitmap bitmap, int courseangle) {
        mCanvas = mHolder.lockCanvas();
        if (mCanvas != null)
            mCanvas.drawColor(Color.BLACK);
        int y = viewwidth / bitmap.getWidth() * bitmap.getHeight();  //通过宽度计算出等比例放大缩小的图片的高度
        bitmap = Bitmap.createScaledBitmap(bitmap, viewwidth, y, false);  //放大缩小图片为x,y大小
        if (style == COMMON) {
            if (mCanvas != null)
                mCanvas.drawBitmap(bitmap, 0, 0, null);
        } else if (style == SPLIT) {
            bitmap = Bitmap.createScaledBitmap(bitmap, x, y, false);  //放大缩小图片为x,y大小
            if (mCanvas != null) {
                mCanvas.drawBitmap(bitmap, 0, 0, null);
                mCanvas.drawBitmap(bitmap, viewwidth - x, 0, null);
            }
        } else {
            bitmap = Bitmap.createScaledBitmap(bitmap, viewwidth / 2, y, false);  //放大缩小图片为x,y大小
            if (mCanvas != null) {
                mCanvas.drawBitmap(bitmap, 0, 0, null);
                mCanvas.drawBitmap(bitmap, viewwidth / 2, 0, null);
                mCanvas.drawBitmap(background, 0, 0, null);
            }
        }
//        if (mCanvas != null)
//            mCanvas.drawText(String.valueOf(courseangle), 100, 100, mPaint);
        if (mCanvas != null)
            mHolder.unlockCanvasAndPost(mCanvas);//对画布内容进行提交

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
//            if (ShowBtn)
//                btn.OnTouchEvent(event);
//            else
//                btn.logic();
        }
        return true;
    }

    public static void finish() {

    }
}


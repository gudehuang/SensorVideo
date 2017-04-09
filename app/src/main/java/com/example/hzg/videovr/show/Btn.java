package com.example.hzg.videovr.show;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.MotionEvent;



import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;

import com.example.hzg.videovr.R;

/**
 * Created by william on 2017/1/19.
 */
public class Btn {
    private Paint mPaint ;
    private Bitmap back, commonmode, vrmode ,newvrmode;
    private int bx, by;
    private int btnx, btny;
    private int circlex , circley ;
    private int showtime = 3 ;

    public Btn(int w , int h, Context context) {
        back = BitmapFactory.decodeResource(context.getResources(), R.drawable.back);
        back = Bitmap.createScaledBitmap(back, 75, 75, false);
        commonmode = BitmapFactory.decodeResource(context.getResources(), R.drawable.commonmode);
        commonmode = Bitmap.createScaledBitmap(commonmode, 100, 100, false);
        vrmode = BitmapFactory.decodeResource(context.getResources(), R.drawable.vrmode);
        vrmode = Bitmap.createScaledBitmap(vrmode, 100, 100, false);
        newvrmode = BitmapFactory.decodeResource(context.getResources(), R.drawable.newvrmode);
        newvrmode = Bitmap.createScaledBitmap(newvrmode, 100, 100, false);
        bx = w / 40;
        by = w / 40;
        btnx = w - commonmode.getWidth() - 10;
        btny = h / 2 - commonmode.getWidth() / 2;
        circlex = w - 60;
        circley = h / 2;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setColor(Color.BLACK);
        mPaint.setAlpha(100);
    }

    public void draw(Canvas canvas) {
        if (MySurfaceView2.ShowBtn) {
            canvas.drawBitmap(back, bx, by, null);
            canvas.drawCircle(circlex, circley, 60, mPaint);
            if (MySurfaceView2.style%3 == MySurfaceView2.COMMON)
                canvas.drawBitmap(commonmode, btnx, btny, null);
            else if (MySurfaceView2.style%3 == MySurfaceView2.VR)
                canvas.drawBitmap(vrmode, btnx, btny, null);
            else
                canvas.drawBitmap(newvrmode, btnx, btny, null);
        }
    }

    public void OnTouchEvent(MotionEvent event) {
        int pointX = (int) event.getX();
        int pointY = (int) event.getY();
        if (event.getAction() == MotionEvent.ACTION_UP) {
            //按钮VR的触发事件
            if (pointX > btnx && pointX < btnx + commonmode.getWidth()) {
                if (pointY > btny && pointY < btny + commonmode.getHeight()) {
                    MySurfaceView2.style++ ;
                    System.out.println(MySurfaceView2.style);
                }
            } else if (pointX > bx && pointX < bx + back.getWidth()) { //按钮back的触发事件
                if (pointY > by && pointY < by + back.getHeight()) {
                }
            } else
                MySurfaceView2.ShowBtn = false;
        }
    }

    public void logic(){
        MySurfaceView2.ShowBtn = true ;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                MySurfaceView2.ShowBtn = false ;
            }
        }).start();

    }
}


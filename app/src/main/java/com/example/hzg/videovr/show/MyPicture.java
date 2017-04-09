package com.example.hzg.videovr.show;

import org.opencv.core.Mat;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by william on 2017/2/17.
 */
public class MyPicture {

    private int angle ;
    private Mat mat ;
    private int id ;

    public MyPicture(int id ,int angle ,Mat mat)
    {
        this.id = id ;
        this.angle = angle ;
        this.mat = mat ;
    }

    public int getAngle() {
        return angle;
    }

    public Mat getMat() {
        return mat;
    }

    public static Map<Integer,MyPicture> MpMap = new HashMap<Integer,MyPicture>();
    public static void addItem(MyPicture mypicture )
    {
        MpMap.put(mypicture.id,mypicture);
    }
}

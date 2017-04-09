package com.example.hzg.videovr.videoio;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import static android.content.ContentValues.TAG;

/**
 * Created by hzg on 2017/2/27.
 */

public class VideoReaderList implements VideoReader {
    private ArrayList<Integer> sensorList;
    private SparseArray<Mat> data;
    private VideoCapture videoCapture;
    private  String videoMessage;
    private Context mContext;
    private  int type;
    private  int length;
 public VideoReaderList(String filename)
 {
     try {
         ObjectInputStream ois=new ObjectInputStream(new FileInputStream(filename+".vr"));
         sensorList= (ArrayList<Integer>) ois.readObject();
         length=sensorList.size()-1;
         type=sensorList.remove(length);
     } catch (IOException e) {
         e.printStackTrace();
     } catch (ClassNotFoundException e) {
         e.printStackTrace();
     }
     videoCapture=new VideoCapture(filename);
     int count = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
     int width = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH);
     int heiht = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
     videoMessage="视频总帧数:"+count+"  尺寸："+width+"*"+heiht;
     data=new SparseArray<>();
     for (int i:sensorList)
     {
         Mat mat=new Mat();
         boolean readed=videoCapture.read(mat);
         //颜色转换 将BGR转为RGB
         Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGBA);
         data.put(i, mat);
         Log.d(TAG, "put Mat isread?" + readed + " x:" + i + " mat:" + mat);
     }
     videoCapture.release();
 }
    @Override
    public int getSensor(int position) {
        return sensorList.get(position);
    }

    @Override
    public Mat getMat(int position) {
        return data.get(position);
    }

    @Override
    public int size() {
        return sensorList.size();
    }

    @Override
    public String getVideoMessage() {
       return  videoMessage;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int getSensorIndex(int sensor) {
        return 0;
    }

    @Override
    public void readMat(int position, Mat mat) {

    }
}

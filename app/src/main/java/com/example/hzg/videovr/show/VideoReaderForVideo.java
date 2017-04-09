package com.example.hzg.videovr.show;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

/**
 * Created by hzg on 2017/2/27.
 */

public class VideoReaderForVideo implements VideoReader {
    private ArrayList<Integer> sensorList;
    private VideoCapture videoCapture;
    private  String videoMessage;
    private Context mContext;
    private  int type=VideoReader.TYPE_HORIZONTAL;
    private  int  length;
    private  Mat mat;
 public VideoReaderForVideo(String filename) {
     try {
         ObjectInputStream ois=new ObjectInputStream(new FileInputStream(filename+".vr"));
         sensorList= (ArrayList<Integer>) ois.readObject();
         length=sensorList.size()-1;
         type=sensorList.get(length);
         sensorList.remove(length);
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
 }
    @Override
    public int getSensor(int position) {
        return sensorList.get(position);
    }
    @Override
    public Mat getMat(int position) {
        if (mat==null)
            mat=new Mat();
        videoCapture.set(Videoio.CAP_PROP_POS_FRAMES,position);
        videoCapture.read(mat);
        return mat;
    }
    public void readMat(int position,Mat mat) {
        videoCapture.set(Videoio.CAP_PROP_POS_FRAMES,position);
        videoCapture.read(mat);
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

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int getSensorIndex(int sensor) {


        return 0;
    }

    public VideoCapture getVideoCapture() {
        return videoCapture;
    }

    public void  release() {
        if (videoCapture!=null)
            videoCapture.release();
        if (mat!=null)
            mat.release();
        if (sensorList!=null) {
            sensorList.clear();
            sensorList=null;
        }
    }
}

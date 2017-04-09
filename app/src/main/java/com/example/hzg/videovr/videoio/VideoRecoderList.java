package com.example.hzg.videovr.videoio;

import android.content.Context;
import android.widget.Toast;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Created by hzg on 2017/2/27.
 */

public class VideoRecoderList implements  VideoRecoder {
    private VideoWriter videoWriter;
    private ArrayList<Integer> sensorList;
    private ArrayList<Integer> sensorListY;
    private  String filename;
    private Context mContext;
    private  int type=VideoReader.TYPE_UNKNOW;
   public VideoRecoderList(VideoWriter writer,ArrayList<Integer> list)
{
    videoWriter=writer;
    sensorList=list;
}
public VideoRecoderList( Context context,String filename,int fourcc,double fps, Size framesize)
{
    mContext=context;
    videoWriter=new VideoWriter(filename,fourcc,fps, framesize);
    sensorList=new ArrayList<>();
    this.filename=filename;
}
public VideoRecoderList( Context context,int type,String filename,int fourcc,double fps, Size framesize)
{
    mContext=context;
    videoWriter=new VideoWriter(filename,fourcc,fps, framesize);
    sensorList=new ArrayList<>();
    sensorListY=new ArrayList<>();

    this.filename=filename;
    this.type=type;
}
    @Override
    public void write(Mat mat, int sensor) {
        videoWriter.write(mat);
        if (type==VideoReader.TYPE_HORIZONTAL)
        sensorList.add(sensor);
        else  sensorListY.add(sensor);
    }
    public void write(Mat mat, int sensor,int sensorY) {
        videoWriter.write(mat);
        sensorList.add(sensor);
        sensorListY.add(sensorY);
        if (Math.abs(sensor-sensorList.get(0))>20)
            type=VideoReader.TYPE_HORIZONTAL;
        if (Math.abs(sensorY-sensorListY.get(0))>20)
            type=VideoReader.TYPE_VERCICAL;
    }

    @Override
    public void saveToSdcard() {
        if (type==VideoReader.TYPE_VERCICAL)sensorList=sensorListY;
        sensorList.add(type);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename + ".vr"));
            oos.writeObject(sensorList);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        videoWriter.release();
        sensorList.clear();
        Toast.makeText(mContext,filename+"保存成功",Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean isOpened() {
        return videoWriter.isOpened();
    }

    @Override
    public boolean contains(int sensor) {
        return sensorList.contains(sensor);
    }
    public boolean contains(int sensor,int sensorY) {
        return sensorList.contains(sensor)&&sensorListY.contains(sensorY);
    }

    @Override
    public int getType() {
        return type;
    }
}

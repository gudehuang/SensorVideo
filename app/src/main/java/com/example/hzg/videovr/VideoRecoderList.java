package com.example.hzg.videovr;

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
    private  String filename;
    private Context mContext;
    VideoRecoderList(VideoWriter writer,ArrayList<Integer> list)
{
    videoWriter=writer;
    sensorList=list;
}
VideoRecoderList( Context context,String filename,int fourcc,double fps, Size framesize)
{
    mContext=context;
    videoWriter=new VideoWriter(filename,fourcc,fps, framesize);
    sensorList=new ArrayList<>();
    this.filename=filename;
}
    @Override
    public void write(Mat mat, int sensor) {
        videoWriter.write(mat);
        sensorList.add(sensor);
    }

    @Override
    public void saveToSdcard() {
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
}

package com.example.hzg.videovr;

import android.content.Context;
import android.widget.Toast;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.LinkedHashSet;

/**
 * Created by hzg on 2017/2/27.
 */

public class VideoRecoderSet implements VideoRecoder {

   private VideoWriter videoWriter;
    private LinkedHashSet<Integer> sensorSet;
    private String filename;
    private Context mContext;

    VideoRecoderSet(VideoWriter writer,LinkedHashSet<Integer> set)
    {
        videoWriter=writer;
        sensorSet=set;
    }
    VideoRecoderSet(Context context,String filename, int fourcc,double fps,Size framesize)
    {
        mContext=context;
        videoWriter=new VideoWriter(filename,fourcc,fps, framesize);
        sensorSet=new LinkedHashSet<>();
        this.filename=filename;
    }
    @Override
    public void write(Mat mat, int sensor) {
      videoWriter.write(mat);
        sensorSet.add(sensor);
    }

    @Override
    public void saveToSdcard() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename + ".vr"));
            oos.writeObject(sensorSet);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        videoWriter.release();
        sensorSet.clear();
        Toast.makeText(mContext,filename+"保存成功",Toast.LENGTH_LONG).show();

    }

    @Override
    public boolean isOpened() {
        return videoWriter.isOpened();
    }

    @Override
    public boolean contains(int sensor) {
        return sensorSet.contains(sensor);
    }
}

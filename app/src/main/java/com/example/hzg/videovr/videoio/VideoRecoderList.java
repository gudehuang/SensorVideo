package com.example.hzg.videovr.videoio;

import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.hzg.videovr.MainActivityCv4;
import com.example.hzg.videovr.R;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Created by hzg on 2017/2/27.
 */

public class VideoRecoderList implements  VideoRecoder {
    private VideoWriter videoWriter;
    static  int ORIENTATION_VERTICAL=0x001;
    static  int ORIENTATION_HORIZONTAL=0x002;
    private ArrayList<Integer> sensorList;
    private ArrayList<Integer> sensorListY;
    private  String filename;
    private  String absoluteFilename;
    private Context mContext;
    private AlertDialog.Builder builder;
    private  int type=VideoReader.TYPE_UNKNOW;
    private  int orientation=ORIENTATION_VERTICAL;
public VideoRecoderList( Context context,int type,String filename,int fourcc,double fps, Size framesize)
{
    mContext=context;
    absoluteFilename=MainActivityCv4.dataDir+"/"+filename+".avi";
    if (framesize.width>framesize.height) {
        orientation = ORIENTATION_HORIZONTAL;
    }
    videoWriter=new VideoWriter(absoluteFilename,fourcc,fps, framesize);
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
        if (Math.abs(sensor-sensorList.get(0))>10)
            type=VideoReader.TYPE_HORIZONTAL;
        if (Math.abs(sensorY-sensorListY.get(0))>10)
            type=VideoReader.TYPE_VERCICAL;
    }

    @Override
    public void saveToSdcard() {

        if (type==VideoReader.TYPE_VERCICAL)sensorList=sensorListY;
        sensorList.add(type);
        if (sensorList.size()>20) {
            try {
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename + ".vr"));
                oos.writeObject(sensorList);
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            videoWriter.release();
            sensorList.clear();
            Toast.makeText(mContext, filename + "保存成功", Toast.LENGTH_LONG).show();
            Snackbar.make(LayoutInflater.from(mContext).inflate(R.layout.snackbar,null),
                    "保存成功",Snackbar.LENGTH_LONG).setAction("重名名", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder=new AlertDialog.Builder(mContext);
                    final EditText editText=new EditText(mContext);
                    builder.setView(editText);
                    builder.setPositiveButton("保存", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        File file=new File(filename);
                          String newName=file.getParent()+"/"+editText.getText()+"avi";
                            System.out.println(newName);
                            file.renameTo(new File(newName));
                        }
                    });
                      builder.setTitle(filename);
                    builder.show();
                }
            }).show();
        }
        else
        {
            videoWriter.release();
            File file=new File(filename);
            file.delete();
            Toast.makeText(mContext, filename + "录取内容过少，已删除", Toast.LENGTH_LONG).show();
        }
    }
    public void saveToSdcard(final View view) {

        if (type==VideoReader.TYPE_VERCICAL)sensorList=sensorListY;
        sensorList.add(type);
        if (sensorList.size()>20) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            videoWriter.release();
            if (orientation==ORIENTATION_VERTICAL)
            {   File oldfile=new File(absoluteFilename);
                absoluteFilename=MainActivityCv4.dataDirA+"/"+filename+".avi";
                File newFile=new File(absoluteFilename);
                oldfile.renameTo(newFile);
            }
            else if (orientation==ORIENTATION_HORIZONTAL)
            {
                if (type==VideoReader.TYPE_VERCICAL) {
                    File oldfile=new File(absoluteFilename);
                    absoluteFilename = MainActivityCv4.dataDirV + "/" + filename + ".avi";
                    File newFile=new File(absoluteFilename);
                    oldfile.renameTo(newFile);
                }
                else
                {
                    File oldfile=new File(absoluteFilename);
                    absoluteFilename = MainActivityCv4.dataDirH+ "/" + filename + ".avi";
                    File newFile=new File(absoluteFilename);
                    oldfile.renameTo(newFile);
                }
            }
            try {
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(absoluteFilename + ".vr"));
                oos.writeObject(sensorList);
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sensorList.clear();
            Snackbar.make(view,absoluteFilename+"文件保存成功",Snackbar.LENGTH_LONG).setAction("重名名", new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    builder=new AlertDialog.Builder(mContext);
                    final EditText editText=new EditText(mContext);
                    builder.setView(editText);
                    builder.setPositiveButton("保存",null);
                    builder.setTitle("重名名");
                    final AlertDialog dialog=builder.show();
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            File fileV=new File(absoluteFilename);
                            File fileS=new File(absoluteFilename+".vr");
                                String newVName=fileV.getParent()+"/"+editText.getText()+".avi";
                                String newSName=fileS.getParent()+"/"+editText.getText()+".avi.vr";
                                System.out.println(newVName);
                                System.out.println(newSName);
                            File nfileV=new File(newVName);
                            if (!nfileV.exists()) {
                                File nfileS = new File(newSName);
                                fileV.renameTo(new File(newVName));
                                fileS.renameTo(new File(newSName));
                                Snackbar.make(view,"重命名成功",Snackbar.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                            else {
                                editText.setError("已存在该文件名");
                            }
                            }

                    });
                }
            }).show();
        }
        else
        {
            videoWriter.release();
            File file=new File(filename);
            file.delete();
            Snackbar.make(view,"录取内容过少，已删除",Snackbar.LENGTH_LONG).show();

        }
    }

    @Override
    public boolean isOpened() {
        return videoWriter.isOpened();
    }

    @Override
    public boolean contains(int sensor) {
        if (type==VideoReader.TYPE_VERCICAL)
            return sensorListY.contains(sensor);
            else
        return sensorList.contains(sensor);
    }
    public boolean contains(int sensor,int sensorY) {
        return sensorList.contains(sensor)&&sensorListY.contains(sensorY);
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public int getSize() {
        int i=-1;
        if (type==VideoReader.TYPE_VERCICAL)
            i=sensorListY.size();
        else  i=sensorList.size();
        return i;
    }

    @Override
    public void release() {
        sensorListY=null;
        sensorList=null;
        videoWriter=null;
        builder=null;
    }
}

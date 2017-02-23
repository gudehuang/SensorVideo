package com.example.hzg.videovr;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by hzg on 2017/2/23.
 */

public  class myUtils {
    static String TAG="myUtils";
    public static  ArrayList<Float> readSensor(String dataPath)
    {
        ArrayList<Float> sensorData=null;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataPath));
            sensorData = (ArrayList<Float>) ois.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            return  sensorData;
        }
    }
    public static void initMatMap(VideoCapture capture, HashMap<Float,Mat> matMap, ArrayList<Float> sensorList) {
        for (float i:sensorList) {
            //videoCapture.set(Videoio.CAP_PROP_POS_FRAMES,i);
            Mat mat=new Mat();
            boolean readed=capture.read(mat);
            //颜色转换 将BGR转为RGB
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGBA);
            matMap.put(i, mat);
            Log.d(TAG, "put Mat isread?" + readed + " x:" + i + " mat:" + mat);
        }

    }
    public static void showFilesDialog(final Context context, final String dirPath, final Class target) {
        File file = new File(dirPath);
        final String[] list = file.list();
        final ArrayList<String> arrayList = new ArrayList<String>();
        for (String line : list) {
            if (!line.contains(".vr"))
                arrayList.add(line);

        }
        final String[] results = new String[arrayList.size()];
        arrayList.toArray(results);
        if (results.length < 1) {

            Toast.makeText(context, "没有文件", Toast.LENGTH_LONG).show();
        } else {


            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("选择文件");


            final int[] select = {0};
            builder.setSingleChoiceItems(results, 0, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    select[0] = i;
                }
            });
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(context, target);
                    intent.putExtra("path", dirPath + "/" + results[select[0]]);
                    context.startActivity(intent);

                }
            });
            builder.setNegativeButton("取消", null);
            builder.setNeutralButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    new File(dirPath + "/" + results[select[0]]).delete();
                    new File(dirPath + "/" + results[select[0]]+".vr").delete();
                    showFilesDialog(context, dirPath, target);
                }
            });
            builder.show();
        }
    }
}

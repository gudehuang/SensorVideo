package com.example.hzg.videovr.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.core.CvType;
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

    public static void showFilesDialog(final Context context, final String dirPath, final String type,final Class target) {
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
                    intent.putExtra("type",type);
                    context.startActivity(intent);

                }
            });
            builder.setNegativeButton("取消", null);
            builder.setNeutralButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    new File(dirPath + "/" + results[select[0]]).delete();
                    new File(dirPath + "/" + results[select[0]]+".vr").delete();
                    showFilesDialog(context, dirPath,type, target);
                }
            });
            builder.show();
        }
    }

    public static void main(String[] args) {
        Mat mat=new Mat(2,2, CvType.CV_8UC1);
        byte[] bytes=new byte[]{0,1,2,3};
        mat.put(0,0,bytes);
        Mat matT=new Mat();
        Core.transpose(mat,matT);
        System.out.println(mat.get(0,0));
        System.out.println(matT.get(0,0));



    }

}

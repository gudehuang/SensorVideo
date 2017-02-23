package com.example.hzg.videovr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;

import static android.content.ContentValues.TAG;

/**
 * Created by hzg on 2017/2/19.
 */

public class OpenCvUtils {
    public static String matToJson(Mat mat) {
        JsonObject obj = new JsonObject();

        if (mat.isContinuous()) {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < mat.rows(); i++) {
                double[] d = mat.get(i, 0);
                builder.append(d[0]);
                if (i != mat.rows() - 1) {
                    builder.append(",");
                }
            }

            obj.addProperty("rows", mat.rows());
            obj.addProperty("cols", mat.cols());
            obj.addProperty("type", mat.type());
            obj.addProperty("data", builder.toString());

            Gson gson = new Gson();
            String json = gson.toJson(obj);

            Log.d("opencv_detector", "json: " + json);
            return json;
        } else {
            Log.e("opencv_detector", "Mat not continuous.");
        }
        return "{}";
    }

    public static Mat matFromJson(String json) {
        JsonParser parser = new JsonParser();
        JsonObject JsonObject = parser.parse(json).getAsJsonObject();

        int rows = JsonObject.get("rows").getAsInt();
        int cols = JsonObject.get("cols").getAsInt();
        int type = JsonObject.get("type").getAsInt();

        String dataString = JsonObject.get("data").getAsString();

        Mat mat = new Mat(rows, cols, type);

        int rowIndex = 0;

        for (String s : dataString.split(",")) {
            mat.put(rowIndex++, 0, Double.parseDouble(s));
        }

        return mat;

    }
    public static String matToJson(Mat mat,int i){
        JsonObject obj = new JsonObject();

        if(mat.isContinuous()){
            int cols = mat.cols();
            int rows = mat.rows();
            int elemSize = (int) mat.elemSize();

            byte[] data = new byte[cols * rows * elemSize];

            mat.get(0, 0, data);

            obj.addProperty("rows", mat.rows());
            obj.addProperty("cols", mat.cols());
            obj.addProperty("type", mat.type());

            // We cannot set binary data to a json object, so:
            // Encoding data byte array to Base64.
            String dataString = new String(Base64.encode(data, Base64.DEFAULT));
            obj.addProperty("data", dataString);
            Gson gson = new Gson();
            String json = gson.toJson(obj);

            return json;
        } else {
            Log.e(TAG, "Mat not continuous.");
        }
        return "{}";
    }

    public static Mat matFromJson(String json,int i){
        JsonParser parser = new JsonParser();
        JsonObject JsonObject = parser.parse(json).getAsJsonObject();

        int rows = JsonObject.get("rows").getAsInt();
        int cols = JsonObject.get("cols").getAsInt();
        int type = JsonObject.get("type").getAsInt();

        String dataString = JsonObject.get("data").getAsString();
        byte[] data = Base64.decode(dataString.getBytes(), Base64.DEFAULT);

        Mat mat = new Mat(rows, cols, type);
        mat.put(0, 0, data);

        return mat;
    }
    public static byte[] getBytes(Bitmap bitmap){
        //实例化字节数组输出流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, baos);//压缩位图
        return baos.toByteArray();//创建分配字节数组
    }
    public static Bitmap getBitmap(byte[] data){
        return BitmapFactory.decodeByteArray(data, 0, data.length);//从字节数组解码位图
    }
}

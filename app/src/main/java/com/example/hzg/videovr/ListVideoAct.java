package com.example.hzg.videovr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by hzg on 2017/2/18.
 */

public class ListVideoAct extends AppCompatActivity {
    private  RecyclerView recyclerView;
    private VideoView videoView;
    private HashMap<Integer,String> dataMap;
    private  String dataPath;
    private  ImageAdapter adapter;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataPath=getIntent().getStringExtra("path");
        Log.d("ListAct","datapath："+dataPath);
        if (dataPath==null)
        {
            Log.d("onCreate","error,please give a path");
            finish();
        }
        setContentView(R.layout.list);
        recyclerView= (RecyclerView) findViewById(R.id.list_image);
        try {
            ObjectInputStream ois=new ObjectInputStream(new FileInputStream(dataPath));
            dataMap= (HashMap<Integer, String>) ois.readObject();
            Log.d("readDataMap","size:"+dataMap.size());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        adapter=new ImageAdapter(dataMap);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

    }
    class  ImageViewHolder extends  RecyclerView.ViewHolder {
        private TextView itemName;
        private ImageView itemImage;
        public ImageViewHolder(View itemView) {
            super(itemView);
            itemName= (TextView) itemView.findViewById(R.id.item_tv_name);
            itemImage= (ImageView) itemView.findViewById(R.id.item_iv_image);
        }
    }

    class  ImageAdapter  extends RecyclerView.Adapter<ImageViewHolder>{
        private  HashMap<Integer,String> data;
        private ArrayList<Integer> keys;
        private  Bitmap CacheBitmap;
        public  ImageAdapter(HashMap<Integer,String> data)
        {
            this.data=data;
            keys=new ArrayList<>(data.keySet());
        }
        @Override
        public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
             View root= LayoutInflater.from(parent.getContext()).inflate(R.layout.item,parent,false);
            ImageViewHolder holder=new ImageViewHolder(root);
            return holder;
        }

        @Override
        public void onBindViewHolder(ImageViewHolder holder, int position) {
               holder.itemName.setText(""+keys.get(position));
            Mat mat=OpenCvUtils.matFromJson(data.get(keys.get(position)),1);
            Bitmap bitmap=Bitmap.createBitmap(mat.width(),mat.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat,bitmap);
                holder.itemImage.setImageBitmap(bitmap);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    public static Bitmap getBitmap(byte[] data){
        return BitmapFactory.decodeByteArray(data, 0, data.length);//从字节数组解码位图
    }
    public void  readVideo(String name)
    {
        VideoCapture videoCapture=new VideoCapture(name);

    }
}

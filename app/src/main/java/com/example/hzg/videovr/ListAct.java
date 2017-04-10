package com.example.hzg.videovr;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import com.example.hzg.videovr.videoio.VideoReader;
import com.example.hzg.videovr.videoio.VideoReaderList;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

/**
 * Created by hzg on 2017/2/18.
 */

public class ListAct extends AppCompatActivity {
    private RecyclerView recyclerView;
    private String dataPath;
    private ImageAdapter adapter;
    private String TAG = "ListAct";
    private VideoReader videoReader;
    private ProgressDialog proDialog;
    private final static int CANCELL_DIALOG=0x111;
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case CANCELL_DIALOG:
                    proDialog.dismiss();
                    proDialog=null;
                    adapter.setVideoReader(videoReader);
                    adapter.notifyDataSetChanged();
            }
        }
    };
    private String type;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataPath = getIntent().getStringExtra("path");
        type=getIntent().getStringExtra("type");
        Log.d("ListAct", "datapath：" + dataPath);
        if (dataPath == null) {
            Log.d("onCreate", "error,please give a path");
            finish();
        }
        setContentView(R.layout.list);
        recyclerView = (RecyclerView) findViewById(R.id.list_image);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (type.equals("list"))
               videoReader=new VideoReaderList(dataPath);
                handler.sendEmptyMessage(CANCELL_DIALOG);
            }
        }).start();
        adapter = new ImageAdapter(videoReader);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        proDialog=ProgressDialog.show(this,"正在从视频获取数据","数据处理中，请稍等、、、、",false,false);
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {
        private TextView itemName;
        private ImageView itemImage;

        public ImageViewHolder(View itemView) {
            super(itemView);
            itemName = (TextView) itemView.findViewById(R.id.item_tv_name);
            itemImage = (ImageView) itemView.findViewById(R.id.item_iv_image);
        }
    }

    class ImageAdapter extends RecyclerView.Adapter<ImageViewHolder>  {
        private VideoReader videoReader;
        private  String videoMessage;
        public ImageAdapter(VideoReader videoReader) {
            this.videoReader =videoReader;

        }
        public  void setVideoReader(VideoReader videoReader)
        {
            this.videoReader=videoReader;
            videoMessage=videoReader.getVideoMessage();
        }
        @Override
        public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
            ImageViewHolder holder = new ImageViewHolder(root);
            return holder;
        }

        @Override
        public void onBindViewHolder(ImageViewHolder holder, int position) {
            holder.itemName.setText(videoMessage+ "\n图片数:"+(position+1)+"/"+videoReader.size()+"       方位角:"+videoReader.getSensor(position));
            Mat mat = videoReader.getMat(videoReader.getSensor(position));
            Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, bitmap);
            holder.itemImage.setImageBitmap(bitmap);
        }

        @Override
        public int getItemCount() {

            return videoReader==null?0:videoReader.size();
        }
    }

}

package com.example.hzg.videovr;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import static com.example.hzg.videovr.myUtils.initMatMap;
import static com.example.hzg.videovr.myUtils.readSensor;

/**
 * Created by hzg on 2017/2/18.
 */

public class ListAct extends AppCompatActivity {
    private RecyclerView recyclerView;
    private HashMap<Float, Mat> dataMap;
    private ArrayList<Float> sensorData;
    private String dataPath;
    private ImageAdapter adapter;
    private VideoCapture videoCapture;
    private String TAG = "ListAct";
    private String videomessage;
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
                    adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataPath = getIntent().getStringExtra("path");
        Log.d("ListAct", "datapath：" + dataPath);
        if (dataPath == null) {
            Log.d("onCreate", "error,please give a path");
            finish();
        }
        setContentView(R.layout.list);
        recyclerView = (RecyclerView) findViewById(R.id.list_image);
        videoCapture = new VideoCapture(dataPath);
        sensorData=readSensor(dataPath+".vr");
        dataMap=new HashMap<>();
        getVideoMessage();
        new Thread(new Runnable() {
            @Override
            public void run() {
               initMatMap(videoCapture,dataMap,sensorData);
                handler.sendEmptyMessage(CANCELL_DIALOG);
            }
        }).start();
        adapter = new ImageAdapter(dataMap);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        proDialog=ProgressDialog.show(this,"正在从视频获取数据","数据处理中，请稍等、、、、",false,false);
    }

    private void getVideoMessage() {
        Log.d("ListAct", "videoCapture isOpened?" + videoCapture.isOpened());
        int count = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
        int width = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int heiht = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        videomessage="视频总帧数:"+count+"  尺寸："+width+"*"+heiht;
        Log.d(TAG, "video size:"+videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH) + "*" + videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
        Log.d(TAG, "video frame count:" + videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT));
        Log.d(TAG, "sensordata count:" + sensorData.size());
    }

//    private void readMatDataFormVideo(String dataPath,ArrayList<Float> sensorData,VideoCapture videoCapture,HashMap<Float,Mat> dataMap) {
//        try {
//            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataPath + ".vr"));
//            sensorData = (ArrayList<Float>) ois.readObject();
//            Log.d(TAG, "readsensorData:" + sensorData);
//            videoCapture = new VideoCapture(dataPath);
//            Log.d("ListAct", "videoCapture isOpened?" + videoCapture.isOpened());
//            dataMap = new HashMap<>();
//            int count = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
//            int width = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH);
//            int heiht = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
//            videomessage="视频总帧数:"+count+"  尺寸："+width+"*"+heiht;
//            Log.d(TAG, "video size:"+videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH) + "*" + videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
//            Log.d(TAG, "video frame count:" + videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT));
//            Log.d(TAG, "sensordata count:" + sensorData.size());
//            final VideoCapture finalVideoCapture = videoCapture;
//            final HashMap<Float, Mat> finalDataMap = dataMap;
//            final ArrayList<Float> finalSensorData = sensorData;
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    initMatMap(finalVideoCapture, finalDataMap, finalSensorData);
//                    handler.sendEmptyMessage(CANCELL_DIALOG);
//                }
//            }).start();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//    }



    class ImageViewHolder extends RecyclerView.ViewHolder {
        private TextView itemName;
        private ImageView itemImage;

        public ImageViewHolder(View itemView) {
            super(itemView);
            itemName = (TextView) itemView.findViewById(R.id.item_tv_name);
            itemImage = (ImageView) itemView.findViewById(R.id.item_iv_image);
        }
    }

    class ImageAdapter extends RecyclerView.Adapter<ImageViewHolder> {
        private HashMap<Float, Mat> data;

        public ImageAdapter(HashMap<Float, Mat> data) {
            this.data = data;

        }

        @Override
        public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
            ImageViewHolder holder = new ImageViewHolder(root);
            return holder;
        }

        @Override
        public void onBindViewHolder(ImageViewHolder holder, int position) {
            holder.itemName.setText(videomessage+ "\n图片数:"+(position+1)+"/"+sensorData.size()+"       方位角:"+sensorData.get(position));
            Mat mat = data.get(sensorData.get(position));
            Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, bitmap);
            holder.itemImage.setImageBitmap(bitmap);
        }

        @Override
        public int getItemCount() {

            return data==null?0:data.size();
        }
    }

}

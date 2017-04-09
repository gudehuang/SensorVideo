package com.example.hzg.videovr.show;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.hzg.videovr.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.util.ArrayList;

public class FileActivity extends AppCompatActivity {

    private RecyclerView filerecyclerview;
    ArrayList<File> filelist = new ArrayList<>();
    RecyclerView.LayoutManager layoutManager ;
    private static final String TAG = "FileActivity";
    private VideoCapture videoCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);
        Log.i(TAG, "Trying to load OpenCV library");
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback)) {
            Log.e(TAG, "Cannot connect to OpenCV Manager");
        } else {
            Log.i(TAG, "connect to OpenCV Manager ");
        }
        filerecyclerview = (RecyclerView) findViewById(R.id.filelist);
        getData();
        filerecyclerview.setAdapter(new MyAdapter());
        layoutManager=new GridLayoutManager(this,2);
        filerecyclerview.setLayoutManager(layoutManager);
        filerecyclerview.setOnClickListener(new FileRecyclerViewOnClickListener());
    }

    private void getData() {
        String filePath = Environment.getExternalStorageDirectory().getPath() + "/360video";
        File parentFile = new File(filePath);
        File[] files = parentFile.listFiles();
        for (File file : files) {
            if (file.getName().endsWith(".avi")) {
                filelist.add(file);
            }
        }
        System.out.println(filelist.size());
    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(FileActivity.this).inflate(R.layout.filelist, parent, false);
            MyViewHolder holder = new MyViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, final int position) {
            holder.fileimage.setImageBitmap(getThumbnail(filelist.get(position).getAbsolutePath()));
            holder.filename.setText(filelist.get(position).getName());
            holder.fileimage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent=new Intent(FileActivity.this,MainActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("Path", filelist.get(position).getPath());
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return filelist.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            private ImageView fileimage;
            private TextView filename;

            public MyViewHolder(View itemView) {
                super(itemView);
                fileimage = (ImageView) itemView.findViewById(R.id.fileimage);
                filename = (TextView) itemView.findViewById(R.id.filename);
            }
        }
    }

    public Bitmap getThumbnail(String filePath) {
        Mat mat = new Mat();
        videoCapture = new VideoCapture(filePath);
        System.out.println(videoCapture.isOpened());
        Bitmap bitmap = Bitmap.createBitmap((int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH),
                (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT), Bitmap.Config.ARGB_8888);
        videoCapture.set(Videoio.CAP_PROP_POS_FRAMES, 1);
        videoCapture.read(mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGB);
        Utils.matToBitmap(mat,bitmap);
        return bitmap;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private class FileRecyclerViewOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

        }
    }
}

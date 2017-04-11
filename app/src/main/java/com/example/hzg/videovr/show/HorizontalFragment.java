package com.example.hzg.videovr.show;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.hzg.videovr.R;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by william on 2017/4/11.
 */

public class HorizontalFragment extends Fragment {

    private RecyclerView myRecyclerView;
    ArrayList<File> filelist = new ArrayList<>();
    RecyclerView.LayoutManager layoutManager;
    MyAdapter myAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.recycler, container, false);
        myRecyclerView = (RecyclerView) view.findViewById(R.id.myrecyclerview);
        myAdapter = new MyAdapter();
        myRecyclerView.setAdapter(myAdapter);
        layoutManager = new GridLayoutManager(getActivity(), 2);
        myRecyclerView.setLayoutManager(layoutManager);
        new MyTask().execute();
        return view;
    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.filelist, parent, false);
            MyViewHolder myviewholder = new MyViewHolder(view);
            return myviewholder;
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, final int position) {
            if (filelist.size() > 0) {
                holder.filename.setText(filelist.get(position).getName());
                holder.fileimage.setImageBitmap(getThumbnail(filelist.get(position).getAbsolutePath()));
                holder.fileimage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), ShowActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("Path", filelist.get(position).getPath());
                        intent.putExtras(bundle);
                        startActivity(intent);
                    }
                });
                holder.fileimage.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        AlertDialog.Builder builder=new AlertDialog.Builder(v.getContext());
                        builder.setTitle("删除文件");
                        builder.setMessage("确定删除文件？");
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                File file=new File( filelist.get(position).getPath())   ;
                                File file1=new File( filelist.get(position).getPath()+".vr")   ;
                                if (file.exists())file.delete();
                                if (file1.exists())file1.delete();
                                filelist.clear();
                                getData();
                                myAdapter.notifyDataSetChanged();
                            }
                        });
                        builder.setNeutralButton("取消",null);
                        builder.show();
                        return  true;
                    }
                });
            }
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
        VideoCapture videoCapture = new VideoCapture(filePath);
        System.out.println(videoCapture.isOpened());
        Bitmap bitmap=null;
        if (videoCapture.isOpened()) {
           bitmap = Bitmap.createBitmap((int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH),
                    (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT), Bitmap.Config.ARGB_8888);
            videoCapture.set(Videoio.CAP_PROP_POS_FRAMES, 1);
            videoCapture.read(mat);
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGB);
            Utils.matToBitmap(mat, bitmap);
        }
        else  bitmap= BitmapFactory.decodeResource(getResources(),R.drawable.readerr);
        videoCapture.release();
        mat.release();
        return bitmap;
    }

    private void getData() {
        String filePath = Environment.getExternalStorageDirectory().getPath() + "/360video" + "/horizontal";
        File parentFile = new File(filePath);
        File[] files = parentFile.listFiles();
        for (File file : files) {
            if (file.getName().endsWith(".avi")) {
                filelist.add(file);
            }
        }
        Log.i("SIZE", String.valueOf(filelist.size()));
    }

    class MyTask extends AsyncTask<Void, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            if (!(filelist.size() > 0))
                getData();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            myAdapter.notifyDataSetChanged();
        }
    }


}

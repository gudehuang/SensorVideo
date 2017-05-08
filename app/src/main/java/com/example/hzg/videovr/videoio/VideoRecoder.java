package com.example.hzg.videovr.videoio;

import org.opencv.core.Mat;

/**
 * 录制视频的接口
 * Created by hzg on 2017/2/27.
 */

public interface VideoRecoder {
    /**
     * 写入函数
     * @param mat  写入的视频帧
     * @param sensor  对应的传感器参数
     */
    void write(Mat mat,int sensor);
    void write(Mat mat,int sensor,int sensorY);

    /**
     * 保存函数，结束录制并把文件写入sd卡中
     */
    void saveToSdcard();

    /**
     * 判定视频写入器是否开启
     * @return
     */
    boolean isOpened();

    /**
     * 查询传入参数是否已记录
     * @param sensor
     * @return
     */
    boolean contains(int sensor);
    boolean contains(int sensor,int sensorY);

    /**
     * 获取视频录制类型
     *    VideoReader.TYPE_HORIZONTAL  --横向录制;
          VideoReader.TYPE_VERCICAL    --纵向录制;
          VideoReader.TYPE_UNKNOW      --未指明状态，记录时判定方向 ;
     * @return
     */
    int getType();
    int getSize();
    void release();
}

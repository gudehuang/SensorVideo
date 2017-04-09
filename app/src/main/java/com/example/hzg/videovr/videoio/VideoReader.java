package com.example.hzg.videovr.videoio;

import org.opencv.core.Mat;


/**
 * 读取视频和传感器文件的接口
 */
public interface VideoReader {
    static final int  TYPE_HORIZONTAL=361;
    static final int  TYPE_VERCICAL=-361;
    static final int  TYPE_UNKNOW=362;
    static final int  TYPE_360=363;
    int  getSensor(int position);
    Mat getMat(int position);
    int size();
    String getVideoMessage();
    int getType();
    int getLength();
    int  getSensorIndex(int sensor);
    void  readMat(int position, Mat mat);
}

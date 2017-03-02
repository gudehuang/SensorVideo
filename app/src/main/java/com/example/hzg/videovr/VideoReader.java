package com.example.hzg.videovr;

import org.opencv.core.Mat;

/**
 * Created by hzg on 2017/2/27.
 */

public interface VideoReader {
    int  getSensor(int position);
    Mat getMat(int position);
    int size();
    String getVideoMessage();
}

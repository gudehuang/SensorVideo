package com.example.hzg.videovr;

import org.opencv.core.Mat;

/**
 * Created by hzg on 2017/2/27.
 */

public interface VideoRecoder {
    void write(Mat mat,int sensor);
    void saveToSdcard();
    boolean isOpened();
    boolean contains(int sensor);

}

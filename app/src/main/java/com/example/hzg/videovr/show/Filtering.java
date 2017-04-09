package com.example.hzg.videovr.show;

/**
 * 简单的传感器数据滤波处理
 * 采用定长数组循环存储传感器数据，求平均值
 * Created by hzg on 2017/3/11.
 */
public class Filtering {
    float[] floats;
    int index = 0;
    int size = 0;

    public Filtering(int size) {
        floats = new float[size];
        this.size = size;
    }

    public void put(float sensor) {

            floats[index % size] = sensor;
            index++;

    }

    public float getResult() {
        float sum = 0;
        float start=floats[0];
        for (float f : floats) {
            //处理0/360边界数值问题（数据可能为359，0，1，求平均值得出的数据偏差过大）
            //以数组第一个数为基准，比它大特定数值（这里为20）的数减360，解决（0，1，359）这种情况
            if (f-start>20)f=f-360;
            //以数组第一个数为基准，比它小特定数值（这里为20）的数加360，解决（359，1，0）这种情况
            if (f-start<-20)f=f+360;
            sum += f;
        }
        return (sum / size)%360;
    }
}

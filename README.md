# SVV
svv是sensor vr video的简写。正如字面意思，项目的主要目的是使用传感器和视频模拟vr观影体验。
项目的主要功能有两个：录制和播放。
项目使用opencv开源库，用于视频的录制。    
svv录制的视频数据有两个文件，一个是视频文件，一个是传感器文件，这两个文件的数据存在一一对应的关系。
播放时，读取视频文件和传感器文件，根据手机此时的传感器信息从传感器文件查找出合适的数据，再根据传感器文件的数据找到视频文件的对应帧，进行展示。     
适配Android4.4——Android7.0

版本信息：   
 V1.1  2017/4/12      
[下载地址](http://pan.plyz.net/s/?u=2251461691&p=SVV1.1.apk)

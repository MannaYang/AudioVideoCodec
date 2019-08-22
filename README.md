# AudioVideoCodec
一款视频录像机，支持AudioRecord录音、MediaCodec输出AAC、MediaMuxer合成音频视频并输出mp4，支持自动对焦、屏幕亮度调节、录制视频时长监听、手势缩放调整焦距等
## 功能简介
  目前包含基本的音频、视频录制与合成操作,功能如下:
  1. 基于AudioRecord录制原始PCM格式音频数据
  2. 基于MediaCodec编码输出音频为AAC格式
  3. 基于MediaCdec.createInputSurface()创建Surface,EGLContext绑定Surface并通过渲染FBO已绑定的纹理录制视频
  4. 基于FBO离屏纹理绘制水印纹理并添加至录制视频文件
  5. 基于MediaMuxer合成音频、视频数据并输出MP4视频文件
  6. 提供基于shader语言修改片元着色器color完成黑白滤镜
  7. 提供相机预览时自动调节预览焦距、屏幕亮度
  8. 提供相机预览时切换闪光灯、前后置摄像头
  9. 提供相机预览时双指缩放调节预览画面、SeekBar拖动调节
  
 未实现:
 1. 手动点击屏幕聚焦缩放预览画面
 2. 横屏预览、录制时上下滑动屏幕左半部分调节屏幕亮度、右半部分调节音频音量
 3. 预览滤镜贴纸、美颜等其它滤镜功能
 4. 音频录制时回声消除、静音降噪
 
    未实现原因 : 该部分内容需要对OpenGL ES绘制原理、音频数据存储、手势滑动处理、Camera相机聚焦等需要有深入理解
    
    
## 采坑之旅
  1. 前后摄像头切换
  目前测试的华为P8max上由后置切换为前置时，始终报setParameters failed,经过逐行代码验证,是由于设置parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO)
  自动对焦导致，部分华为、三星手机前置摄像头不允许聚焦(目前自有测试机如此,如果有华为、三星手机测试可行欢迎提供型号)
  
  2. 自动对焦失效问题
  目前项目中自动对焦采用的是 ： parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);遇到的问题就是移动屏幕时对焦会屏幕闪烁,
  包括Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;因手动点击聚焦未实现,此处未深入研究适配所有机型,欢迎测试其它机型并给出有效方案!
  
  3. 设置Camera预览大小、图片大小导致setParameters failed
  目前项目CameraManager管理类中,通过获取手机设备支持的预览大小、图片大小,取最适合当前预览的尺寸,解决上述问题；一般报setParameters failed需要检查
  对Camera.Parameters设置的参数仔细检查
  
  4. MediaCodec.queueInputBuffer参数中的时间戳问题
  之前对该处时间戳计算公式理解有误,导致录制的视频始终无法播放,一帧音频帧大小 int size = 采样率 x 位宽 x 采样时间 x 通道数,此处时间戳单位时微秒,
  pcm录制的原始单位是bit,双声道16bit,采用byte[]、short[]装载数据需要进行换算，1 byte = 8 bit,项目中有详细注释,欢迎查证!
  
  
## 公共库
  目前该项目直接是以单个Activity承载预览、录制、闪光灯、切换镜头、黑白滤镜、手势缩放、SeekBar显示缩放进度等功能,library提供的是aop权限申请库
  
## 截图展示
  初始预览 ：
    
  ![image](https://github.com/MannaYang/AudioVideoCodec/blob/master/screenshot/ic_preview.png)
  
  闪光灯 ：
    
  ![image](https://github.com/MannaYang/AudioVideoCodec/blob/master/screenshot/ic_flash.png)
  
  滤镜：
    
  ![image](https://github.com/MannaYang/AudioVideoCodec/blob/master/screenshot/ic_filter.png)
  
  录制中 ：
    
  ![image](https://github.com/MannaYang/AudioVideoCodec/blob/master/screenshot/ic_recording.png)
  
  后置切换前置 ：
    
  ![image](https://github.com/MannaYang/AudioVideoCodec/blob/master/screenshot/ic_switch_camera.png)
  
  播放水印视频 ：
    
  ![image](https://github.com/MannaYang/AudioVideoCodec/blob/master/screenshot/ic_water_paly.png)
    
    
## 感谢开源
  https://github.com/ChinaZeng/SurfaceRecodeDemo
  
## 我的个人新球

  免费加入星球一起讨论项目、研究新技术,共同成长!
  
![image](https://github.com/MannaYang/AudioVideoCodec/blob/master/screenshot/xiaomiquan.png)

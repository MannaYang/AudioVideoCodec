package com.manna.codec.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGLContext;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.manna.codec.surface.EglSurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.manna.codec.MediaCodecConstant.audioStop;
import static com.manna.codec.MediaCodecConstant.encodeStart;
import static com.manna.codec.MediaCodecConstant.surfaceChange;
import static com.manna.codec.MediaCodecConstant.surfaceCreate;
import static com.manna.codec.MediaCodecConstant.videoStop;

/**
 * 编解码管理类
 */
public class MediaEncodeManager {
    private static final String TAG = "MediaEncodeManager";

    private EglSurfaceView.Render eglSurfaceRender;
    private Surface surface;

    private MediaMuxer mediaMuxer;

    private MediaCodec audioCodec;
    private MediaCodec.BufferInfo audioBuffer;

    private MediaCodec videoCodec;
    private MediaCodec.BufferInfo videoBuffer;

    private int sampleRate;
    private int channelCount;
    private int audioFormat;
    private int width;
    private int height;

    //时间戳
    private long presentationTimeUs;

    private AudioCodecThread audioThread;
    private VideoCodecThread videoThread;
    private EglRenderThread eglThread;

    public MediaEncodeManager(EglSurfaceView.Render eglSurfaceRender) {
        this.eglSurfaceRender = eglSurfaceRender;
    }

    public void initMediaCodec(String filePath, int mediaFormat, String audioType, int sampleRate,
                               int channelCount, int audioFormat, String videoType, int width, int height) {
        initMediaMuxer(filePath, mediaFormat);
        initVideoCodec(videoType, width, height);
        initAudioCodec(audioType, sampleRate, channelCount);
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        this.audioFormat = audioFormat;
        this.width = width;
        this.height = height;
    }

    public void initThread(MediaMuxerChangeListener listener, EGLContext eglContext, int renderMode) {
        eglThread = new EglRenderThread(surface, eglContext, eglSurfaceRender, renderMode, width, height);

        videoThread = new VideoCodecThread(videoCodec, videoBuffer, mediaMuxer, listener);
        audioThread = new AudioCodecThread(audioCodec, audioBuffer, mediaMuxer, listener);
    }

    private void initMediaMuxer(String filePath, int mediaFormat) {
        try {
            mediaMuxer = new MediaMuxer(filePath, mediaFormat);
        } catch (IOException e) {
            Log.e(TAG, "initMediaMuxer: 文件打开失败,path=" + filePath);
        }
    }

    private void initAudioCodec(String audioType, int sampleRate, int channels) {
        try {
            audioCodec = MediaCodec.createEncoderByType(audioType);
            MediaFormat audioFormat = MediaFormat.createAudioFormat(audioType, sampleRate, channels);
            int BIT_RATE = 96000;
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            int MAX_INOUT_SIZE = 8192;
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_INOUT_SIZE);

            audioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            audioBuffer = new MediaCodec.BufferInfo();

        } catch (IOException e) {
            Log.e(TAG, "initAudioCodec: 音频类型无效");
        }
    }

    private void initVideoCodec(String videoType, int width, int height) {
        try {
            videoCodec = MediaCodec.createEncoderByType(videoType);
            MediaFormat videoFormat = MediaFormat.createVideoFormat(videoType, width, height);

            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            //MediaFormat.KEY_FRAME_RATE -- 可通过Camera#Parameters#getSupportedPreviewFpsRange获取
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            //width*height*N  N标识码率低、中、高，类似可设置成1，3，5，码率越高视频越大，也越清晰
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4);
            //每秒关键帧数
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                videoFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
                videoFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);
            }

            videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            videoBuffer = new MediaCodec.BufferInfo();
            surface = videoCodec.createInputSurface();
        } catch (IOException e) {
            Log.e(TAG, "initVideoCodec: 视频类型无效");
        }
    }

    public void setPcmSource(byte[] pcmBuffer, int buffSize) {
        if (audioCodec == null) {
            return;
        }
        try {

            int buffIndex = audioCodec.dequeueInputBuffer(0);
            if (buffIndex < 0) {
                return;
            }
            ByteBuffer byteBuffer;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                byteBuffer = audioCodec.getInputBuffer(buffIndex);
            } else {
                byteBuffer = audioCodec.getInputBuffers()[buffIndex];
            }
            if (byteBuffer == null) {
                return;
            }
            byteBuffer.clear();
            byteBuffer.put(pcmBuffer);
            //presentationTimeUs = 1000000L * (buffSize / 2) / sampleRate
            //一帧音频帧大小 int size = 采样率 x 位宽 x 采样时间 x 通道数
            // 1s时间戳计算公式  presentationTimeUs = 1000000L * (totalBytes / sampleRate/ audioFormat / channelCount / 8 )
            //totalBytes : 传入编码器的总大小
            //1000 000L : 单位为 微秒，换算后 = 1s,
            //除以8     : pcm原始单位是bit, 1 byte = 8 bit, 1 short = 16 bit, 用 Byte[]、Short[] 承载则需要进行换算
            presentationTimeUs += (long) (1.0 * buffSize / (sampleRate * channelCount * (audioFormat / 8)) * 1000000.0);
            Log.d(TAG, "pcm一帧时间戳 = " + presentationTimeUs / 1000000.0f);
            audioCodec.queueInputBuffer(buffIndex, 0, buffSize, presentationTimeUs, 0);
        } catch (java.lang.IllegalStateException e) {
            //audioCodec 线程对象已释放MediaCodec对象
            Log.d(TAG, "setPcmSource: " + "MediaCodec对象已释放");
        }
    }

    public void startEncode() {
        if (surface == null) {
            Log.e(TAG, "startEncode: createInputSurface创建失败");
            return;
        }
        encodeStart = false;
        eglThread.start();
        videoThread.start();
        audioThread.start();

        surfaceCreate = true;
        surfaceChange = true;

        audioStop = false;
        videoStop = false;
    }

    public void stopEncode() {
        encodeStart = false;

        audioThread.stopAudioCodec();
        audioThread = null;
        videoThread.stopVideoCodec();
        videoThread = null;
        eglThread.stopEglRender();
        eglThread = null;

        surfaceCreate = false;
        surfaceChange = false;
    }
}

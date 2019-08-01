package com.manna.codec.record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.util.Log;

/**
 * 音频采集,边采集边转码AAC
 */
public class AudioCapture {

    private static final String TAG = "AudioCapture.class";

    //默认参数
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIGS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIGS, AUDIO_FORMAT);

    private boolean isStartRecord = false;
    private boolean isStopRecord = false;
    private boolean isDebug = true;

    private AudioRecord audioRecord;

    /**
     * 采集回调监听
     */
    private AudioCaptureListener captureListener;

    /**
     * 采集子线程
     */
    private Thread threadCapture;

    public void start() {
        start(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIGS, AUDIO_FORMAT);
    }

    public void start(int audioSource, int sampleRate, int channels, int audioFormat) {
        if (isStartRecord) {
            if (isDebug)
                Log.d(TAG, "音频录制已经开启");
            return;
        }

        //各厂商实现存在差异
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channels, audioFormat);

        if (bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            if (isDebug)
                Log.d(TAG, "无效参数");
            return;
        }

        if (isDebug)
            Log.d(TAG, "bufferSize = ".concat(String.valueOf(bufferSize)).concat("byte"));

        audioRecord = new AudioRecord(AudioCapture.AUDIO_SOURCE, sampleRate, channels, audioFormat, bufferSize);

        audioRecord.startRecording();

        isStopRecord = false;

        threadCapture = new Thread(new CaptureRunnable());
        threadCapture.start();

        isStartRecord = true;
        if (isDebug) {
            Log.d(TAG, "音频录制开启...");
        }
    }

    public void stop() {
        if (!isStartRecord) {
            return;
        }
        isStopRecord=true;
        threadCapture.interrupt();

        audioRecord.stop();
        audioRecord.release();

        isStartRecord=false;
        captureListener=null;
    }

    /**
     * 子线程读取采集到的数据
     */
    private class CaptureRunnable implements Runnable {

        @Override
        public void run() {
            while (!isStopRecord) {
                byte[] buffer = new byte[bufferSize];
                int readRecord = audioRecord.read(buffer, 0, bufferSize);
                if (readRecord > 0) {
                    if (captureListener != null)
                        captureListener.onCaptureListener(buffer,readRecord);
                    if (isDebug) {
                        Log.d(TAG, "音频采集数据源 -- ".concat(String.valueOf(readRecord)).concat(" -- bytes"));
                    }
                } else {
                    if (isDebug)
                        Log.d(TAG, "录音采集异常");
                }
                //延迟写入 SystemClock  --  Android专用
                SystemClock.sleep(10);
            }
        }
    }

    public interface AudioCaptureListener {

        /**
         * 音频采集回调数据源
         *
         * @param audioSource ：音频采集回调数据源
         * @param audioReadSize :每次读取数据的大小
         */
        void onCaptureListener(byte[] audioSource,int audioReadSize);
    }

    public AudioCaptureListener getCaptureListener() {
        return captureListener;
    }

    public void setCaptureListener(AudioCaptureListener captureListener) {
        this.captureListener = captureListener;
    }
}

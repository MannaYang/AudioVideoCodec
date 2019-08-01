package com.manna.codec.codec;

import android.media.MediaCodec;
import android.media.MediaMuxer;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.manna.codec.MediaCodecConstant;

import java.nio.ByteBuffer;

import static com.manna.codec.MediaCodecConstant.audioTrackIndex;
import static com.manna.codec.MediaCodecConstant.videoTrackIndex;

/**
 * 音频写入thread
 */
public class AudioCodecThread extends Thread {

    private static final String TAG = "AudioCodecThread.class";

    private MediaCodec audioCodec;
    private MediaCodec.BufferInfo bufferInfo;
    private MediaMuxer mediaMuxer;

    private boolean isStop;

    private long pts;

    private MediaMuxerChangeListener listener;

    AudioCodecThread(MediaCodec mediaCodec, MediaCodec.BufferInfo bufferInfo, MediaMuxer mediaMuxer,
                     @NonNull MediaMuxerChangeListener listener) {
        this.audioCodec = mediaCodec;
        this.bufferInfo = bufferInfo;
        this.mediaMuxer = mediaMuxer;
        this.listener = listener;
        pts = 0;
        audioTrackIndex = -1;
    }

    @Override
    public void run() {
        super.run();
        isStop = false;
        audioCodec.start();
        while (true) {
            if (isStop) {
                audioCodec.stop();
                audioCodec.release();
                audioCodec = null;
                MediaCodecConstant.audioStop = true;

                if (MediaCodecConstant.videoStop) {
                    mediaMuxer.stop();
                    mediaMuxer.release();
                    mediaMuxer = null;
                    listener.onMediaMuxerChangeListener(MediaCodecConstant.MUXER_STOP);
                    break;
                }
            }

            if (audioCodec == null)
                break;
            int outputBufferIndex = audioCodec.dequeueOutputBuffer(bufferInfo, 0);
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                audioTrackIndex = mediaMuxer.addTrack(audioCodec.getOutputFormat());
                if (videoTrackIndex != -1) {
                    mediaMuxer.start();
                    //标识编码开始
                    MediaCodecConstant.encodeStart = true;
                    listener.onMediaMuxerChangeListener(MediaCodecConstant.MUXER_START);
                }
            } else {
                while (outputBufferIndex >= 0) {
                    if (!MediaCodecConstant.encodeStart) {
                        Log.d(TAG, "run: 线程延迟");
                        SystemClock.sleep(10);
                        continue;
                    }

                    ByteBuffer outputBuffer = audioCodec.getOutputBuffers()[outputBufferIndex];
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                    if (pts == 0) {
                        pts = bufferInfo.presentationTimeUs;
                    }
                    bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - pts;
                    mediaMuxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo);

                    audioCodec.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = audioCodec.dequeueOutputBuffer(bufferInfo, 0);
                }
            }
        }
    }

    void stopAudioCodec() {
        isStop = true;
    }
}

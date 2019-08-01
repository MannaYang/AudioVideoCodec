package com.manna.codec.codec;

public interface MediaMuxerChangeListener {
    /**
     * 音视频合成状态回调 开始 -- 停止
     *
     * @param type int
     */
    void onMediaMuxerChangeListener(int type);

    /**
     * 视频录制时长回调
     *
     * @param time 时长
     */
    void onMediaInfoListener(int time);
}

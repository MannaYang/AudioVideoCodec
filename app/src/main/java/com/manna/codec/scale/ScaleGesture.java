package com.manna.codec.scale;

import android.view.ScaleGestureDetector;

import java.util.HashMap;

/**
 * 双指缩放预览页面
 */
public class ScaleGesture implements ScaleGestureDetector.OnScaleGestureListener {

    private float scaleCapacity;
    private ScaleGestureListener listener;


    public ScaleGesture(ScaleGestureListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (detector.getCurrentSpan() > scaleCapacity) {
            listener.zoomLarge();
        } else {
            listener.zoomLittle();
        }
        scaleCapacity = detector.getCurrentSpan();
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        scaleCapacity = detector.getCurrentSpan();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        scaleCapacity = detector.getCurrentSpan();
        listener.zoomEnd();
    }

    public interface ScaleGestureListener {
        /**
         * 放大
         */
        void zoomLarge();

        /**
         * 缩小
         */
        void zoomLittle();

        void zoomEnd();
    }
}

package com.manna.codec.surface;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;

import com.manna.codec.utils.DisplayUtils;

import java.io.IOException;
import java.util.List;

/**
 * 相机管理类
 */
public class CameraManager {

    private static final String TAG = "CameraManager.class";

    private SurfaceTexture surfaceTexture;
    private Camera camera;
    private Context context;

    private int screenWidth;
    private int screenHeight;
    private int previewWidth;
    private int previewHeight;

    private int defaultCameraId = 0;

    public CameraManager(Context context) {
        this.context = context;
        this.screenWidth = DisplayUtils.getScreenWidth(context);
        this.screenHeight = DisplayUtils.getScreenHeight(context);
    }

    public void startCamera(SurfaceTexture surfaceTexture, int cameraId) {
        this.surfaceTexture = surfaceTexture;
        startCamera(cameraId);
    }

    private void startCamera(int cameraId) {
        try {
            camera = Camera.open(cameraId);
            camera.setPreviewTexture(surfaceTexture);

            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            parameters.setPreviewFormat(ImageFormat.NV21);

            //设置对焦模式，后置摄像头开启时打开，切换到前置时关闭(三星、华为不能设置前置对焦,魅族、小米部分机型可行)
            if (cameraId == 0) {
                //小米、魅族手机存在对焦无效情况，需要针对设备适配，想要无感知对焦完全适配最好是监听加速度传感器
                camera.cancelAutoFocus();
                //这种设置方式存在屏幕闪烁一下问题,包括Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            Camera.Size size = getCameraSize(parameters.getSupportedPreviewSizes(), screenWidth,
                    screenHeight, 0.1f);
            parameters.setPreviewSize(size.width, size.height);
            //水平方向未旋转，所以宽就是竖直方向的高，对应旋转操作
            Log.d(TAG, "startCamera: 预览宽:" + size.width + " -- " + "预览高:" + size.height);
            previewWidth = size.width;
            previewHeight = size.height;

            size = getCameraSize(parameters.getSupportedPictureSizes(), screenWidth, screenHeight, 0.1f);
            parameters.setPictureSize(size.width, size.height);
            //水平方向未旋转，所以宽就是竖直方向的高
            Log.d(TAG, "startCamera: 图片宽:" + size.width + " -- " + "图片高:" + size.height);

            camera.setParameters(parameters);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopPreview() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public void autoFocus() {
        if (camera != null) {
            camera.autoFocus(null);
        }
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }

    /**
     * 获取相机硬件实际支持的预览大小、图像大小
     *
     * @param cameraSize List<Camera.Size>
     * @param width      屏幕宽
     * @param height     屏幕高
     * @param diff       加权重比
     * @return 实际支持的预览大小、图像大小
     */
    private Camera.Size getCameraSize(List<Camera.Size> cameraSize, int width, int height, double diff) {
        if (width < height) {
            int temp = height;
            height = width;
            width = temp;
        }
        double ratio = (double) width / height;
        if (cameraSize == null || cameraSize.isEmpty()) {
            Log.d(TAG, "getCameraSize: 获取相机预览数据失败");
            return null;
        }

        Camera.Size outputSize = null;
        for (Camera.Size currentSize : cameraSize) {
            double currentRatio = (double) currentSize.width / currentSize.height;
            double currentDiff = Math.abs(currentRatio - ratio);
            if (currentDiff > diff) {
                continue;
            }
            if (outputSize == null) {
                outputSize = currentSize;
            } else {
                if (outputSize.width * outputSize.height < currentSize.width * currentSize.height) {
                    outputSize = currentSize;
                }
            }
            diff = currentDiff;
        }
        if (outputSize == null) {
            diff += 0.1f;
            if (diff > 1.0f) {
                outputSize = cameraSize.get(0);
            } else {
                outputSize = getCameraSize(cameraSize, width, height, diff);
            }
        }
        return outputSize;
    }

    /**
     * 设置闪光灯
     *
     * @param flashMode ：开关控制
     */
    public void setFlashMode(String flashMode) {
        boolean isFlashDevice = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!isFlashDevice) {
            //不支持闪光灯
            return;
        }
        Camera.Parameters parameters = camera.getParameters();
        parameters.setFlashMode(flashMode);
        camera.setParameters(parameters);
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (defaultCameraId == 0) {
                //后置切前置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    restartCameraPreview(i);
                    defaultCameraId = i;
                    break;
                }
            } else {
                //前置切后置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    restartCameraPreview(i);
                    defaultCameraId = i;
                    break;
                }
            }
        }
    }

    /**
     * 重新预览Camera
     *
     * @param cameraId :cameraId
     */
    private void restartCameraPreview(int cameraId) {
        stopPreview();
        startCamera(cameraId);
    }

    /**
     * 返回当前缩放值
     *
     * @param scaleRatio 缩放系数， 0 表示不缩放，1 表示放大，2 表示缩小
     * @return int
     */
    public int getZoomValue(int scaleRatio) {
        Camera.Parameters parameters = camera.getParameters();
        if (!parameters.isZoomSupported()) {
            return -1;
        }
        if (scaleRatio == 0) {
            return -1;
        }
        int defaultZoom = parameters.getZoom();
        int zoomValue;
        if (scaleRatio == 1) {
            zoomValue = defaultZoom + 1;
            if (zoomValue < parameters.getMaxZoom()) {
                parameters.setZoom(zoomValue);
                camera.setParameters(parameters);
            }
        } else {
            zoomValue = defaultZoom - 1;
            if (zoomValue >= 0) {
                parameters.setZoom(zoomValue);
                camera.setParameters(parameters);
            }
        }
        return zoomValue;
    }

    /**
     * 设置缩放值
     *
     * @param zoomValue ：SeekBar拖动的值
     */
    public void setZoomValue(int zoomValue) {
        Camera.Parameters parameters = camera.getParameters();
        if (!parameters.isZoomSupported()) {
            return;
        }
        Log.d(TAG, "setZoomValue: " + zoomValue);
        Log.d(TAG, "setZoomValue: Camera最大缩放值" + getMaxZoomValue());
        if (zoomValue > getMaxZoomValue()) {
            zoomValue = getMaxZoomValue();
        }
        parameters.setZoom(zoomValue);
        camera.setParameters(parameters);
    }

    /**
     * 返回默认最大缩放值
     *
     * @return int
     */
    public int getMaxZoomValue() {
        Camera.Parameters parameters = camera.getParameters();
        if (!parameters.isZoomSupported()) {
            return -1;
        }
        return parameters.getMaxZoom();
    }
}

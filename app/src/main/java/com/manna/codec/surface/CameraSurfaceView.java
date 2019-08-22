package com.manna.codec.surface;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

/**
 * 视图预览
 */
public class CameraSurfaceView extends EglSurfaceView implements CameraFBORender.OnSurfaceListener {

    private CameraManager cameraManager;
    private CameraFBORender render;
    private int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private int textureId;

    private Context context;

    public CameraSurfaceView(Context context) {
        this(context, null);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.context = context;
        cameraManager = new CameraManager(context);
        render = new CameraFBORender(context);
        render.setOnSurfaceListener(this);
        setSurfaceRender(render);

        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        previewAngle(context);
    }

    @Override
    public void onSurfaceCreate(SurfaceTexture surfaceTexture, int fboTextureId) {
        cameraManager.startCamera(surfaceTexture, cameraId);
        this.textureId = fboTextureId;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//        requestRender();
    }


    public void previewAngle(Context context) {
        int angle = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        render.resetMatrix();
        switch (angle) {
            case Surface.ROTATION_0:
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    render.setAngle(90, 0, 0, 1);
                    render.setAngle(180, 1, 0, 0);
                } else {
                    render.setAngle(90f, 0f, 0f, 1f);
                }
                break;
            case Surface.ROTATION_90:
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    render.setAngle(180, 0, 0, 1);
                    render.setAngle(180, 0, 1, 0);
                } else {
                    render.setAngle(90f, 0f, 0f, 1f);
                }
                break;
            case Surface.ROTATION_180:
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    render.setAngle(90f, 0.0f, 0f, 1f);
                    render.setAngle(180f, 0.0f, 1f, 0f);
                } else {
                    render.setAngle(-90, 0f, 0f, 1f);
                }
                break;
            case Surface.ROTATION_270:
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    render.setAngle(180f, 0.0f, 1f, 0f);
                } else {
                    render.setAngle(0f, 0f, 0f, 1f);
                }
                break;
        }
    }

    public int getTextureId() {
        return textureId;
    }

    public int getCameraPreviewWidth() {
        return cameraManager.getPreviewWidth();
    }

    public int getCameraPreviewHeight() {
        return cameraManager.getPreviewHeight();
    }

    public void setFlashMode(String flashMode) {
        cameraManager.setFlashMode(flashMode);
    }

    /**
     * 摄像头切换
     *
     * @param isSwitch true 表示后置切换前置，false表示前置切换后置
     */
    public void switchCamera(boolean isSwitch) {
        if (isSwitch) {
            cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        cameraManager.switchCamera();
        previewAngle(context);
    }

    /**
     * 获取当前相机预览缩放值
     */
    public int getZoomValue(int scaleRatio) {
        return cameraManager.getZoomValue(scaleRatio);
    }

    /**
     * 设置SeekBar拖动值
     *
     * @param zoomValue int
     */
    public void setZoomValue(int zoomValue) {
        cameraManager.setZoomValue(zoomValue);
    }

    /**
     * 设置滤镜类型
     *
     * @param type ：int
     */
    public void setType(int type) {
        render.setType(type);
    }

    public int getType() {
        return render.getType();
    }

    /**
     * 设置滤镜颜色
     *
     * @param color ：float
     */
    public void setColor(float[] color) {
        render.setColor(color);
    }

    public float[] getColor() {
        return render.getColor();
    }
}

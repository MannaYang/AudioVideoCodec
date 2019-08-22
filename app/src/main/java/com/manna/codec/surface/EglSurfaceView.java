package com.manna.codec.surface;

import android.content.Context;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * 预览
 */
public class EglSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private Surface surface;
    private Thread eglThread;
    private EGLContext eglContext;
    private int renderMode;
    private Render surfaceRender;

    private EglRunnable runnable;
    private EglManager eglManager;


    public EglSurfaceView(Context context) {
        this(context, null);
    }

    public EglSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EglSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //获取surfaceHolder预览对象
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (surface == null) {
            surface = holder.getSurface();
        }

        runnable = new EglRunnable();

        eglThread = new Thread(runnable);
        runnable.isSurfaceCreate = true;
        eglThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        runnable.width = width;
        runnable.height = height;
        runnable.isSurfaceChange = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        runnable.isStop = true;
        runnable.requestRender();
        eglThread.isInterrupted();
        surface = null;
        eglContext = null;
    }

    private class EglRunnable implements Runnable {

        private boolean isSurfaceCreate;
        private boolean isSurfaceChange;
        private boolean isStart;
        private boolean isStop;

        private int width;
        private int height;

        private Object object;

        @Override
        public void run() {
            isStart = false;
            isStop = false;
            object = new Object();
            eglManager = new EglManager();
            eglManager.init(surface, eglContext);
            while (true) {
                if (isStop) {
                    release();
                    break;
                }
                if (isStart) {
                    if (renderMode == GLSurfaceView.RENDERMODE_WHEN_DIRTY) {
                        synchronized (object) {
                            try {
                                object.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (renderMode == GLSurfaceView.RENDERMODE_CONTINUOUSLY) {
                        try {
                            Thread.sleep(1000 / 60);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (isSurfaceCreate && surfaceRender != null) {
                    surfaceRender.onSurfaceCreated();
                    isSurfaceCreate = false;
                }
                if (isSurfaceChange && surfaceRender != null) {
                    surfaceRender.onSurfaceChanged(width, height);
                    isSurfaceChange = false;
                }

                if (surfaceRender != null) {
                    surfaceRender.onDrawFrame();
                    if (!isStart) {
                        //首次调用
                        surfaceRender.onDrawFrame();
                    }
                }
                //OpenGL ES绘制完毕后调用swapBuffer,将前台的FrameBuffer和后台FrameBuffer进行交换
                eglManager.swapBuffer();
                isStart = true;
            }
        }

        /**
         * 释放资源
         */
        private void release() {
            eglManager.release();
        }

        /**
         * 重新绘制
         */
        public void requestRender() {
            if (object != null) {
                synchronized (object) {
                    object.notifyAll();
                }
            }
        }
    }

    public Render getSurfaceRender() {
        return surfaceRender;
    }

    public void setRenderMode(int renderMode) {
        this.renderMode = renderMode;
    }

    public int getRenderMode() {
        return renderMode;
    }

    public void requestRender() {
        if (runnable != null) {
            runnable.requestRender();
        }
    }

    public EGLContext getEglContext() {
        if (eglManager != null) {
            return eglManager.getEglContext();
        }
        return null;
    }

    /**
     * 回调接口
     */
    public interface Render {
        void onSurfaceCreated();

        void onSurfaceChanged(int width, int height);

        void onDrawFrame();

    }

    public void setSurfaceRender(Render surfaceRender) {
        this.surfaceRender = surfaceRender;
    }
}

package com.manna.codec.surface;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.util.Log;
import android.view.Surface;

/**
 * EGL管理类
 * EGL环境配置
 * 1.获取默认的EGLDisplay
 * 2.对EGLDisplay进行初始化
 * 3.输入预设置的参数获取EGL支持的EGLConfig
 * 4.通过EGLDisplay和EGLConfig创建一个EGLContext上下文环境
 * 5.创建一个EGLSurface来连接EGL和设备的屏幕
 * 6.在渲染线程绑定EGLSurface和EGLContext
 * 7.进行OpenGL ES的API渲染步骤
 * 8.调用SwapBuffer进行双缓冲切换显示渲染画面
 * 9.释放EGL相关资源EGLSurface、EGLContext、EGLDisplay
 */
public class EglManager {

    private static final String TAG = "EglManager.class";

    private EGLDisplay eglDisplay;
    private EGLSurface eglSurface;
    private EGLContext eglContext;

    /**
     * EGL配置步骤1-7
     *
     * @param surface ：surface
     * @param context ：EGLContext
     */
    public void init(Surface surface, EGLContext context) {

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.d(TAG, "获取显示设备失败 ");
            return;
        }
        int[] version = new int[2];
        boolean isInit = EGL14.eglInitialize(eglDisplay, version, 0, version, 1);
        if (!isInit) {
            Log.d(TAG, "EGL14初始化失败");
            return;
        }

        //参数配置以 id - value 格式组成
        int[] attribute = new int[]{
                EGL14.EGL_BUFFER_SIZE, 32,//颜色缓冲区所有组成颜色的位数
                EGL14.EGL_ALPHA_SIZE, 8,//缓冲区透明度位数
                EGL14.EGL_RED_SIZE, 8,//缓冲区红色位数
                EGL14.EGL_BLUE_SIZE, 8,//缓冲区蓝色位数
                EGL14.EGL_GREEN_SIZE, 8,//缓冲区绿色位数
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,//渲染窗口支持的布局类型
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,//EGL窗口支持的类型
                EGL14.EGL_NONE//标识结尾信息
        };

        EGLConfig[] eglConfig = new EGLConfig[1];
        int[] numConfigs = new int[1];
        boolean isConfig = EGL14.eglChooseConfig(eglDisplay, attribute, 0, eglConfig,
                0, eglConfig.length, numConfigs, 0);
        if (!isConfig || numConfigs[0] < 0) {
            Log.d(TAG, "config参数配置失败");
            return;
        }

        //指定OpenGL ES 2.0版本
        int[] contextAttribute = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};

        if (context == null) {
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig[0], EGL14.EGL_NO_CONTEXT,
                    contextAttribute, 0);
        } else {
            //传入context，表示与其它OpenGL ES上下文共享资源
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig[0], context,
                    contextAttribute, 0);
        }
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            Log.d(TAG, "EGLContext 创建失败");
            return;
        }

        int[] attrs = {EGL14.EGL_NONE};
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig[0], surface, attrs, 0);
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            Log.d(TAG, "连接EGL和设备屏幕失败");
            return;
        }

        //绑定eglContext
        boolean isMakeCurrent = EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
        if (!isMakeCurrent) {
            Log.d(TAG, "eglContext绑定失败");
        }
    }

    /**
     * 调用SwapBuffer进行双缓冲切换显示渲染画面
     */
    public boolean swapBuffer() {
        return EGL14.eglSwapBuffers(eglDisplay, eglSurface);
    }

    /**
     * 释放EGL相关资源EGLSurface、EGLContext、EGLDisplay
     */
    public void release() {
        if (eglSurface != null) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(eglDisplay, eglSurface);
            eglSurface = null;
        }
        if (eglContext != null) {
            EGL14.eglDestroyContext(eglDisplay, eglContext);
            eglContext = null;
        }
        if (eglDisplay != null) {
            EGL14.eglTerminate(eglDisplay);
            eglDisplay = null;
        }
    }

    /**
     * 获取EGLContext
     */
    public EGLContext getEglContext(){
        return eglContext;
    }
}

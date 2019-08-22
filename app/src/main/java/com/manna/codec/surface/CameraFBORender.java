package com.manna.codec.surface;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.manna.codec.utils.DisplayUtils;
import com.manna.codec.utils.ShaderUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 离屏渲染处理
 */
public class CameraFBORender implements EglSurfaceView.Render, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "CameraFBORender.class";

    //顶点
    private float[] vertexPoint = {
            -1f, -1f, 0f,
            1f, -1f, 0f,
            -1f, 1f, 0f,
            1f, 1f, 0f
    };

    //纹理
    private float[] texturePoint = {
            0f, 1f, 0f,
            1f, 1f, 0f,
            0f, 0f, 0f,
            1f, 0f, 0f
    };

    //位置
    private FloatBuffer vertexBuffer;
    //纹理
    private FloatBuffer textureBuffer;
    private int program;
    //坐标数组中每个顶点的坐标数
    private int coordinateVertex = 3;

    //顶点位置
    private int vPosition;
    //纹理位置
    private int tPosition;
    private int uMatrix;

    private int fboId;
    private int fboTextureId;

    private int cameraTextureId;

    private int vboId;

    private float[] matrix = new float[16];
    private int screenWidth;
    private int screenHeight;

    private Context context;
    private SurfaceTexture surfaceTexture;
    private CameraRender cameraRender;
    private OnSurfaceListener onSurfaceListener;

    public CameraFBORender(Context context) {
        this.context = context;
        screenWidth = DisplayUtils.getScreenWidth(context);
        screenHeight = DisplayUtils.getScreenHeight(context);
        cameraRender = new CameraRender(context);

        vertexBuffer = ByteBuffer.allocateDirect(vertexPoint.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexPoint);
        vertexBuffer.position(0);

        textureBuffer = ByteBuffer.allocateDirect(texturePoint.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(texturePoint);
        textureBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated() {
        program = ShaderUtils.createProgram(context.getResources(), "vertex_shader.glsl",
                "fragment_shader.glsl");
        if (program > 0) {
            //获取顶点坐标字段
            vPosition = GLES20.glGetAttribLocation(program, "av_Position");
            //获取纹理坐标字段
            tPosition = GLES20.glGetAttribLocation(program, "af_Position");
            uMatrix = GLES20.glGetUniformLocation(program, "u_Matrix");

            //创建vbo
            createVBO();
            //创建fbo
            createFBO(screenWidth, screenHeight);
            //创建相机预览扩展纹理
            createCameraTexture();
        }
        cameraRender.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        cameraRender.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame() {
        //调用触发onFrameAvailable，更新预览图像
        updateTextImage();
        //清空颜色
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //设置背景颜色
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        //使用程序
        GLES20.glUseProgram(program);

        //绑定fbo
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);

        //摄像头预览扩展纹理赋值
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);

        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glEnableVertexAttribArray(tPosition);

        //给变换矩阵赋值
        GLES20.glUniformMatrix4fv(uMatrix, 1, false, matrix, 0);

        //使用VBO设置纹理和顶点值
        useVboSetVertext();

        //绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(vPosition);
        GLES20.glDisableVertexAttribArray(tPosition);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        //渲染显示
        cameraRender.onDraw(fboTextureId);
    }

    /**
     * 初始化矩阵
     */
    public void resetMatrix() {
        //初始化
        Matrix.setIdentityM(matrix, 0);
    }


    /**
     * 旋转
     *
     * @param angle
     * @param x
     * @param y
     * @param z
     */
    public void setAngle(float angle, float x, float y, float z) {
        //旋转
        Matrix.rotateM(matrix, 0, angle, x, y, z);
    }

    public void updateTextImage() {
        surfaceTexture.updateTexImage();
    }

    /**
     * 创建vbo
     */
    private void createVBO() {
        //1. 创建VBO
        int[] vbos = new int[1];
        GLES20.glGenBuffers(vbos.length, vbos, 0);
        vboId = vbos[0];
        //2. 绑定VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
        //3. 分配VBO需要的缓存大小
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexPoint.length * 4 + texturePoint.length * 4, null, GLES20.GL_STATIC_DRAW);
        //4. 为VBO设置顶点数据的值
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexPoint.length * 4, vertexBuffer);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, vertexPoint.length * 4, texturePoint.length * 4, textureBuffer);
        //5. 解绑VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    /**
     * 使用vbo设置顶点位置
     */
    private void useVboSetVertext() {
        //1. 绑定VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
        //2. 设置顶点数据
        GLES20.glVertexAttribPointer(vPosition, 3, GLES20.GL_FLOAT, false, 12, 0);
        GLES20.glVertexAttribPointer(tPosition, 3, GLES20.GL_FLOAT, false, 12,
                vertexPoint.length * 4);
        //3. 解绑VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }


    /**
     * 创建fbo
     *
     * @param w
     * @param h
     */
    private void createFBO(int w, int h) {
        //1. 创建FBO
        int[] fbos = new int[1];
        GLES20.glGenFramebuffers(1, fbos, 0);
        fboId = fbos[0];
        //2. 绑定FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);

        //3. 创建FBO纹理
        int[] textureIds = new int[1];
        //创建纹理
        GLES20.glGenTextures(1, textureIds, 0);
        fboTextureId = textureIds[0];
        //绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId);
        //环绕（超出纹理坐标范围）  （s==x t==y GL_REPEAT 重复）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        //过滤（纹理像素映射到坐标点）  （缩小、放大：GL_LINEAR线性）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        //4. 把纹理绑定到FBO
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, fboTextureId, 0);

        //5. 设置FBO分配内存大小
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        //6. 检测是否绑定成功
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
                != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.d(TAG, "createFBO: 纹理绑定FBO失败");
        }
        //7. 解绑纹理和FBO
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    /**
     * 创建摄像头预览扩展纹理
     */
    private void createCameraTexture() {
        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        cameraTextureId = textureIds[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);
        //环绕（超出纹理坐标范围）  （s==x t==y GL_REPEAT 重复）
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        //过滤（纹理像素映射到坐标点）  （缩小、放大：GL_LINEAR线性）
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        surfaceTexture = new SurfaceTexture(cameraTextureId);
        surfaceTexture.setOnFrameAvailableListener(this);

        if (onSurfaceListener != null) {
            //回调给CameraManager获取surfaceTexture：通过camera.setPreviewTexture(surfaceTexture);
            onSurfaceListener.onSurfaceCreate(surfaceTexture, fboTextureId);
        }

        // 解绑扩展纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        onSurfaceListener.onFrameAvailable(surfaceTexture);
    }

    public void setOnSurfaceListener(OnSurfaceListener onSurfaceListener) {
        this.onSurfaceListener = onSurfaceListener;
    }

    public interface OnSurfaceListener {
        void onSurfaceCreate(SurfaceTexture surfaceTexture, int fboTextureId);

        void onFrameAvailable(SurfaceTexture surfaceTexture);
    }

    /***
     * 设置滤镜类型
     * @param type :int
     */
    public void setType(int type) {
        cameraRender.setType(type);
    }

    public int getType() {
        return cameraRender.getType();
    }

    /**
     * 设置滤镜颜色
     *
     * @param color :float[]
     */
    public void setColor(float[] color) {
        cameraRender.setColor(color);
    }

    public float[] getColor() {
        return cameraRender.getColor();
    }
}

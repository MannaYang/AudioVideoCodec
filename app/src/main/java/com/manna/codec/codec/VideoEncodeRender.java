package com.manna.codec.codec;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.manna.codec.surface.EglSurfaceView;
import com.manna.codec.utils.ShaderUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 录制视频Render
 */
public class VideoEncodeRender implements EglSurfaceView.Render {
    //顶点坐标
    private float vertexData[] = {
            -1f, -1f, 0.0f, // bottom left
            1f, -1f, 0.0f, // bottom right
            -1f, 1f, 0.0f, // top left
            1f, 1f, 0.0f,  // top right

            0f, 0f, 0f,//水印预留位置
            0f, 0f, 0f,
            0f, 0f, 0f,
            0f, 0f, 0f
    };

    //纹理坐标  对应顶点坐标
    private float textureData[] = {
            0f, 1f, 0.0f, // bottom left
            1f, 1f, 0.0f, // bottom right
            0f, 0f, 0.0f, // top left
            1f, 0f, 0.0f,  // top right
    };
    //顶点上的点
    private final int COORDS_PER_VERTEX = 3;

    //一个顶点占4字节
    private int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    //位置
    private FloatBuffer vertexBuffer;
    //纹理
    private FloatBuffer textureBuffer;
    private int program;
    private int avPosition;

    //纹理位置
    private int afPosition;
    //纹理  默认第0个位置 可以不获取
    private int texture;


    //vbo id
    private int vboId;

    private int textureId;

    private Context context;

    //滤镜传入类型
    private int type;
    //滤镜传入颜色
    private float[] color;

    //水印图片
    private Bitmap bitmap;
    //水印纹理id
    private int waterTextureId;

    //颜色类型切换
    private int changeType;
    //颜色值切换
    private int changeColor;

    public VideoEncodeRender(Context context, int textureId, int type, float[] color) {
        this.context = context;
        this.textureId = textureId;
        this.type = type;
        this.color = color;

        initWater();

        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureBuffer = ByteBuffer.allocateDirect(textureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureData);
        textureBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated() {

        //启用透明
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        program = ShaderUtils.createProgram(context.getResources(), "vertex_shader_screen.glsl",
                "fragment_shader_screen.glsl");

        if (program > 0) {
            //获取顶点坐标字段
            avPosition = GLES20.glGetAttribLocation(program, "av_Position");
            //获取纹理坐标字段
            afPosition = GLES20.glGetAttribLocation(program, "af_Position");
            //获取纹理字段
            texture = GLES20.glGetUniformLocation(program, "sTexture");

            //滤镜传入类型
            changeType = GLES20.glGetUniformLocation(program, "vChangeType");
            //对应滤镜需要的颜色
            changeColor = GLES20.glGetUniformLocation(program, "vChangeColor");

            //创建vbo
            createVBO();

            //创建水印纹理
            createWaterTextureId();
        }
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        //宽高
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame() {
        //清空颜色
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //设置背景颜色
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        //使用程序
        GLES20.glUseProgram(program);

        GLES20.glUniform1i(changeType, type);
        GLES20.glUniform3fv(changeColor, 1, color, 0);

        //设置纹理
        //绑定渲染纹理  默认是第0个位置
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glEnableVertexAttribArray(avPosition);
        GLES20.glEnableVertexAttribArray(afPosition);

        //使用VBO设置纹理和顶点值
        useVboSetVertext();

        //绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(avPosition);
        GLES20.glDisableVertexAttribArray(afPosition);
        //解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        //开始绘制
        drawWater();
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
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4 + textureData.length * 4, null, GLES20.GL_STATIC_DRAW);
        //4. 为VBO设置顶点数据的值
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexData.length * 4, vertexBuffer);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, textureData.length * 4, textureBuffer);
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
        GLES20.glVertexAttribPointer(avPosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, 0);
        GLES20.glVertexAttribPointer(afPosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexData.length * 4);
        //3. 解绑VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    /**
     * 初始化水印坐标，顶点坐标数组中下标为0-11的为顶点坐标，下标12以后为水印坐标
     */
    private void initWater() {
        bitmap = ShaderUtils.createTextImage("Manna Yang", 35, "#ffffff",
                "#00000000", 0);

        //计算宽高比
        float r = 1.0f * bitmap.getWidth() / bitmap.getHeight();
        //保证在-1.0 - 1.0坐标系范围内
        float w = r * 0.1f;
        //虚拟坐标，文字绘制在坐标系左下角，以文字中心为虚拟坐标原点，所有坐标满足原坐标系左下方向的坐标，
        //即坐标都为负数
        vertexData[12] = -0.8f;
        vertexData[13] = -0.8f;
        vertexData[14] = 0;//左下

        vertexData[15] = w - 0.8f;
        vertexData[16] = -0.8f;
        vertexData[17] = 0;//右下

        vertexData[18] = -0.8f;
        vertexData[19] = -0.7f;
        vertexData[20] = 0;//左上

        vertexData[21] = w - 0.8f;
        vertexData[22] = -0.7f;
        vertexData[23] = 0;//右上
    }

    /**
     * 创建水印纹理id
     */
    private void createWaterTextureId() {

        int[] textureIds = new int[1];
        //创建纹理
        GLES20.glGenTextures(1, textureIds, 0);
        waterTextureId = textureIds[0];
        //绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, waterTextureId);
        //环绕（超出纹理坐标范围）  （s==x t==y GL_REPEAT 重复）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        //过滤（纹理像素映射到坐标点）  （缩小、放大：GL_LINEAR线性）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        ByteBuffer bitmapBuffer = ByteBuffer.allocate(bitmap.getHeight() * bitmap.getWidth() * 4);//RGBA
        bitmap.copyPixelsToBuffer(bitmapBuffer);
        //将bitmapBuffer位置移动到初始位置
        bitmapBuffer.flip();

        //设置内存大小绑定内存地址
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap.getWidth(), bitmap.getHeight(),
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bitmapBuffer);

        //解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    /**
     * 绘制水印
     */
    public void drawWater() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, waterTextureId);

        GLES20.glEnableVertexAttribArray(avPosition);
        GLES20.glEnableVertexAttribArray(afPosition);

        GLES20.glVertexAttribPointer(avPosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride,
                vertexStride * 4);//四个坐标之后的是水印的坐标
        GLES20.glVertexAttribPointer(afPosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride,
                vertexData.length * 4);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(avPosition);
        GLES20.glDisableVertexAttribArray(afPosition);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }
}

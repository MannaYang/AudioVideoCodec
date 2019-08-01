package com.manna.codec.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;

import java.io.InputStream;

/**
 * shader创建工具类
 */
public class ShaderUtils {
    private ShaderUtils() {
    }

    /**
     * 创建着色器程序
     *
     * @param resources   ：Context.getResource()
     * @param vertexRes   :顶点着色器资源文件
     * @param fragmentRes ：片元着色器资源文件
     * @return int - 着色器程序
     */
    public static int createProgram(Resources resources, String vertexRes, String fragmentRes) {
        return createProgram(loadAssetSource(resources, vertexRes), loadAssetSource(resources, fragmentRes));
    }

    /**
     * 从资源文件读取着色器代码
     *
     * @param resources ：Context.getResource()
     * @param shaderRes :assets文件夹下 着色器 代码文件
     * @return ：String -  读取的着色器代码
     */
    private static String loadAssetSource(Resources resources, String shaderRes) {
        StringBuilder result = new StringBuilder();
        try {
            InputStream is = resources.getAssets().open(shaderRes);
            int ch;
            byte[] buffer = new byte[1024];
            while (-1 != (ch = is.read(buffer))) {
                result.append(new String(buffer, 0, ch));
            }
        } catch (Exception e) {
            return null;
        }
        return result.toString().replaceAll("\\r\\n", "\n");
    }

    /**
     * 创建着色器程序
     *
     * @param vertexShaderCode   ：顶点着色器代码
     * @param fragmentShaderCode ：片元着色器代码
     * @return program
     */
    private static int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        if (vertexShader == 0) {
            return 0;
        }

        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        if (fragmentShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (0 != program) {
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    /**
     * 加载shader
     *
     * @param type      ：shader类型
     * @param shadeCode ：着色器代码
     * @return shader
     */
    private static int loadShader(int type, String shadeCode) {
        int shader = GLES20.glCreateShader(type);
        if (0 != shader) {
            GLES20.glShaderSource(shader, shadeCode);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    /**
     * 创建水印图片
     *
     * @param text      文本
     * @param textSize  文字大小
     * @param textColor 颜色
     * @param bgColor   背景色
     * @param padding   间距
     * @return Bitmap
     */
    public static Bitmap createTextImage(String text, int textSize, String textColor, String bgColor, int padding) {

        Paint paint = new Paint();
        paint.setColor(Color.parseColor(textColor));
        paint.setTextSize(textSize);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        float width = paint.measureText(text, 0, text.length());

        float top = paint.getFontMetrics().top;
        float bottom = paint.getFontMetrics().bottom;

        Bitmap bm = Bitmap.createBitmap((int) (width + padding * 2), (int) ((bottom - top) + padding * 2), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);

        canvas.drawColor(Color.parseColor(bgColor));
        canvas.drawText(text, padding, -top + padding, paint);
        return bm;
    }
}

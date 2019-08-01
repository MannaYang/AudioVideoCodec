#extension GL_OES_EGL_image_external : require //申明使用扩展纹理
precision mediump float;//精度 为float
varying vec2 v_texPo;//纹理位置  接收于vertex_shader
uniform sampler2D  sTexture;
uniform int vChangeType;
uniform vec3 vChangeColor;

void main() {
    vec4 nColor=texture2D(sTexture, v_texPo);
    if (vChangeType==1){
        //黑白滤镜
        float changeColor=nColor.r*vChangeColor.r+nColor.g*vChangeColor.g+nColor.b*vChangeColor.b;
        gl_FragColor=vec4(changeColor, changeColor, changeColor, nColor.a);
    } else {
        gl_FragColor=nColor;
    }
}
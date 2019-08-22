package com.manna.codec;

import android.Manifest;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.media.MediaMuxer;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.manna.codec.codec.MediaEncodeManager;
import com.manna.codec.codec.MediaMuxerChangeListener;
import com.manna.codec.codec.VideoEncodeRender;
import com.manna.codec.record.AudioCapture;
import com.manna.codec.scale.ScaleGesture;
import com.manna.codec.surface.CameraSurfaceView;
import com.manna.codec.utils.AlphaAnimationUtils;
import com.manna.codec.utils.ByteUtils;
import com.manna.codec.utils.FileUtils;
import com.manna.library_plugin.permission.Permission;
import com.manna.library_plugin.permission.PermissionConstants;
import com.manna.library_plugin.permission.PermissionGlobalCallback;
import com.manna.library_plugin.permission.PermissionManage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.manna.codec.utils.DisplayUtils.adjustBrightness;

/**
 * 录像功能 -- 录音+录视频+编解码+合成视频
 */
public class MainActivity extends AppCompatActivity implements MediaMuxerChangeListener,
        ScaleGesture.ScaleGestureListener {

    private final String TAG = "MainActivity.class";
    private CameraSurfaceView cameraSurfaceView;
    private ImageView ivRecord, ivSplash, ivSwitch, ivFocus, ivFilter;
    private SeekBar sbScale;

    //录音
    private AudioCapture audioCapture;

    private MediaEncodeManager mediaEncodeManager;

    //开启 -- 关闭录制
    private boolean isStartRecord = false;
    //开启 -- 关闭闪光灯
    private boolean isFlashOpen = false;
    //开启 -- 关闭前后摄像头切换
    private boolean isSwitchCamera = false;

    //手势缩放监听
    private ScaleGestureDetector scaleGestureDetector;
    //SeekBar是否正在拖动
    private boolean isSeekBarOnTouch = false;

    //默认滤镜 -- 原色， 1 -- 表示黑白
    private int filterType = 0;

    private Handler calcDecibel = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //存储权限获取的缓存目录
        initPermissionManage();

        init();

        //调节屏幕亮度
        adjustBrightness(0.6f, this);

        //手势缩放监听
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGesture(this));
    }

    @Permission(permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA},
            rationales = {PermissionConstants.AUDIO_RATIONALE, PermissionConstants.CAMERA_RATIONALE},
            rejects = {PermissionConstants.AUDIO_REJECT, PermissionConstants.CAMERA_REJECT})
    private void init() {
        setContentView(R.layout.activity_main);

        ivRecord = findViewById(R.id.iv_record);
        ivSplash = findViewById(R.id.iv_splash);
        ivSwitch = findViewById(R.id.iv_switch);
        ivFocus = findViewById(R.id.iv_focus);
        ivFilter = findViewById(R.id.iv_filter);

        sbScale = findViewById(R.id.sb_scale);
        sbScale.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        sbScale.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);

        cameraSurfaceView = findViewById(R.id.camera_surface_view);

        audioCapture = new AudioCapture();

        ivRecord.setOnClickListener(v -> {
            //录像
            if (!isStartRecord) {
                //开启录像时，不允许切换摄像头
                ivSwitch.setVisibility(View.GONE);
                //开启录像时，不允许切换滤镜
                ivFilter.setVisibility(View.GONE);

                initMediaCodec();
                mediaEncodeManager.startEncode();
                audioCapture.start();
                ivRecord.setImageResource(R.mipmap.ic_stop_record);
                isStartRecord = true;
            } else {
                ivSwitch.setVisibility(View.GONE);
                ivFilter.setVisibility(View.VISIBLE);

                isStartRecord = false;
                mediaEncodeManager.stopEncode();
                audioCapture.stop();
                ivRecord.setImageResource(R.mipmap.ic_start_record);
            }
        });

        ivSplash.setOnClickListener(v -> {
            //切换闪光灯
            if (!isFlashOpen) {
                //打开
                cameraSurfaceView.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                isFlashOpen = true;
                ivSplash.setImageResource(R.mipmap.ic_splash_on);
            } else {
                //关闭
                cameraSurfaceView.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                isFlashOpen = false;
                ivSplash.setImageResource(R.mipmap.ic_splash_off);
            }
        });

        ivSwitch.setOnClickListener(v -> {
            //切换前后摄像头 -- 默认开启后置摄像头
            if (!isSwitchCamera) {
                //true 后置切前置，不允许开启闪光灯
                ivSplash.setVisibility(View.GONE);

                cameraSurfaceView.switchCamera(true);
                isSwitchCamera = true;
            } else {
                ivSplash.setVisibility(View.VISIBLE);

                cameraSurfaceView.switchCamera(false);
                isSwitchCamera = false;
            }
        });

        //SeekBar拖动
        sbScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isSeekBarOnTouch) {
                    cameraSurfaceView.setZoomValue(progress);
                    if (progress == 0) {
                        seekBar.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeekBarOnTouch = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeekBarOnTouch = false;
                new AlphaAnimationUtils().setHideAnimation(ivFocus, 2000);
            }
        });

        //原色 -  黑白滤镜
        ivFilter.setOnClickListener(v -> {
            //默认是原色
            if (filterType == 0) {
                //由原色 -- 黑白,采用去色原理，只要把RGB三通道的色彩信息设置成一样,
                // 即：R＝G＝B，那么图像就变成了灰色，为了保证图像亮度不变，同一个通道中的R+G+B=1
                //0.213+0.715+0.072=1
                filterType = 1;
                setRenderAttr(filterType, new float[]{0.213f, 0.715f, 0.072f});
            } else {
                //由黑白 -- 原色
                filterType = 0;
                setRenderAttr(filterType, new float[]{0.0f, 0.0f, 0.0f});
            }
        });
    }

    /***
     * 设置滤镜颜色
     * @param type :区分滤镜类型
     * @param color ：对应滤镜颜色数组
     */
    private void setRenderAttr(int type, float[] color) {
        cameraSurfaceView.setType(type);
        cameraSurfaceView.setColor(color);
        cameraSurfaceView.requestRender();
    }

    private void initMediaCodec() {
        String currentDate = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.CHINA).format(new Date());
        String fileName = "/VID_".concat(currentDate).concat(".mp4");
        String filePath = FileUtils.getDiskCachePath(this) + fileName;
        int mediaFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;
        //String audioType = MediaFormat.MIMETYPE_AUDIO_AAC;
        String audioType = "audio/mp4a-latm";
        //String videoType = MediaFormat.MIMETYPE_VIDEO_AVC;
        String videoType = "video/avc";
        int sampleRate = 44100;
        int channelCount = 2;//单声道 channelCount=1 , 双声道  channelCount=2
        //AudioCapture.class类中采集音频采用的位宽：AudioFormat.ENCODING_PCM_16BIT ，此处应传入16bit，
        // 用作计算pcm一帧的时间戳
        int audioFormat = 16;
        //预览
        int width = cameraSurfaceView.getCameraPreviewHeight();
        int height = cameraSurfaceView.getCameraPreviewWidth();

        mediaEncodeManager = new MediaEncodeManager(new VideoEncodeRender(this,
                cameraSurfaceView.getTextureId(), cameraSurfaceView.getType(), cameraSurfaceView.getColor()));

        mediaEncodeManager.initMediaCodec(filePath, mediaFormat, audioType, sampleRate,
                channelCount, audioFormat, videoType, width, height);

        mediaEncodeManager.initThread(this, cameraSurfaceView.getEglContext(),
                GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    //录音线程数据回调
    private void setPcmRecordListener() {
        if (audioCapture.getCaptureListener() == null)
            audioCapture.setCaptureListener((audioSource, audioReadSize) -> {
                if (MediaCodecConstant.audioStop || MediaCodecConstant.videoStop) {
                    return;
                }
                mediaEncodeManager.setPcmSource(audioSource, audioReadSize);
                //计算分贝的一种方式
                calcDecibel.postDelayed(() -> {
                    double dBValue = ByteUtils.calcDecibelValue(audioSource, audioReadSize);
                    Log.d(TAG, "calcDecibelLevel: 分贝值 = " + dBValue);
                }, 200);
            });
    }

    //初始化aop权限
    private void initPermissionManage() {
        //初始化全局权限申请回调
        PermissionManage.init(new PermissionGlobalCallback() {
            @Override
            public void shouldShowRational(String permission, int ration) {
                //权限被拒绝,未勾选不再提醒,可根据ration值不同显示对应提示信息
                if (ration == PermissionConstants.AUDIO_RATIONALE) {
                    showRationaleDialog(PermissionConstants.MSG_AUDIO_RATIONALE);
                } else if (ration == PermissionConstants.CAMERA_RATIONALE) {
                    showRationaleDialog(PermissionConstants.MSG_CAMERA_RATIONALE);
                }
            }

            @Override
            public void onPermissionReject(String permission, int reject) {
                //权限被拒绝，勾选不再提醒,可根据ration值不同显示对应提示信息
                if (reject == PermissionConstants.AUDIO_REJECT) {
                    showRationaleDialog(PermissionConstants.MSG_AUDIO_REJECT);
                } else if (reject == PermissionConstants.CAMERA_REJECT) {
                    showRationaleDialog(PermissionConstants.MSG_CAMERA_REJECT);
                }
            }
        });
    }

    //权限弹窗
    private void showRationaleDialog(String msg) {
        showRejectDialog(msg);
    }

    private void showRejectDialog(String msg) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("权限申请")
                .setMessage(msg)
                .setPositiveButton("跳转设置", (dialog, which) -> {
                    dialog.dismiss();
                    PermissionManage.startSettingsActivity(MainActivity.this);
                    finish();
                })
                .setNegativeButton("取消", (dialog, i) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }

    @Override
    public void onMediaMuxerChangeListener(int type) {
        if (type == MediaCodecConstant.MUXER_START) {
            Log.d(TAG, "onMediaMuxerChangeListener --- " + "视频录制开始了");
            setPcmRecordListener();
        }
    }

    @Override
    public void onMediaInfoListener(int time) {
        Log.d(TAG, "视频录制时长 --- " + time);
    }

    @Override
    public void zoomLarge() {
        //返回值可设置seekBar
        int seekValue = cameraSurfaceView.getZoomValue(MediaCodecConstant.LARGE_SCALE);
        if (seekValue > 100) {
            return;
        }
        if (sbScale.getVisibility() == View.GONE) {
            sbScale.setVisibility(View.VISIBLE);
        }
        sbScale.setProgress(seekValue);
    }

    @Override
    public void zoomLittle() {
        int seekValue = cameraSurfaceView.getZoomValue(MediaCodecConstant.LITTLE_SCALE);
        if (seekValue <= 1) {
            sbScale.setVisibility(View.GONE);
            return;
        }
        sbScale.setProgress(seekValue);
    }

    @Override
    public void zoomEnd() {
        //手指缩放完成,延迟2s隐藏,留出预览位置
        if (!isSeekBarOnTouch) {
            return;
        }
        new AlphaAnimationUtils().setHideAnimation(ivFocus, 2000);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() >= 2) {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        } else {
            //非双指操作,点击屏幕任意位置聚焦，并显示
            ivFocus.setX(event.getX());
            ivFocus.setY(event.getY());
            ivFocus.setVisibility(View.VISIBLE);
            new AlphaAnimationUtils().setHideAnimation(ivFocus, 2000);
            return true;
        }
    }
}

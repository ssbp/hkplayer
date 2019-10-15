package com.changsir.videoplayer.hikvision.utils;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.changsir.videoplayer.hikvision.HikviUtil;
import com.changsir.videoplayer.hikvision.VideoModel;

/**
 * 预览认证
 */
public class PreviewThread implements Runnable {

    private static final int FAILED = 0;
    private static final int SUCCESS = 1;
    private static final int PROCESS = 2;

    private OnHikviOptListener onHikviOptListener;
    private VideoModel videoModel;
    private HikviUtil hikviUtil;
    private HikviHandler hikviHandler;

    public PreviewThread(VideoModel videoModel, HikviUtil hikviUtil) {
        this.videoModel = videoModel;
        this.hikviUtil = hikviUtil;
        hikviHandler = new HikviHandler();
    }

    public void setOnHikviOptListener(OnHikviOptListener onHikviOptListener) {
        this.onHikviOptListener = onHikviOptListener;
    }

    /**
     * 开启
     */
    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        sendMsg(PROCESS, "正在初始化视频");
        if(hikviUtil.isLiving(videoModel.getChannel())) {
            //4.准备播放
            sendMsg(SUCCESS, "正在加载视频");
        } else {
            sendMsg(FAILED, "当前视频已掉线");
        }
    }

    /**
     * 发送handler
     * @param code
     * @param content
     */
    private void sendMsg(int code, String content) {
        Message msg = hikviHandler.obtainMessage();
        msg.what = code;
        msg.obj = content;
        msg.sendToTarget();
    }

    class HikviHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            int what = msg.what;
            String content = msg.obj.toString();

            if(null != onHikviOptListener) {
                switch (what) {
                    case PROCESS:
                        onHikviOptListener.process(content);
                        break;
                    case FAILED:
                        onHikviOptListener.failed(content);
                        break;
                    case SUCCESS:
                        onHikviOptListener.finish(content);
//                        hikviUtil.startPreview(videoModel.getChannel());
                        break;
                }
            }
        }
    }
}
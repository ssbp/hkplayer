package com.changsir.videoplayer.hikvision.utils;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.changsir.videoplayer.hikvision.HikviUtil;
import com.changsir.videoplayer.hikvision.VideoModel;

/**
 * 登录认证
 */
public class LoginThread implements Runnable {

    private static final int FAILED = 0;
    private static final int SUCCESS = 1;
    private static final int PROCESS = 2;

    private OnHikviOptListener onHikviOptListener;
    private VideoModel videoModel;
    private HikviUtil hikviUtil;
    private LoginHandler loginHandler;

    public LoginThread(VideoModel videoModel, HikviUtil hikviUtil) {
        this.videoModel = videoModel;
        this.hikviUtil = hikviUtil;
        loginHandler = new LoginHandler();
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
        //1.初始化
        boolean initR = hikviUtil.init(videoModel);
        if (initR) {
            sendMsg(PROCESS, "正在认证视频信息");
            //2.登陆
            if (hikviUtil.loginDevice() != -1) {
                //登录成功
                sendMsg(SUCCESS, "认证成功");
            } else {
                sendMsg(FAILED, "认证视频失败");
            }
        } else {
            sendMsg(FAILED, "视频初始化失败");
        }
    }

    /**
     * 发送handler
     * @param code
     * @param content
     */
    private void sendMsg(int code, String content) {
        Message msg = loginHandler.obtainMessage();
        msg.what = code;
        msg.obj = content;
        msg.sendToTarget();
    }

    class LoginHandler extends Handler {
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
                        break;
                }
            }
        }
    }
}
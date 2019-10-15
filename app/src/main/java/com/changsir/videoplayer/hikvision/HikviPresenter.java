package com.changsir.videoplayer.hikvision;

import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.changsir.videoplayer.hikvision.utils.HistoryThread;
import com.changsir.videoplayer.hikvision.utils.LoginThread;
import com.changsir.videoplayer.hikvision.utils.OnHikviOptListener;
import com.changsir.videoplayer.hikvision.utils.PreviewThread;
import com.hikvision.netsdk.NET_DVR_PTZCFG;

import org.MediaPlayer.PlayM4.Player;

import java.util.Calendar;

/**
 * 海康播放器逻辑处理
 */
public class HikviPresenter {

    private static final String TAG = "HikviPresenter";

    private LoginThread loginThread;
    private PreviewThread previewThread;

    private boolean allowPTZ = false;
    private HikviUtil hikviUtil;
    private VideoModel videoModel;

    private PlayerActivityInterface playerActivityInterface;

    public HikviPresenter(VideoModel videoModel) {
        hikviUtil = new HikviUtil();
        this.videoModel = videoModel;

        //登录
        loginThread = new LoginThread(videoModel, hikviUtil);
        previewThread = new PreviewThread(videoModel, hikviUtil);
    }

    public void setPlayerActivityInterface(PlayerActivityInterface playerActivityInterface) {
        this.playerActivityInterface = playerActivityInterface;
    }

    /**
     * 开始预览
     */
    public void startLogin(OnHikviOptListener onHikviOptListener) {
        loginThread.setOnHikviOptListener(onHikviOptListener);
        loginThread.start();
    }

    /**
     * 获取云台协议
     */
    public NET_DVR_PTZCFG fetchPTZControl() {
        if(null != hikviUtil) {
            NET_DVR_PTZCFG p = hikviUtil.fetchPTZControl();
            if(null != p) {
                Log.e("=----", "支持");
                allowPTZ = true;
            }
           return p;
        }
        allowPTZ = false;
        Log.e("=----", "不支持");
        return null;
    }

    /**
     * 获取是否允许云台
     * @return
     */
    public boolean isAllowPTZ() {
        return allowPTZ;
    }

    /**
     * 云台操作
     * @param r
     */
    public boolean ptzControl(int r) {
        if(null != hikviUtil && allowPTZ) {
            return hikviUtil.ptzControl(r);
        }
        return false;
    }

    /**
     * 开始预览
     */
    public void startPreview(SurfaceView surfaceView, OnHikviOptListener onHikviOptListener) {
        previewThread.setOnHikviOptListener(new OnHikviOptListener() {
            @Override
            public void failed(String content) {
                if(null != playerActivityInterface) {
                    playerActivityInterface.updateProgress(content);
                }
            }

            @Override
            public void finish(String content) {
                if(null != hikviUtil) {
                    hikviUtil.startPreview(videoModel.getChannel(), surfaceView, onHikviOptListener);
                }
            }

            @Override
            public void process(String content) {
                if(null != playerActivityInterface) {
                    playerActivityInterface.updateProgress(content);
                }
            }
        });
        previewThread.start();
    }

    /**
     * 获取录像列表
     */
    public void getHistoryList(Calendar calendar, HistoryThread.OnHikviHistoryListener onHikviHistoryListener) {
        new HistoryThread(videoModel, hikviUtil)
                .setSearchDay(calendar)
                .setOnHikviHistoryListener(onHikviHistoryListener)
                .start();
    }

    /**
     * 开始回放
     * @param name
     * @param surfaceView
     */
    public void startPlayback(String name, SurfaceView surfaceView) {
        if(null != hikviUtil) {
            hikviUtil.stopPlayback();
            hikviUtil.startPlayBackByName(name, surfaceView);
        }
    }

    /**
     * 停止回放
     */
    public void stopPlayback() {
        if(null != hikviUtil) {
            hikviUtil.stopPlayback();
        }
    }

    /**
     * 停止预览
     */
    public void stopPreview() {
        //暂停播放
        if(null != hikviUtil) {
            hikviUtil.stopSinglePreview();
        }
    }

    /**
     * 销毁
     */
    public void destory() {
        new Thread(new DestoryHkhandler()).start();
    }

    /**
     * 发生改变
     * @param holder
     */
    public void surfaceChange(SurfaceHolder holder) {
        int m_iPort = hikviUtil.getIPort();
        if (-1 == m_iPort) {
            return;
        }
        Surface surface = holder.getSurface();
        if (true == surface.isValid()) {
            if (false == Player.getInstance()
                    .setVideoWindow(m_iPort, 0, holder)) {
            }
        }
    }

    /**
     * 释放端口
     * @param holder
     */
    public void surfaceDestory(SurfaceHolder holder) {
        int m_iPort = hikviUtil.getIPort();
        Log.e(TAG, "释放播放端口!" + m_iPort);
        if (-1 == m_iPort) {
            return;
        }
        if (true == holder.getSurface().isValid()) {
            if (false == Player.getInstance().setVideoWindow(m_iPort, 0, null)) {
                Log.e(TAG, "播放端口释放失败!");
            }
        }
        hikviUtil.stopSinglePreview();
    }

    /**
     * 销毁
     */
    class DestoryHkhandler implements Runnable {
        @Override
        public void run() {
            if(null != hikviUtil) {
                hikviUtil.logoutDevice();
                hikviUtil.destory();
            }
        }
    }

}

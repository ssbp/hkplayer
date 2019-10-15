package com.changsir.videoplayer.hikvision.utils;

import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;

import com.changsir.videoplayer.hikvision.HikviUtil;
import com.changsir.videoplayer.hikvision.VideoModel;
import com.hikvision.netsdk.NET_DVR_FINDDATA_V30;

import java.util.Calendar;
import java.util.List;

/**
 * 回放列表
 */
public class HistoryThread implements Runnable {

    private static final int FAILED = 0;
    private static final int FINISH = 1;
    private static final int START = 2;

    private OnHikviHistoryListener onHikviHistoryListener;
    private VideoModel videoModel;
    private HikviUtil hikviUtil;
    private HikviHandler hikviHandler;
    private Calendar calendar;

    public HistoryThread(VideoModel videoModel, HikviUtil hikviUtil) {
        this.videoModel = videoModel;
        this.hikviUtil = hikviUtil;
        hikviHandler = new HikviHandler();
    }

    public HistoryThread setOnHikviHistoryListener(OnHikviHistoryListener onHikviHistoryListener) {
        this.onHikviHistoryListener = onHikviHistoryListener;
        return this;
    }

    public HistoryThread setSearchDay(Calendar calendar) {
        this.calendar = calendar;
        return this;
    }

    /**
     * 开启
     */
    public void start() {
        if(null != calendar) {
            new Thread(this).start();
        }
    }

    @Override
    public void run() {
        Message msg = hikviHandler.obtainMessage();
        msg.what = START;
        msg.sendToTarget();

        Calendar start = Calendar.getInstance();
        start.setTime(calendar.getTime());
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 10);

        List<NET_DVR_FINDDATA_V30> list = hikviUtil.getPlayBackList(videoModel.getChannel(), start, calendar);

        msg = hikviHandler.obtainMessage();
        msg.what = FINISH;
        msg.obj = list;
        msg.sendToTarget();
    }

    class HikviHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            int what = msg.what;

            if(null != onHikviHistoryListener) {
                switch (what) {
                    case START:
                        onHikviHistoryListener.onStart();
                        break;
                    case FAILED:
                        onHikviHistoryListener.onError();
                        break;
                    case FINISH:
                        onHikviHistoryListener.onFinish((List<NET_DVR_FINDDATA_V30>)msg.obj);
                        break;
                }
            }
        }
    }

    public interface OnHikviHistoryListener {
        void onStart();
        void onFinish(List<NET_DVR_FINDDATA_V30> list);
        void onError();
    }
}
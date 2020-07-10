package com.changsir.videoplayer.rtsp;

import android.media.MediaPlayer;
import android.util.Log;

public class VideoSizeChangeListener implements MediaPlayer.OnVideoSizeChangedListener {

    private int sWidth;
    private int sHeight;
    private OnChangeSizeListener onChangeSizeListener;

    public VideoSizeChangeListener(int width, int height, OnChangeSizeListener onChangeSizeListener) {
        this.sWidth = width;
        this.sHeight = height;
        this.onChangeSizeListener = onChangeSizeListener;
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
        if (width == 0 || height == 0) {
            Log.e("MEDIA_PLAYER", "invalid video width(" + width + ") or height(" + height
                    + ")");
            return;
        }

        if (width > sWidth || height > sHeight) {
            //如果video的宽或者高超出了当前屏幕的大小，则要进行缩放
            float wRatio = (float) width / sWidth;
            float hRatio = (float) height / sHeight;

            //选择大的一个进行缩放
            float ratio = Math.max(wRatio, hRatio);

            width = (int) Math.ceil((float) width / ratio);
            height = (int) Math.ceil((float) height / ratio);


            if(null != onChangeSizeListener) {
                onChangeSizeListener.finish(width, height);
            }
        }
    }

    public interface OnChangeSizeListener {
        void finish(int width, int height);
    }
}

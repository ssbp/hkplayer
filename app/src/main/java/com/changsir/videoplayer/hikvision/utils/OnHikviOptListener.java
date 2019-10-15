package com.changsir.videoplayer.hikvision.utils;

public interface OnHikviOptListener {
    void failed(String content);
    void finish(String content);
    void process(String content);
}

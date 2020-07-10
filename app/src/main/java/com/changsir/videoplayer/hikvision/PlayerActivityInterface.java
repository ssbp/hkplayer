package com.changsir.videoplayer.hikvision;

public interface PlayerActivityInterface {
    void hideProgress();
    void updateProgress(String content);
    void playCallBack(String name);
    void updateDate(String date);
}

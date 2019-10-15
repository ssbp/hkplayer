package com.changsir.videoplayer.hikvision;

public interface PlayerActivityInterface {
    void hideProgress();
    void updateProgress(String content);
    void playCallBacy(String name);
    void updateDate(String date);
}

package com.changsir.videoplayer;

import com.changsir.videoplayer.hikvision.PlayerActivityInterface;

/**
 * 统一视频操作接口
 */
public interface VideoPresenter {

    /**
     * 设置播放器界面操作接口
     * @param playerActivityInterface
     */
    void setPlayerActivityInterface(PlayerActivityInterface playerActivityInterface);
}

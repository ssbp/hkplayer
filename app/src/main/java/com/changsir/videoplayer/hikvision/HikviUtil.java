package com.changsir.videoplayer.hikvision;

import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;

import com.changsir.videoplayer.PlayerActivity;
import com.hikvision.netsdk.ExceptionCallBack;
import com.hikvision.netsdk.HCNetSDK;
import com.hikvision.netsdk.NET_DVR_DEVICEINFO_V30;
import com.hikvision.netsdk.NET_DVR_DIGITAL_CHANNEL_STATE;
import com.hikvision.netsdk.NET_DVR_PICCFG_V30;
import com.hikvision.netsdk.NET_DVR_PREVIEWINFO;
import com.hikvision.netsdk.RealPlayCallBack;

import org.MediaPlayer.PlayM4.Player;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * 海康SDK工具类
 */
public class HikviUtil {

    private static final String TAG = "HikviUtil";

    private String address;
    private int port;
    private String username;
    private String passwd;

    private NET_DVR_DEVICEINFO_V30 m_oNetDvrDeviceInfoV30 = null;

    private int m_iLogID = -1; // return by NET_DVR_Login_v30
    private int m_iPlayID = -1; // return by NET_DVR_RealPlay_V30
    private int m_iPlaybackID = -1; // return by NET_DVR_PlayBackByTime

    private int m_iPort = -1; // play port
    private int m_iStartChan = 0; // 起始通道号
    private int m_iChanNum = 0; // 通道数量
    private boolean m_bNeedDecode = true;
    private boolean m_bStopPlayback = false;

    private SurfaceView surfaceView;

    private PlayerActivity.HikviHandler hikviHandler;

    /**
     * 初始化
     * @param address
     * @param port
     * @param username
     * @param passwd
     */
    public boolean init(String address, int port, String username, String passwd) {
        this.address = address;
        this.passwd = passwd;
        this.username = username;
        this.port = port;

        return initSdk();
    }

    public boolean init(VideoModel videoModel) {
        if(null == videoModel)
            return false;

        this.address = videoModel.getAddress();
        this.passwd = videoModel.getPasswd();
        this.username = videoModel.getUsername();
        this.port = videoModel.getPort();

        return initSdk();
    }

    public HikviUtil(SurfaceView surfaceView, PlayerActivity.HikviHandler hikviHandler) {
        this.surfaceView = surfaceView;
        this.hikviHandler = hikviHandler;
    }

    /**
     * 初始化海康
     *
     * @return
     */
    public boolean initSdk() {
        // init net sdk
        if (!HCNetSDK.getInstance().NET_DVR_Init()) {
            Log.e(TAG, "海康初始化失败");
            return false;
        }
        HCNetSDK.getInstance().NET_DVR_SetLogToFile(3, "/mnt/sdcard/sdklog/",
                true);
        return true;
    }

    /**
     * 清理
     *
     * @return
     */
    public boolean destory() {
        return HCNetSDK.getInstance().NET_DVR_Cleanup();
    }

    public void startPreview(int chanNum) {
        try {
            if (m_iLogID < 0) {
                Log.e(TAG, "请先登录");
                sendMsg(0, "视频未认证");
                return;
            }
            if (m_bNeedDecode) {
                if (m_iPlayID < 0) {
                    startSinglePreview(chanNum);
                } else {
                    stopSinglePreview();
                }
            }
        } catch (Exception err) {
            Log.e(TAG, "error: " + err.toString());
            sendMsg(0, "视频播放失败");
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

    /**
     * 单个预览
     */
    private void startSinglePreview(int chanNum) {
        if (m_iPlaybackID >= 0) {
            Log.i(TAG, "请先停止回放");
            sendMsg(0, "正在回放，请先停止");
            return;
        }

        RealPlayCallBack fRealDataCallBack = getRealPlayerCbf();
        if (fRealDataCallBack == null) {
            Log.e(TAG, "fRealDataCallBack object is failed!");
            sendMsg(0, "视频播放失败，请重试");
            return;
        }
        NET_DVR_PREVIEWINFO previewInfo = new NET_DVR_PREVIEWINFO();
        previewInfo.lChannel = chanNum;
        previewInfo.dwStreamType = 1; // substream 1子码流
        previewInfo.bBlocked = 0;     //0非阻塞 1阻塞

        m_iPlayID = HCNetSDK.getInstance().NET_DVR_RealPlay_V40(m_iLogID,
                previewInfo, fRealDataCallBack);
        if (m_iPlayID < 0) {
            Log.e(TAG, "NET_DVR_RealPlay is failed!Err:"
                    + HCNetSDK.getInstance().NET_DVR_GetLastError());
            sendMsg(0, "视频播放失败（CODE:" + HCNetSDK.getInstance().NET_DVR_GetLastError() + "），请重试");
            return;
        }

        Log.i(TAG,"单个播放成功 ***********************"+m_iPort+"***************************");
    }

    //停止多个播放
    private void stopMultiPreview() {
        int i = 0;
        for (i = 0; i < 4; i++) {
            //playView[i].stopPreview();
        }
        m_iPlayID = -1;
    }

    //停止单个预览
    public void stopSinglePreview() {
        if (m_iPlayID < 0) {
            return;
        }
        // net sdk stop preview
        if (!HCNetSDK.getInstance().NET_DVR_StopRealPlay(m_iPlayID)) {
            Log.e(TAG, "停止预览失败!Err:"
                    + HCNetSDK.getInstance().NET_DVR_GetLastError());

            sendMsg(0, "视频停止失败，请重试");
            return;
        }

        m_iPlayID = -1;
        stopSinglePlayer();
    }

    private void stopSinglePlayer() {
        Player.getInstance().stopSound();
        // player stop play
        if (!Player.getInstance().stop(m_iPort)) {
            Log.e(TAG, "停止播放失败!");
            sendMsg(0, "视频停止失败，请重试");
            return;
        }

        if (!Player.getInstance().closeStream(m_iPort)) {
            Log.e(TAG, "关闭播放流失败!");
            sendMsg(0, "视频停止失败，请重试");
            return;
        }
        if (!Player.getInstance().freePort(m_iPort)) {
            Log.e(TAG, "释放端口失败!" + m_iPort);
            sendMsg(0, "视频停止失败，请重试");
            return;
        }
        m_iPort = -1;
    }

    /**
     * 登陆设备
     *
     * @return
     */
    public int loginDevice() {
        // get instance
        m_oNetDvrDeviceInfoV30 = new NET_DVR_DEVICEINFO_V30();
        if (null == m_oNetDvrDeviceInfoV30) {
            Log.e(TAG, "创建HKNetDvrDeviceInfoV30对象失败!");
            return -1;
        }

        //调用NET_DVR_Login_v30登陆, port 8000 as default
        m_iLogID = HCNetSDK.getInstance().NET_DVR_Login_V30(address, port,
                username, passwd, m_oNetDvrDeviceInfoV30);

        if (m_iLogID < 0) {
            Log.e(TAG, "NET_DVR_Login 登陆失败!Err:"
                    + HCNetSDK.getInstance().NET_DVR_GetLastError());
            return -1;
        }
        if (m_oNetDvrDeviceInfoV30.byChanNum > 0) {
            m_iStartChan = m_oNetDvrDeviceInfoV30.byStartChan;
            m_iChanNum = m_oNetDvrDeviceInfoV30.byChanNum;
        } else if (m_oNetDvrDeviceInfoV30.byIPChanNum > 0) {
            m_iStartChan = m_oNetDvrDeviceInfoV30.byStartDChan;
            m_iChanNum = m_oNetDvrDeviceInfoV30.byIPChanNum
                    + m_oNetDvrDeviceInfoV30.byHighDChanNum * 256;
        }

        Log.i(TAG, "NET_DVR_Login 登陆成功! logId:" + m_iLogID);

        return m_iLogID;
    }

    /**
     * 登出
     *
     * @return
     */
    public boolean logoutDevice() {
        if (m_iLogID != -1) {
            return HCNetSDK.getInstance().NET_DVR_Logout_V30(m_iLogID);
        }
        return false;
    }


    /**
     * 获取通道状态
     */
    public List<Integer> getLiveChannel() {
        List<Integer> channelList = new ArrayList<>();
        NET_DVR_DIGITAL_CHANNEL_STATE struChanState = new NET_DVR_DIGITAL_CHANNEL_STATE();
        if (HCNetSDK.getInstance().NET_DVR_GetDVRConfig(m_iLogID, HCNetSDK.getInstance().NET_DVR_GET_DIGITAL_CHANNEL_STATE, 0, struChanState)) {
            byte[] status = struChanState.byDigitalChanState;
            for (int i = 0; i < status.length; i++) {
                byte s = status[i];
                if(s == 1) {
                    channelList.add(i+m_iStartChan);
                }
            }
        }
        return channelList;
    }

    /**
     * 通道是否在线
     * @param channel
     * @return
     */
    public boolean isLiving(int channel) {
        return getLiveChannel().contains(channel);
    }

    /**
     * 获取通道名称,很慢
     * @param chan
     * @return
     */
    private String getChannelName(int chan) {
        NET_DVR_PICCFG_V30 net_dvr_piccfg_v30 = new NET_DVR_PICCFG_V30();
        HCNetSDK.getInstance().NET_DVR_GetDVRConfig(m_iLogID, HCNetSDK.NET_DVR_GET_PICCFG_V30, chan, net_dvr_piccfg_v30);
        byte[] b = net_dvr_piccfg_v30.sChanName;
        try {
            int end = 0;
            for(int i = 0; i < b.length; i++) {
                if(b[i] == 0) {
                    end = i;
                    break;
                }
            }
            byte[] na = new byte[end];
            System.arraycopy(b, 0, na, 0, na.length);

            String s = new String(na,"GB2312");
            return s;
        } catch (UnsupportedEncodingException e) {
            return "Camera"+chan;
        }
    }

    /**
     * 异常
     *
     * @return
     */
    private ExceptionCallBack getExceptiongCbf() {
        ExceptionCallBack oExceptionCbf = new ExceptionCallBack() {
            public void fExceptionCallBack(int iType, int iUserID, int iHandle) {
                System.out.println("recv exception, type:" + iType);
            }
        };
        return oExceptionCbf;
    }

    /**
     * 获取视频回调
     *
     * @return
     */
    private RealPlayCallBack getRealPlayerCbf() {
        RealPlayCallBack cbf = (iRealHandle, iDataType, pDataBuffer, iDataSize) -> {
            // player channel 1
            processRealData(1, iDataType, pDataBuffer,
                    iDataSize, Player.STREAM_REALTIME);
        };
        return cbf;
    }

    /**
     * 播放进度
     *
     * @param iPlayViewNo
     * @param iDataType
     * @param pDataBuffer
     * @param iDataSize
     * @param iStreamMode
     */
    private void processRealData(int iPlayViewNo, int iDataType,
                                byte[] pDataBuffer, int iDataSize, int iStreamMode) {
        if (!m_bNeedDecode) {
            // Log.i(TAG, "iPlayViewNo:" + iPlayViewNo + ",iDataType:" +
            // iDataType + ",iDataSize:" + iDataSize);
            sendMsg(0, "视频播放失败，请重试");
        } else {
            if (HCNetSDK.NET_DVR_SYSHEAD == iDataType) {
                if (m_iPort >= 0) {
                    return;
                }
                m_iPort = Player.getInstance().getPort();
                if (m_iPort == -1) {
                    Log.e(TAG, "获取端口失败: "
                            + Player.getInstance().getLastError(m_iPort));
                    sendMsg(0, "视频播放失败，请重试");
                    return;
                }
                //取消移动侦测
                Player.getInstance().renderPrivateData(m_iPort,Player.PRIVATE_RENDER.RENDER_MD,0);

                if (iDataSize > 0) {
                    if (!Player.getInstance().setStreamOpenMode(m_iPort,iStreamMode)) {
                        Log.e(TAG, "设置流打开模式失败");
                        sendMsg(0, "视频播放失败，请重试");
                        return;
                    }
                    if (!Player.getInstance().openStream(m_iPort, pDataBuffer,
                            iDataSize, 2 * 1024 * 1024)) // open stream
                    {
                        Log.e(TAG, "openStream failed");
                        sendMsg(0, "视频播放失败，请重试");
                        return;
                    }
                    if (!Player.getInstance().play(m_iPort, surfaceView.getHolder())) {
                        Log.e(TAG, "播放失败");
                        sendMsg(0, "视频播放失败，请重试");
                        return;
                    } else {
                        sendMsg(3, "视频播放成功");
                        Log.i(TAG, "播放成功");
                    }
                    if (!Player.getInstance().playSound(m_iPort)) {
                        Log.e(TAG, "播放声音失败:"
                                + Player.getInstance().getLastError(m_iPort));
                        sendMsg(-1, "视频声音播放失败");
                        return;
                    }
                }
            } else {
                if (!Player.getInstance().inputData(m_iPort, pDataBuffer,
                        iDataSize)) {
                    // Log.e(TAG, "inputData failed with: " +
                    // Player.getInstance().getLastError(m_iPort));
                    for (int i = 0; i < 4000 && m_iPlaybackID >= 0 && !m_bStopPlayback; i++) {
                        if (Player.getInstance().inputData(m_iPort,
                                pDataBuffer, iDataSize)) {
                            break;
                        }

                        if (i % 100 == 0) {
                            Log.e(TAG, "inputData failed with: " + Player.getInstance().getLastError(m_iPort) + ", i:" + i);
                        }

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }
    }

    public int getIPort() {
        return m_iPort;
    }

}

package com.changsir.videoplayer.hikvision;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.changsir.videoplayer.hikvision.utils.OnHikviOptListener;
import com.hikvision.netsdk.ExceptionCallBack;
import com.hikvision.netsdk.HCNetSDK;
import com.hikvision.netsdk.NET_DVR_DEVICEINFO_V30;
import com.hikvision.netsdk.NET_DVR_DIGITAL_CHANNEL_STATE;
import com.hikvision.netsdk.NET_DVR_FILECOND;
import com.hikvision.netsdk.NET_DVR_FINDDATA_V30;
import com.hikvision.netsdk.NET_DVR_PICCFG_V30;
import com.hikvision.netsdk.NET_DVR_PREVIEWINFO;
import com.hikvision.netsdk.NET_DVR_PTZCFG;
import com.hikvision.netsdk.RealPlayCallBack;

import org.MediaPlayer.PlayM4.Player;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.jna.HCNetSDKByJNA.NET_DVR_FILE_NOFIND;
import static com.jna.HCNetSDKByJNA.NET_DVR_FILE_SUCCESS;
import static com.jna.HCNetSDKByJNA.NET_DVR_ISFINDING;
import static com.jna.HCNetSDKByJNA.NET_DVR_NOMOREFILE;

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
    private boolean m_bStopPlayback = false;

    private PlayerHandler playerHandler;


    /**
     * 初始化
     *
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
        if (null == videoModel)
            return false;

        this.address = videoModel.getAddress();
        this.passwd = videoModel.getPasswd();
        this.username = videoModel.getUsername();
        this.port = videoModel.getPort();

        return initSdk();
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

    /**
     * 获取回放列表
     */
    public List<NET_DVR_FINDDATA_V30> getPlayBackList(int channel, Calendar start, Calendar end) {

        List<NET_DVR_FINDDATA_V30> playBackList = new ArrayList<>();

        NET_DVR_FILECOND net_dvr_filecond = new NET_DVR_FILECOND();
        net_dvr_filecond.dwFileType = 0xFF;
        net_dvr_filecond.lChannel = channel; //通道号
        net_dvr_filecond.dwIsLocked = 0xFF;
        net_dvr_filecond.dwUseCardNo = 0;
        net_dvr_filecond.struStartTime.dwYear = start.get(Calendar.YEAR); //开始时间
        net_dvr_filecond.struStartTime.dwMonth = start.get(Calendar.MONTH) + 1;
        net_dvr_filecond.struStartTime.dwDay = start.get(Calendar.DAY_OF_MONTH) ;
        net_dvr_filecond.struStartTime.dwHour = start.get(Calendar.HOUR_OF_DAY);
        net_dvr_filecond.struStartTime.dwMinute = start.get(Calendar.MINUTE);
        net_dvr_filecond.struStartTime.dwSecond = start.get(Calendar.SECOND);
        net_dvr_filecond.struStopTime.dwYear = end.get(Calendar.YEAR); //结束时间
        net_dvr_filecond.struStopTime.dwMonth = end.get(Calendar.MONTH) + 1;
        net_dvr_filecond.struStopTime.dwDay = end.get(Calendar.DAY_OF_MONTH);
        net_dvr_filecond.struStopTime.dwHour = end.get(Calendar.HOUR_OF_DAY);
        net_dvr_filecond.struStopTime.dwMinute = end.get(Calendar.MINUTE);
        net_dvr_filecond.struStopTime.dwSecond = end.get(Calendar.SECOND);

        int lFindHandle = HCNetSDK.getInstance().NET_DVR_FindFile_V30(m_iLogID, net_dvr_filecond);

        if (lFindHandle < 0) {
            Log.e(TAG, "查找录像文件失败:" + HCNetSDK.getInstance().NET_DVR_GetLastError());
            return null;
        }

        //获取录像内容
        while (true) {
            NET_DVR_FINDDATA_V30 net_dvr_finddata_v30 = new NET_DVR_FINDDATA_V30();
            int findResult = HCNetSDK.getInstance().NET_DVR_FindNextFile_V30(lFindHandle, net_dvr_finddata_v30);
            if (findResult == NET_DVR_ISFINDING) {
                continue;
            } else if (findResult == NET_DVR_FILE_SUCCESS) {
                playBackList.add(0, net_dvr_finddata_v30);
            } else if (findResult == NET_DVR_FILE_NOFIND || findResult == NET_DVR_NOMOREFILE) {
                Log.e(TAG, "查找录像文件结束:" + HCNetSDK.getInstance().NET_DVR_GetLastError());
                break;
            } else {
                Log.e(TAG, "查找录像文件详细失败:" + HCNetSDK.getInstance().NET_DVR_GetLastError());
                break;
            }
        }

        //停止查找
        if (lFindHandle >= 0) {
            HCNetSDK.getInstance().NET_DVR_FindClose_V30(lFindHandle);
        }

        //按文件播放，需要先查找
//        HCNetSDK.getInstance().NET_DVR_PlayBackByName()
        return playBackList;
    }

    /**
     * 根据文件名回放数据
     * @param name
     * @param surfaceView
     */
    public void startPlayBackByName(String name, SurfaceView surfaceView) {
        if (null == surfaceView) {
            return;
        }

        stopSinglePreview();
        stopPlayback();

        m_iPlaybackID = HCNetSDK.getInstance().NET_DVR_PlayBackByName(m_iLogID, name, surfaceView.getHolder().getSurface());
        if (m_iPlaybackID < 0) {
            return;
        }

        HCNetSDK.getInstance().NET_DVR_PlayBackControl_V40(m_iPlaybackID, HCNetSDK.NET_DVR_PLAYSTART, null, 0, null);

        //海康JNA调用去除移动侦测
//        m_iPort = HCNetSDKJNAInstance.getInstance().NET_DVR_GetPlayBackPlayerIndex(m_iLogID);
//        HCNetSDKJNAInstance.getInstance().NET_DVR_GetPlayBackPlayerIndex(m_iPlaybackID_1);
//        Player.getInstance().renderPrivateData(m_iPort, Player.PRIVATE_RENDER.RENDER_MD, 0);
    }

    /**
     * 获取云台协议
     * @return
     */
    public NET_DVR_PTZCFG fetchPTZControl() {
        NET_DVR_PTZCFG net_dvr_ptzcfg = new NET_DVR_PTZCFG();
        boolean result = HCNetSDK.getInstance().NET_DVR_GetPTZProtocol(m_iLogID, net_dvr_ptzcfg);
        if(result) {
            return net_dvr_ptzcfg;
        }
        return null;
    }

    /**
     * FIXME 建议修改为按下开始，松开结束
     * @param opt
     */
    public boolean ptzControl(int opt) {
//        TILT_UP 21 云台上仰
//        TILT_DOWN 22 云台下俯
//        PAN_LEFT 23 云台左转
//                PAN_RIGHT
        //开始
        boolean r = HCNetSDK.getInstance().NET_DVR_PTZControl(m_iPlayID, opt, 0);
        //结束
        HCNetSDK.getInstance().NET_DVR_PTZControl(m_iPlayID, opt, 1);
        return r;
    }

    /**
     * 调用预览
     * @param chanNum
     * @param surfaceView
     * @param onHikviOptListener
     */
    public void startPreview(int chanNum, SurfaceView surfaceView, OnHikviOptListener onHikviOptListener) {

        if (null == surfaceView) {
            return;
        }

        try {
            if (m_iLogID < 0) {
                Log.e(TAG, "请先登录");
                return;
            }
            if (m_iPlayID < 0) {

                playerHandler = new PlayerHandler(onHikviOptListener);
                startSinglePreview(chanNum, surfaceView);
            } else {
                stopSinglePreview();
            }
        } catch (Exception err) {
            Log.e(TAG, "error: " + err.toString());
        }
    }

    /**
     * 单个预览
     */
    private void startSinglePreview(int chanNum, SurfaceView surfaceView) {
        if (m_iPlaybackID >= 0) {
            Log.i(TAG, "请先停止回放");
            return;
        }

        RealPlayCallBack fRealDataCallBack = getRealPlayerCbf(surfaceView);
        if (fRealDataCallBack == null) {
            Log.e(TAG, "fRealDataCallBack object is failed!");
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
            return;
        }
    }

    /**
     * 停止回放
     */
    public void stopPlayback() {
        if (m_iPlaybackID < 0) {
            return;
        }
        if (!HCNetSDK.getInstance().NET_DVR_StopPlayBack(m_iPlaybackID)) {
            Log.e(TAG, "停止回放失败!Err:"
                    + HCNetSDK.getInstance().NET_DVR_GetLastError());
            return;
        }
        m_iPlaybackID = -1;
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
            return;
        }

        if (!Player.getInstance().closeStream(m_iPort)) {
            Log.e(TAG, "关闭播放流失败!");
            return;
        }
        if (!Player.getInstance().freePort(m_iPort)) {
            Log.e(TAG, "释放端口失败!" + m_iPort);
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
                if (s == 1) {
                    channelList.add(i + m_iStartChan);
                }
            }
        }
        return channelList;
    }

    /**
     * 通道是否在线
     *
     * @param channel
     * @return
     */
    public boolean isLiving(int channel) {
        return getLiveChannel().contains(channel);
    }

    /**
     * 获取通道名称,很慢
     *
     * @param chan
     * @return
     */
    private String getChannelName(int chan) {
        NET_DVR_PICCFG_V30 net_dvr_piccfg_v30 = new NET_DVR_PICCFG_V30();
        HCNetSDK.getInstance().NET_DVR_GetDVRConfig(m_iLogID, HCNetSDK.NET_DVR_GET_PICCFG_V30, chan, net_dvr_piccfg_v30);
        byte[] b = net_dvr_piccfg_v30.sChanName;
        return byte2Str(b);
    }

    /**
     * byte转str
     *
     * @param b
     * @return
     */
    public static String byte2Str(byte[] b) {
        try {
            int end = 0;
            for (int i = 0; i < b.length; i++) {
                if (b[i] == 0) {
                    end = i;
                    break;
                }
            }
            byte[] na = new byte[end];
            System.arraycopy(b, 0, na, 0, na.length);

            String s = new String(na, "GB2312");
            return s;
        } catch (UnsupportedEncodingException e) {
            return null;
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
    private RealPlayCallBack getRealPlayerCbf(SurfaceView surfaceView) {
        RealPlayCallBack cbf = (iRealHandle, iDataType, pDataBuffer, iDataSize) -> {
            // player channel 1
            processRealData(1, iDataType, pDataBuffer,
                    iDataSize, Player.STREAM_REALTIME, surfaceView);
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
                                 byte[] pDataBuffer, int iDataSize, int iStreamMode, SurfaceView surfaceView) {
        if (HCNetSDK.NET_DVR_SYSHEAD == iDataType) {
            if (m_iPort >= 0) {
                return;
            }
            m_iPort = Player.getInstance().getPort();
            if (m_iPort == -1) {
                sendMsg(1, "获取端口失败: "
                        + Player.getInstance().getLastError(m_iPort));
                return;
            }
            //取消移动侦测
            Player.getInstance().renderPrivateData(m_iPort, Player.PRIVATE_RENDER.RENDER_MD, 0);

            if (iDataSize > 0) {
                if (!Player.getInstance().setStreamOpenMode(m_iPort, iStreamMode)) {
                    sendMsg(1, "设置流打开模式失败");
                    return;
                }
                if (!Player.getInstance().openStream(m_iPort, pDataBuffer,
                        iDataSize, 2 * 1024 * 1024)) // open stream
                {
                    sendMsg(1, "流打开模式失败");
                    return;
                }
                if (!Player.getInstance().play(m_iPort, surfaceView.getHolder())) {
                    sendMsg(1, "播放失败");
                    return;
                } else {
                    sendMsg(0, "播放成功");
                }
                if (!Player.getInstance().playSound(m_iPort)) {
                    Log.e(TAG, "播放声音失败:"
                            + Player.getInstance().getLastError(m_iPort));
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

    public int getIPort() {
        return m_iPort;
    }

    private void sendMsg(int code, String content) {
        Message msg = playerHandler.obtainMessage();
        msg.what = code;
        msg.obj = content;
        msg.sendToTarget();
    }

    /**
     * 播放的handler
     */
    class PlayerHandler extends Handler {

        OnHikviOptListener onHikviOptListener;

        public PlayerHandler(OnHikviOptListener onHikviOptListener) {
            this.onHikviOptListener = onHikviOptListener;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if(null != onHikviOptListener) {
                switch (msg.what) {
                    case 0:
                        onHikviOptListener.finish("播放成功");
                        break;
                    case 1:
                        onHikviOptListener.failed(msg.obj.toString());
                        break;
                }
            }
        }
    }

}

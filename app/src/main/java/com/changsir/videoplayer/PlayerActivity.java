package com.changsir.videoplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;

import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.changsir.videoplayer.hikvision.HikviUtil;
import com.changsir.videoplayer.hikvision.VideoModel;

import org.MediaPlayer.PlayM4.Player;

import java.lang.ref.WeakReference;

/**
 * 播放界面
 */
public class PlayerActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = "PlayerActivity";
    public static final String PLAYER_DATA = "PLAYER_DATA";

    private SurfaceView mSurfaceView;
    private HikviUtil hikviUtil;
    private VideoModel videoModel;
    private HikviHandler hikviHandler;
    private ProgressBar progressBar;
    private TextView progressText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        progressBar = findViewById(R.id.hkPlayerProgressBar);
        progressText = findViewById(R.id.hkPlayerProgressText);

        //保持屏幕常量
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceView.getHolder().addCallback(this);
        initData();
    }

    private void initData() {
        videoModel = getIntent().getParcelableExtra(PLAYER_DATA);
        if(null == videoModel) {
            Toast.makeText(this, "播放数据获取失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        hikviHandler = new HikviHandler(this);
        hikviUtil = new HikviUtil(mSurfaceView, hikviHandler);
    }

    private void loadHK() {
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);
        progressText.setText("正在加载");
        new Thread(new LoginThread()).start();
    }

    /**
     * 登陆线程
     */
    class LoginThread implements Runnable {
        @Override
        public void run() {

            sendMsg(1, "正在初始化视频");

            //1.初始化
            boolean initR = hikviUtil.init(videoModel);
            if(initR) {
                sendMsg(1, "正在认证视频信息");
                //2.登陆
                if(hikviUtil.loginDevice()!=-1) {
                    sendMsg(1, "正在检查通道状态");
                    //3.检查通道
                    if(hikviUtil.isLiving(videoModel.getChannel())) {
                        //4.准备播放
                        sendMsg(2, "正在加载视频");
                    } else {
                        sendMsg(0, "当前视频已掉线");
                    }
                } else {
                    sendMsg(0, "认证视频失败");
                }
            } else {
                sendMsg(0, "视频初始化失败");
            }
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
     * 播放线程Handler
     */
    public class HikviHandler extends Handler {
        private final WeakReference<PlayerActivity> mActivity;

        public HikviHandler(PlayerActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            PlayerActivity activity = mActivity.get();
            if (activity != null) {
                int what = msg.what;
                String content = msg.obj.toString();

                switch(what) {
                    case 0:
                        //失败
                        Toast.makeText(activity,content, Toast.LENGTH_SHORT).show();
                        finish();
                        break;
                    case 1:
                        //成功
                        progressText.setText(content);
                        break;
                    case 2:
                        //5.调用预览
                        progressText.setText(content);
                        hikviUtil.startPreview(videoModel.getChannel());
                        break;
                    case 3:
                        //播放成功
                        progressBar.setVisibility(View.GONE);
                        progressText.setVisibility(View.GONE);
                        break;
                    case -1:
                        //错误，只弹出内容
                        Toast.makeText(activity,content, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(TAG, "创建视频窗口");
        int m_iPort = hikviUtil.getIPort();
        mSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        if (-1 == m_iPort) {
            return;
        }
        Surface surface = holder.getSurface();
        if (true == surface.isValid()) {
            if (false == Player.getInstance()
                    .setVideoWindow(m_iPort, 0, holder)) {
                Log.e(TAG, "Player setVideoWindow failed!");
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
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

        Log.e(TAG, "停止预览");
        hikviUtil.stopSinglePreview();
    }

    @Override
    protected void onDestroy() {
        new Thread(new DestoryHkhandler()).start();
        Log.e(TAG, "销毁资源");
        super.onDestroy();
    }

    class DestoryHkhandler implements Runnable {
        @Override
        public void run() {
            Log.e(TAG, "开始销毁资源");
            hikviHandler.removeCallbacksAndMessages(null);
            hikviUtil.logoutDevice();
            hikviUtil.destory();
        }
    }

    @Override
    protected void onPause() {
        //暂停播放
        hikviUtil.stopSinglePreview();
        super.onPause();
    }

    @Override
    protected void onResume() {
        //播放
        loadHK();
        super.onResume();
    }
}

package com.changsir.videoplayer.rtsp;

import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.aaronhan.rtspclient.RtspClient;
import com.changsir.videoplayer.R;

public class RTSPActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView;
    private RtspClient rtspClient;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtsp);

        mSurfaceView = findViewById(R.id.surfaceView);
        button = findViewById(R.id.button);
        button.setOnClickListener(view -> {
            if(null != rtspClient) {
                rtspClient.start();
            }
        });

        initVideo();
    }

    private void initVideo() {
        //使用RTP传输协议选择，支持"tcp"和"udp"传入值
        String method = "tcp";

//        rtsp://admin:dybh12345@free.idcfengye.com:10433/Streaming/Channels/102
        String host = "rtsp://free.idcfengye.com:10433/Streaming/Channels/102";

        //支持传入用户名密码，某些RTSP服务器需要认证使用
        String username = "admin";
        String password = "dyhb12345";

        //默认是udp协议，传入认证用户名和密码
        rtspClient = new RtspClient(method, host, username, password);
        rtspClient.setSurfaceView(mSurfaceView);

        //只传入地址或地址加端口
        //默认无用户名密码认证，默认使用udp协议
//        RtspClient(host);
//        RtspClient(host, port);
//
//        //传入使用协议
//        RtspClient(method, host);
//        RtspClient(method, host, port);
//
//        //传入使用协议和认证信息
//        RtspClient(method, host, username, password);
//        RtspClient(method, host, username, password, port);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(null != rtspClient) {
            rtspClient.shutdown();
        }
    }
}
